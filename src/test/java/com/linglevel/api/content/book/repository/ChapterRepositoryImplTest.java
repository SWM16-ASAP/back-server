package com.linglevel.api.content.book.repository;

import com.linglevel.api.common.AbstractDatabaseTest;
import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.common.ProgressStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(ChapterRepositoryImpl.class)
class ChapterRepositoryImplTest extends AbstractDatabaseTest {

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private BookProgressRepository bookProgressRepository;

    private static final String BOOK_ID = "book-1";
    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        bookProgressRepository.deleteAll();
        chapterRepository.deleteAll();

        chapterRepository.saveAll(List.of(
            createChapter(1, "Chapter 1"),
            createChapter(2, "Chapter 2"),
            createChapter(3, "Chapter 3")
        ));
    }

    @Test
    @DisplayName("진도 정보가 없으면 NOT_STARTED 필터는 모든 챕터를 반환한다")
    void findChaptersWithFilters_returnsAllChaptersWhenNoProgress() {
        GetChaptersRequest request = GetChaptersRequest.builder()
            .progress(ProgressStatus.NOT_STARTED)
            .build();

        Page<Chapter> result = chapterRepository.findChaptersWithFilters(BOOK_ID, request, USER_ID, defaultPageable());

        assertThat(result.getContent()).extracting(Chapter::getChapterNumber).containsExactly(1, 2, 3);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("V3 chapterProgresses 기준으로 IN_PROGRESS와 COMPLETED를 구분한다")
    void findChaptersWithFilters_usesV3ChapterProgresses() {
        BookProgress progress = new BookProgress();
        progress.setUserId(USER_ID);
        progress.setBookId(BOOK_ID);
        progress.setChapterProgresses(List.of(
            BookProgress.ChapterProgressInfo.builder()
                .chapterNumber(1)
                .progressPercentage(100.0)
                .isCompleted(true)
                .build(),
            BookProgress.ChapterProgressInfo.builder()
                .chapterNumber(2)
                .progressPercentage(50.0)
                .isCompleted(false)
                .build()
        ));
        bookProgressRepository.save(progress);

        GetChaptersRequest inProgressRequest = GetChaptersRequest.builder()
            .progress(ProgressStatus.IN_PROGRESS)
            .build();
        GetChaptersRequest completedRequest = GetChaptersRequest.builder()
            .progress(ProgressStatus.COMPLETED)
            .build();
        GetChaptersRequest notStartedRequest = GetChaptersRequest.builder()
            .progress(ProgressStatus.NOT_STARTED)
            .build();

        Page<Chapter> inProgress = chapterRepository.findChaptersWithFilters(BOOK_ID, inProgressRequest, USER_ID, defaultPageable());
        Page<Chapter> completed = chapterRepository.findChaptersWithFilters(BOOK_ID, completedRequest, USER_ID, defaultPageable());
        Page<Chapter> notStarted = chapterRepository.findChaptersWithFilters(BOOK_ID, notStartedRequest, USER_ID, defaultPageable());

        assertThat(inProgress.getContent()).extracting(Chapter::getChapterNumber).containsExactly(2);
        assertThat(completed.getContent()).extracting(Chapter::getChapterNumber).containsExactly(1);
        assertThat(notStarted.getContent()).extracting(Chapter::getChapterNumber).containsExactly(3);
    }

    @Test
    @DisplayName("fallback 데이터에서는 currentReadChapterNumber 기준으로 챕터 상태를 구분한다")
    void findChaptersWithFilters_usesFallbackProgressData() {
        BookProgress progress = new BookProgress();
        progress.setUserId(USER_ID);
        progress.setBookId(BOOK_ID);
        progress.setCurrentReadChapterNumber(2);
        bookProgressRepository.save(progress);

        GetChaptersRequest completedRequest = GetChaptersRequest.builder()
            .progress(ProgressStatus.COMPLETED)
            .build();
        GetChaptersRequest inProgressRequest = GetChaptersRequest.builder()
            .progress(ProgressStatus.IN_PROGRESS)
            .build();
        GetChaptersRequest notStartedRequest = GetChaptersRequest.builder()
            .progress(ProgressStatus.NOT_STARTED)
            .build();

        Page<Chapter> completed = chapterRepository.findChaptersWithFilters(BOOK_ID, completedRequest, USER_ID, defaultPageable());
        Page<Chapter> inProgress = chapterRepository.findChaptersWithFilters(BOOK_ID, inProgressRequest, USER_ID, defaultPageable());
        Page<Chapter> notStarted = chapterRepository.findChaptersWithFilters(BOOK_ID, notStartedRequest, USER_ID, defaultPageable());

        assertThat(completed.getContent()).extracting(Chapter::getChapterNumber).containsExactly(1);
        assertThat(inProgress.getContent()).extracting(Chapter::getChapterNumber).containsExactly(2);
        assertThat(notStarted.getContent()).extracting(Chapter::getChapterNumber).containsExactly(3);
    }

    private Pageable defaultPageable() {
        return PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "chapterNumber"));
    }

    private Chapter createChapter(int chapterNumber, String title) {
        Chapter chapter = new Chapter();
        chapter.setId("chapter-" + chapterNumber);
        chapter.setBookId(BOOK_ID);
        chapter.setChapterNumber(chapterNumber);
        chapter.setTitle(title);
        return chapter;
    }
}
