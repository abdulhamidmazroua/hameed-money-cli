package org.hameed.hameedmoneycomputer.market;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "market_quotes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"base_asset_id", "quote_asset_id", "quote_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "base_asset_id", nullable = false)
    private Asset baseAsset;

    @ManyToOne
    @JoinColumn(name = "quote_asset_id", nullable = false)
    private Asset quoteAsset;

    @Column(name = "quote_date", nullable = false)
    /** When the price applies (epoch millis); set explicitly, never by auditing. */
    private Long quoteDate;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;
}
