package com.linglevel.api.content.article.repository.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArticleChunkCount {
    private final String articleId;
    private final DifficultyLevel difficultyLevel;
    private final long count;
}
