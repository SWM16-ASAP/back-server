package com.linglevel.api.bookmarks.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "북마크 토글 응답")
public class BookmarkToggleResponse {
    @Schema(description = "토글 후의 북마크 상태 (true: 북마크됨, false: 북마크 해제됨)", example = "true")
    private boolean bookmarked;
}