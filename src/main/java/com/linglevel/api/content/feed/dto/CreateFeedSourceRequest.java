package com.linglevel.api.content.feed.dto;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.feed.entity.FeedContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedSourceRequest {

    @NotBlank(message = "URL is required")
    private String url;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Content type is required")
    private FeedContentType contentType;

    @NotNull(message = "Category is required")
    private ContentCategory category;

    private List<String> tags;
}
