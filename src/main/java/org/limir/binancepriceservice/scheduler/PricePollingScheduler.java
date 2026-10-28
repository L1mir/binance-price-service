package org.limir.binancepriceservice.scheduler;

import org.limir.binancepriceservice.client.BinanceApiException;
import org.limir.binancepriceservice.client.BinanceRateLimitException;
import org.limir.binancepriceservice.service.PriceIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class PricePollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PricePollingScheduler.class);

    private final PriceIngestionService priceIngestionService;
    private final Clock clock;

    /**
     * Polls are skipped until this moment after Binance answered 429/418 with {@code Retry-After}.
     */
    private Instant pausedUntil = Instant.MIN;

    public PricePollingScheduler(PriceIngestionService priceIngestionService, Clock clock) {
        this.priceIngestionService = priceIngestionService;
        this.clock = clock;
    }

    /**
     * Every failure is caught and logged, so the next poll always runs on schedule.
     * {@code fixedDelay} starts the next poll only after the previous one has finished, so polls never overlap.
     */
    @Scheduled(fixedDelayString = "${binance.poll-interval}")
    public void poll() {
        Instant now = clock.instant();
        if (now.isBefore(pausedUntil)) {
            log.debug("Poll skipped: Binance rate limit pause until {}", pausedUntil);
            return;
        }

        try {
            priceIngestionService.fetchAndSave();
        } catch (BinanceRateLimitException e) {
            pauseAfterRateLimit(e, now);
        } catch (BinanceApiException e) {
            log.warn("Binance average price poll failed: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error during average price poll", e);
        }
    }

    private void pauseAfterRateLimit(BinanceRateLimitException e, Instant now) {
        Optional<Duration> retryAfter = e.getRetryAfter();
        if (retryAfter.isPresent()) {
            pausedUntil = now.plus(retryAfter.get());
            log.warn("{}, polling paused until {}", e.getMessage(), pausedUntil);
        } else {
            log.warn("{}, retrying on the next scheduled poll", e.getMessage());
        }
    }
}
