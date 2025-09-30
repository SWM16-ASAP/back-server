package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.entity.BookProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookProgressRepository extends MongoRepository<BookProgress, String> {
    Optional<BookProgress> findByUserIdAndBookId(String UserId, String bookId);
    Page<BookProgress> findAllByUserId(String userId, Pageable pageable);
    List<BookProgress> findAllByUserId(String userId);
    List<BookProgress> findByBookId(String bookId);
}
