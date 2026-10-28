package org.limir.binancepriceservice.client;

/**
 * Binance could not be called or returned an unusable response.
 */
public class BinanceApiException extends RuntimeException {

    public BinanceApiException(String message) {
        super(message);
    }

    public BinanceApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
