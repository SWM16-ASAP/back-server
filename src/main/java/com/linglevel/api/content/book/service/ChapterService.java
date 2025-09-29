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

    private ChapterResponse convertToChapterResponse(Chapter chapter, String bookId, String userId) {
        // 기본값 설정
        int currentReadChunkNumber = 0;
        double progressPercentage = 0.0;

        if (userId != null) {
            BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
                .orElse(null);

            if (bookProgress != null) {
                int userCurrentChapterNumber = bookProgress.getCurrentReadChapterNumber() != null
                    ? bookProgress.getCurrentReadChapterNumber() : 0;
                int userCurrentChunkNumber = bookProgress.getCurrentReadChunkNumber() != null
                    ? bookProgress.getCurrentReadChunkNumber() : 0;

                // 챕터 진도 계산 로직
                if (chapter.getChapterNumber() < userCurrentChapterNumber) {
                    // 현재 읽고 있는 챕터 이전의 챕터들: 100% 완료
                    currentReadChunkNumber = chapter.getChunkCount();
                    progressPercentage = 100.0;
                } else if (chapter.getChapterNumber().equals(userCurrentChapterNumber)) {
                    // 현재 읽고 있는 챕터: 청크 기준 진행률 계산
                    currentReadChunkNumber = userCurrentChunkNumber;
                    if (chapter.getChunkCount() != null && chapter.getChunkCount() > 0) {
                        progressPercentage = (double) userCurrentChunkNumber / chapter.getChunkCount() * 100.0;
                    }
                } else {
                    // 현재 읽고 있는 챕터 이후의 챕터들: 0% 진행
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