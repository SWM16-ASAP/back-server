package com.linglevel.api.admin.service;

import com.linglevel.api.admin.dto.UpdateChunkRequest;
import com.linglevel.api.content.article.dto.ArticleChunkResponse;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.article.exception.ArticleErrorCode;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.repository.ArticleChunkRepository;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.content.book.dto.ChunkResponse;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.s3.service.S3StaticService;
import com.linglevel.api.s3.strategy.ArticlePathStrategy;
import com.linglevel.api.s3.strategy.BookPathStrategy;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final ChunkRepository chunkRepository;
    private final ChapterRepository chapterRepository;
    private final BookRepository bookRepository;
    private final BookProgressRepository bookProgressRepository;
    private final ArticleRepository articleRepository;
    private final ArticleChunkRepository articleChunkRepository;
    private final S3StaticService s3StaticService;
    private final BookPathStrategy bookPathStrategy;
    private final ArticlePathStrategy articlePathStrategy;
    private final DailyCompletionRepository dailyCompletionRepository;
    private final UserStudyReportRepository userStudyReportRepository;

    public ChunkResponse updateBookChunk(String bookId, String chapterId, String chunkId, UpdateChunkRequest request) {
        log.info("Updating book chunk - bookId: {}, chapterId: {}, chunkId: {}", bookId, chapterId, chunkId);

        // 책 존재 확인
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BooksException(BooksErrorCode.BOOK_NOT_FOUND));

        // 챕터 존재 확인
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));

        // 챕터가 해당 책에 속하는지 확인
        if (!chapter.getBookId().equals(bookId)) {
            throw new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND);
        }

        // 청크 존재 확인
        Chunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new BooksException(BooksErrorCode.CHUNK_NOT_FOUND));

        // 청크가 해당 챕터에 속하는지 확인
        if (!chunk.getChapterId().equals(chapterId)) {
            throw new BooksException(BooksErrorCode.CHUNK_NOT_FOUND);
        }

        // 청크 내용 수정
        chunk.updateContent(request.getContent(), request.getDescription());
        Chunk updatedChunk = chunkRepository.save(chunk);

        log.info("Book chunk updated successfully - chunkId: {}", chunkId);

        return ChunkResponse.from(updatedChunk);
    }

    public ArticleChunkResponse updateArticleChunk(String articleId, String chunkId, UpdateChunkRequest request) {
        log.info("Updating article chunk - articleId: {}, chunkId: {}", articleId, chunkId);

        // 기사 존재 확인
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));

        // 청크 존재 확인
        ArticleChunk chunk = articleChunkRepository.findById(chunkId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND));

        // 청크가 해당 기사에 속하는지 확인
        if (!chunk.getArticleId().equals(articleId)) {
            throw new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND);
        }

        // 청크 내용 수정
        chunk.updateContent(request.getContent(), request.getDescription());
        ArticleChunk updatedChunk = articleChunkRepository.save(chunk);

        log.info("Article chunk updated successfully - chunkId: {}", chunkId);

        return ArticleChunkResponse.from(updatedChunk);
    }

    public void deleteBook(String bookId) {
        log.info("Starting book deletion - bookId: {}", bookId);

        // 책 존재 확인
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BooksException(BooksErrorCode.BOOK_NOT_FOUND));

        try {
            // 1. S3에서 책 관련 파일들 삭제
            s3StaticService.deleteFiles(bookId, bookPathStrategy);
            log.info("S3 book files deleted successfully - bookId: {}", bookId);

            // 2. 책과 관련된 진도 기록 삭제
            List<BookProgress> progressList = bookProgressRepository.findByBookId(bookId);
            if (!progressList.isEmpty()) {
                bookProgressRepository.deleteAll(progressList);
                log.info("Book progress records deleted - count: {}", progressList.size());
            }

            // 3. 청크 삭제
            List<Chapter> chapters = chapterRepository.findByBookIdOrderByChapterNumber(bookId);
            for (Chapter chapter : chapters) {
                List<Chunk> chunks = chunkRepository.findByChapterIdOrderByChunkNumber(chapter.getId());
                if (!chunks.isEmpty()) {
                    chunkRepository.deleteAll(chunks);
                    log.info("Chunks deleted for chapter - chapterId: {}, count: {}", chapter.getId(), chunks.size());
                }
            }

            // 4. 챕터 삭제
            if (!chapters.isEmpty()) {
                chapterRepository.deleteAll(chapters);
                log.info("Chapters deleted - count: {}", chapters.size());
            }

            // 5. 책 삭제
            bookRepository.delete(book);
            log.info("Book deleted successfully - bookId: {}", bookId);

        } catch (Exception e) {
            log.error("Error during book deletion - bookId: {}, error: {}", bookId, e.getMessage(), e);
            throw new BooksException(BooksErrorCode.BOOK_DELETION_FAILED);
        }
    }

    public void deleteArticle(String articleId) {
        log.info("Starting article deletion - articleId: {}", articleId);

        // 기사 존재 확인
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));

        try {
            // 1. S3에서 기사 관련 파일들 삭제
            s3StaticService.deleteFiles(articleId, articlePathStrategy);
            log.info("S3 article files deleted successfully - articleId: {}", articleId);

            // 2. 기사 청크 삭제
            List<ArticleChunk> chunks = articleChunkRepository.findByArticleIdAndDifficultyLevelOrderByChunkNumber(
                    articleId, article.getDifficultyLevel(), PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            if (!chunks.isEmpty()) {
                articleChunkRepository.deleteAll(chunks);
                log.info("Article chunks deleted - count: {}", chunks.size());
            }

            // 3. 기사 삭제
            articleRepository.delete(article);
            log.info("Article deleted successfully - articleId: {}", articleId);

        } catch (Exception e) {
            log.error("Error during article deletion - articleId: {}, error: {}", articleId, e.getMessage(), e);
            throw new ArticleException(ArticleErrorCode.ARTICLE_DELETION_FAILED);
        }
    }

    public void resetTodayStreak(String userId) {
        log.info("Admin resetting today's streak for user: {}", userId);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        dailyCompletionRepository.findByUserIdAndCompletionDate(userId, today)
                .ifPresent(todayCompletion -> {
                    List<String> todayContentIds = todayCompletion.getCompletedContents() != null
                            ? todayCompletion.getCompletedContents().stream()
                                .map(c -> c.getContentId())
                                .toList()
                            : List.of();

                    dailyCompletionRepository.delete(todayCompletion);
                    log.info("Deleted today's DailyCompletion for user: {}", userId);

                    userStudyReportRepository.findByUserId(userId).ifPresent(report -> {
                        if (report.getCompletedContentIds() != null && !todayContentIds.isEmpty()) {
                            report.getCompletedContentIds().removeAll(todayContentIds);
                        }

                        if (report.getLastCompletionDate() != null && report.getLastCompletionDate().isEqual(today)) {
                            dailyCompletionRepository.findTopByUserIdAndCompletionDateBeforeOrderByCompletionDateDesc(userId, today)
                                    .ifPresentOrElse(
                                            lastCompletion -> {
                                                report.setLastCompletionDate(lastCompletion.getCompletionDate());
                                                report.setCurrentStreak(lastCompletion.getStreakCount() != null ? lastCompletion.getStreakCount() : 0);
                                            },
                                            () -> {
                                                report.setLastCompletionDate(null);
                                                report.setCurrentStreak(0);
                                                report.setStreakStartDate(null);
                                            }
                                    );
                        }

                        userStudyReportRepository.save(report);
                        log.info("UserStudyReport reverted for user: {}", userId);
                    });
                });
    }
}