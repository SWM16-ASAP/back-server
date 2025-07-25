package com.linglevel.api.suggestions.service;

import com.linglevel.api.common.dto.DiscordWebhookRequest;
import com.linglevel.api.suggestions.dto.SuggestionRequest;
import com.linglevel.api.suggestions.dto.SuggestionResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SuggestionsService {

    private final String webhookUrl;
    private final RestTemplate restTemplate;

    public SuggestionsService(@Value("${discord.webhook.suggestion.url}") String webhookUrl,
                              RestTemplate restTemplate) {
        this.webhookUrl = webhookUrl;
        this.restTemplate = restTemplate;
    }

    public SuggestionResponse saveSuggestion(SuggestionRequest request) {
        String message = "> **" + request.getTags() + "**(" + request.getEmail() + ")" + "\n"
                + "```" + request.getContent() + "```";

        DiscordWebhookRequest discordRequest = new DiscordWebhookRequest(message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DiscordWebhookRequest> requestEntity = new HttpEntity<>(discordRequest, headers);

        restTemplate.postForEntity(webhookUrl, requestEntity, String.class);

        return new SuggestionResponse("Suggestion submitted successfully.");
    }
}
