package com.linglevel.api.word.service;

import com.linglevel.api.word.dto.VariantType;
import com.linglevel.api.word.entity.WordVariant;
import com.linglevel.api.word.repository.WordVariantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordVariantServiceTest {

	@Mock
	private WordVariantRepository wordVariantRepository;

	@InjectMocks
	private WordVariantService wordVariantService;

	@Test
	@DisplayName("기존 WordVariant가 여러 개 있으면 모두 원형 후보로 반환한다")
	void getOriginalForms_existingVariants_returnsAllOriginalForms() {
		// given
		String word = "saw";
		when(wordVariantRepository.findAllByWord(word)).thenReturn(List.of(
				WordVariant.builder()
					.word(word)
					.originalForm("see")
					.variantTypes(List.of(VariantType.PAST_TENSE))
					.build(),
				WordVariant.builder()
					.word(word)
					.originalForm("saw")
					.variantTypes(List.of(VariantType.ORIGINAL_FORM))
					.build()));

		// when
		List<String> originalForms = wordVariantService.getOriginalForms(word);

		// then
		assertThat(originalForms).containsExactly("see", "saw");
	}

	@Test
	@DisplayName("기존 WordVariant가 없으면 빈 원형 후보 목록을 반환한다")
	void getOriginalForms_noExistingVariants_returnsEmptyList() {
		// given
		String word = "saw";
		when(wordVariantRepository.findAllByWord(word)).thenReturn(List.of());

		// when
		List<String> originalForms = wordVariantService.getOriginalForms(word);

		// then
		assertThat(originalForms).isEmpty();
	}

}
