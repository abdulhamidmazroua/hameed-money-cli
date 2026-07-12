package org.hameed.hameedmoneycli.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hameed.hameedmoneycli.constants.IngestedTransactionStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ingested_staged_transactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "row_index"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestedStagedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private IngestionStagingSession session;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private IngestedTransactionStatus status = IngestedTransactionStatus.PENDING;

    @Column(name = "raw_date", nullable = false, length = 100)
    private String rawDate;

    @Column(name = "raw_description", nullable = false, length = 500)
    private String rawDescription;

    @Column(name = "raw_amount", nullable = false, length = 100)
    private String rawAmount;

    @Column(name = "parsed_date")
    private Long parsedDate;

    @Column(name = "parsed_amount", precision = 19, scale = 4)
    private BigDecimal parsedAmount;

    @Column(name = "parse_error", length = 500)
    private String parseError;

    @Column(name = "classifier", length = 20)
    private String classifier;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "matched_rule_id")
    private IngestionRule matchedRule;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "suggested_account_id")
    private Account suggestedAccount;

    @Column(name = "suggested_tx_type", length = 30)
    private String suggestedTxType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "override_account_id")
    private Account overrideAccount;

    @Column(name = "override_description", length = 500)
    private String overrideDescription;

    @Column(name = "override_amount", precision = 19, scale = 4)
    private BigDecimal overrideAmount;

    @Column(name = "user_notes", length = 500)
    private String userNotes;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "applied_tx_id")
    private Transaction appliedTransaction;

    @Column(name = "llm_reasoning", length = 1000)
    private String llmReasoning;

    @Column(columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Long createdAt;

    public boolean hasError() {
        return parseError != null && !parseError.isBlank();
    }

    public boolean isClassified() {
        return suggestedAccount != null || overrideAccount != null;
    }

    public Account effectiveAccount() {
        return overrideAccount != null ? overrideAccount : suggestedAccount;
    }

    public String effectiveDescription() {
        return overrideDescription != null ? overrideDescription : rawDescription;
    }

    public BigDecimal effectiveAmount() {
        return overrideAmount != null ? overrideAmount : parsedAmount;
    }
}
