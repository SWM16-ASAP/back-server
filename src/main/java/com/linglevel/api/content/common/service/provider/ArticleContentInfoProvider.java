package com.linglevel.api.content.common.service.provider;

import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.service.ContentInfo;
import com.linglevel.api.content.common.service.ContentInfoProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 기사 콘텐츠 정보 제공자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleContentInfoProvider implements ContentInfoProvider {

    private final ArticleRepository articleRepository;

    @Override
    public ContentType getSupportedType() {
        return ContentType.ARTICLE;
    }

    @Override
    public ContentInfo getContentInfo(String contentId) {
        try {
            return articleRepository.findById(contentId)
                    .map(this::convertToContentInfo)
                    .orElse(ContentInfo.builder().build());
        } catch (Exception e) {
            return ContentInfo.builder().build();
        }
    }

    private ContentInfo convertToContentInfo(Article article) {
        return ContentInfo.builder()
                .title(article.getTitle())
                .author(article.getAuthor())
                .coverImageUrl(article.getCoverImageUrl())
                .readingTime(article.getReadingTime())
                .build();
    }
}