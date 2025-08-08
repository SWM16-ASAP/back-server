package com.linglevel.api.words.service;

import com.linglevel.api.words.dto.WordResponse;
import com.linglevel.api.words.entity.Word;
import com.linglevel.api.words.exception.WordsErrorCode;
import com.linglevel.api.words.exception.WordsException;
import com.linglevel.api.words.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordService {
    
    private final WordRepository wordRepository;
    
    public Page<WordResponse> getWords(int page, int limit, String search) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Word> words;
        
        if (search != null && !search.trim().isEmpty()) {
            words = wordRepository.findByWordContainingIgnoreCase(search.trim(), pageable);
        } else {
            words = wordRepository.findAll(pageable);
        }
        
        return words.map(this::convertToResponse);
    }
    
    public Optional<WordResponse> getWordByWord(String word) {
        return wordRepository.findByWord(word)
                .map(this::convertToResponse);
    }
    
    public WordResponse createWord(String word) {
        if (wordRepository.existsByWord(word)) {
            throw new WordsException(WordsErrorCode.WORD_ALREADY_EXISTS);
        }
        
        Word newWord = Word.builder()
                .word(word)
                .build();
        
        Word savedWord = wordRepository.save(newWord);
        return convertToResponse(savedWord);
    }
    
    private WordResponse convertToResponse(Word word) {
        return WordResponse.builder()
                .id(word.getId())
                .word(word.getWord())
                .build();
    }
}