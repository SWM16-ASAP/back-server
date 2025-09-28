package com.linglevel.api.content.common.service;

import com.linglevel.api.content.common.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 콘텐츠 타입별 정보 제공자를 관리하는 팩토리
 */
@Slf4j
@Component
public class ContentInfoProviderFactory {

    private final Map<ContentType, ContentInfoProvider> providers;

    public ContentInfoProviderFactory(List<ContentInfoProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        ContentInfoProvider::getSupportedType,
                        Function.identity()
                ));

        log.info("Registered content info providers: {}", providers.keySet());
    }

    /**
     * 콘텐츠 타입에 맞는 정보 제공자를 반환
     */
    public ContentInfoProvider getProvider(ContentType contentType) {
        ContentInfoProvider provider = providers.get(contentType);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
        return provider;
    }

    /**
     * 콘텐츠 정보를 조회
     */
    public ContentInfo getContentInfo(String contentId, ContentType contentType) {
        ContentInfoProvider provider = getProvider(contentType);
        ContentInfo contentInfo = provider.getContentInfo(contentId);

        if (!contentInfo.isPresent()) {
            throw new IllegalArgumentException("Content not found: " + contentId);
        }

        return contentInfo;
    }
}