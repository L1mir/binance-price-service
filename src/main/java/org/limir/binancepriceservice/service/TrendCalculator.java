package org.limir.binancepriceservice.service;

import java.math.BigDecimal;

/**
 * Classifies the price movement between the first and the last price of an interval.
 */
public final class TrendCalculator {

    private TrendCalculator() {
    }

    /**
     * The change is compared with {@code firstPrice * flatThresholdPercent / 100} instead of computing a percentage,
     * so there is no division and no rounding.
     *
     * @return FLAT if the price changed by at most {@code flatThresholdPercent} percent of the first price,
     * otherwise UPWARD or DOWNWARD by the direction of the change
     */
    public static Trend calculate(BigDecimal firstPrice, BigDecimal lastPrice, BigDecimal flatThresholdPercent) {
        BigDecimal change = lastPrice.subtract(firstPrice);
        BigDecimal flatTolerance = firstPrice.multiply(flatThresholdPercent).movePointLeft(2);
        if (change.abs().compareTo(flatTolerance) <= 0) {
            return Trend.FLAT;
        }
        return change.signum() > 0 ? Trend.UPWARD : Trend.DOWNWARD;
    }
}
