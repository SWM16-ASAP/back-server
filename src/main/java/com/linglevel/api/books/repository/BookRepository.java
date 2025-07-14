package com.linglevel.api.books.repository;

import com.linglevel.api.books.entity.Book;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookRepository extends MongoRepository<Book, String> {
}