package org.limir.binancepriceservice.service;

import org.limir.binancepriceservice.client.BinanceAvgPriceResponse;
import org.limir.binancepriceservice.client.BinanceClient;
import org.limir.binancepriceservice.config.BinanceProperties;
import org.limir.binancepriceservice.repository.PriceSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class PriceIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PriceIngestionService.class);

    private final BinanceClient binanceClient;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final BinanceProperties binanceProperties;
    private final Clock clock;

    public PriceIngestionService(BinanceClient binanceClient,
                                 PriceSnapshotRepository priceSnapshotRepository,
                                 BinanceProperties binanceProperties,
                                 Clock clock) {
        this.binanceClient = binanceClient;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.binanceProperties = binanceProperties;
        this.clock = clock;
    }

    /**
     * Fetches the current average price of the configured symbol and stores it.
     * The Binance call is made outside of a DB transaction, so a slow response does not hold a connection.
     * A response with an already stored close time is skipped, so repeated polls never create duplicates.
     */
    public void fetchAndSave() {
        String symbol = binanceProperties.symbol();
        BinanceAvgPriceResponse response = binanceClient.getAveragePrice(symbol);
        Instant fetchedAt = clock.instant();
        Instant closeTime = Instant.ofEpochMilli(response.closeTime());

        int inserted = priceSnapshotRepository.insertIfAbsent(symbol, response.price(), closeTime, fetchedAt);
        if (inserted == 1) {
            log.info("Saved {} average price {} (closeTime {})", symbol, response.price(), closeTime);
        } else {
            log.info("Skipped {} average price: closeTime {} is already stored", symbol, closeTime);
        }
    }
}
