package org.limir.binancepriceservice.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TrendCalculatorTest {

    private static final BigDecimal HALF_PERCENT = new BigDecimal("0.5");

    @Test
    void upwardWhenPriceRoseMoreThanThreshold() {
        assertThat(trend("100", "101", HALF_PERCENT)).isEqualTo(Trend.UPWARD);
    }

    @Test
    void downwardWhenPriceFellMoreThanThreshold() {
        assertThat(trend("100", "99", HALF_PERCENT)).isEqualTo(Trend.DOWNWARD);
    }

    @Test
    void flatWhenChangeIsWithinThreshold() {
        assertThat(trend("100", "100.3", HALF_PERCENT)).isEqualTo(Trend.FLAT);
        assertThat(trend("100", "99.7", HALF_PERCENT)).isEqualTo(Trend.FLAT);
    }

    @Test
    void flatWhenChangeEqualsThreshold() {
        assertThat(trend("100", "100.5", HALF_PERCENT)).isEqualTo(Trend.FLAT);
        assertThat(trend("100", "99.5", HALF_PERCENT)).isEqualTo(Trend.FLAT);
    }

    @Test
    void notFlatWhenChangeIsJustAboveThreshold() {
        assertThat(trend("100", "100.50000001", HALF_PERCENT)).isEqualTo(Trend.UPWARD);
    }

    @Test
    void flatForEqualPricesWithZeroThreshold() {
        assertThat(trend("100", "100", BigDecimal.ZERO)).isEqualTo(Trend.FLAT);
    }

    private static Trend trend(String firstPrice, String lastPrice, BigDecimal flatThresholdPercent) {
        return TrendCalculator.calculate(new BigDecimal(firstPrice), new BigDecimal(lastPrice), flatThresholdPercent);
    }
}
