package org.limir.binancepriceservice.client;

import java.math.BigDecimal;

/**
 * Body of {@code GET /api/v3/avgPrice}. Binance sends the price as a JSON string,
 * which is read into {@link BigDecimal} without loss of precision.
 *
 * @param price     average price over the last {@code mins} minutes (the {@code mins} field is not used)
 * @param closeTime time of the last trade, epoch milliseconds
 */
public record BinanceAvgPriceResponse(BigDecimal price, Long closeTime) {
}
