package com.linglevel.api.streak.entity;

import lombok.Builder;
import lombok.Getter;
import jakarta.persistence.*;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Table(name = "freeze_transactions")
public class FreezeTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Integer amount;

	private java.time.LocalDate effectiveDate;

	@Column(length = 500)
	private String description;

	@Builder.Default
	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

}
