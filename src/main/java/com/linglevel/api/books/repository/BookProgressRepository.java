package com.linglevel.api.books.repository;

import com.linglevel.api.books.entity.BookProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookProgressRepository extends MongoRepository<BookProgress, String> {
}
