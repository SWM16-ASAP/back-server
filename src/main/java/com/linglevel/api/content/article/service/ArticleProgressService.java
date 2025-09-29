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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleProgressService {

    private final ArticleService articleService;
    private final ArticleChunkService articleChunkService;
    private final ArticleProgressRepository articleProgressRepository;
    private final UserRepository userRepository;

    @Transactional
    public ArticleProgressResponse updateProgress(String articleId, ArticleProgressUpdateRequest request, String username) {
        // 아티클 존재 여부 확인
        if (!articleService.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }

        // 사용자 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsersException(UsersErrorCode.USER_NOT_FOUND));

        // chunkId로부터 chunk 정보 조회
        ArticleChunk chunk = articleChunkService.findById(request.getChunkId());

        // chunk가 해당 article에 속하는지 검증
        if (!chunk.getArticleId().equals(articleId)) {
            throw new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND_IN_ARTICLE);
        }

        String userId = user.getId();

        ArticleProgress articleProgress = articleProgressRepository.findByUserIdAndArticleId(userId, articleId)
                .orElse(new ArticleProgress());

        articleProgress.setUserId(userId);
        articleProgress.setArticleId(articleId);
        articleProgress.setChunkId(request.getChunkId());
        articleProgress.setCurrentReadChunkNumber(chunk.getChunkNumber());
        articleProgress.setUpdatedAt(LocalDateTime.now());

        articleProgressRepository.save(articleProgress);

        return convertToArticleProgressResponse(articleProgress);
    }

    @Transactional(readOnly = true)
    public ArticleProgressResponse getProgress(String articleId, String username) {
        // 아티클 존재 여부 확인
        if (!articleService.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }

        // 사용자 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsersException(UsersErrorCode.USER_NOT_FOUND));

        String userId = user.getId();

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
        newProgress.setUpdatedAt(LocalDateTime.now());

        return articleProgressRepository.save(newProgress);
    }

    private ArticleProgressResponse convertToArticleProgressResponse(ArticleProgress progress) {
        return ArticleProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .articleId(progress.getArticleId())
                .chunkId(progress.getChunkId())
                .currentReadChunkNumber(progress.getCurrentReadChunkNumber())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}