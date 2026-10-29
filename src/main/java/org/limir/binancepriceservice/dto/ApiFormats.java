package org.limir.binancepriceservice.dto;

public final class ApiFormats {

    /**
     * ISO-8601 in UTC with seconds precision, as required for {@code fetchedAt}: {@code YYYY-MM-DDTHH:mm:ssZ}.
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private ApiFormats() {
    }
}
