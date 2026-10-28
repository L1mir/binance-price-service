package org.limir.binancepriceservice.client;

import org.springframework.http.HttpStatusCode;

import java.time.Duration;
import java.util.Optional;

/**
 * Binance rejected the request because of rate limits: 429 (limit exceeded) or 418 (IP banned).
 * Requests must not be sent again until {@link #getRetryAfter()} has passed, otherwise the ban gets longer.
 */
public class BinanceRateLimitException extends BinanceApiException {

    private final Duration retryAfter;

    public BinanceRateLimitException(HttpStatusCode status, Duration retryAfter, Throwable cause) {
        super("Binance rate limit response " + status
                + (retryAfter == null ? " without valid Retry-After" : ", retry after " + retryAfter), cause);
        this.retryAfter = retryAfter;
    }

    /**
     * @return wait time from the {@code Retry-After} header, empty if Binance did not send a valid one
     */
    public Optional<Duration> getRetryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
