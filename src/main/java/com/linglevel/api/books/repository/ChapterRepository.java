package com.linglevel.api.books.repository;

import com.linglevel.api.books.entity.Chapter;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChapterRepository extends MongoRepository<Chapter, String> {
} 