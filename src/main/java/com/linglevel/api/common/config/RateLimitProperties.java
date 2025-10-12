package com.linglevel.api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rate.limit")
public class RateLimitProperties {

    private int capacity;

    private Refill refill = new Refill();

    @Getter
    @Setter
    public static class Refill {

        private Duration duration = new Duration();

        @Getter
        @Setter
        public static class Duration {

            private long minutes;
        }
    }
}
