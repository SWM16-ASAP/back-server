package com.linglevel.api.fcm.entity;

import com.linglevel.api.i18n.CountryCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fcm_tokens")
public class FcmToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@Column(nullable = false)
	private Long userId;

	@NotNull
	@Column(nullable = false, length = 255)
	private String deviceId;

	@NotNull
	@Column(nullable = false, unique = true, length = 500)
	private String fcmToken;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private FcmPlatform platform;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private CountryCode countryCode;

	@Column(length = 50)
	private String appVersion;

	@Column(length = 100)
	private String osVersion;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Builder.Default
	@Column(nullable = false)
	private Boolean isActive = true;

}
