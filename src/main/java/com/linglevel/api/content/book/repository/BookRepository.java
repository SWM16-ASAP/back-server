package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.entity.Book;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface
BookRepository extends MongoRepository<Book, String> {
}