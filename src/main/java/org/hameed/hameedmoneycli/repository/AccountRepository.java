package org.hameed.hameedmoneycli.repository;

import org.hameed.hameedmoneycli.enums.AccountType;
import org.hameed.hameedmoneycli.model.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByNameIgnoreCase(String name);

    @Query("select a from Account a where a.asset.id is not null")
    List<Account> getLeafAccounts();

    boolean existsByParent_Id(Long parentId);

    boolean existsByAsset_IdAndMasterType(Long assetId, AccountType masterType);

    List<Account> findByMasterType(AccountType masterType);
}
