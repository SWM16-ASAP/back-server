package com.linglevel.api.word.repository;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordRepository extends MongoRepository<Word, String> {
    /**
     * 단어와 언어 쌍으로 검색
     * 같은 단어도 언어 쌍별로 다른 Word 문서가 존재할 수 있음
     */
    Optional<Word> findByWordAndSourceLanguageCodeAndTargetLanguageCode(
        String word,
        LanguageCode sourceLanguageCode,
        LanguageCode targetLanguageCode
    );

    /**
     * 단어와 target 언어로 검색 (sourceLanguageCode 무시)
     * 대부분의 경우 특정 단어는 하나의 source 언어만 가짐 (예: "run"은 항상 EN)
     */
    Optional<Word> findByWordAndTargetLanguageCode(
        String word,
        LanguageCode targetLanguageCode
    );

    @Query("{'word': {$regex: ?0, $options: 'i'}}")
    Page<Word> findByWordContainingIgnoreCase(String word, Pageable pageable);

    boolean existsByWord(String word);

    /**
     * isEssential 필드로 필터링
     */
    List<Word> findAllByIsEssential(Boolean isEssential);

    /**
     * isEssential 필드로 개수 조회
     */
    long countByIsEssential(Boolean isEssential);

    /**
     * 필수 단어 중 특정 target 언어로 필터링
     */
    List<Word> findAllByIsEssentialAndTargetLanguageCode(Boolean isEssential, LanguageCode targetLanguageCode);

    /**
     * 필수 단어를 페이징으로 조회
     */
    Page<Word> findAllByIsEssential(Boolean isEssential, Pageable pageable);
}