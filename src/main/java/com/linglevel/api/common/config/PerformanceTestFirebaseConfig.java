package com.linglevel.api.common.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Date;

@Configuration
@Profile("performance-test")
public class PerformanceTestFirebaseConfig {

	@Bean(destroyMethod = "delete")
	public FirebaseApp performanceTestFirebaseApp() {
		GoogleCredentials credentials = GoogleCredentials
			.create(new AccessToken("performance-test", new Date(Long.MAX_VALUE)));
		FirebaseOptions options = FirebaseOptions.builder()
			.setCredentials(credentials)
			.setProjectId("llv-performance-test")
			.build();
		return FirebaseApp.initializeApp(options, "performance-test");
	}

	@Bean
	public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
		return FirebaseAuth.getInstance(firebaseApp);
	}

	@Bean
	public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
		return FirebaseMessaging.getInstance(firebaseApp);
	}

}
