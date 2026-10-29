package org.limir.binancepriceservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.limir.binancepriceservice.service.Trend;

import java.time.Instant;

/**
 * Field names follow the response format from the assignment, hence snake_case.
 *
 * @param startDate requested {@code from}
 * @param endDate   requested {@code to}
 */
public record TrendResponse(
        String symbol,
        @JsonProperty("start_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiFormats.DATE_TIME_PATTERN, timezone = "UTC")
        Instant startDate,
        @JsonProperty("end_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiFormats.DATE_TIME_PATTERN, timezone = "UTC")
        Instant endDate,
        Trend trend
) {
}
