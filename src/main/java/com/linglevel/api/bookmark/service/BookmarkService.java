package com.linglevel.api.bookmark.service;

import com.linglevel.api.bookmark.dto.BookmarkedWordResponse;
import com.linglevel.api.bookmark.entity.WordBookmark;
import com.linglevel.api.bookmark.exception.BookmarksErrorCode;
import com.linglevel.api.bookmark.exception.BookmarksException;
import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.service.WordVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final WordVariantService wordVariantService;

    public Page<BookmarkedWordResponse> getBookmarkedWords(String userId, int page, int limit, String search) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "bookmarkedAt"));
        
        if (search != null && !search.trim().isEmpty()) {
            // 검색어가 있는 경우: 단어를 먼저 검색한 후 북마크 필터링
            List<Word> matchingWords = wordRepository.findByWordContainingIgnoreCase(search.trim(), PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            List<String> words = matchingWords.stream().map(Word::getWord).collect(Collectors.toList());
            
            if (words.isEmpty()) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
            
            Page<WordBookmark> bookmarks = wordBookmarkRepository.findByUserIdAndWordIn(userId, words, pageable);
            return convertToBookmarkedWordResponseDirect(bookmarks);
        } else {
            // 검색어가 없는 경우: 모든 북마크 조회
            Page<WordBookmark> bookmarks = wordBookmarkRepository.findByUserId(userId, pageable);
            return convertToBookmarkedWordResponseDirect(bookmarks);
        }
    }
    
    @Transactional
    public void addWordBookmark(String userId, String wordStr) {
        // 단어의 원형 찾기 (언어 중립적)
        String originalForm = wordVariantService.getOriginalForm(wordStr);

        if (wordBookmarkRepository.existsByUserIdAndWord(userId, originalForm)) {
            throw new BookmarksException(BookmarksErrorCode.WORD_ALREADY_BOOKMARKED);
        }

        WordBookmark bookmark = WordBookmark.builder()
                .userId(userId)
                .word(originalForm)
                .bookmarkedAt(LocalDateTime.now())
                .build();

        wordBookmarkRepository.save(bookmark);
    }
    
    @Transactional
    public void removeWordBookmark(String userId, String wordStr) {
        // 단어의 원형 찾기 (언어 중립적)
        String originalForm = wordVariantService.getOriginalForm(wordStr);

        if (!wordVariantService.exists(originalForm)) {
            throw new BookmarksException(BookmarksErrorCode.WORD_NOT_FOUND);
        }

        if (!wordBookmarkRepository.existsByUserIdAndWord(userId, originalForm)) {
            throw new BookmarksException(BookmarksErrorCode.WORD_BOOKMARK_NOT_FOUND);
        }

        wordBookmarkRepository.deleteByUserIdAndWord(userId, originalForm);
    }
    
    @Transactional
    public boolean toggleWordBookmark(String userId, String wordStr) {
        // 단어의 원형 찾기 (언어 중립적)
        String originalForm = wordVariantService.getOriginalForm(wordStr);

        boolean isBookmarked = wordBookmarkRepository.existsByUserIdAndWord(userId, originalForm);

        if (isBookmarked) {
            // 북마크 해제
            wordBookmarkRepository.deleteByUserIdAndWord(userId, originalForm);
            return false;
        } else {
            // 북마크 추가
            WordBookmark bookmark = WordBookmark.builder()
                    .userId(userId)
                    .word(originalForm)
                    .bookmarkedAt(LocalDateTime.now())
                    .build();
            wordBookmarkRepository.save(bookmark);
            return true;
        }
    }
    
    private Page<BookmarkedWordResponse> convertToBookmarkedWordResponseDirect(Page<WordBookmark> bookmarks) {
        List<BookmarkedWordResponse> responses = new ArrayList<>();
        
        for (WordBookmark bookmark : bookmarks.getContent()) {
             wordRepository.findByWord(bookmark.getWord())
                    .ifPresent(word -> responses.add(BookmarkedWordResponse.builder()
                            .id(word.getId())
                            .word(word.getWord())
                            .bookmarkedAt(bookmark.getBookmarkedAt())
                            .build()));
        }
        
        return new PageImpl<>(responses, bookmarks.getPageable(), bookmarks.getTotalElements());
    }
}