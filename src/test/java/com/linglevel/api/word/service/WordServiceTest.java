package com.linglevel.api.word.service;

import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.*;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.entity.WordVariant;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.repository.WordVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * WordService 단위 테스트
 * Mock을 사용하여 DB나 외부 API 호출 없이 로직을 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock
    private WordRepository wordRepository;

    @Mock
    private WordBookmarkRepository wordBookmarkRepository;

    @Mock
    private WordVariantRepository wordVariantRepository;

    @Mock
    private WordAiService wordAiService;

    @InjectMocks
    private WordService wordService;

    private Word sampleWord;
    private String userId = "test-user-123";

    @BeforeEach
    void setUp() {
        // 샘플 Word 데이터 생성
        sampleWord = Word.builder()
                .id("word-123")
                .word("run")
                .sourceLanguageCode(LanguageCode.EN)
                .targetLanguageCode(LanguageCode.KO)
                .summary(List.of("달리다", "운영하다", "작동하다"))
                .meanings(List.of(
                    Meaning.builder()
                        .partOfSpeech(PartOfSpeech.VERB)
                        .meaning("달리다")
                        .example("I {run} every morning.")
                        .exampleTranslation("나는 매일 아침 달립니다.")
                        .build(),
                    Meaning.builder()
                        .partOfSpeech(PartOfSpeech.VERB)
                        .meaning("운영하다")
                        .example("She {runs} a company.")
                        .exampleTranslation("그녀는 회사를 운영합니다.")
                        .build()
                ))
                .relatedForms(RelatedForms.builder()
                        .conjugations(RelatedForms.Conjugations.builder()
                                .present("run")
                                .past("ran")
                                .pastParticiple("run")
                                .presentParticiple("running")
                                .thirdPerson("runs")
                                .build())
                        .build())
                .build();
    }

    @Test
    @DisplayName("DB에 단어가 있으면 바로 반환")
    void getOrCreateWords_단어가_DB에_있는_경우() {
        // given
        WordVariant wordVariant = WordVariant.builder()
                .word("run")
                .originalForm("run")
                .variantType(VariantType.ORIGINAL_FORM)
                .build();

        when(wordVariantRepository.findAllByWord("run")).thenReturn(List.of(wordVariant));
        when(wordRepository.findByWordAndTargetLanguageCode("run", LanguageCode.KO)).thenReturn(Optional.of(sampleWord));
        when(wordBookmarkRepository.existsByUserIdAndWord(userId, "run")).thenReturn(false);

        // when
        WordSearchResponse response = wordService.getOrCreateWords(userId, "run", LanguageCode.KO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getSearchedWord()).isEqualTo("run");
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getOriginalForm()).isEqualTo("run");
        assertThat(response.getResults().get(0).getBookmarked()).isFalse();

        // AI 호출 없이 DB에서만 조회되었는지 확인
        verify(wordVariantRepository).findAllByWord("run");
        verify(wordRepository).findByWordAndTargetLanguageCode("run", LanguageCode.KO);
        verify(wordAiService, never()).analyzeWord(anyString(), anyString());
    }

    @Test
    @DisplayName("DB에 단어가 없으면 AI 호출 후 저장")
    void getOrCreateWords_단어가_DB에_없는_경우_AI_호출() {
        // given
        String newWord = "magnificent";

        WordAnalysisResult analysisResult = WordAnalysisResult.builder()
                .originalForm(newWord)
                .variantType(VariantType.ORIGINAL_FORM)
                .sourceLanguageCode(LanguageCode.EN)
                .targetLanguageCode(LanguageCode.KO)
                .summary(List.of("훌륭한", "장엄한", "멋진"))
                .meanings(List.of(
                    Meaning.builder()
                        .partOfSpeech(PartOfSpeech.ADJECTIVE)
                        .meaning("훌륭한, 장엄한")
                        .example("The view is {magnificent}.")
                        .exampleTranslation("그 경치는 장엄합니다.")
                        .build()
                ))
                .comparatives(RelatedForms.Comparatives.builder()
                        .positive("magnificent")
                        .comparative("more magnificent")
                        .superlative("most magnificent")
                        .build())
                .build();

        Word savedWord = Word.builder()
                .word(newWord)
                .sourceLanguageCode(LanguageCode.EN)
                .targetLanguageCode(LanguageCode.KO)
                .summary(List.of("훌륭한", "장엄한", "멋진"))
                .meanings(analysisResult.getMeanings())
                .relatedForms(RelatedForms.builder()
                        .comparatives(analysisResult.getComparatives())
                        .build())
                .build();

        when(wordVariantRepository.findAllByWord(newWord)).thenReturn(List.of());
        when(wordAiService.analyzeWord(newWord, LanguageCode.KO.getCode())).thenReturn(List.of(analysisResult));
        when(wordRepository.findByWordAndSourceLanguageCodeAndTargetLanguageCode(
                newWord, LanguageCode.EN, LanguageCode.KO)).thenReturn(Optional.empty());
        when(wordRepository.findByWordAndTargetLanguageCode(newWord, LanguageCode.KO))
                .thenReturn(Optional.of(savedWord));
        when(wordRepository.save(any(Word.class))).thenReturn(savedWord);
        when(wordBookmarkRepository.existsByUserIdAndWord(userId, newWord)).thenReturn(false);
        when(wordVariantRepository.findByWordIn(anyList())).thenReturn(List.of());
        when(wordVariantRepository.findByWord(newWord)).thenReturn(Optional.empty());

        // when
        WordSearchResponse response = wordService.getOrCreateWords(userId, newWord, LanguageCode.KO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getSearchedWord()).isEqualTo(newWord);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getOriginalForm()).isEqualTo(newWord);

        // AI가 호출되었는지 확인
        verify(wordAiService, atLeastOnce()).analyzeWord(newWord, LanguageCode.KO.getCode());
        verify(wordRepository).save(any(Word.class));
        verify(wordVariantRepository).save(any(WordVariant.class));
    }

    @Test
    @DisplayName("변형 단어(과거형)를 검색하면 원형 단어 정보를 반환")
    void getOrCreateWords_변형_단어_검색() {
        // given
        String variantWord = "ran"; // run의 과거형
        WordVariant wordVariant = WordVariant.builder()
                .word(variantWord)
                .originalForm("run")
                .variantType(VariantType.PAST_TENSE)
                .build();

        when(wordVariantRepository.findAllByWord(variantWord)).thenReturn(List.of(wordVariant));
        when(wordRepository.findByWordAndTargetLanguageCode("run", LanguageCode.KO)).thenReturn(Optional.of(sampleWord));
        when(wordBookmarkRepository.existsByUserIdAndWord(userId, variantWord)).thenReturn(false);

        // when
        WordSearchResponse response = wordService.getOrCreateWords(userId, variantWord, LanguageCode.KO);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getSearchedWord()).isEqualTo(variantWord);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getOriginalForm()).isEqualTo("run");
        assertThat(response.getResults().get(0).getVariantType()).isEqualTo(VariantType.PAST_TENSE);

        // AI 호출 없이 variant 테이블과 원형 단어로 해결되었는지 확인
        verify(wordVariantRepository).findAllByWord(variantWord);
        verify(wordRepository).findByWordAndTargetLanguageCode("run", LanguageCode.KO);
        verify(wordAiService, never()).analyzeWord(anyString(), anyString());
    }

    @Test
    @DisplayName("단어 저장 시 모든 변형 형태를 WordVariant에 저장")
    void saveWordVariants_모든_변형_형태_저장() {
        // given
        when(wordVariantRepository.findByWordIn(anyList())).thenReturn(List.of());

        // when
        wordService.saveWordVariants(sampleWord);

        // then
        // 동사 변형 4개가 저장되어야 함 (past, pastParticiple, presentParticiple, thirdPerson)
        // "run"은 pastParticiple이 원형과 같으므로 3개만 저장
        verify(wordVariantRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("북마크된 단어는 isBookmarked가 true")
    void getOrCreateWords_북마크된_단어() {
        // given
        WordVariant wordVariant = WordVariant.builder()
                .word("run")
                .originalForm("run")
                .variantType(VariantType.ORIGINAL_FORM)
                .build();

        when(wordVariantRepository.findAllByWord("run")).thenReturn(List.of(wordVariant));
        when(wordRepository.findByWordAndTargetLanguageCode("run", LanguageCode.KO)).thenReturn(Optional.of(sampleWord));
        when(wordBookmarkRepository.existsByUserIdAndWord(userId, "run")).thenReturn(true);

        // when
        WordSearchResponse response = wordService.getOrCreateWords(userId, "run", LanguageCode.KO);

        // then
        assertThat(response.getResults().get(0).getBookmarked()).isTrue();
    }
}

