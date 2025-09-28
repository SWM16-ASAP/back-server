package com.linglevel.api.banner.entity;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.i18n.CountryCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "contentBanners")
public class ContentBanner {

    @Id
    private String id;

    private CountryCode countryCode;

    private String contentId;

    private ContentType contentType;

    private String contentTitle;

    private String contentAuthor;

    private String contentCoverImageUrl;

    private Integer contentReadingTime;

    private String subtitle;

    private String title;

    private String description;

    private Integer displayOrder = 9;

    private Boolean isActive = true;

    private LocalDateTime createdAt;
}