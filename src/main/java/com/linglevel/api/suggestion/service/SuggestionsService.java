package com.linglevel.api.suggestion.service;

import com.linglevel.api.common.dto.DiscordWebhookRequest;
import com.linglevel.api.suggestion.dto.SuggestionRequest;
import com.linglevel.api.suggestion.dto.SuggestionResponse;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SuggestionsService {

    private final String webhookUrl;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    public SuggestionsService(@Value("${discord.webhook.suggestion.url}") String webhookUrl,
                              RestTemplate restTemplate,
                              UserRepository userRepository) {
        this.webhookUrl = webhookUrl;
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
    }

    public SuggestionResponse saveSuggestion(SuggestionRequest request, String username) {
        User user = userRepository.findByUsername(username).orElse(null);

        String userInfo;
        if (user != null) {
            String accountEmail = user.getEmail() != null ? user.getEmail() : "계정 이메일 없음";
            String inputEmail = request.getEmail() != null && !request.getEmail().equals("익명") ? request.getEmail() : "입력 이메일 없음";
            userInfo = user.getId() + " | " + accountEmail;
        } else {
            userInfo = "사용자 정보 없음";
        }

        String message = "**" + request.getTags() + "**(" + request.getEmail() + ")\n"
                + "> " + userInfo + "\n"
                + "```" + request.getContent() + "```";

        DiscordWebhookRequest discordRequest = new DiscordWebhookRequest(message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DiscordWebhookRequest> requestEntity = new HttpEntity<>(discordRequest, headers);

        restTemplate.postForEntity(webhookUrl, requestEntity, String.class);

        return new SuggestionResponse("Suggestion submitted successfully.");
    }
}
