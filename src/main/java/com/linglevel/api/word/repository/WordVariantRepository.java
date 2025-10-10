package com.linglevel.api.word.repository;

import com.linglevel.api.word.entity.WordVariant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordVariantRepository extends MongoRepository<WordVariant, String> {
    Optional<WordVariant> findByWord(String word);

    List<WordVariant> findAllByWord(String word);

    boolean existsByWord(String word);

    List<WordVariant> findByWordIn(List<String> words);

    Optional<WordVariant> findByWordAndOriginalForm(String word, String originalForm);
}
