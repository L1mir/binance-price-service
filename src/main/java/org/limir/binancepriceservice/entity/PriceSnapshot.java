package org.limir.binancepriceservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Average price of a trading pair as received from Binance at a specific moment.
 * Rows are never changed after insert, hence {@link Immutable}.
 */
@Entity
@Immutable
@Table(name = "price_snapshot")
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "close_time")
    private Instant closeTime;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    protected PriceSnapshot() {
    }

    public PriceSnapshot(String symbol, BigDecimal price, Instant closeTime, Instant fetchedAt) {
        this.symbol = symbol;
        this.price = price;
        this.closeTime = closeTime;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Instant getCloseTime() {
        return closeTime;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
