package com.linglevel.api.word.service;

import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.word.dto.Definition;
import com.linglevel.api.word.dto.VariantType;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.dto.WordResponse;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.entity.WordVariant;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.repository.WordVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordService {

    private final WordRepository wordRepository;
    private final WordBookmarkRepository wordBookmarkRepository;
    private final WordVariantRepository wordVariantRepository;
    private final WordAiService wordAiService;

    public Page<WordResponse> getWords(String userId, int page, int limit, String search) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Word> words;

        if (search != null && !search.trim().isEmpty()) {
            words = wordRepository.findByWordContainingIgnoreCase(search.trim(), pageable);
        } else {
            words = wordRepository.findAll(pageable);
        }

        List<String> wordStrings = words.getContent().stream()
                .map(Word::getWord)
                .collect(Collectors.toList());

        List<String> bookmarkedWords = wordBookmarkRepository.findByUserIdAndWordIn(userId, wordStrings, Pageable.unpaged())
                .getContent().stream()
                .map(bookmark -> bookmark.getWord())
                .collect(Collectors.toList());

        return words.map(word -> convertToResponse(word, bookmarkedWords.contains(word.getWord())));
    }

    public WordResponse getOrCreateWord(String userId, String word) {
        WordVariant wordVariant = getOrCreateWordEntity(word);
        boolean isBookmarked = wordBookmarkRepository.existsByUserIdAndWord(userId, word);

        // 원형 단어 가져오기
        Word originalWord = wordRepository.findByWord(wordVariant.getOriginalForm())
                .orElseThrow(() -> new WordsException(WordsErrorCode.WORD_NOT_FOUND));

        return convertToResponse(originalWord, isBookmarked, wordVariant.getVariantType(), wordVariant.getOriginalForm());
    }

    @Transactional
    public WordVariant getOrCreateWordEntity(String word) {

        // 1. WordVariant에서 검색 (변형 형태인지 확인)
        Optional<WordVariant> variantOpt = wordVariantRepository.findByWord(word);
        if (variantOpt.isPresent()) {
            return variantOpt.get();
        }

        // 2. DB에 없으면 AI 호출 (트랜잭션 밖에서 호출)
        log.info("Word '{}' not found in database. Calling AI to analyze...", word);
        WordAnalysisResult analysisResult = wordAiService.analyzeWord(word);

        // 3. 트랜잭션 내에서 DB 저장 처리
        return saveWordFromAnalysis(word, analysisResult);
    }

    @Transactional
    public WordVariant saveWordFromAnalysis(String word, WordAnalysisResult analysisResult) {
        // 1. 원형 단어가 이미 존재하는지 확인
        String originalForm = analysisResult.getOriginalForm();
        Word originalWord = wordRepository.findByWord(originalForm)
                .orElseGet(() -> {
                    // 원형 단어가 없으면 새로 저장
                    Word newWord = convertAnalysisResultToWord(analysisResult);
                    Word savedWord = wordRepository.save(newWord);
                    log.info("Saved new original word: {}", originalForm);
                    
                    // 변형 형태들을 WordVariant에 저장
                    saveWordVariants(savedWord);
                    
                    return savedWord;
                });

        Optional<WordVariant> existingVariant = wordVariantRepository.findByWord(word);
        if (existingVariant.isPresent()) {
            log.info("Variant already exists: {} -> {}", word, originalForm);
            return existingVariant.get();
        }

        // 2. 입력 단어를 WordVariant에 저장 (중복 체크)
        VariantType variantType = analysisResult.getVariantType() != null
                ? analysisResult.getVariantType()
                : VariantType.ORIGINAL_FORM;

        WordVariant inputVariant = createVariant(word, originalForm, variantType);
        wordVariantRepository.save(inputVariant);
        log.info("Saved input variant: {} -> {} ({})", word, originalForm, variantType);

        return inputVariant;
    }

    private Word convertAnalysisResultToWord(WordAnalysisResult result) {
        List<Definition> definitions = List.of(
                Definition.builder()
                        .meaningsKo(result.getMeaningsKo())
                        .meaningsJa(result.getMeaningsJa())
                        .examples(result.getExamples())
                        .build()
        );

        return Word.builder()
                .word(result.getOriginalForm())
                .partOfSpeech(result.getPartOfSpeech())
                .conjugations(result.getConjugations())
                .comparatives(result.getComparatives())
                .plural(result.getPlural())
                .definitions(definitions)
                .build();
    }

    @Transactional
    public void saveWordVariants(Word word) {
        List<WordVariant> variants = new ArrayList<>();

        // 동사 변형 저장
        if (word.getConjugations() != null) {
            var conj = word.getConjugations();
            if (conj.getPast() != null && !conj.getPast().equals(word.getWord())) {
                variants.add(createVariant(conj.getPast(), word.getWord(), VariantType.PAST_TENSE));
            }
            if (conj.getPastParticiple() != null && !conj.getPastParticiple().equals(word.getWord())) {
                variants.add(createVariant(conj.getPastParticiple(), word.getWord(), VariantType.PAST_PARTICIPLE));
            }
            if (conj.getPresentParticiple() != null && !conj.getPresentParticiple().equals(word.getWord())) {
                variants.add(createVariant(conj.getPresentParticiple(), word.getWord(), VariantType.PRESENT_PARTICIPLE));
            }
            if (conj.getThirdPerson() != null && !conj.getThirdPerson().equals(word.getWord())) {
                variants.add(createVariant(conj.getThirdPerson(), word.getWord(), VariantType.THIRD_PERSON));
            }
        }

        // 형용사/부사 변형 저장
        if (word.getComparatives() != null) {
            var comp = word.getComparatives();
            if (comp.getComparative() != null && !comp.getComparative().equals(word.getWord())) {
                variants.add(createVariant(comp.getComparative(), word.getWord(), VariantType.COMPARATIVE));
            }
            if (comp.getSuperlative() != null && !comp.getSuperlative().equals(word.getWord())) {
                variants.add(createVariant(comp.getSuperlative(), word.getWord(), VariantType.SUPERLATIVE));
            }
        }

        // 명사 복수형 저장
        if (word.getPlural() != null) {
            var plural = word.getPlural();
            if (plural.getPlural() != null && !plural.getPlural().equals(word.getWord())) {
                variants.add(createVariant(plural.getPlural(), word.getWord(), VariantType.PLURAL));
            }
        }

        // N+1 문제 해결: 한 번에 조회 후 필터링
        if (!variants.isEmpty()) {
            List<String> variantWords = variants.stream()
                    .map(WordVariant::getWord)
                    .collect(Collectors.toList());
            
            // 이미 존재하는 variant들을 한 번의 쿼리로 조회
            List<WordVariant> existingVariants = wordVariantRepository.findByWordIn(variantWords);
            List<String> existingWords = existingVariants.stream()
                    .map(WordVariant::getWord)
                    .collect(Collectors.toList());
            
            // 새로운 variant만 필터링하여 배치 저장
            List<WordVariant> newVariants = variants.stream()
                    .filter(variant -> !existingWords.contains(variant.getWord()))
                    .collect(Collectors.toList());
            
            if (!newVariants.isEmpty()) {
                wordVariantRepository.saveAll(newVariants);
                newVariants.forEach(variant -> 
                    log.info("Saved variant: {} -> {} ({})", variant.getWord(), variant.getOriginalForm(), variant.getVariantType())
                );
            }
        }
    }

    private WordVariant createVariant(String variantWord, String originalForm, VariantType type) {
        return WordVariant.builder()
                .word(variantWord)
                .originalForm(originalForm)
                .variantType(type)
                .build();
    }

    private WordResponse convertToResponse(Word word, boolean isBookmarked) {
        return convertToResponse(word, isBookmarked, null, null);
    }

    private WordResponse convertToResponse(Word word, boolean isBookmarked, VariantType variantType, String originalForm) {
        return WordResponse.builder()
                .id(word.getId())
                .word(word.getWord())
                .originalForm(originalForm)
                .variantType(variantType)
                .partOfSpeech(word.getPartOfSpeech())
                .definitions(word.getDefinitions())
                .relatedForms(buildRelatedForms(word))
                .bookmarked(isBookmarked)
                .build();
    }

    private com.linglevel.api.word.dto.RelatedForms buildRelatedForms(Word word) {
        return com.linglevel.api.word.dto.RelatedForms.builder()
                .conjugations(word.getConjugations())
                .comparatives(word.getComparatives())
                .plural(word.getPlural())
                .build();
    }
}