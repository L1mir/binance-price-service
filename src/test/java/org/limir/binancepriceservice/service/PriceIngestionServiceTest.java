package org.limir.binancepriceservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.limir.binancepriceservice.client.BinanceApiException;
import org.limir.binancepriceservice.client.BinanceAvgPriceResponse;
import org.limir.binancepriceservice.client.BinanceClient;
import org.limir.binancepriceservice.config.BinanceProperties;
import org.limir.binancepriceservice.repository.PriceSnapshotRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceIngestionServiceTest {

    private static final String SYMBOL = "BNBUSDT";
    private static final Instant NOW = Instant.parse("2026-10-29T10:00:00Z");

    @Mock
    private BinanceClient binanceClient;

    @Mock
    private PriceSnapshotRepository priceSnapshotRepository;

    private PriceIngestionService priceIngestionService;

    @BeforeEach
    void setUp() {
        BinanceProperties properties = new BinanceProperties(URI.create("https://api.binance.com"), SYMBOL,
                Duration.ofSeconds(30), Duration.ofSeconds(5), Duration.ofSeconds(10));
        priceIngestionService = new PriceIngestionService(binanceClient, priceSnapshotRepository, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void savesPriceWithCloseTimeFromBinanceAndFetchedAtFromClock() {
        when(binanceClient.getAveragePrice(SYMBOL))
                .thenReturn(new BinanceAvgPriceResponse(new BigDecimal("612.34000000"), 1694061154503L));

        priceIngestionService.fetchAndSave();

        verify(priceSnapshotRepository).insertIfAbsent(SYMBOL, new BigDecimal("612.34000000"),
                Instant.parse("2023-09-07T04:32:34.503Z"), NOW);
    }

    @Test
    void savesNothingWhenBinanceFails() {
        when(binanceClient.getAveragePrice(SYMBOL)).thenThrow(new BinanceApiException("Binance responded 503"));

        assertThatThrownBy(() -> priceIngestionService.fetchAndSave()).isInstanceOf(BinanceApiException.class);

        verifyNoInteractions(priceSnapshotRepository);
    }
}
