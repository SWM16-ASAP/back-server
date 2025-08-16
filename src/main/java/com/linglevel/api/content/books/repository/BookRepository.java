package com.linglevel.api.content.books.repository;

import com.linglevel.api.content.books.entity.Book;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface
BookRepository extends MongoRepository<Book, String> {
}