package com.linglevel.api.bookmarks.service;

import com.linglevel.api.bookmarks.dto.BookmarkedWordResponse;
import com.linglevel.api.bookmarks.entity.WordBookmark;
import com.linglevel.api.bookmarks.exception.BookmarksErrorCode;
import com.linglevel.api.bookmarks.exception.BookmarksException;
import com.linglevel.api.bookmarks.repository.WordBookmarkRepository;
import com.linglevel.api.words.entity.Word;
import com.linglevel.api.words.repository.WordRepository;
import com.linglevel.api.words.service.WordService;
import com.linglevel.api.words.exception.WordsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {

    private final WordBookmarkRepository wordBookmarkRepository;
    private final WordRepository wordRepository;
    private final WordService wordService;

    public Page<BookmarkedWordResponse> getBookmarkedWords(String userId, int page, int limit, String search) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        
        if (search != null && !search.trim().isEmpty()) {
            // 검색어가 있는 경우: 단어를 먼저 검색한 후 북마크 필터링
            List<Word> matchingWords = wordRepository.findByWordContainingIgnoreCase(search.trim(), PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            List<String> wordIds = matchingWords.stream().map(Word::getId).collect(Collectors.toList());
            
            if (wordIds.isEmpty()) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
            
            Page<WordBookmark> bookmarks = wordBookmarkRepository.findByUserIdAndWordIdIn(userId, wordIds, pageable);
            return convertToBookmarkedWordResponse(bookmarks, matchingWords);
        } else {
            // 검색어가 없는 경우: 모든 북마크 조회
            Page<WordBookmark> bookmarks = wordBookmarkRepository.findByUserId(userId, pageable);
            return convertToBookmarkedWordResponseWithWordLookup(bookmarks);
        }
    }
    
    @Transactional
    public void addWordBookmark(String userId, String wordStr) {
        // 단어 존재 확인, 없으면 WordService를 통해 자동 생성
        Word word = wordRepository.findByWord(wordStr)
                .orElseGet(() -> {
                        wordService.createWord(wordStr);
                        return wordRepository.findByWord(wordStr)
                                .orElseThrow(() -> new BookmarksException(BookmarksErrorCode.WORD_NOT_FOUND));
                });

        if (wordBookmarkRepository.existsByUserIdAndWordId(userId, word.getId())) {
            throw new BookmarksException(BookmarksErrorCode.WORD_ALREADY_BOOKMARKED);
        }

        WordBookmark bookmark = WordBookmark.builder()
                .userId(userId)
                .wordId(word.getId())
                .bookmarkedAt(LocalDateTime.now())
                .build();
        
        wordBookmarkRepository.save(bookmark);
    }
    
    @Transactional
    public void removeWordBookmark(String userId, String wordStr) {
        Word word = wordRepository.findByWord(wordStr)
                .orElseThrow(() -> new BookmarksException(BookmarksErrorCode.WORD_NOT_FOUND));

        if (!wordBookmarkRepository.existsByUserIdAndWordId(userId, word.getId())) {
            throw new BookmarksException(BookmarksErrorCode.WORD_BOOKMARK_NOT_FOUND);
        }

        wordBookmarkRepository.deleteByUserIdAndWordId(userId, word.getId());
    }
    
    private Page<BookmarkedWordResponse> convertToBookmarkedWordResponse(Page<WordBookmark> bookmarks, List<Word> words) {
        List<BookmarkedWordResponse> responses = new ArrayList<>();
        
        for (WordBookmark bookmark : bookmarks.getContent()) {
            Word word = words.stream()
                    .filter(w -> w.getId().equals(bookmark.getWordId()))
                    .findFirst()
                    .orElse(null);
            
            if (word != null) {
                responses.add(BookmarkedWordResponse.builder()
                        .id(word.getId())
                        .word(word.getWord())
                        .bookmarkedAt(bookmark.getBookmarkedAt())
                        .build());
            }
        }
        
        return new PageImpl<>(responses, bookmarks.getPageable(), bookmarks.getTotalElements());
    }
    
    private Page<BookmarkedWordResponse> convertToBookmarkedWordResponseWithWordLookup(Page<WordBookmark> bookmarks) {
        List<BookmarkedWordResponse> responses = new ArrayList<>();
        
        for (WordBookmark bookmark : bookmarks.getContent()) {
            Word word = wordRepository.findById(bookmark.getWordId()).orElse(null);
            if (word != null) {
                responses.add(BookmarkedWordResponse.builder()
                        .id(word.getId())
                        .word(word.getWord())
                        .bookmarkedAt(bookmark.getBookmarkedAt())
                        .build());
            }
        }
        
        return new PageImpl<>(responses, bookmarks.getPageable(), bookmarks.getTotalElements());
    }
}