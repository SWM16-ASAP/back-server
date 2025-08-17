package com.linglevel.api.content.article.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArticleErrorCode {
    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Article not found."),
    CHUNK_NOT_FOUND(HttpStatus.NOT_FOUND, "Chunk not found."),
    INVALID_SORT_BY(HttpStatus.BAD_REQUEST, "Invalid sort_by parameter. Must be one of: view_count, average_rating, created_at."),
    INVALID_TAGS_FORMAT(HttpStatus.BAD_REQUEST, "Invalid tags format. Tags should be comma-separated strings."),
    INVALID_DIFFICULTY_LEVEL(HttpStatus.BAD_REQUEST, "Invalid difficulty level.");

    private final HttpStatus status;
    private final String message;
}