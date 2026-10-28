package org.limir.binancepriceservice.repository;

import org.limir.binancepriceservice.entity.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {
}
