package com.linglevel.api.word.repository;

import com.linglevel.api.word.entity.InvalidWord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvalidWordRepository extends MongoRepository<InvalidWord, String> {

    Optional<InvalidWord> findByWord(String word);

    boolean existsByWord(String word);
}
