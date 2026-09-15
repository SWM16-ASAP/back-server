package com.linglevel.api.word.repository;

import com.linglevel.api.word.entity.WordVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordVariantRepository extends JpaRepository<WordVariant, Long> {

	List<WordVariant> findAllByWord(String word);

	List<WordVariant> findByWordIn(List<String> words);

	Optional<WordVariant> findByWordAndOriginalForm(String word, String originalForm);

	List<WordVariant> findAllByOriginalForm(String originalForm);

}
