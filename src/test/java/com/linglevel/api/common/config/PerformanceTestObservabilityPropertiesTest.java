package com.linglevel.api.common.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceTestObservabilityPropertiesTest {

	@Test
	void performanceTestProfileEnablesPrometheusHttpHistogramsAndTomcatMetrics() throws IOException {
		Properties properties = new Properties();
		try (InputStream input = getClass().getResourceAsStream("/application-performance-test.properties")) {
			assertThat(input).isNotNull();
			properties.load(input);
		}

		assertThat(properties.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health,prometheus");
		assertThat(properties.getProperty("management.metrics.tags.application")).isEqualTo("llv-api");
		assertThat(properties.getProperty("management.metrics.distribution.percentiles-histogram.http.server.requests"))
			.isEqualTo("true");
		assertThat(properties.getProperty("server.tomcat.mbeanregistry.enabled")).isEqualTo("true");
	}

}
