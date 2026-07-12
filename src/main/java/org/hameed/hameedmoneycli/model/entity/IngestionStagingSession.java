package org.hameed.hameedmoneycli.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hameed.hameedmoneycli.enums.StagingSessionStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ingestion_staging_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionStagingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_code", nullable = false, length = 20)
    private String sourceCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "anchored_account_id", nullable = false)
    private Account anchoredAccount;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private Integer totalRows = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StagingSessionStatus status = StagingSessionStatus.PARSED;

    @Column(name = "stats_json", columnDefinition = "TEXT")
    private String statsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Long createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Long updatedAt;
}
