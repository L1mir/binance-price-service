package org.limir.binancepriceservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /**
     * Single source of the current time, so that tests can substitute a fixed clock.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
