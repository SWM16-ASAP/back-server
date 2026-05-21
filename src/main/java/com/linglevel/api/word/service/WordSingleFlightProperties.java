package com.linglevel.api.word.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "word.single-flight")
public class WordSingleFlightProperties {

    private boolean enabled = true;

    private long lockTtlMs = 20_000;

    private long waitTimeoutMs = 5_000;

    private long resultTtlMs = 60_000;

    private String promptVersion = "v1";

    private String model = "default";

    private String schemaVersion = "v2";

    private boolean redlockEnabled = false;

    private List<String> redlockNodeAddresses = new ArrayList<>();
}
