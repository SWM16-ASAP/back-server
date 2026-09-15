package com.linglevel.api.word.repository;

import com.linglevel.api.word.entity.InvalidWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvalidWordRepository extends JpaRepository<InvalidWord, Long> {

	Optional<InvalidWord> findByWord(String word);

}
