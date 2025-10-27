package com.linglevel.api.streak.dto;

import com.linglevel.api.content.common.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingSession implements Serializable {
    private ContentType contentType;
    private String contentId;
    private Instant startedAt;
}
