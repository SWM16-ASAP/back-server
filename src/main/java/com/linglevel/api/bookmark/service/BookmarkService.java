package com.linglevel.api.bookmark.service;

import com.linglevel.api.bookmark.dto.BookmarkedWordResponse;
import com.linglevel.api.bookmark.entity.WordBookmark;
import com.linglevel.api.bookmark.exception.BookmarksErrorCode;
import com.linglevel.api.bookmark.exception.BookmarksException;
import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.WordSearchResponse;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.service.WordService;
import com.linglevel.api.word.service.WordVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class BookmarkService {

	private final WordBookmarkRepository wordBookmarkRepository;

	private final WordRepository wordRepository;

	private final WordVariantService wordVariantService;

	private final WordService wordService;

	private final BookmarkWriter bookmarkWriter;

	public Page<BookmarkedWordResponse> getBookmarkedWords(String userId, int page, int limit, String search) {
		Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "bookmarkedAt", "id"));
		Page<WordBookmark> bookmarks = search != null && !search.trim().isEmpty() ? wordBookmarkRepository
			.findByUserIdAndWordContainingIgnoreCase(Long.valueOf(userId), search.trim(), pageable)
				: wordBookmarkRepository.findByUserId(Long.valueOf(userId), pageable);
		return convertToBookmarkedWordResponseDirect(bookmarks);
	}

	public void addWordBookmark(String userId, String wordStr) {
		var response = wordService.getOrCreateWords(userId, wordStr, LanguageCode.KO);
		bookmarkWriter.add(userId, resolveFirstOriginalForm(response));
	}

	public void removeWordBookmark(String userId, String wordStr) {
		bookmarkWriter.remove(userId, wordStr, wordVariantService.getOriginalForms(wordStr));
	}

	public boolean toggleWordBookmark(String userId, String wordStr) {
		var response = wordService.getOrCreateWords(userId, wordStr, LanguageCode.KO);
		return bookmarkWriter.toggle(userId, resolveFirstOriginalForm(response));
	}

	public boolean toggleWordBookmarkById(String userId, String wordId) {
		Word word = wordRepository.findById(wordId)
			.orElseThrow(() -> new BookmarksException(BookmarksErrorCode.WORD_NOT_FOUND));
		return bookmarkWriter.toggle(userId, word.getWord());
	}

	private Page<BookmarkedWordResponse> convertToBookmarkedWordResponseDirect(Page<WordBookmark> bookmarks) {
		List<BookmarkedWordResponse> responses = new ArrayList<>();

		for (WordBookmark bookmark : bookmarks.getContent()) {
			responses.add(BookmarkedWordResponse.builder()
				.id(bookmark.getId().toString())
				.word(bookmark.getWord())
				.bookmarkedAt(bookmark.getBookmarkedAt())
				.build());
		}

		return new PageImpl<>(responses, bookmarks.getPageable(), bookmarks.getTotalElements());
	}

	private String resolveFirstOriginalForm(WordSearchResponse wordSearchResponse) {
		List<String> originalForms = extractDistinctOriginalForms(wordSearchResponse);
		if (originalForms.isEmpty()) {
			throw new BookmarksException(BookmarksErrorCode.WORD_NOT_FOUND);
		}

		return originalForms.get(0);
	}

	private List<String> extractDistinctOriginalForms(WordSearchResponse wordSearchResponse) {
		return wordSearchResponse.getResults().stream().map(result -> result.getOriginalForm()).distinct().toList();
	}

}
