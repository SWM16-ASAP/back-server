package com.linglevel.api.bookmark.service;

import com.linglevel.api.bookmark.entity.WordBookmark;
import com.linglevel.api.bookmark.exception.*;
import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Stream;

/** SQL mutations only. Resolve words through MongoDB/AI before entering this boundary. */
@Service
@RequiredArgsConstructor
public class BookmarkWriter {

	private final WordBookmarkRepository bookmarks;

	private final UserRepository users;

	@Transactional
	public void add(String userId, String word) {
		Long id = lockUser(userId);
		if (bookmarks.findForUpdate(id, word).isPresent())
			throw new BookmarksException(BookmarksErrorCode.WORD_ALREADY_BOOKMARKED);
		bookmarks.save(WordBookmark.builder().userId(id).word(word).build());
	}

	@Transactional
	public boolean toggle(String userId, String word) {
		Long id = lockUser(userId);
		var existing = bookmarks.findForUpdate(id, word);
		if (existing.isPresent()) {
			bookmarks.delete(existing.get());
			return false;
		}
		bookmarks.save(WordBookmark.builder().userId(id).word(word).build());
		return true;
	}

	@Transactional
	public void remove(String userId, String input, List<String> originalForms) {
		Long id = lockUser(userId);
		var existing = Stream.concat(Stream.of(input), originalForms.stream())
			.distinct()
			.map(word -> bookmarks.findForUpdate(id, word))
			.flatMap(java.util.Optional::stream)
			.findFirst()
			.orElseThrow(() -> new BookmarksException(BookmarksErrorCode.WORD_BOOKMARK_NOT_FOUND));
		bookmarks.delete(existing);
	}

	public enum NormalizationResult {

		UNCHANGED, UPDATED, DUPLICATE_REMOVED

	}

	@Transactional
	public NormalizationResult normalize(String userId, Long bookmarkId, String originalForm) {
		Long id = lockUser(userId);
		var existing = bookmarks.findForUpdateById(id, bookmarkId);
		if (existing.isEmpty() || existing.get().getWord().equals(originalForm))
			return NormalizationResult.UNCHANGED;
		if (bookmarks.findForUpdate(id, originalForm).isPresent()) {
			bookmarks.delete(existing.get());
			return NormalizationResult.DUPLICATE_REMOVED;
		}
		existing.get().setWord(originalForm);
		return NormalizationResult.UPDATED;
	}

	private Long lockUser(String userId) {
		Long id = Long.valueOf(userId);
		users.findForUpdateById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
		return id;
	}

}
