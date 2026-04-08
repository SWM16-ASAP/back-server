package com.linglevel.api.content.book.repository;

import com.linglevel.api.common.AbstractDatabaseTest;
import com.linglevel.api.content.book.dto.GetBooksRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
import org.bson.Document;
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
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(BookRepositoryImpl.class)
class BookRepositoryImplTest extends AbstractDatabaseTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookProgressRepository bookProgressRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        bookProgressRepository.deleteAll();
        bookRepository.deleteAll();

        bookRepository.saveAll(List.of(
            createBook("book-1", "Alpha", Instant.parse("2026-01-01T00:00:00Z")),
            createBook("book-2", "Beta", Instant.parse("2026-01-02T00:00:00Z")),
            createBook("book-3", "Gamma", Instant.parse("2026-01-03T00:00:00Z"))
        ));

        mongoTemplate.insert(createProgressDocument("book-2", false, 40.0), "bookProgress");
        mongoTemplate.insert(createProgressDocument("book-3", true, 100.0), "bookProgress");
    }

    @Test
    @DisplayName("NOT_STARTED 필터는 시작하지 않은 책(문서 없음 또는 normalizedProgress 0)을 반환한다")
    void findBooksWithFilters_returnsNotStartedBooks() {
        GetBooksRequest request = GetBooksRequest.builder()
            .progress(ProgressStatus.NOT_STARTED)
            .build();

        Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

        assertThat(result.getContent()).extracting(Book::getId).containsExactly("book-1");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("IN_PROGRESS 필터는 normalizedProgress > 0이고 완료되지 않은 책만 반환한다")
    void findBooksWithFilters_returnsInProgressBooks() {
        GetBooksRequest request = GetBooksRequest.builder()
            .progress(ProgressStatus.IN_PROGRESS)
            .build();

        Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

        assertThat(result.getContent()).extracting(Book::getId).containsExactly("book-2");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("COMPLETED 필터는 완료된 책만 반환한다")
    void findBooksWithFilters_returnsCompletedBooks() {
        GetBooksRequest request = GetBooksRequest.builder()
            .progress(ProgressStatus.COMPLETED)
            .build();

        Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

        assertThat(result.getContent()).extracting(Book::getId).containsExactly("book-3");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("조건에 맞는 progress가 없으면 빈 페이지를 반환한다")
    void findBooksWithFilters_returnsEmptyPageWhenNoProgressMatch() {
        bookProgressRepository.deleteAll();
        mongoTemplate.insert(createProgressDocument("book-1", false, 0.0), "bookProgress");

        GetBooksRequest request = GetBooksRequest.builder()
            .progress(ProgressStatus.IN_PROGRESS)
            .build();

        Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("normalizedProgress가 0이고 미완료인 책은 NOT_STARTED로 분류한다")
    void findBooksWithFilters_includesZeroProgressAsNotStarted() {
        mongoTemplate.insert(createProgressDocument("book-1", false, 0.0), "bookProgress");

        GetBooksRequest request = GetBooksRequest.builder()
            .progress(ProgressStatus.NOT_STARTED)
            .build();

        Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

        assertThat(result.getContent()).extracting(Book::getId).containsExactly("book-1");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    private Pageable defaultPageable() {
        return PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    private Book createBook(String id, String title, Instant createdAt) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor("Author");
        book.setDifficultyLevel(DifficultyLevel.A1);
        book.setChapterCount(10);
        book.setCreatedAt(createdAt);
        return book;
    }

    private Document createProgressDocument(String bookId, boolean isCompleted, double normalizedProgress) {
        return new Document("userId", USER_ID)
            .append("bookId", bookId)
            .append("isCompleted", isCompleted)
            .append("normalizedProgress", normalizedProgress);
    }
}
