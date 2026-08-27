package com.linglevel.api.word.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "word.single-flight")
public class WordSingleFlightProperties {

	private boolean enabled = true;

	private long waitTimeoutMs = 5_000;

	private long resultCacheTtlMs = 30_000;

	private String resultSchemaVersion = "v2";

}
