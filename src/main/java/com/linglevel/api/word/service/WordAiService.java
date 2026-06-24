package com.linglevel.api.word.service;

import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import com.linglevel.api.word.model.Meaning;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WordAiService {

	private static final String PROMPT_TEMPLATE_PATH = "prompts/word-analysis.md";

	private final ChatModel chatModel;

	private final Validator validator;

	private final String promptTemplate;

	public WordAiService(ChatModel chatModel) {
		this.chatModel = chatModel;
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		this.validator = factory.getValidator();
		this.promptTemplate = loadPromptTemplate();
	}

	private String loadPromptTemplate() {
		try {
			return new ClassPathResource(PROMPT_TEMPLATE_PATH).getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to load word analysis prompt template: " + PROMPT_TEMPLATE_PATH, e);
		}
	}

	public List<WordAnalysisResult> analyzeWord(String word, String targetLanguage) {
		try {
			BeanOutputConverter<WordAnalysisResult[]> outputConverter = new BeanOutputConverter<>(
					WordAnalysisResult[].class);

			String format = outputConverter.getFormat();

			PromptTemplate promptTemplate = new PromptTemplate(this.promptTemplate);
			Prompt prompt = promptTemplate
				.create(Map.of("word", word, "targetLanguage", targetLanguage, "format", format));

			ChatResponse chatResponse = ChatClient.create(chatModel).prompt(prompt).call().chatResponse();

			String response = chatResponse.getResult().getOutput().getText();

			// 토큰 사용량 및 비용 로깅
			if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
				var usage = chatResponse.getMetadata().getUsage();
				long inputTokens = usage.getPromptTokens();
				long outputTokens = usage.getGenerationTokens();
				long totalTokens = usage.getTotalTokens();

				double inputCostUsd = (inputTokens / 1000.0) * 0.00017;
				double outputCostUsd = (outputTokens / 1000.0) * 0.000085;
				double totalCostUsd = inputCostUsd + outputCostUsd;

				// 환율: 1 USD = 1430 KRW
				double totalCostKrw = totalCostUsd * 1430;

				log.info("📊 Token Usage for word '{}': Input={}, Output={}, Total={}", word, inputTokens, outputTokens,
						totalTokens);
				log.info("💰 Cost: ${} (₩{}) = Input: ${} + Output: ${}", String.format("%.6f", totalCostUsd),
						String.format("%.2f", totalCostKrw), String.format("%.6f", inputCostUsd),
						String.format("%.6f", outputCostUsd));
			}

			// 전체 응답은 debug 레벨로만 출력 (응답이 길어서 info 레벨에서는 제외)
			log.debug("AI Response for word '{}' (target: {}): {}", word, targetLanguage, response);

			WordAnalysisResult[] results = outputConverter.convert(response);

			// Validation 수행
			for (WordAnalysisResult result : results) {
				validateResult(result, word);
			}

			// ENUM 필터링 - variantTypes와 partOfSpeech에서 유효하지 않은 값 제거 (AI 실수 방지)
			results = filterInvalidEnumValues(results, word);

			// 같은 originalForm을 가진 결과를 병합 (AI가 잘못 분리한 경우 대비)
			List<WordAnalysisResult> mergedResults = mergeDuplicateOriginalForms(results, word);

			// 빈 결과 검증 - AI가 무의미한 단어라고 판단한 경우
			if (mergedResults.isEmpty()) {
				log.info("AI returned empty result for '{}' (meaningless/gibberish word)", word);
				throw new WordsException(WordsErrorCode.WORD_IS_MEANINGLESS);
			}

			// 요약 정보 로깅
			String summary = mergedResults.stream()
				.map(r -> r.getOriginalForm() + " ("
						+ String.join(", ", r.getVariantTypes().stream().map(Enum::name).toArray(String[]::new)) + ")")
				.collect(Collectors.joining(", "));
			log.info("✅ AI analysis completed for '{}': {} result(s) - {}", word, mergedResults.size(), summary);

			return mergedResults;
		}
		catch (WordsException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Failed to analyze word '{}' with AI (target: {})", word, targetLanguage, e);
			throw new WordsException(WordsErrorCode.WORD_ANALYSIS_FAILED, e);
		}
	}

	/**
	 * AI 응답 결과의 유효성을 검증
	 */
	private void validateResult(WordAnalysisResult result, String word) {
		Set<ConstraintViolation<WordAnalysisResult>> violations = validator.validate(result);

		if (!violations.isEmpty()) {
			String errors = violations.stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.collect(Collectors.joining(", "));

			log.error("AI response validation failed for word '{}': {}", word, errors);
			throw new IllegalArgumentException("Invalid AI response for word '" + word + "': " + errors);
		}

		log.debug("AI response validation passed for word '{}'", word);
	}

	/**
	 * 같은 originalForm을 가진 결과들을 하나로 병합 AI가 프롬프트를 무시하고 같은 원형에 대해 여러 항목을 반환한 경우 처리
	 */
	private List<WordAnalysisResult> mergeDuplicateOriginalForms(WordAnalysisResult[] results, String word) {
		if (results == null || results.length == 0) {
			return List.of();
		}

		// originalForm 기준으로 그룹화
		Map<String, List<WordAnalysisResult>> groupedByOriginalForm = Arrays.stream(results)
			.collect(Collectors.groupingBy(WordAnalysisResult::getOriginalForm));

		List<WordAnalysisResult> mergedList = new ArrayList<>();

		for (Map.Entry<String, List<WordAnalysisResult>> entry : groupedByOriginalForm.entrySet()) {
			List<WordAnalysisResult> group = entry.getValue();

			if (group.size() == 1) {
				// 중복 없음 - 그대로 추가
				mergedList.add(group.get(0));
			}
			else {
				// 중복 발견 - 병합 필요
				log.warn("Merging {} duplicate entries for originalForm '{}' (input word: '{}')", group.size(),
						entry.getKey(), word);

				WordAnalysisResult merged = mergeResults(group);
				mergedList.add(merged);
			}
		}

		return mergedList;
	}

	/**
	 * variantTypes와 partOfSpeech에서 유효하지 않은 값들을 필터링 AI가 실수로 ENUM에 없는 값을 넣은 경우 제거
	 */
	private WordAnalysisResult[] filterInvalidEnumValues(WordAnalysisResult[] results, String word) {
		if (results == null || results.length == 0) {
			return results;
		}

		List<WordAnalysisResult> filteredResults = new ArrayList<>();

		for (WordAnalysisResult result : results) {
			List<com.linglevel.api.word.dto.VariantType> originalVariantTypes = result.getVariantTypes();

			if (originalVariantTypes == null || originalVariantTypes.isEmpty()) {
				log.warn("Empty variantTypes for word '{}' (originalForm: '{}')", word, result.getOriginalForm());
				continue;
			}

			// 1. 유효한 VariantType만 필터링
			List<com.linglevel.api.word.dto.VariantType> validVariantTypes = originalVariantTypes.stream()
				.filter(vt -> vt != null)
				.collect(Collectors.toList());

			if (validVariantTypes.isEmpty()) {
				log.warn("All variantTypes were invalid for word '{}' (originalForm: '{}'), skipping this result", word,
						result.getOriginalForm());
				continue;
			}

			if (validVariantTypes.size() < originalVariantTypes.size()) {
				log.warn("Filtered invalid variantTypes for word '{}' (originalForm: '{}'): {} -> {}", word,
						result.getOriginalForm(), originalVariantTypes.size(), validVariantTypes.size());
			}

			// 2. 유효한 PartOfSpeech를 가진 meanings만 필터링
			List<Meaning> originalMeanings = result.getMeanings();
			List<Meaning> validMeanings = new ArrayList<>();

			if (originalMeanings != null) {
				int invalidCount = 0;
				for (Meaning meaning : originalMeanings) {
					if (meaning.getPartOfSpeech() != null) {
						validMeanings.add(meaning);
					}
					else {
						invalidCount++;
					}
				}

				if (invalidCount > 0) {
					log.warn("Filtered {} invalid partOfSpeech(es) for word '{}' (originalForm: '{}'): {} -> {}",
							invalidCount, word, result.getOriginalForm(), originalMeanings.size(),
							validMeanings.size());
				}
			}

			if (validMeanings.isEmpty()) {
				log.warn(
						"All meanings have invalid partOfSpeech for word '{}' (originalForm: '{}'), skipping this result",
						word, result.getOriginalForm());
				continue;
			}

			// 필터링된 variantTypes와 meanings로 새 결과 생성
			WordAnalysisResult filteredResult = WordAnalysisResult.builder()
				.sourceLanguageCode(result.getSourceLanguageCode())
				.targetLanguageCode(result.getTargetLanguageCode())
				.originalForm(result.getOriginalForm())
				.variantTypes(validVariantTypes)
				.summary(result.getSummary())
				.meanings(validMeanings)
				.conjugations(result.getConjugations())
				.comparatives(result.getComparatives())
				.plural(result.getPlural())
				.build();

			filteredResults.add(filteredResult);
		}

		return filteredResults.toArray(new WordAnalysisResult[0]);
	}

	/**
	 * 같은 originalForm을 가진 여러 결과를 하나로 병합
	 */
	private WordAnalysisResult mergeResults(List<WordAnalysisResult> results) {
		if (results.isEmpty()) {
			throw new IllegalArgumentException("Cannot merge empty results");
		}

		WordAnalysisResult first = results.get(0);

		// variantTypes 병합 (중복 제거)
		List<com.linglevel.api.word.dto.VariantType> mergedVariantTypes = results.stream()
			.flatMap(r -> r.getVariantTypes().stream())
			.distinct()
			.collect(Collectors.toList());

		// meanings 병합 (중복 제거 - partOfSpeech와 meaning이 같은 것은 제외)
		List<Meaning> mergedMeanings = results.stream()
			.flatMap(r -> r.getMeanings().stream())
			.collect(Collectors.toMap(m -> m.getPartOfSpeech() + ":" + m.getMeaning(), m -> m,
					(existing, replacement) -> existing))
			.values()
			.stream()
			.collect(Collectors.toList());

		// 첫 번째 결과를 기반으로 병합된 결과 생성
		return WordAnalysisResult.builder()
			.sourceLanguageCode(first.getSourceLanguageCode())
			.targetLanguageCode(first.getTargetLanguageCode())
			.originalForm(first.getOriginalForm())
			.variantTypes(mergedVariantTypes)
			.summary(first.getSummary()) // 첫 번째 것 사용
			.meanings(mergedMeanings)
			.conjugations(first.getConjugations()) // 첫 번째 것 사용
			.comparatives(first.getComparatives()) // 첫 번째 것 사용
			.plural(first.getPlural()) // 첫 번째 것 사용
			.build();
	}

}
