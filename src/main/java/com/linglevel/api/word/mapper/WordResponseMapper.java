package com.linglevel.api.word.mapper;

import com.linglevel.api.word.dto.VariantType;
import com.linglevel.api.word.dto.WordResponse;
import com.linglevel.api.word.dto.WordSearchResponse;
import com.linglevel.api.word.entity.Word;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WordResponseMapper {

	public WordResponse toWordResponse(Word word, boolean bookmarked, List<VariantType> variantTypes,
			String originalForm) {
		return WordResponse.builder()
			.id(word.getId())
			.originalForm(originalForm)
			.variantTypes(variantTypes)
			.sourceLanguageCode(word.getSourceLanguageCode())
			.targetLanguageCode(word.getTargetLanguageCode())
			.summary(word.getSummary())
			.meanings(word.getMeanings())
			.relatedForms(word.getRelatedForms())
			.bookmarked(bookmarked)
			.isEssential(word.getIsEssential())
			.build();
	}

	public WordSearchResponse toWordSearchResponse(String searchedWord, List<WordResponse> results) {
		return WordSearchResponse.builder().searchedWord(searchedWord).results(results).build();
	}

}
