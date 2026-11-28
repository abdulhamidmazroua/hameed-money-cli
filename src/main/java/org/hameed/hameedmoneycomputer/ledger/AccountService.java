package org.hameed.hameedmoneycomputer.ledger;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycomputer.market.Asset;
import org.hameed.hameedmoneycomputer.market.AssetStore;
import org.hameed.hameedmoneycomputer.shared.BusinessRuleException;
import org.hameed.hameedmoneycomputer.shared.NotFoundException;
import org.hameed.hameedmoneycomputer.shared.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Accounts and their balances: the account tree, opening balances and reconciliation adjustments.
 * Every leaf asset gets three SYSTEM accounts ({@code <SYMBOL>:Opening Balance}, {@code ...:Balance Increase
 * Adjustment}, {@code ...:Balance Decrease Adjustment}) that act as the counterpart of those adjustments.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    static final String OPENING_BALANCE = "Opening Balance";
    static final String BALANCE_INCREASE = "Balance Increase Adjustment";
    static final String BALANCE_DECREASE = "Balance Decrease Adjustment";
    static final String MANUAL_SOURCE = "MANUAL_ENTRY";

    private final AccountStore accounts;
    private final TransactionStore transactions;
    private final SourceSystemStore sources;
    private final AssetStore assets;
    private final List<AccountReferences> references;

    /** Creates an account. Leaf names are prefixed with their asset symbol, e.g. {@code USD:Wallet}. */
    @Transactional
    public AccountView createAccount(AccountCreateDto newAccount) {
        return AccountView.from(create(newAccount));
    }

    /** Creates an ASSET leaf account and posts its opening balance. */
    @Transactional
    public AccountView createAccountWithOpeningBalance(String name, Long parentAccountId, String assetSymbol,
                                                       BigDecimal balance) {
        Asset asset = assets.findBySymbol(assetSymbol)
                .orElseThrow(() -> new NotFoundException("Asset with symbol " + assetSymbol + " not found"));
        Account account = create(new AccountCreateDto(name, AccountType.ASSET, parentAccountId, asset.getId(), true));
        postOpeningBalance(account, balance);
        return AccountView.from(account);
    }

    public List<AccountView> getAllAccounts() {
        return accounts.findAll().stream().map(AccountView::from).toList();
    }

    public AccountView getAccount(long id) {
        return AccountView.from(require(id));
    }

    public AccountView getAccountByName(String name) {
        return accounts.findByName(name)
                .map(AccountView::from)
                .orElseThrow(() -> new NotFoundException("Account with name '" + name + "' not found"));
    }

    public List<AccountView> findAccounts(AccountFilter filter) {
        return accounts.find(filter).stream().map(AccountView::from).toList();
    }

    /** Current balance in the account's own asset (zero for folders). */
    public BigDecimal balance(long accountId) {
        require(accountId);
        return transactions.balanceOf(accountId);
    }

    @Transactional
    public void deleteAccount(long id) {
        Account account = require(id);
        String name = account.getName();

        if (accounts.hasChildren(id)) {
            throw new BusinessRuleException("Cannot delete account '" + name + "': it has child accounts. Delete or reassign children first.");
        }
        if (transactions.involvesAccount(id)) {
            throw new BusinessRuleException("Cannot delete account '" + name + "': it has related transactions. Delete transactions first.");
        }
        if (sources.isAnchoredTo(id)) {
            throw new BusinessRuleException("Cannot delete account '" + name + "': it is anchored to a source system. Delete the source system first.");
        }
        for (AccountReferences reference : references) {
            Optional<String> reason = reference.blockingReason(account);
            if (reason.isPresent()) {
                throw new BusinessRuleException("Cannot delete account '" + name + "': " + reason.get());
            }
        }
        accounts.delete(account);
    }

    /** Posts an opening balance against the asset's {@code Opening Balance} system account. */
    @Transactional
    public void openBalance(long accountId, BigDecimal balance) {
        postOpeningBalance(requireLeaf(accountId, "Cannot post an opening balance."), balance);
    }

    /**
     * Brings the balance to {@code actualBalance} with one adjustment against the asset's increase/decrease system
     * account. Does nothing when the balance already matches.
     */
    @Transactional
    public void reconcile(long accountId, BigDecimal actualBalance) {
        Account leaf = requireLeaf(accountId, "Cannot reconcile.");
        Asset asset = leaf.getAsset();

        BigDecimal difference = actualBalance.subtract(transactions.balanceOf(leaf.getId()));
        if (difference.signum() == 0) {
            return;
        }

        boolean increase = difference.signum() > 0;
        Account adjustment = accounts.findByName(asset.getSymbol() + ":" + (increase ? BALANCE_INCREASE : BALANCE_DECREASE))
                .orElseThrow(() -> new BusinessRuleException("No " + (increase ? "increase" : "decrease")
                        + " adjustment SYSTEM account found for asset " + asset.getSymbol()));

        // Money flows into the leaf when the balance should rise (ASSET/EXPENSE) — the opposite for the other types.
        boolean intoLeaf = increase == increasesOnInflow(leaf);
        String description = "Balance " + (increase ? "increase" : "decrease") + " adjustment for " + leaf.getName()
                + " (actual: " + actualBalance + ")";
        transactions.save(systemTransaction(description,
                intoLeaf ? adjustment : leaf,
                intoLeaf ? leaf : adjustment,
                difference.abs()));
    }

    private Account create(AccountCreateDto newAccount) {
        if (newAccount.name() == null || newAccount.name().isBlank()) {
            throw new ValidationException("Account name must not be empty");
        }
        if (newAccount.accountType() == null) {
            throw new ValidationException("Account type is required");
        }
        Account parent = null;
        if (newAccount.parentAccountId() != null) {
            parent = accounts.findById(newAccount.parentAccountId())
                    .orElseThrow(() -> new NotFoundException("Parent account with ID " + newAccount.parentAccountId() + " not found"));
        }
        Asset asset = null;
        if (newAccount.assetId() != null) {
            asset = assets.findById(newAccount.assetId())
                    .orElseThrow(() -> new NotFoundException("Asset with ID " + newAccount.assetId() + " not found"));
        }

        String name = newAccount.name().trim();
        if (asset != null && !name.startsWith(asset.getSymbol() + ":")) {
            name = asset.getSymbol() + ":" + name;
        }
        if (accounts.findByName(name).isPresent()) {
            throw new BusinessRuleException("An account named '" + name + "' already exists");
        }

        Account account = accounts.save(Account.builder()
                .name(name)
                .masterType(newAccount.accountType())
                .parent(parent)
                .asset(asset)
                .isInternal(Boolean.TRUE.equals(newAccount.isInternal()))
                .build());

        if (asset != null) {
            createSystemAccounts(asset);
        }
        return account;
    }

    private void createSystemAccounts(Asset asset) {
        if (accounts.existsByAssetAndType(asset.getId(), AccountType.SYSTEM)) {
            return;
        }
        for (String name : List.of(OPENING_BALANCE, BALANCE_INCREASE, BALANCE_DECREASE)) {
            accounts.save(Account.builder()
                    .name(asset.getSymbol() + ":" + name)
                    .masterType(AccountType.SYSTEM)
                    .asset(asset)
                    .isInternal(false)
                    .build());
        }
    }

    private void postOpeningBalance(Account leaf, BigDecimal balance) {
        Asset asset = leaf.getAsset();
        Account opening = accounts.findByName(asset.getSymbol() + ":" + OPENING_BALANCE)
                .orElseThrow(() -> new BusinessRuleException(
                        "No opening balance SYSTEM account found for asset " + asset.getSymbol() + ". " +
                        "Create a leaf account with this asset first so the SYSTEM trio is generated."));

        boolean intoLeaf = increasesOnInflow(leaf);
        transactions.save(systemTransaction("Opening balance for " + leaf.getName(),
                intoLeaf ? opening : leaf,
                intoLeaf ? leaf : opening,
                balance));
    }

    private Transaction systemTransaction(String description, Account from, Account to, BigDecimal amount) {
        SourceSystem manual = sources.findByCode(MANUAL_SOURCE)
                .orElseThrow(() -> new BusinessRuleException(MANUAL_SOURCE + " source system not found in seed data."));
        return Transaction.builder()
                .description(description)
                .type(TransactionType.SYSTEM_ADJUSTMENT)
                .fromAccount(from)
                .toAccount(to)
                .fromAmount(amount)
                .toAmount(amount)
                .transactionDate(System.currentTimeMillis())
                .sourceSystem(manual)
                .feeAmount(BigDecimal.ZERO)
                .isSystemAdjustment(true)
                .build();
    }

    /** ASSET and EXPENSE balances grow with money flowing in; the other types grow with money flowing out. */
    private static boolean increasesOnInflow(Account account) {
        return account.getMasterType() == AccountType.ASSET || account.getMasterType() == AccountType.EXPENSE;
    }

    private Account require(long id) {
        return accounts.findById(id)
                .orElseThrow(() -> new NotFoundException("Account with ID " + id + " not found"));
    }

    private Account requireLeaf(long id, String action) {
        Account account = require(id);
        if (account.getAsset() == null) {
            throw new BusinessRuleException("Account '" + account.getName() + "' is a folder (no asset). " + action);
        }
        return account;
    }
}
