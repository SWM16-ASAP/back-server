package com.linglevel.api.word.service;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.VariantType;
import com.linglevel.api.word.dto.WordAnalysisResult;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordVariantServiceTest {

    @Mock
    private WordVariantRepository wordVariantRepository;

    @Mock
    private WordAiService wordAiService;

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
                        .build()
        ));

        // when
        List<String> originalForms = wordVariantService.getOriginalForms(word);

        // then
        assertThat(originalForms).containsExactly("see", "saw");
        verify(wordAiService, never()).analyzeWord(word, LanguageCode.KO.getCode());
    }

    @Test
    @DisplayName("DB에 없으면 AI 분석 결과의 여러 원형 후보를 모두 저장한다")
    void getOrCreateWordVariants_aiReturnsMultipleResults_savesAllVariants() {
        // given
        String word = "saw";
        List<WordAnalysisResult> analysisResults = List.of(
                WordAnalysisResult.builder()
                        .originalForm("see")
                        .variantTypes(List.of(VariantType.PAST_TENSE))
                        .sourceLanguageCode(LanguageCode.EN)
                        .targetLanguageCode(LanguageCode.KO)
                        .build(),
                WordAnalysisResult.builder()
                        .originalForm("saw")
                        .variantTypes(List.of(VariantType.ORIGINAL_FORM))
                        .sourceLanguageCode(LanguageCode.EN)
                        .targetLanguageCode(LanguageCode.KO)
                        .build()
        );

        when(wordVariantRepository.findAllByWord(word)).thenReturn(List.of());
        when(wordAiService.analyzeWord(word, LanguageCode.KO.getCode())).thenReturn(analysisResults);
        when(wordVariantRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<WordVariant> variants = wordVariantService.getOrCreateWordVariants(word);

        // then
        assertThat(variants)
                .extracting(WordVariant::getOriginalForm)
                .containsExactly("see", "saw");
        verify(wordVariantRepository).saveAll(anyList());
    }
}
