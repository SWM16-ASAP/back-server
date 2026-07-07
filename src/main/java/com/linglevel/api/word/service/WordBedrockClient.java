package com.linglevel.api.word.service;

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

	public ChatResponse call(Prompt prompt) {
		return ChatClient.create(chatModel).prompt(prompt).call().chatResponse();
	}

}
