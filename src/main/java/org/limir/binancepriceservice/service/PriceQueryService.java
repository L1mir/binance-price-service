package org.limir.binancepriceservice.service;

import org.limir.binancepriceservice.config.BinanceProperties;
import org.limir.binancepriceservice.config.TrendProperties;
import org.limir.binancepriceservice.dto.AveragePriceResponse;
import org.limir.binancepriceservice.dto.PageResponse;
import org.limir.binancepriceservice.dto.PriceResponse;
import org.limir.binancepriceservice.dto.TimeRange;
import org.limir.binancepriceservice.dto.TrendResponse;
import org.limir.binancepriceservice.entity.PriceSnapshot;
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
    private final TrendProperties trendProperties;

    public PriceQueryService(PriceSnapshotRepository priceSnapshotRepository,
                             BinanceProperties binanceProperties,
                             TrendProperties trendProperties) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.binanceProperties = binanceProperties;
        this.trendProperties = trendProperties;
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

    public AveragePriceResponse getAveragePrice(TimeRange range) {
        String symbol = binanceProperties.symbol();
        return priceSnapshotRepository.findAveragePrice(symbol, range.from(), range.to())
                .map(AveragePriceResponse::new)
                .orElseThrow(() -> noPricesInRange(symbol, range));
    }

    /**
     * Compares the first and the last stored price of the interval, see {@link TrendCalculator}.
     */
    public TrendResponse getTrend(TimeRange range) {
        String symbol = binanceProperties.symbol();
        PriceSnapshot first = priceSnapshotRepository
                .findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(symbol, range.from(), range.to())
                .orElseThrow(() -> noPricesInRange(symbol, range));
        PriceSnapshot last = priceSnapshotRepository
                .findFirstBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(symbol, range.from(), range.to())
                .orElseThrow(() -> noPricesInRange(symbol, range));

        Trend trend = TrendCalculator.calculate(first.getPrice(), last.getPrice(),
                trendProperties.flatThresholdPercent());
        return new TrendResponse(symbol, range.from(), range.to(), trend);
    }

    private static PriceNotFoundException noPricesInRange(String symbol, TimeRange range) {
        return new PriceNotFoundException(
                "No prices stored for " + symbol + " between " + range.from() + " and " + range.to());
    }
}
