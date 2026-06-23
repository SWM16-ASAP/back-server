package com.linglevel.api.bookmark.service;

import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.service.WordService;
import com.linglevel.api.word.service.WordVariantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

	@InjectMocks
	private BookmarkService bookmarkService;

	@Test
	@DisplayName("variant 원형 후보가 없어도 입력 단어 북마크가 있으면 삭제한다")
	void removeWordBookmark_noVariantCandidate_deletesBookmarkByInputWord() {
		// given
		String userId = "user-1";
		String word = "run";
		when(wordVariantService.getOriginalForms(word)).thenReturn(List.of());
		when(wordBookmarkRepository.existsByUserIdAndWord(userId, word)).thenReturn(true);

		// when
		bookmarkService.removeWordBookmark(userId, word);

		// then
		verify(wordBookmarkRepository).deleteByUserIdAndWord(userId, word);
	}

	@Test
	@DisplayName("variant 원형 후보 중 실제 북마크된 단어를 찾아 삭제한다")
	void removeWordBookmark_variantCandidates_deletesExistingBookmarkedOriginalForm() {
		// given
		String userId = "user-1";
		String word = "ran";
		when(wordVariantService.getOriginalForms(word)).thenReturn(List.of("run"));
		when(wordBookmarkRepository.existsByUserIdAndWord(userId, word)).thenReturn(false);
		when(wordBookmarkRepository.existsByUserIdAndWord(userId, "run")).thenReturn(true);

		// when
		bookmarkService.removeWordBookmark(userId, word);

		// then
		verify(wordBookmarkRepository).deleteByUserIdAndWord(userId, "run");
	}

	@Test
	@DisplayName("입력 단어와 variant 원형 후보가 모두 북마크되어 있으면 입력 단어를 우선 삭제한다")
	void removeWordBookmark_exactBookmarkExists_deletesInputWordBeforeVariantCandidate() {
		// given
		String userId = "user-1";
		String word = "saw";
		when(wordVariantService.getOriginalForms(word)).thenReturn(List.of("see", "saw"));
		when(wordBookmarkRepository.existsByUserIdAndWord(userId, word)).thenReturn(true);

		// when
		bookmarkService.removeWordBookmark(userId, word);

		// then
		verify(wordBookmarkRepository).deleteByUserIdAndWord(userId, word);
	}

}
