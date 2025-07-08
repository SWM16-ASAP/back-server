package com.linglevel.api.books.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BooksErrorCode {
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "Book not found."),
    CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND, "Chapter not found."),
    CHAPTER_NOT_FOUND_IN_BOOK(HttpStatus.NOT_FOUND, "Chapter not found in this book."),
    CHUNK_NOT_FOUND(HttpStatus.NOT_FOUND, "Chunk not found."),
    INVALID_DIFFICULTY_LEVEL(HttpStatus.BAD_REQUEST, "Invalid difficulty level."),
    INVALID_SORT_BY(HttpStatus.BAD_REQUEST, "Invalid sort_by parameter. Must be one of: view_count, average_rating, created_at."),
    INVALID_TAGS_FORMAT(HttpStatus.BAD_REQUEST, "Invalid tags format. Tags should be comma-separated strings."),
    INVALID_CHUNK_NUMBER(HttpStatus.BAD_REQUEST, "Invalid chunkNumber. Must be a positive integer."),
    INVALID_PAGINATION(HttpStatus.BAD_REQUEST, "Invalid pagination parameters.");
    
    private final HttpStatus status;
    private final String message;
} 