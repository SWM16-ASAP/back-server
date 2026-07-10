package com.linglevel.api.word.service;

import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WordBedrockClient {

	private final ChatModel chatModel;

	@CircuitBreaker(name = "wordBedrock", fallbackMethod = "fallback")
	public ChatResponse call(Prompt prompt) {
		return ChatClient.create(chatModel).prompt(prompt).call().chatResponse();
	}

	private ChatResponse fallback(Prompt prompt, Exception e) {
		throw new WordsException(WordsErrorCode.WORD_AI_TEMPORARILY_UNAVAILABLE, e);
	}

}
