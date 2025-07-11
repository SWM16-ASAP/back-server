package com.linglevel.api.books.repository;

import com.linglevel.api.books.entity.Chunk;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChunkRepository extends MongoRepository<Chunk, String> {
} 