package com.linglevel.api.word.service;

import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.word.dto.Definition;
import com.linglevel.api.word.dto.RelatedForms;
import com.linglevel.api.word.dto.VariantType;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.dto.WordResponse;
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
                .partOfSpeech(List.of("verb"))
                .conjugations(RelatedForms.Conjugations.builder()
                        .past("ran")
                        .pastParticiple("run")
                        .presentParticiple("running")
                        .thirdPerson("runs")
                        .build())
                .definitions(List.of(
                    Definition.builder()
                        .meaningsKo(List.of("달리다", "운영하다"))
                        .meaningsJa(List.of("走る", "経営する"))
                        .examples(List.of("I run every morning.", "She runs a company."))
                        .build()
                ))
                .build();
    }

    @Test
    @DisplayName("DB에 단어가 있으면 바로 반환")
    void getOrCreateWord_단어가_DB에_있는_경우() {
        // given
        WordVariant wordVariant = WordVariant.builder()
                .word("run")
                .originalForm("run")
                .variantType(VariantType.ORIGINAL_FORM)
                .build();

        when(wordVariantRepository.findByWord("run")).thenReturn(Optional.of(wordVariant));
        when(wordRepository.findByWord("run")).thenReturn(Optional.of(sampleWord));
        when(wordBookmarkRepository.existsByUserIdAndWord(userId, "run")).thenReturn(false);

        // when
        WordResponse response = wordService.getOrCreateWord(userId, "run");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getWord()).isEqualTo("run");
        assertThat(response.getPartOfSpeech()).contains("verb");
        assertThat(response.getBookmarked()).isFalse();

        // AI 호출 없이 DB에서만 조회되었는지 확인
        verify(wordVariantRepository).findByWord("run");
        verify(wordRepository).findByWord("run");
        verify(wordAiService, never()).analyzeWord(anyString());
    }

    @Test
    @DisplayName("DB에 단어가 없으면 AI 호출 후 저장")
    void getOrCreateWord_단어가_DB에_없는_경우_AI_호출() {
        // given
        String newWord = "magnificent";

        WordAnalysisResult analysisResult = WordAnalysisResult.builder()
                .originalForm(newWord)
                .variantType(VariantType.ORIGINAL_FORM)
                .partOfSpeech(List.of("adjective"))
                .comparatives(RelatedForms.Comparatives.builder()
                        .comparative("more magnificent")
                        .superlative("most magnificent")
                        .build())
                .meaningsKo(List.of("훌륭한", "장엄한"))
                .meaningsJa(List.of("素晴らしい", "壮大な"))
                .examples(List.of("The view is magnificent."))
                .build();

        Word savedWord = Word.builder()
                .word(newWord)
                .partOfSpeech(List.of("adjective"))
                .comparatives(RelatedForms.Comparatives.builder()
                        .comparative("more magnificent")
                        .superlative("most magnificent")
                        .build())
                .definitions(List.of(
                    Definition.builder()
                        .meaningsKo(List.of("훌륭한", "장엄한"))
                        .meaningsJa(List.of("素晴らしい", "壮大な"))
                        .examples(List.of("The view is magnificent."))
                        .build()
                ))
                .build();

        when(wordVariantRepository.findByWord(newWord)).thenReturn(Optional.empty());
        when(wordAiService.analyzeWord(newWord)).thenReturn(analysisResult);
        when(wordRepository.findByWord(newWord)).thenReturn(Optional.empty()).thenReturn(Optional.of(savedWord));
        when(wordRepository.save(any(Word.class))).thenReturn(savedWord);
        when(wordBookmarkRepository.existsByUserIdAndWord(userId, newWord)).thenReturn(false);
        when(wordVariantRepository.findByWordIn(anyList())).thenReturn(List.of());

        // when
        WordResponse response = wordService.getOrCreateWord(userId, newWord);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getWord()).isEqualTo(newWord);
        assertThat(response.getPartOfSpeech()).contains("adjective");

        // AI가 호출되었는지 확인
        verify(wordAiService).analyzeWord(newWord);
        verify(wordRepository).save(any(Word.class));
        verify(wordVariantRepository).save(any(WordVariant.class));
    }

    @Test
    @DisplayName("변형 단어(과거형)를 검색하면 원형 단어 정보를 반환")
    void getOrCreateWord_변형_단어_검색() {
        // given
        String variantWord = "ran"; // run의 과거형
        WordVariant wordVariant = WordVariant.builder()
                .word(variantWord)
                .originalForm("run")
                .variantType(VariantType.PAST_TENSE)
                .build();

        when(wordVariantRepository.findByWord(variantWord)).thenReturn(Optional.of(wordVariant));
        when(wordRepository.findByWord("run")).thenReturn(Optional.of(sampleWord));
        when(wordBookmarkRepository.existsByUserIdAndWord(userId, variantWord)).thenReturn(false);

        // when
        WordResponse response = wordService.getOrCreateWord(userId, variantWord);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getWord()).isEqualTo("run"); // 원형 단어 반환
        assertThat(response.getOriginalForm()).isEqualTo("run");
        assertThat(response.getVariantType()).isNotNull();
        assertThat(response.getVariantType()).isEqualTo(VariantType.PAST_TENSE);

        // AI 호출 없이 variant 테이블과 원형 단어로 해결되었는지 확인
        verify(wordVariantRepository).findByWord(variantWord);
        verify(wordRepository).findByWord("run");
        verify(wordAiService, never()).analyzeWord(anyString());
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
    void getOrCreateWord_북마크된_단어() {
        // given
        WordVariant wordVariant = WordVariant.builder()
                .word("run")
                .originalForm("run")
                .variantType(VariantType.ORIGINAL_FORM)
                .build();

        when(wordVariantRepository.findByWord("run")).thenReturn(Optional.of(wordVariant));
        when(wordRepository.findByWord("run")).thenReturn(Optional.of(sampleWord));
        when(wordBookmarkRepository.existsByUserIdAndWord(userId, "run")).thenReturn(true);

        // when
        WordResponse response = wordService.getOrCreateWord(userId, "run");

        // then
        assertThat(response.getBookmarked()).isTrue();
    }
}

