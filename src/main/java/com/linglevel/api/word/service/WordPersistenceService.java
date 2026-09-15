package com.linglevel.api.word.service;

import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.VariantType;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.entity.InvalidWord;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.entity.WordVariant;
import com.linglevel.api.word.model.RelatedForms;
import com.linglevel.api.word.repository.InvalidWordRepository;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.repository.WordVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordPersistenceService {

	private final WordRepository wordRepository;

	private final WordVariantRepository wordVariantRepository;

	private final InvalidWordRepository invalidWordRepository;

	private final PlatformTransactionManager transactionManager;

	/**
	 * MySQL 유니크 제약을 이용한 동시 삽입 경쟁 복구는 실패한 INSERT를 별도의 REQUIRES_NEW 트랜잭션에서 실행해야 한다. 같은 트랜잭션
	 * 안에서 DataIntegrityViolationException을 잡기만 하면, Hibernate가 물리 트랜잭션을 이미 rollback-only로
	 * 표시해 버려서 EntityManager.clear()로도 되돌릴 수 없고, 이후 커밋 시점에 UnexpectedRollbackException이
	 * 발생한다.
	 *
	 * 복구용 재조회도 반드시 이 메서드로 실행해야 한다 — MySQL의 기본 격리 수준인 REPEATABLE READ에서는, 바깥쪽 메서드 트랜잭션이 이미
	 * 오래 열려 있었다면 그 트랜잭션의 스냅샷이 경쟁 상대가 방금 커밋한 행을 보지 못할 수 있다. 재조회를 새 트랜잭션(새 스냅샷)에서 실행해야 항상
	 * 최신 커밋 상태를 읽는다.
	 */
	private <T> T runInNewTransaction(Supplier<T> action) {
		TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
		requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return requiresNew.execute(status -> action.get());
	}

	@Transactional
	public List<WordVariant> saveAnalysisResults(String word, List<WordAnalysisResult> analysisResults,
			Optional<InvalidWord> cachedInvalidWord) {
		List<WordVariant> savedVariants = new ArrayList<>();
		for (WordAnalysisResult analysisResult : analysisResults) {
			WordVariant savedVariant = saveWordFromAnalysis(word, analysisResult);
			savedVariants.add(savedVariant);
		}

		cachedInvalidWord.ifPresent(invalidWord -> {
			invalidWordRepository.delete(invalidWord);
			log.info("Removed word '{}' from invalid word cache after successful AI analysis (was attempt {}/3)", word,
					invalidWord.getAttemptCount());
		});

		return savedVariants;
	}

	@Transactional
	public List<WordVariant> forceSaveAnalysisResults(String word, LanguageCode targetLanguage, boolean overwrite,
			boolean deleteVariants, List<WordAnalysisResult> analysisResults) {
		if (overwrite) {
			deleteExistingWords(word, targetLanguage, deleteVariants);
		}

		List<WordVariant> savedVariants = new ArrayList<>();
		for (WordAnalysisResult analysisResult : analysisResults) {
			WordVariant savedVariant = saveWordFromAnalysis(word, analysisResult);
			savedVariants.add(savedVariant);
		}

		return savedVariants;
	}

	@Transactional
	public Word saveWord(WordAnalysisResult analysisResult) {
		Word newWord = convertAnalysisResultToWord(analysisResult);
		try {
			return runInNewTransaction(() -> wordRepository.save(newWord));
		}
		catch (DataIntegrityViolationException e) {
			return runInNewTransaction(() -> findPersistedWord(analysisResult)).orElseThrow(() -> e);
		}
	}

	private WordVariant saveWordFromAnalysis(String word, WordAnalysisResult analysisResult) {
		String originalForm = analysisResult.getOriginalForm();
		LanguageCode sourceLanguageCode = analysisResult.getSourceLanguageCode();
		LanguageCode targetLanguageCode = analysisResult.getTargetLanguageCode();

		findPersistedWord(analysisResult).orElseGet(() -> saveWordAndVariants(analysisResult));

		Optional<WordVariant> existingVariant = wordVariantRepository.findByWordAndOriginalForm(word, originalForm);
		if (existingVariant.isPresent()) {
			return existingVariant.get();
		}

		List<VariantType> variantTypes = analysisResult.getVariantTypes() != null
				&& !analysisResult.getVariantTypes().isEmpty() ? analysisResult.getVariantTypes()
						: List.of(VariantType.ORIGINAL_FORM);

		WordVariant inputVariant = createVariant(word, originalForm, variantTypes);
		return saveWordVariantRecoveringDuplicate(inputVariant);
	}

	private Word saveWordAndVariants(WordAnalysisResult analysisResult) {
		Word savedWord;
		try {
			savedWord = runInNewTransaction(() -> wordRepository.save(convertAnalysisResultToWord(analysisResult)));
		}
		catch (DataIntegrityViolationException e) {
			return runInNewTransaction(() -> findPersistedWord(analysisResult)).orElseThrow(() -> e);
		}
		saveWordVariants(savedWord);
		return savedWord;
	}

	private Optional<Word> findPersistedWord(WordAnalysisResult analysisResult) {
		return wordRepository.findByWordAndSourceLanguageCodeAndTargetLanguageCode(analysisResult.getOriginalForm(),
				analysisResult.getSourceLanguageCode(), analysisResult.getTargetLanguageCode());
	}

	private void saveWordVariants(Word word) {
		List<WordVariant> variants = new ArrayList<>();

		RelatedForms relatedForms = word.getRelatedForms();
		if (relatedForms == null) {
			return;
		}

		if (relatedForms.getConjugations() != null) {
			var conj = relatedForms.getConjugations();
			if (conj.getPast() != null && !conj.getPast().equals(word.getWord())) {
				variants.add(createVariant(conj.getPast(), word.getWord(), List.of(VariantType.PAST_TENSE)));
			}
			if (conj.getPastParticiple() != null && !conj.getPastParticiple().equals(word.getWord())) {
				variants
					.add(createVariant(conj.getPastParticiple(), word.getWord(), List.of(VariantType.PAST_PARTICIPLE)));
			}
			if (conj.getPresentParticiple() != null && !conj.getPresentParticiple().equals(word.getWord())) {
				variants.add(createVariant(conj.getPresentParticiple(), word.getWord(),
						List.of(VariantType.PRESENT_PARTICIPLE)));
			}
			if (conj.getThirdPerson() != null && !conj.getThirdPerson().equals(word.getWord())) {
				variants.add(createVariant(conj.getThirdPerson(), word.getWord(), List.of(VariantType.THIRD_PERSON)));
			}
		}

		if (relatedForms.getComparatives() != null) {
			var comp = relatedForms.getComparatives();
			if (comp.getComparative() != null && !comp.getComparative().equals(word.getWord())) {
				variants.add(createVariant(comp.getComparative(), word.getWord(), List.of(VariantType.COMPARATIVE)));
			}
			if (comp.getSuperlative() != null && !comp.getSuperlative().equals(word.getWord())) {
				variants.add(createVariant(comp.getSuperlative(), word.getWord(), List.of(VariantType.SUPERLATIVE)));
			}
		}

		if (relatedForms.getPlural() != null) {
			var plural = relatedForms.getPlural();
			if (plural.getPlural() != null && !plural.getPlural().equals(word.getWord())) {
				variants.add(createVariant(plural.getPlural(), word.getWord(), List.of(VariantType.PLURAL)));
			}
		}

		if (!variants.isEmpty()) {
			List<WordVariant> uniqueVariants = variants.stream()
				.collect(Collectors.toMap(WordVariant::getWord, variant -> variant, (existing, replacement) -> {
					List<VariantType> mergedTypes = new ArrayList<>(existing.getVariantTypes());
					replacement.getVariantTypes().forEach(type -> {
						if (!mergedTypes.contains(type)) {
							mergedTypes.add(type);
						}
					});
					existing.setVariantTypes(mergedTypes);
					return existing;
				}))
				.values()
				.stream()
				.toList();

			List<String> variantWords = uniqueVariants.stream().map(WordVariant::getWord).collect(Collectors.toList());

			List<WordVariant> existingVariants = wordVariantRepository.findByWordIn(variantWords);
			List<String> existingWords = existingVariants.stream().map(WordVariant::getWord).toList();

			List<WordVariant> newVariants = uniqueVariants.stream()
				.filter(variant -> !existingWords.contains(variant.getWord()))
				.collect(Collectors.toList());

			newVariants.forEach(this::saveWordVariantRecoveringDuplicate);
		}
	}

	private WordVariant saveWordVariantRecoveringDuplicate(WordVariant variant) {
		try {
			return runInNewTransaction(() -> wordVariantRepository.save(variant));
		}
		catch (DataIntegrityViolationException e) {
			return runInNewTransaction(
					() -> wordVariantRepository.findByWordAndOriginalForm(variant.getWord(), variant.getOriginalForm()))
				.orElseThrow(() -> e);
		}
	}

	@Transactional
	public void saveInvalidWord(String word) {
		Optional<InvalidWord> existingInvalidWord = invalidWordRepository.findByWord(word);

		if (existingInvalidWord.isPresent()) {
			InvalidWord invalidWord = existingInvalidWord.get();
			invalidWord.setAttemptCount(invalidWord.getAttemptCount() + 1);
			invalidWordRepository.save(invalidWord);
			log.info("Updated invalid word '{}' attempt count: {}", word, invalidWord.getAttemptCount());
			return;
		}

		InvalidWord invalidWord = InvalidWord.builder()
			.word(word)
			.attemptedAt(LocalDateTime.now())
			.attemptCount(1)
			.build();
		invalidWordRepository.save(invalidWord);
		log.info("Cached invalid word '{}' permanently (attempt 1/3)", word);
	}

	private void deleteExistingWords(String word, LanguageCode targetLanguage, boolean deleteVariants) {
		List<WordVariant> existingVariants = wordVariantRepository.findAllByWord(word);
		if (existingVariants.isEmpty()) {
			return;
		}

		for (WordVariant variant : existingVariants) {
			String originalForm = variant.getOriginalForm();
			wordRepository.findByWordAndTargetLanguageCode(originalForm, targetLanguage).ifPresent(wordToDelete -> {
				wordRepository.delete(wordToDelete);
			});
		}

		if (deleteVariants) {
			wordVariantRepository.deleteAll(existingVariants);
			log.info("Deleted {} existing WordVariants for word '{}' (complete reset)", existingVariants.size(), word);
			return;
		}

		log.info("Kept {} existing WordVariants for word '{}' (only Word deleted)", existingVariants.size(), word);
	}

	private Word convertAnalysisResultToWord(WordAnalysisResult result) {
		RelatedForms relatedForms = RelatedForms.builder()
			.conjugations(result.getConjugations())
			.comparatives(result.getComparatives())
			.plural(result.getPlural())
			.build();

		return Word.builder()
			.word(result.getOriginalForm())
			.sourceLanguageCode(result.getSourceLanguageCode())
			.targetLanguageCode(result.getTargetLanguageCode())
			.summary(result.getSummary())
			.meanings(result.getMeanings())
			.relatedForms(relatedForms)
			.build();
	}

	private WordVariant createVariant(String variantWord, String originalForm, List<VariantType> types) {
		return WordVariant.builder().word(variantWord).originalForm(originalForm).variantTypes(types).build();
	}

}
