package org.limir.binancepriceservice.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Settings of the trend calculation, bound from the {@code trend.*} properties.
 *
 * @param flatThresholdPercent maximum price change, in percent of the first price, that is still reported as FLAT
 */
@Validated
@ConfigurationProperties(prefix = "trend")
public record TrendProperties(
        @NotNull @DecimalMin("0") BigDecimal flatThresholdPercent
) {
}
