package org.limir.binancepriceservice.repository;

import org.limir.binancepriceservice.entity.PriceSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findFirstBySymbolOrderByFetchedAtDesc(String symbol);

    /**
     * Both bounds are inclusive.
     */
    Page<PriceSnapshot> findBySymbolAndFetchedAtBetween(String symbol, Instant from, Instant to, Pageable pageable);

    /**
     * Inserts a snapshot unless one with the same symbol and close time already exists.
     *
     * @return 1 if the row was inserted, 0 if it was a duplicate
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO price_snapshot (symbol, price, close_time, fetched_at)
            VALUES (:symbol, :price, :closeTime, :fetchedAt)
            ON CONFLICT (symbol, close_time) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("symbol") String symbol,
                       @Param("price") BigDecimal price,
                       @Param("closeTime") Instant closeTime,
                       @Param("fetchedAt") Instant fetchedAt);
}
