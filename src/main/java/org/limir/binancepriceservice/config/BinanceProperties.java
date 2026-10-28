package org.limir.binancepriceservice.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * Settings for polling the Binance average price endpoint, bound from the {@code binance.*} properties.
 *
 * @param baseUrl        Binance REST API base URL
 * @param symbol         trading pair to poll; the pattern is the one Binance accepts for the {@code symbol} parameter
 * @param pollInterval   delay between the end of one poll and the start of the next
 * @param connectTimeout maximum time to establish a connection to Binance
 * @param readTimeout    maximum time to wait for a Binance response
 */
@Validated
@ConfigurationProperties(prefix = "binance")
public record BinanceProperties(
        @NotNull URI baseUrl,
        @NotNull @Pattern(regexp = "^[A-Z0-9_.-]{1,20}$") String symbol,
        @NotNull @DurationMin(nanos = 0, inclusive = false) Duration pollInterval,
        @NotNull @DurationMin(nanos = 0, inclusive = false) Duration connectTimeout,
        @NotNull @DurationMin(nanos = 0, inclusive = false) Duration readTimeout
) {
}
