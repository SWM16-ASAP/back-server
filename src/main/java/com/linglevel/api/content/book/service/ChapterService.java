package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.ChapterResponse;
import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.ProgressStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final BookProgressRepository bookProgressRepository;
    private final ChunkRepository chunkRepository;
    private final UserRepository userRepository;
    private final BookService bookService;

    public PageResponse<ChapterResponse> getChapters(String bookId, GetChaptersRequest request, String username) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(
            request.getPage() - 1,
            request.getLimit(),
            Sort.by("chapterNumber").ascending()
        );

        Page<Chapter> chapterPage = chapterRepository.findByBookId(bookId, pageable);
        
        String userId = getUserId(username);

        List<ChapterResponse> chapterResponses = chapterPage.getContent().stream()
            .map(chapter -> convertToChapterResponse(chapter, bookId, userId))
            .collect(Collectors.toList());

        if (request.getProgress() != null && userId != null) {
            chapterResponses = filterByProgress(chapterResponses, request.getProgress());
        }

        return new PageResponse<>(chapterResponses, chapterPage);
    }

    public ChapterResponse getChapter(String bookId, String chapterId, String username) {
        if (!bookService.existsById(bookId)) {
            throw new BooksException(BooksErrorCode.BOOK_NOT_FOUND);
        }

        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));

        if (!bookId.equals(chapter.getBookId())) {
            throw new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK);
        }

        String userId = getUserId(username);
        return convertToChapterResponse(chapter, bookId, userId);
    }

    public boolean existsById(String chapterId) {
        return chapterRepository.existsById(chapterId);
    }

    public Chapter findById(String chapterId) {
        return chapterRepository.findById(chapterId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));
    }

    public Chapter findFirstByBookId(String bookId) {
        return chapterRepository.findFirstByBookIdOrderByChapterNumberAsc(bookId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.CHAPTER_NOT_FOUND));
    }

    private String getUserId(String username) {
        if (username == null) return null;
        return userRepository.findByUsername(username)
            .map(User::getId)
            .orElse(null);
    }

    private List<ChapterResponse> filterByProgress(List<ChapterResponse> chapterResponses, ProgressStatus progressFilter) {
        if (progressFilter == null) {
            return chapterResponses;
        }

        return chapterResponses.stream()
            .filter(chapter -> {
                return switch (progressFilter) {
                    case NOT_STARTED -> chapter.getProgressPercentage() == 0.0;
                    case IN_PROGRESS -> chapter.getProgressPercentage() > 0.0 && chapter.getProgressPercentage() < 100.0;
                    case COMPLETED -> chapter.getProgressPercentage() == 100.0;
                };
            })
            .collect(Collectors.toList());
    }

    private ChapterResponse convertToChapterResponse(Chapter chapter, String bookId, String userId) {
        int currentReadChunkNumber = 0;
        double progressPercentage = 0.0;

        if (userId != null) {
            BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElse(null);

            if (bookProgress != null) {
                Integer currentChapterNumber = bookProgress.getCurrentReadChapterNumber() != null
                    ? bookProgress.getCurrentReadChapterNumber() : 0;
                Integer currentChunkNumber = bookProgress.getCurrentReadChunkNumber() != null
                    ? bookProgress.getCurrentReadChunkNumber() : 0;

                if (chapter.getChapterNumber() < currentChapterNumber) {
                    // 현재 읽고 있는 챕터보다 이전 → 100% (이미 지나감)
                    currentReadChunkNumber = chapter.getChunkCount();
                    progressPercentage = 100.0;
                } else if (chapter.getChapterNumber().equals(currentChapterNumber)) {
                    // 현재 읽고 있는 챕터 → 백분율 계산
                    currentReadChunkNumber = currentChunkNumber;
                    if (chapter.getChunkCount() != null && chapter.getChunkCount() > 0) {
                        progressPercentage = (double) currentChunkNumber / chapter.getChunkCount() * 100.0;
                    }
                } else {
                    // 아직 안 읽은 챕터 → 0%
                    currentReadChunkNumber = 0;
                    progressPercentage = 0.0;
                }
            }
        }

        return ChapterResponse.builder()
            .id(chapter.getId())
            .chapterNumber(chapter.getChapterNumber())
            .title(chapter.getTitle())
            .chapterImageUrl(chapter.getChapterImageUrl())
            .description(chapter.getDescription())
            .chunkCount(chapter.getChunkCount())
            .currentReadChunkNumber(currentReadChunkNumber)
            .progressPercentage(progressPercentage)
            .readingTime(chapter.getReadingTime())
            .build();
    }
}
