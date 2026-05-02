package org.hameed.hameedmoneycli.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hameed.hameedmoneycli.enums.AssetCategory;

@Entity
@Table(name = "asset")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AssetCategory category; // 'STOCK', 'CASH', 'CRYPTO', 'COMMODITY'

    @Column(name = "is_tradable", nullable = false)
    private Boolean isTradable = true;
}