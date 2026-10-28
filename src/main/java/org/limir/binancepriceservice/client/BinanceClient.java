package org.limir.binancepriceservice.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
public class BinanceClient {

    private static final String AVG_PRICE_PATH = "/api/v3/avgPrice";

    private final RestClient restClient;

    public BinanceClient(RestClient binanceRestClient) {
        this.restClient = binanceRestClient;
    }

    /**
     * Requests the current average price of the given trading pair.
     *
     * @throws BinanceRateLimitException if Binance answered 429 or 418
     * @throws BinanceApiException       on any other HTTP error, network failure or incomplete response
     */
    public BinanceAvgPriceResponse getAveragePrice(String symbol) {
        BinanceAvgPriceResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(AVG_PRICE_PATH).queryParam("symbol", symbol).build())
                    .retrieve()
                    .body(BinanceAvgPriceResponse.class);
        } catch (RestClientResponseException e) {
            if (isRateLimit(e.getStatusCode())) {
                throw new BinanceRateLimitException(e.getStatusCode(), parseRetryAfter(e.getResponseHeaders()), e);
            }
            throw new BinanceApiException(
                    "Binance responded " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new BinanceApiException("Binance request failed: " + e.getMessage(), e);
        }

        if (response == null || response.price() == null || response.closeTime() == null) {
            throw new BinanceApiException("Binance returned an incomplete response: " + response);
        }
        return response;
    }

    private static boolean isRateLimit(HttpStatusCode status) {
        return status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS) || status.isSameCodeAs(HttpStatus.I_AM_A_TEAPOT);
    }

    /**
     * Binance sends {@code Retry-After} as a number of seconds.
     */
    private static Duration parseRetryAfter(HttpHeaders headers) {
        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
