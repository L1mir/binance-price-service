package org.limir.binancepriceservice.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.limir.binancepriceservice.dto.AveragePriceResponse;
import org.limir.binancepriceservice.dto.PageResponse;
import org.limir.binancepriceservice.dto.PriceResponse;
import org.limir.binancepriceservice.dto.TimeRange;
import org.limir.binancepriceservice.dto.TrendResponse;
import org.limir.binancepriceservice.service.PriceQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * {@code from} and {@code to} are ISO-8601 instants, e.g. {@code 2026-09-01T00:00:00Z}.
 */
@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    /**
     * Upper bound for {@code size}, so that a single request cannot load the whole table.
     */
    private static final int MAX_PAGE_SIZE = 100;

    private final PriceQueryService priceQueryService;

    public PriceController(PriceQueryService priceQueryService) {
        this.priceQueryService = priceQueryService;
    }

    @GetMapping("/latest")
    public PriceResponse getLatestPrice() {
        return priceQueryService.getLatestPrice();
    }

    @GetMapping
    public PageResponse<PriceResponse> getPrices(@RequestParam Instant from,
                                                 @RequestParam Instant to,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return priceQueryService.getPrices(new TimeRange(from, to), page, size);
    }

    @GetMapping("/average")
    public AveragePriceResponse getAveragePrice(@RequestParam Instant from, @RequestParam Instant to) {
        return priceQueryService.getAveragePrice(new TimeRange(from, to));
    }

    @GetMapping("/trend")
    public TrendResponse getTrend(@RequestParam Instant from, @RequestParam Instant to) {
        return priceQueryService.getTrend(new TimeRange(from, to));
    }
}
