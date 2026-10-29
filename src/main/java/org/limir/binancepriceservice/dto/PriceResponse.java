package org.limir.binancepriceservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.limir.binancepriceservice.entity.PriceSnapshot;

import java.time.Instant;

/**
 * @param price     plain decimal string, as Binance sends it, so that clients do not lose precision
 * @param fetchedAt moment the price was received from Binance
 */
public record PriceResponse(
        String symbol,
        String price,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiFormats.DATE_TIME_PATTERN, timezone = "UTC")
        Instant fetchedAt
) {

    public static PriceResponse from(PriceSnapshot snapshot) {
        return new PriceResponse(snapshot.getSymbol(), snapshot.getPrice().toPlainString(), snapshot.getFetchedAt());
    }
}
