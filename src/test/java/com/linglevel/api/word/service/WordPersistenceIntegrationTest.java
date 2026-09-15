package com.linglevel.api.word.service;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.word.dto.PartOfSpeech;
import com.linglevel.api.word.dto.VariantType;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.entity.InvalidWord;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.entity.WordVariant;
import com.linglevel.api.word.model.Meaning;
import com.linglevel.api.word.model.RelatedForms;
import com.linglevel.api.word.repository.InvalidWordRepository;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.repository.WordVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WordPersistenceService.class)
class WordPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	WordRepository words;

	@Autowired
	WordVariantRepository variants;

	@Autowired
	InvalidWordRepository invalidWords;

	@Autowired
	WordPersistenceService persistenceService;

	@Test
	void wordUniqueConstraintCoversWordTargetAndSourceLanguage() {
		words.saveAndFlush(word("run", LanguageCode.EN, LanguageCode.KO));
		Word duplicate = word("run", LanguageCode.EN, LanguageCode.KO);
		assertThatThrownBy(() -> words.saveAndFlush(duplicate))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void sameWordWithDifferentTargetLanguageIsADistinctRow() {
		words.saveAndFlush(word("run", LanguageCode.EN, LanguageCode.KO));
		words.saveAndFlush(word("run", LanguageCode.EN, LanguageCode.JA));
		assertThat(words.count()).isEqualTo(2);
	}

	@Test
	void wordVariantUniqueConstraintCoversWordAndOriginalForm() {
		variants.saveAndFlush(WordVariant.builder().word("ran").originalForm("run").build());
		WordVariant duplicate = WordVariant.builder().word("ran").originalForm("run").build();
		assertThatThrownBy(() -> variants.saveAndFlush(duplicate))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void invalidWordUniqueConstraintCoversWordAlone() {
		invalidWords.saveAndFlush(InvalidWord.builder()
			.word("asdfqwer")
			.attemptedAt(java.time.LocalDateTime.now())
			.attemptCount(1)
			.build());
		InvalidWord duplicate = InvalidWord.builder()
			.word("asdfqwer")
			.attemptedAt(java.time.LocalDateTime.now())
			.attemptCount(1)
			.build();
		assertThatThrownBy(() -> invalidWords.saveAndFlush(duplicate))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void meaningsAndRelatedFormsRoundTripAsJson() {
		Word saved = words.saveAndFlush(word("see", LanguageCode.EN, LanguageCode.KO));
		words.flush();
		Word loaded = words.findById(saved.getId()).orElseThrow();
		assertThat(loaded.getSummary()).containsExactly("보다");
		assertThat(loaded.getMeanings()).hasSize(1);
		assertThat(loaded.getMeanings().get(0).getPartOfSpeech()).isEqualTo(PartOfSpeech.VERB);
		assertThat(loaded.getRelatedForms().getConjugations().getPast()).isEqualTo("saw");
	}

	@Test
	void saveWordRecoversExistingRowInsteadOfFailingOnDuplicateInsert() {
		WordAnalysisResult analysisResult = WordAnalysisResult.builder()
			.originalForm("run")
			.sourceLanguageCode(LanguageCode.EN)
			.targetLanguageCode(LanguageCode.KO)
			.summary(List.of("달리다"))
			.meanings(List.of(Meaning.builder()
				.partOfSpeech(PartOfSpeech.VERB)
				.meaning("달리다")
				.example("I run.")
				.exampleTranslation("나는 달린다.")
				.build()))
			.build();

		Word first = persistenceService.saveWord(analysisResult);
		Word second = persistenceService.saveWord(analysisResult);

		assertThat(words.count()).isEqualTo(1);
		assertThat(second.getId()).isEqualTo(first.getId());
	}

	@Test
	void saveAnalysisResultsRecoversExistingVariantInsteadOfFailingOnDuplicateInsert() {
		WordAnalysisResult analysisResult = WordAnalysisResult.builder()
			.originalForm("run")
			.variantTypes(List.of(VariantType.ORIGINAL_FORM))
			.sourceLanguageCode(LanguageCode.EN)
			.targetLanguageCode(LanguageCode.KO)
			.summary(List.of("달리다"))
			.meanings(List.of(Meaning.builder()
				.partOfSpeech(PartOfSpeech.VERB)
				.meaning("달리다")
				.example("I run.")
				.exampleTranslation("나는 달린다.")
				.build()))
			.build();

		List<WordVariant> first = persistenceService.saveAnalysisResults("run", List.of(analysisResult),
				Optional.empty());
		List<WordVariant> second = persistenceService.saveAnalysisResults("run", List.of(analysisResult),
				Optional.empty());

		assertThat(variants.findAllByWord("run")).hasSize(1);
		assertThat(second.get(0).getId()).isEqualTo(first.get(0).getId());
	}

	@Test
	void saveInvalidWordIncrementsAttemptCountOnRepeatedFailure() {
		persistenceService.saveInvalidWord("asdfqwer");
		persistenceService.saveInvalidWord("asdfqwer");

		InvalidWord stored = invalidWords.findByWord("asdfqwer").orElseThrow();
		assertThat(stored.getAttemptCount()).isEqualTo(2);
		assertThat(invalidWords.count()).isEqualTo(1);
	}

	private Word word(String value, LanguageCode source, LanguageCode target) {
		return Word.builder()
			.word(value)
			.sourceLanguageCode(source)
			.targetLanguageCode(target)
			.summary(List.of("보다"))
			.meanings(List.of(Meaning.builder()
				.partOfSpeech(PartOfSpeech.VERB)
				.meaning("보다")
				.example("I see.")
				.exampleTranslation("나는 본다.")
				.build()))
			.relatedForms(RelatedForms.builder()
				.conjugations(RelatedForms.Conjugations.builder().past("saw").build())
				.build())
			.build();
	}

}
