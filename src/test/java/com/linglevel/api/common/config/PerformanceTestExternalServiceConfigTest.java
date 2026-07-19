package com.linglevel.api.common.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.linglevel.api.auth.controller.AuthController;
import com.linglevel.api.s3.config.PerformanceTestS3Config;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceTestExternalServiceConfigTest {

	@Test
	void performanceTestProfileUsesInertFirebaseClientsAndTaskRoleS3Client() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.getEnvironment().setActiveProfiles("performance-test");
			context.getEnvironment()
				.getPropertySources()
				.addFirst(new MapPropertySource("test", Map.of("aws.s3.region", "ap-northeast-2",
						"aws.s3.ai.input.bucket", "test-input", "aws.s3.ai.output.bucket", "test-output")));
			context.register(PerformanceTestFirebaseConfig.class, PerformanceTestS3Config.class, AuthController.class);
			context.refresh();

			FirebaseApp firebaseApp = context.getBean(FirebaseApp.class);
			assertThat(firebaseApp.getOptions().getProjectId()).isEqualTo("llv-performance-test");
			assertThat(context.getBean(FirebaseAuth.class)).isNotNull();
			assertThat(context.getBean(FirebaseMessaging.class)).isNotNull();
			assertThat(context.getBeansOfType(AuthController.class)).isEmpty();

			S3Client aiClient = context.getBean("s3AiClient", S3Client.class);
			assertThat(context.getBean("s3StaticClient", S3Client.class)).isSameAs(aiClient);
			assertThat(context.getBean("aiInputBucketName")).isEqualTo("test-input");
			assertThat(context.getBean("aiOutputBucketName")).isEqualTo("test-output");
		}
	}

}
