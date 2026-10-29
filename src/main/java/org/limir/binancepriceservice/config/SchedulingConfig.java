package org.limir.binancepriceservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Polling is on unless {@code binance.polling.enabled=false}.
 * Integration tests turn it off and trigger polls themselves, so that results do not depend on timing.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "binance.polling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
