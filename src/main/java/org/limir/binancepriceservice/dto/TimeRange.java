package org.limir.binancepriceservice.dto;

import org.limir.binancepriceservice.exception.InvalidTimeRangeException;

import java.time.Instant;

/**
 * Closed interval {@code [from, to]} of {@code fetchedAt} values requested by a client.
 */
public record TimeRange(Instant from, Instant to) {

    public TimeRange {
        if (from.isAfter(to)) {
            throw new InvalidTimeRangeException("'from' (" + from + ") must not be after 'to' (" + to + ")");
        }
    }
}
