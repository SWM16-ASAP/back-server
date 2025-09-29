package com.linglevel.api.word.service;

import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.word.dto.WordResponse;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordService {

    private final WordRepository wordRepository;
    private final WordBookmarkRepository wordBookmarkRepository;

    public Page<WordResponse> getWords(String userId, int page, int limit, String search) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Word> words;

        if (search != null && !search.trim().isEmpty()) {
            words = wordRepository.findByWordContainingIgnoreCase(search.trim(), pageable);
        } else {
            words = wordRepository.findAll(pageable);
        }

        List<String> wordStrings = words.getContent().stream()
                .map(Word::getWord)
                .collect(Collectors.toList());

        List<String> bookmarkedWords = wordBookmarkRepository.findByUserIdAndWordIn(userId, wordStrings, Pageable.unpaged())
                .getContent().stream()
                .map(bookmark -> bookmark.getWord())
                .collect(Collectors.toList());

        return words.map(word -> convertToResponse(word, bookmarkedWords.contains(word.getWord())));
    }

    public WordResponse getOrCreateWord(String userId, String word) {
        Word wordEntity = getOrCreateWordEntity(word);
        boolean isBookmarked = wordBookmarkRepository.existsByUserIdAndWord(userId, word);
        return convertToResponse(wordEntity, isBookmarked);
    }

    @Transactional
    public Word getOrCreateWordEntity(String word) {
        return wordRepository.findByWord(word)
                .orElseGet(() -> {
                    Word newWord = Word.builder()
                            .word(word)
                            .build();
                    return wordRepository.save(newWord);
                });
    }

    private WordResponse convertToResponse(Word word, boolean isBookmarked) {
        return WordResponse.builder()
                .id(word.getId())
                .word(word.getWord())
                .bookmarked(isBookmarked)
                .build();
    }
}