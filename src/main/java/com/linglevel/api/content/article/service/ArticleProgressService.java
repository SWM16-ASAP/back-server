package com.linglevel.api.content.article.service;

import com.linglevel.api.content.article.dto.ArticleProgressResponse;
import com.linglevel.api.content.article.dto.ArticleProgressUpdateRequest;
import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.article.entity.ArticleProgress;
import com.linglevel.api.content.article.exception.ArticleErrorCode;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.repository.ArticleProgressRepository;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.exception.UsersErrorCode;
import com.linglevel.api.user.exception.UsersException;
import com.linglevel.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleProgressService {

    private final ArticleService articleService;
    private final ArticleChunkService articleChunkService;
    private final ArticleProgressRepository articleProgressRepository;

    @Transactional
    public ArticleProgressResponse updateProgress(String articleId, ArticleProgressUpdateRequest request, String userId) {
        // 아티클 존재 여부 확인
        if (!articleService.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }

        // chunkId로부터 chunk 정보 조회
        ArticleChunk chunk = articleChunkService.findById(request.getChunkId());

        // chunk가 해당 article에 속하는지 검증
        if (chunk.getArticleId() == null || !chunk.getArticleId().equals(articleId)) {
            throw new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND_IN_ARTICLE);
        }

        ArticleProgress articleProgress = articleProgressRepository.findByUserIdAndArticleId(userId, articleId)
                .orElse(new ArticleProgress());

        // Null 체크
        if (chunk.getChunkNumber() == null) {
            throw new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND);
        }

        articleProgress.setUserId(userId);
        articleProgress.setArticleId(articleId);
        articleProgress.setChunkId(request.getChunkId());
        articleProgress.setCurrentReadChunkNumber(chunk.getChunkNumber());

        // max 진도 업데이트 (current가 max보다 크면 max도 업데이트)
        if (articleProgress.getMaxReadChunkNumber() == null ||
            chunk.getChunkNumber() > articleProgress.getMaxReadChunkNumber()) {
            articleProgress.setMaxReadChunkNumber(chunk.getChunkNumber());
        }

        // 완료 조건 자동 체크 (한번 true가 되면 계속 유지)
        var article = articleService.findById(articleId);
        boolean isCompleted = chunk.getChunkNumber() >= article.getChunkCount();
        articleProgress.setIsCompleted(articleProgress.getIsCompleted() != null && articleProgress.getIsCompleted() || isCompleted);

        articleProgressRepository.save(articleProgress);

        return convertToArticleProgressResponse(articleProgress);
    }

    @Transactional(readOnly = true)
    public ArticleProgressResponse getProgress(String articleId, String userId) {
        // 아티클 존재 여부 확인
        if (!articleService.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }

        ArticleProgress articleProgress = articleProgressRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseGet(() -> initializeProgress(userId, articleId));

        return convertToArticleProgressResponse(articleProgress);
    }

    private ArticleProgress initializeProgress(String userId, String articleId) {
        // 첫 번째 청크로 초기화
        ArticleChunk firstChunk = articleChunkService.findFirstByArticleId(articleId);

        ArticleProgress newProgress = new ArticleProgress();
        newProgress.setUserId(userId);
        newProgress.setArticleId(articleId);
        newProgress.setChunkId(firstChunk.getId());
        newProgress.setCurrentReadChunkNumber(firstChunk.getChunkNumber());
        newProgress.setMaxReadChunkNumber(firstChunk.getChunkNumber());
        // updatedAt은 @LastModifiedDate에 의해 자동 설정됨

        return articleProgressRepository.save(newProgress);
    }

    @Transactional
    public void deleteProgress(String articleId, String userId) {
        if (!articleService.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }

        ArticleProgress articleProgress = articleProgressRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.PROGRESS_NOT_FOUND));

        articleProgressRepository.delete(articleProgress);
    }

    private ArticleProgressResponse convertToArticleProgressResponse(ArticleProgress progress) {
        return ArticleProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .articleId(progress.getArticleId())
                .chunkId(progress.getChunkId())
                .currentReadChunkNumber(progress.getCurrentReadChunkNumber())
                .maxReadChunkNumber(progress.getMaxReadChunkNumber())
                .isCompleted(progress.getIsCompleted())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}