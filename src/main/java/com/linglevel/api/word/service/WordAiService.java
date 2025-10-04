package com.linglevel.api.word.service;

import com.linglevel.api.word.dto.Definition;
import com.linglevel.api.word.dto.WordAnalysisResult;
import com.linglevel.api.word.entity.Word;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WordAiService {

    private final ChatModel chatModel;

    public WordAiService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    private static final String PROMPT_TEMPLATE = """
            Analyze the English word: {word}

            Provide detailed information about this word including:
            - Original form: The base/dictionary form of the word following these rules:
              * For verbs: base form/infinitive (e.g., "run" for "ran", "running", "runs")
              * For adjectives: positive form (e.g., "beautiful" for "more beautiful", "most beautiful")
              * For nouns: singular form (e.g., "bank" for "banks", "bank's")
              * If the input is already in the base form, return the same word
            - Variant type: Identify which type the input word is from: ORIGINAL_FORM (if it's the base form), PAST_TENSE, PAST_PARTICIPLE, PRESENT_PARTICIPLE, THIRD_PERSON, COMPARATIVE, SUPERLATIVE, PLURAL, or UNDEFINED (for possessives like "bank's" or other inflections)
            - Part of speech (can be multiple if the word has different uses)
            - Word forms based on part of speech:
              * If verb: provide conjugations (past, pastParticiple, presentParticiple, thirdPerson)
              * If adjective: provide comparatives (comparative, superlative)
              * If noun: provide plural form
            - Korean translations: For each part of speech, provide 2-3 representative meanings
            - Japanese translations: For each part of speech, provide 2-3 representative meanings
            - Simple English example sentences (3-5 examples covering different parts of speech)

            {format}
            """;

    public WordAnalysisResult analyzeWord(String word) {
        try {
            BeanOutputConverter<WordAnalysisResult> outputConverter =
                new BeanOutputConverter<>(WordAnalysisResult.class);

            String format = outputConverter.getFormat();

            PromptTemplate promptTemplate = new PromptTemplate(PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                "word", word,
                "format", format
            ));

            String response = ChatClient.create(chatModel)
                    .prompt(prompt)
                    .call()
                    .content();

            log.info("AI Response for word '{}': {}", word, response);

            WordAnalysisResult result = outputConverter.convert(response);

            return result;
        } catch (Exception e) {
            log.error("Failed to analyze word '{}' with AI", word, e);
            throw new RuntimeException("AI word analysis failed for word: " + word, e);
        }
    }
}
