package com.linglevel.api.word.service;

import com.linglevel.api.word.exception.WordsErrorCode;
import com.linglevel.api.word.exception.WordsException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.Timer;
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

	private final WordGenerationMetrics metrics;

	@CircuitBreaker(name = "wordBedrock", fallbackMethod = "fallback")
	public ChatResponse call(Prompt prompt) {
		Timer.Sample sample = metrics.startBedrockCall();
		try {
			ChatResponse response = ChatClient.create(chatModel).prompt(prompt).call().chatResponse();
			metrics.recordBedrockCall(sample, "success");
			return response;
		}
		catch (RuntimeException | Error e) {
			metrics.recordBedrockCall(sample, "error");
			throw e;
		}
	}

	private ChatResponse fallback(Prompt prompt, Exception e) {
		if (e instanceof CallNotPermittedException) {
			metrics.recordBedrockRejected();
		}
		throw new WordsException(WordsErrorCode.WORD_AI_TEMPORARILY_UNAVAILABLE, e);
	}

}
