package org.limir.binancepriceservice.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.limir.binancepriceservice.client.BinanceApiException;
import org.limir.binancepriceservice.client.BinanceRateLimitException;
import org.limir.binancepriceservice.service.PriceIngestionService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricePollingSchedulerTest {

    private static final Instant START = Instant.parse("2026-10-29T10:00:00Z");

    @Mock
    private PriceIngestionService priceIngestionService;

    @Mock
    private Clock clock;

    private PricePollingScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PricePollingScheduler(priceIngestionService, clock);
    }

    @Test
    void keepsPollingAfterBinanceFailure() {
        when(clock.instant()).thenReturn(START, START.plusSeconds(30));
        doThrow(new BinanceApiException("Binance responded 503")).when(priceIngestionService).fetchAndSave();

        assertThatCode(() -> scheduler.poll()).doesNotThrowAnyException();
        assertThatCode(() -> scheduler.poll()).doesNotThrowAnyException();

        verify(priceIngestionService, times(2)).fetchAndSave();
    }

    @Test
    void keepsPollingAfterUnexpectedError() {
        when(clock.instant()).thenReturn(START, START.plusSeconds(30));
        doThrow(new IllegalStateException("Database is down")).when(priceIngestionService).fetchAndSave();

        assertThatCode(() -> scheduler.poll()).doesNotThrowAnyException();
        assertThatCode(() -> scheduler.poll()).doesNotThrowAnyException();

        verify(priceIngestionService, times(2)).fetchAndSave();
    }

    @Test
    void skipsPollsUntilRetryAfterHasPassed() {
        when(clock.instant()).thenReturn(START, START.plusSeconds(30), START.plusSeconds(61));
        doThrow(new BinanceRateLimitException(HttpStatus.TOO_MANY_REQUESTS, Duration.ofSeconds(60), null))
                .doNothing()
                .when(priceIngestionService).fetchAndSave();

        scheduler.poll(); // 10:00:00 - rate limited, paused until 10:01:00
        scheduler.poll(); // 10:00:30 - skipped
        scheduler.poll(); // 10:01:01 - polls again

        verify(priceIngestionService, times(2)).fetchAndSave();
    }

    @Test
    void pollsOnNextRunWhenRetryAfterIsMissing() {
        when(clock.instant()).thenReturn(START, START.plusSeconds(30));
        doThrow(new BinanceRateLimitException(HttpStatus.TOO_MANY_REQUESTS, null, null))
                .doNothing()
                .when(priceIngestionService).fetchAndSave();

        scheduler.poll();
        scheduler.poll();

        verify(priceIngestionService, times(2)).fetchAndSave();
    }
}
