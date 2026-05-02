package org.hameed.hameedmoneycli.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "market_quote", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"base_asset_id", "quote_asset_id", "quote_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_asset_id", nullable = false)
    private Asset baseAsset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_asset_id", nullable = false)
    private Asset quoteAsset;

    @Column(name = "quote_date", nullable = false)
    @CreatedDate
    private OffsetDateTime quoteDate;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;
}
