package org.hameed.hameedmoneycli.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hameed.hameedmoneycli.enums.AccountType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "master_type", nullable = false)
    private AccountType masterType;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Account parent;

    /**
     * Null for organizational (parent) nodes; non-null only for leaf accounts where balances and
     * transactions are denominated in this asset.
     */
    @ManyToOne
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "is_internal", nullable = false)
    private Boolean isInternal = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at", nullable = true, insertable = false)
    @LastModifiedDate
    private Instant updatedAt;

}
