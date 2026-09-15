package com.linglevel.api.banner.entity;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.i18n.CountryCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_banners")
public class ContentBanner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private CountryCode countryCode;

	@Column(nullable = false)
	private Long contentId;

	@Enumerated(EnumType.STRING)
	@Column(length = 10, nullable = false)
	private ContentType contentType;

	@Column(length = 500)
	private String contentTitle;

	@Column(length = 500)
	private String contentAuthor;

	@Column(length = 2048)
	private String contentCoverImageUrl;

	private Integer contentReadingTime;

	@Column(length = 500)
	private String subtitle;

	@Column(length = 500, nullable = false)
	private String title;

	@Column(columnDefinition = "text", nullable = false)
	private String description;

	@Column(nullable = false)
	private Integer displayOrder = 9;

	@Column(nullable = false)
	private Boolean isActive = true;

	@Column(nullable = false)
	private LocalDateTime createdAt;

}
