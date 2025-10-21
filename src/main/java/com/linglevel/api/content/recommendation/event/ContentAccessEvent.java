package com.linglevel.api.content.recommendation.event;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.ContentType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ContentAccessEvent extends ApplicationEvent {

    private final String userId;
    private final String contentId;
    private final ContentType contentType;
    private final ContentCategory category;  // nullable

    public ContentAccessEvent(Object source, String userId, String contentId, ContentType contentType, ContentCategory category) {
        super(source);
        this.userId = userId;
        this.contentId = contentId;
        this.contentType = contentType;
        this.category = category;
    }
}
