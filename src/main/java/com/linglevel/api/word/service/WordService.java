package com.linglevel.api.word.service;

import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.dto.WordResponse;
import com.linglevel.api.word.dto.WordSearchResponse;
import com.linglevel.api.word.entity.InvalidWord;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.entity.WordVariant;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.mapper.WordResponseMapper;
import com.linglevel.api.word.repository.InvalidWordRepository;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.repository.WordVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WordService {

	private final WordRepository wordRepository;

	private final WordBookmarkRepository wordBookmarkRepository;

	private final WordVariantRepository wordVariantRepository;

	private final InvalidWordRepository invalidWordRepository;

	private final WordAiService wordAiService;

	private final WordSingleFlightRedisCoordinator singleFlightCoordinator;

	private final WordPersistenceService wordPersistenceService;

	private final WordResponseMapper wordResponseMapper;

	public WordSearchResponse getOrCreateWords(String userId, String word, LanguageCode targetLanguage) {
		List<WordVariant> wordVariants = getOrCreateWordEntities(word, targetLanguage);

		// 각 원형에 대한 WordResponse 생성
		List<WordResponse> results = new ArrayList<>();

		for (WordVariant wordVariant : wordVariants) {
			// 원형 단어를 targetLanguage로 번역된 것 가져오기
			Word originalWord = wordRepository
				.findByWordAndTargetLanguageCode(wordVariant.getOriginalForm(), targetLanguage)
				.orElseGet(() -> {
					return singleFlightCoordinator.execute(wordVariant.getOriginalForm(), targetLanguage, () -> {
						List<WordAnalysisResult> analysisResults = wordAiService
							.analyzeWord(wordVariant.getOriginalForm(), targetLanguage.getCode());
						return wordPersistenceService.saveWord(analysisResults.get(0));
					}, () -> wordRepository.findByWordAndTargetLanguageCode(wordVariant.getOriginalForm(),
							targetLanguage));
				});

			boolean isBookmarked = wordBookmarkRepository.existsByUserIdAndWord(userId, wordVariant.getOriginalForm());

			WordResponse response = wordResponseMapper.toWordResponse(originalWord, isBookmarked,
					wordVariant.getVariantTypes(), wordVariant.getOriginalForm());

			results.add(response);
		}

		return wordResponseMapper.toWordSearchResponse(word, results);
	}

	public List<WordVariant> getOrCreateWordEntities(String word, LanguageCode targetLanguage) {
		// 1. WordVariant에서 검색 (변형 형태인지 확인)
		List<WordVariant> existingVariants = wordVariantRepository.findAllByWord(word);
		if (!existingVariants.isEmpty()) {
			return existingVariants;
		}

		// 2. InvalidWord 캐시 확인 - 3회 유예 후 차단
		Optional<InvalidWord> cachedInvalidWord = invalidWordRepository.findByWord(word);
		int invalidAttemptCountBeforeSingleFlight = cachedInvalidWord.map(InvalidWord::getAttemptCount).orElse(0);
		if (cachedInvalidWord.isPresent()) {
			InvalidWord invalidWord = cachedInvalidWord.get();
			if (invalidWord.getAttemptCount() >= 3) {
				throw new WordsException(WordsErrorCode.WORD_IS_MEANINGLESS);
			}
		}

		// 3. DB에 없으면 AI 호출 (AI 분석 실패 시에만 InvalidWord로 캐싱)
		return singleFlightCoordinator.execute(word, targetLanguage, () -> {
			List<WordAnalysisResult> analysisResults = analyzeWordAndUpdateInvalidCache(word, targetLanguage);
			return wordPersistenceService.saveAnalysisResults(word, analysisResults, cachedInvalidWord);
		}, () -> findWordVariantsAfterSingleFlight(word, invalidAttemptCountBeforeSingleFlight));
	}

	private Optional<List<WordVariant>> findWordVariantsAfterSingleFlight(String word,
			int invalidAttemptCountBeforeSingleFlight) {
		List<WordVariant> existingVariants = wordVariantRepository.findAllByWord(word);
		if (!existingVariants.isEmpty()) {
			return Optional.of(existingVariants);
		}

		Optional<InvalidWord> currentInvalidWord = invalidWordRepository.findByWord(word);
		if (currentInvalidWord.isPresent()) {
			int currentAttemptCount = currentInvalidWord.get().getAttemptCount();

			if (currentAttemptCount >= 3 || currentAttemptCount > invalidAttemptCountBeforeSingleFlight) {
				throw new WordsException(WordsErrorCode.WORD_IS_MEANINGLESS);
			}
		}

		return Optional.empty();
	}

	private void cacheInvalidWordIfMeaningless(String word, WordsException e) {
		if (e.getErrorCode() == WordsErrorCode.WORD_IS_MEANINGLESS) {
			wordPersistenceService.saveInvalidWord(word);
		}
	}

	private List<WordAnalysisResult> analyzeWordAndUpdateInvalidCache(String word, LanguageCode targetLanguage) {
		try {
			return wordAiService.analyzeWord(word, targetLanguage.getCode());
		}
		catch (WordsException e) {
			cacheInvalidWordIfMeaningless(word, e);
			throw e;
		}
	}

	/**
	 * 관리자 전용: 단어를 AI로 강제 재분석
	 * @param word 재분석할 단어
	 * @param targetLanguage 번역 대상 언어
	 * @param overwrite true: 기존 데이터 삭제 후 재생성, false: 기존 유지 + 새로운 의미 추가
	 */
	public void forceReanalyzeWord(String word, LanguageCode targetLanguage, boolean overwrite) {
		forceReanalyzeWord(word, targetLanguage, overwrite, false);
	}

	/**
	 * 관리자 전용: 단어를 AI로 강제 재분석 (Variant 삭제 옵션 포함)
	 * @param word 재분석할 단어
	 * @param targetLanguage 번역 대상 언어
	 * @param overwrite true: 기존 데이터 삭제 후 재생성, false: 기존 유지 + 새로운 의미 추가
	 * @param deleteVariants true: Variant도 함께 삭제 (완전 초기화), false: Variant 유지 (기본값)
	 * @return WordSearchResponse
	 */
	public WordSearchResponse forceReanalyzeWord(String word, LanguageCode targetLanguage, boolean overwrite,
			boolean deleteVariants) {
		List<WordAnalysisResult> analysisResults = wordAiService.analyzeWord(word, targetLanguage.getCode());

		// 분석 결과를 DB에 저장 (빈 결과는 WordAiService에서 예외 발생, overwrite=false면 중복 체크로 인해 새로운 것만
		// 추가됨)
		List<WordVariant> savedVariants = wordPersistenceService.forceSaveAnalysisResults(word, targetLanguage,
				overwrite, deleteVariants, analysisResults);

		// 결과를 WordSearchResponse로 변환하여 반환
		// userId는 null로 전달 (어드민 API이므로 북마크 체크 불필요)
		List<WordResponse> results = new ArrayList<>();
		for (WordVariant wordVariant : savedVariants) {
			Word originalWord = wordRepository
				.findByWordAndTargetLanguageCode(wordVariant.getOriginalForm(), targetLanguage)
				.orElseThrow(() -> new WordsException(WordsErrorCode.WORD_NOT_FOUND));

			WordResponse response = wordResponseMapper.toWordResponse(originalWord, false,
					wordVariant.getVariantTypes(), wordVariant.getOriginalForm());
			results.add(response);
		}

		return wordResponseMapper.toWordSearchResponse(word, results);
	}

}
