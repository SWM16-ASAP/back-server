package com.linglevel.api.books.repository;

import com.linglevel.api.books.entity.BookProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BookProgressRepository extends MongoRepository<BookProgress, String> {
    Optional<BookProgress> findByUserIdAndBookId(String UserId, String bookId);
}
