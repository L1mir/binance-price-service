package org.limir.binancepriceservice.service;

import org.limir.binancepriceservice.config.BinanceProperties;
import org.limir.binancepriceservice.dto.PageResponse;
import org.limir.binancepriceservice.dto.PriceResponse;
import org.limir.binancepriceservice.dto.TimeRange;
import org.limir.binancepriceservice.exception.PriceNotFoundException;
import org.limir.binancepriceservice.repository.PriceSnapshotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Read side of the stored prices. All queries are scoped to the symbol the service polls.
 */
@Service
public class PriceQueryService {

    /**
     * Chronological order; {@code id} makes the order stable if two rows ever share the same {@code fetchedAt}.
     */
    private static final Sort CHRONOLOGICAL = Sort.by("fetchedAt", "id");

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final BinanceProperties binanceProperties;

    public PriceQueryService(PriceSnapshotRepository priceSnapshotRepository, BinanceProperties binanceProperties) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.binanceProperties = binanceProperties;
    }

    public PriceResponse getLatestPrice() {
        String symbol = binanceProperties.symbol();
        return priceSnapshotRepository.findFirstBySymbolOrderByFetchedAtDesc(symbol)
                .map(PriceResponse::from)
                .orElseThrow(() -> new PriceNotFoundException("No prices stored for " + symbol + " yet"));
    }

    public PageResponse<PriceResponse> getPrices(TimeRange range, int page, int size) {
        Page<PriceResponse> prices = priceSnapshotRepository
                .findBySymbolAndFetchedAtBetween(binanceProperties.symbol(), range.from(), range.to(),
                        PageRequest.of(page, size, CHRONOLOGICAL))
                .map(PriceResponse::from);
        return PageResponse.from(prices);
    }
}
