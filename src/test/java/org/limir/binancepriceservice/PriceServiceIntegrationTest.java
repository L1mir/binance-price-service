package org.limir.binancepriceservice;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.limir.binancepriceservice.repository.PriceSnapshotRepository;
import org.limir.binancepriceservice.scheduler.PricePollingScheduler;
import org.limir.binancepriceservice.service.PriceIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Whole application against a real PostgreSQL (Testcontainers) and a fake Binance (WireMock).
 * Scheduled polling is off; each test triggers polls itself.
 */
@SpringBootTest(properties = "binance.polling.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class PriceServiceIntegrationTest {

    private static final String AVG_PRICE_PATH = "/api/v3/avgPrice";
    private static final String FROM = "2000-01-01T00:00:00Z";
    private static final String TO = "2100-01-01T00:00:00Z";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @RegisterExtension
    static WireMockExtension binance = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void binanceBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("binance.base-url", binance::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PriceIngestionService priceIngestionService;

    @Autowired
    private PriceSnapshotRepository priceSnapshotRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * A new scheduler per test, so that a rate limit pause from one test does not affect the others.
     */
    private PricePollingScheduler scheduler;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM price_snapshot");
        scheduler = new PricePollingScheduler(priceIngestionService, Clock.systemUTC());
    }

    @Test
    void storesPolledPriceAndReturnsItFromApi() throws Exception {
        stubAvgPrice("612.34000000", 1694061154503L);

        scheduler.poll();

        mockMvc.perform(get("/api/v1/prices/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                .andExpect(jsonPath("$.price").value("612.34000000"))
                .andExpect(jsonPath("$.fetchedAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")));

        mockMvc.perform(get("/api/v1/prices").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void calculatesAverageAndTrendFromStoredPrices() throws Exception {
        stubAvgPrice("100.00000000", 1694061154000L);
        scheduler.poll();
        stubAvgPrice("110.00000000", 1694061155000L);
        scheduler.poll();

        mockMvc.perform(get("/api/v1/prices/average").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"averagePrice\":105.00000000}"));

        mockMvc.perform(get("/api/v1/prices/trend").param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").value("UPWARD"))
                .andExpect(jsonPath("$.start_date").value(FROM))
                .andExpect(jsonPath("$.end_date").value(TO));
    }

    @Test
    void doesNotStoreDuplicateWhenCloseTimeRepeats() {
        stubAvgPrice("612.34000000", 1694061154503L);

        scheduler.poll();
        scheduler.poll();

        assertThat(priceSnapshotRepository.count()).isEqualTo(1);
    }

    @Test
    void keepsPollingAfterBinanceFailure() {
        binance.stubFor(WireMock.get(urlPathEqualTo(AVG_PRICE_PATH)).willReturn(serverError()));
        scheduler.poll();
        assertThat(priceSnapshotRepository.count()).isZero();

        stubAvgPrice("612.34000000", 1694061154503L);
        scheduler.poll();
        assertThat(priceSnapshotRepository.count()).isEqualTo(1);
    }

    @Test
    void skipsNextPollAfterRateLimit() {
        binance.stubFor(WireMock.get(urlPathEqualTo(AVG_PRICE_PATH))
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "60")));

        scheduler.poll();
        scheduler.poll();

        binance.verify(1, getRequestedFor(urlPathEqualTo(AVG_PRICE_PATH)));
        assertThat(priceSnapshotRepository.count()).isZero();
    }

    @Test
    void returnsErrorsAsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/prices/latest"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/v1/prices").param("from", TO).param("to", FROM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("must not be after")));
    }

    private static void stubAvgPrice(String price, long closeTime) {
        binance.stubFor(WireMock.get(urlPathEqualTo(AVG_PRICE_PATH))
                .withQueryParam("symbol", equalTo("BNBUSDT"))
                .willReturn(okJson("{\"mins\":5,\"price\":\"%s\",\"closeTime\":%d}".formatted(price, closeTime))));
    }
}
