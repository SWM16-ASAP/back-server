package com.linglevel.api.bookmark.service;

import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.service.WordService;
import com.linglevel.api.word.service.WordVariantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import com.linglevel.api.bookmark.entity.WordBookmark;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.user.entity.User;
import java.util.Optional;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

	@Mock
	private WordBookmarkRepository wordBookmarkRepository;

	@Mock
	private WordRepository wordRepository;

	@Mock
	private WordVariantService wordVariantService;

	@Mock
	private WordService wordService;

	@Mock
	private UserRepository users;

	private BookmarkService bookmarkService;

	@BeforeEach
	void setUp() {
		when(users.findForUpdateById(1L)).thenReturn(Optional.of(new User()));
		bookmarkService = new BookmarkService(wordBookmarkRepository, wordRepository, wordVariantService, wordService,
				new BookmarkWriter(wordBookmarkRepository, users));
	}

	@Test
	@DisplayName("variant 원형 후보가 없어도 입력 단어 북마크가 있으면 삭제한다")
	void removeWordBookmark_noVariantCandidate_deletesBookmarkByInputWord() {
		// given
		String userId = "1";
		String word = "run";
		when(wordVariantService.getOriginalForms(word)).thenReturn(List.of());
		WordBookmark existing = WordBookmark.builder().userId(1L).word(word).build();
		when(wordBookmarkRepository.findForUpdate(1L, word)).thenReturn(Optional.of(existing));

		// when
		bookmarkService.removeWordBookmark(userId, word);

		// then
		verify(wordBookmarkRepository).delete(existing);
	}

	@Test
	@DisplayName("variant 원형 후보 중 실제 북마크된 단어를 찾아 삭제한다")
	void removeWordBookmark_variantCandidates_deletesExistingBookmarkedOriginalForm() {
		// given
		String userId = "1";
		String word = "ran";
		when(wordVariantService.getOriginalForms(word)).thenReturn(List.of("run"));
		when(wordBookmarkRepository.findForUpdate(1L, word)).thenReturn(Optional.empty());
		WordBookmark existing = WordBookmark.builder().userId(1L).word("run").build();
		when(wordBookmarkRepository.findForUpdate(1L, "run")).thenReturn(Optional.of(existing));

		// when
		bookmarkService.removeWordBookmark(userId, word);

		// then
		verify(wordBookmarkRepository).delete(existing);
	}

	@Test
	@DisplayName("입력 단어와 variant 원형 후보가 모두 북마크되어 있으면 입력 단어를 우선 삭제한다")
	void removeWordBookmark_exactBookmarkExists_deletesInputWordBeforeVariantCandidate() {
		// given
		String userId = "1";
		String word = "saw";
		when(wordVariantService.getOriginalForms(word)).thenReturn(List.of("see", "saw"));
		WordBookmark existing = WordBookmark.builder().userId(1L).word(word).build();
		when(wordBookmarkRepository.findForUpdate(1L, word)).thenReturn(Optional.of(existing));

		// when
		bookmarkService.removeWordBookmark(userId, word);

		// then
		verify(wordBookmarkRepository).delete(existing);
	}

}
