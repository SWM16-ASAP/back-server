package com.linglevel.api.content.common.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.article.entity.ArticleProgress;
import com.linglevel.api.content.article.repository.ArticleProgressRepository;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.content.article.service.ArticleChunkService;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.dto.GetRecentContentsRequest;
import com.linglevel.api.content.common.dto.RecentContentResponse;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.entity.CustomContentProgress;
import com.linglevel.api.content.custom.repository.CustomContentProgressRepository;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.content.custom.service.CustomContentChunkService;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.linglevel.api.content.article.repository.ArticleChunkRepository;
import com.linglevel.api.content.custom.repository.CustomContentChunkRepository;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final BookRepository bookRepository;
    private final ArticleRepository articleRepository;
    private final CustomContentRepository customContentRepository;
    private final BookProgressRepository bookProgressRepository;
    private final ArticleProgressRepository articleProgressRepository;
    private final CustomContentProgressRepository customContentProgressRepository;
    private final ArticleChunkService articleChunkService;
    private final CustomContentChunkService customContentChunkService;
    private final ArticleChunkRepository articleChunkRepository;
    private final CustomContentChunkRepository customContentChunkRepository;

    private record GenericProgress(String contentId, ContentType contentType, LocalDateTime lastStudiedAt, boolean isCompleted, Object originalProgress) {}

    public PageResponse<RecentContentResponse> getRecentContents(String userId, GetRecentContentsRequest request) {
        List<BookProgress> bookProgresses = bookProgressRepository.findAllByUserId(userId);
        List<ArticleProgress> articleProgresses = articleProgressRepository.findAllByUserId(userId);
        List<CustomContentProgress> customProgresses = customContentProgressRepository.findAllByUserId(userId);

        Stream<GenericProgress> genericProgressStream = Stream.concat(
            bookProgresses.stream().map(p -> new GenericProgress(p.getBookId(), ContentType.BOOK, p.getUpdatedAt(), p.getIsCompleted(), p)),
            Stream.concat(
                articleProgresses.stream().map(p -> new GenericProgress(p.getArticleId(), ContentType.ARTICLE, p.getUpdatedAt(), p.getIsCompleted(), p)),
                customProgresses.stream().map(p -> new GenericProgress(p.getCustomId(), ContentType.CUSTOM, p.getUpdatedAt(), p.getIsCompleted(), p))
            )
        );

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            boolean requiredStatus = "completed".equalsIgnoreCase(request.getStatus());
            genericProgressStream = genericProgressStream.filter(p -> p.isCompleted() == requiredStatus);
        }

        List<GenericProgress> sortedProgresses = genericProgressStream
                .sorted(Comparator.comparing(GenericProgress::lastStudiedAt).reversed())
                .toList();

        int page = request.getPage() > 0 ? request.getPage() - 1 : 0;
        int limit = request.getLimit();
        int totalCount = sortedProgresses.size();
        int fromIndex = page * limit;
        int toIndex = Math.min(fromIndex + limit, totalCount);

        if (fromIndex >= totalCount) {
            return new PageResponse<>(List.of(), request.getPage(), 0, totalCount, false, false);
        }

        List<GenericProgress> paginatedProgresses = sortedProgresses.subList(fromIndex, toIndex);

        Map<ContentType, List<String>> contentIdsByType = paginatedProgresses.stream()
                .collect(Collectors.groupingBy(GenericProgress::contentType,
                         Collectors.mapping(GenericProgress::contentId, Collectors.toList())));

        Map<String, Book> booksMap = bookRepository.findAllById(contentIdsByType.getOrDefault(ContentType.BOOK, List.of()))
                .stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        Map<String, Article> articlesMap = articleRepository.findAllById(contentIdsByType.getOrDefault(ContentType.ARTICLE, List.of()))
                .stream().collect(Collectors.toMap(Article::getId, Function.identity()));
        Map<String, CustomContent> customContentsMap = customContentRepository.findAllById(contentIdsByType.getOrDefault(ContentType.CUSTOM, List.of()))
                .stream().collect(Collectors.toMap(CustomContent::getId, Function.identity()));

        List<RecentContentResponse> result = paginatedProgresses.stream().map(p -> {
            switch (p.contentType()) {
                case BOOK: {
                    Book book = booksMap.get(p.contentId());
                    BookProgress progress = (BookProgress) p.originalProgress();
                    if (book == null) return null;
                    return RecentContentResponse.builder()
                            .contentId(book.getId()).contentType(ContentType.BOOK).title(book.getTitle()).author(book.getAuthor())
                            .coverImageUrl(book.getCoverImageUrl()).difficultyLevel(book.getDifficultyLevel().name()).tags(book.getTags())
                            .readingTime(book.getReadingTime()).chapterCount(book.getChapterCount()).currentReadChapterNumber(progress.getCurrentReadChapterNumber())
                            .progressPercentage(calculatePercentage(progress.getCurrentReadChapterNumber(), book.getChapterCount()))
                            .isCompleted(progress.getIsCompleted()).lastStudiedAt(p.lastStudiedAt()).build();
                }
                case ARTICLE: {
                    Article article = articlesMap.get(p.contentId());
                    ArticleProgress progress = (ArticleProgress) p.originalProgress();
                    if (article == null) return null;

                    Integer currentChunkNumber = 0;
                    try {
                        ArticleChunk chunk = articleChunkService.findById(progress.getChunkId());
                        currentChunkNumber = chunk.getChunkNumber();
                    } catch (Exception e) {
                        currentChunkNumber = 0;
                    }

                    DifficultyLevel difficulty = progress.getCurrentDifficultyLevel() != null ? progress.getCurrentDifficultyLevel() : article.getDifficultyLevel();
                    long totalChunks = articleChunkRepository.countByArticleIdAndDifficultyLevel(article.getId(), difficulty);

                    return RecentContentResponse.builder()
                            .contentId(article.getId()).contentType(ContentType.ARTICLE).title(article.getTitle()).author(article.getAuthor())
                            .coverImageUrl(article.getCoverImageUrl()).difficultyLevel(article.getDifficultyLevel().name()).tags(article.getTags())
                            .readingTime(article.getReadingTime()).chunkCount((int) totalChunks).currentReadChunkNumber(currentChunkNumber)
                            .progressPercentage(calculatePercentage(currentChunkNumber, (int) totalChunks))
                            .isCompleted(progress.getIsCompleted()).lastStudiedAt(p.lastStudiedAt()).build();
                }
                case CUSTOM: {
                    CustomContent custom = customContentsMap.get(p.contentId());
                    CustomContentProgress progress = (CustomContentProgress) p.originalProgress();
                    if (custom == null) return null;

                    Integer currentChunkNumber = 0;
                    try {
                        CustomContentChunk chunk = customContentChunkService.findById(progress.getChunkId());
                        currentChunkNumber = chunk.getChunkNum();
                    } catch (Exception e) {
                        currentChunkNumber = 0;
                    }

                    DifficultyLevel difficulty = progress.getCurrentDifficultyLevel() != null ? progress.getCurrentDifficultyLevel() : custom.getDifficultyLevel();
                    long totalChunks = customContentChunkRepository.countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(custom.getId(), difficulty);

                    return RecentContentResponse.builder()
                            .contentId(custom.getId()).contentType(ContentType.CUSTOM).title(custom.getTitle()).author(custom.getAuthor())
                            .coverImageUrl(custom.getCoverImageUrl()).difficultyLevel(custom.getDifficultyLevel().name()).tags(custom.getTags())
                            .readingTime(custom.getReadingTime()).chunkCount((int) totalChunks).currentReadChunkNumber(currentChunkNumber)
                            .progressPercentage(calculatePercentage(currentChunkNumber, (int) totalChunks))
                            .isCompleted(progress.getIsCompleted()).originUrl(custom.getOriginUrl()).originDomain(custom.getOriginDomain())
                            .lastStudiedAt(p.lastStudiedAt()).build();
                }
                default: return null;
            }
        }).filter(r -> r != null).collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalCount / limit);
        boolean hasNext = request.getPage() < totalPages;
        boolean hasPrevious = request.getPage() > 1;

        return new PageResponse<>(result, request.getPage(), totalPages, totalCount, hasNext, hasPrevious);
    }

    private Double calculatePercentage(Integer current, Integer total) {
        if (total == null || total == 0 || current == null) {
            return 0.0;
        }
        double percentage = ((double) current / total) * 100.0;
        return Math.round(percentage * 10.0) / 10.0; // Round to one decimal place
    }

}
