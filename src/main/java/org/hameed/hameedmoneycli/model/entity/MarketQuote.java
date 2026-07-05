package org.hameed.hameedmoneycli.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
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
    @CreatedDate
    private Instant quoteDate;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;
}
