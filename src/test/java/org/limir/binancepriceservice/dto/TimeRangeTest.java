package org.limir.binancepriceservice.dto;

import org.junit.jupiter.api.Test;
import org.limir.binancepriceservice.exception.InvalidTimeRangeException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeRangeTest {

    private static final Instant MOMENT = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void rejectsFromAfterTo() {
        assertThatThrownBy(() -> new TimeRange(MOMENT.plusSeconds(1), MOMENT))
                .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void acceptsEqualBounds() {
        assertThatCode(() -> new TimeRange(MOMENT, MOMENT)).doesNotThrowAnyException();
    }
}
