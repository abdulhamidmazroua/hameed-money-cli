package org.hameed.hameedmoneycli.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "source_systems")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceSystem {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "anchored_account_id", nullable = false)
    private Account anchoredAccount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

}
