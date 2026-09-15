package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.common.AbstractCatalogTest;
import com.linglevel.api.content.book.dto.GetBooksRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.content.book.entity.BookProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(BookRepositoryImpl.class)
class BookRepositoryImplTest extends AbstractCatalogTest {

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookProgressRepository bookProgressRepository;

	@Autowired
	private com.linglevel.api.user.repository.UserRepository users;

	private final java.util.Map<String, Long> ids = new java.util.HashMap<>();

	private String USER_ID;

	@BeforeEach
	void setUp() {
		USER_ID = users
			.saveAndFlush(com.linglevel.api.user.entity.User.builder()
				.username("reader")
				.role(com.linglevel.api.user.entity.UserRole.USER)
				.build())
			.getId()
			.toString();
		bookProgressRepository.deleteAll();
		bookRepository.deleteAll();

		bookRepository.saveAll(List.of(createBook("book-1", "Alpha", Instant.parse("2026-01-01T00:00:00Z")),
				createBook("book-2", "Beta", Instant.parse("2026-01-02T00:00:00Z")),
				createBook("book-3", "Gamma", Instant.parse("2026-01-03T00:00:00Z"))));

		bookProgressRepository.save(createProgressDocument("book-2", false, 40.0));
		bookProgressRepository.save(createProgressDocument("book-3", true, 100.0));
	}

	@Test
	@DisplayName("NOT_STARTED 필터는 시작하지 않은 책(문서 없음 또는 normalizedProgress 0)을 반환한다")
	void findBooksWithFilters_returnsNotStartedBooks() {
		GetBooksRequest request = GetBooksRequest.builder().progress(ProgressStatus.NOT_STARTED).build();

		Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

		assertThat(result.getContent()).extracting(Book::getId).containsExactly(ids.get("book-1"));
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("IN_PROGRESS 필터는 완료되지 않았고 읽기를 시작한 책을 반환한다")
	void findBooksWithFilters_returnsInProgressBooks() {
		GetBooksRequest request = GetBooksRequest.builder().progress(ProgressStatus.IN_PROGRESS).build();

		Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

		assertThat(result.getContent()).extracting(Book::getId).containsExactly(ids.get("book-2"));
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("normalizedProgress가 0이어도 부분 읽기면 IN_PROGRESS로 분류한다")
	void findBooksWithFilters_includesPartialReadAsInProgress() {
		bookProgressRepository.deleteAll();
		bookProgressRepository.save(createPartialInProgressDocument("book-1", 1, 2, 20.0));

		GetBooksRequest request = GetBooksRequest.builder().progress(ProgressStatus.IN_PROGRESS).build();

		Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

		assertThat(result.getContent()).extracting(Book::getId).containsExactly(ids.get("book-1"));
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("COMPLETED 필터는 완료된 책만 반환한다")
	void findBooksWithFilters_returnsCompletedBooks() {
		GetBooksRequest request = GetBooksRequest.builder().progress(ProgressStatus.COMPLETED).build();

		Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

		assertThat(result.getContent()).extracting(Book::getId).containsExactly(ids.get("book-3"));
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("조건에 맞는 progress가 없으면 빈 페이지를 반환한다")
	void findBooksWithFilters_returnsEmptyPageWhenNoProgressMatch() {
		bookProgressRepository.deleteAll();
		bookProgressRepository.save(createProgressDocument("book-1", false, 0.0));

		GetBooksRequest request = GetBooksRequest.builder().progress(ProgressStatus.IN_PROGRESS).build();

		Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isZero();
	}

	@Test
	@DisplayName("normalizedProgress가 0이고 미완료인 책은 NOT_STARTED로 분류한다")
	void findBooksWithFilters_includesZeroProgressAsNotStarted() {
		bookProgressRepository.save(createProgressDocument("book-1", false, 0.0));

		GetBooksRequest request = GetBooksRequest.builder().progress(ProgressStatus.NOT_STARTED).build();

		Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

		assertThat(result.getContent()).extracting(Book::getId).containsExactly(ids.get("book-1"));
		assertThat(result.getTotalElements()).isEqualTo(1);
	}

	@Test
	@DisplayName("부분 읽기 데이터는 NOT_STARTED에서 제외한다")
	void findBooksWithFilters_excludesPartialReadFromNotStarted() {
		bookProgressRepository.deleteAll();
		bookProgressRepository.save(createPartialInProgressDocument("book-1", 1, 2, 20.0));

		GetBooksRequest request = GetBooksRequest.builder().progress(ProgressStatus.NOT_STARTED).build();

		Page<Book> result = bookRepository.findBooksWithFilters(request, USER_ID, defaultPageable());

		assertThat(result.getContent()).extracting(Book::getId).containsExactly(ids.get("book-2"), ids.get("book-3"));
		assertThat(result.getTotalElements()).isEqualTo(2);
	}

	private Pageable defaultPageable() {
		return PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
	}

	private Book createBook(String id, String title, Instant createdAt) {
		Book book = new Book();

		book.setTitle(title);
		book.setAuthor("Author");
		book.setDifficultyLevel(DifficultyLevel.A1);
		book.setChapterCount(10);
		book.setCreatedAt(createdAt);
		bookRepository.save(book);
		ids.put(id, book.getId());
		return book;
	}

	private BookProgress createProgressDocument(String bookId, boolean isCompleted, double normalizedProgress) {
		BookProgress progress = new BookProgress();
		progress.setUserId(Long.valueOf(USER_ID));
		progress.setBookId(ids.get(bookId));
		progress.setIsCompleted(isCompleted);
		progress.setNormalizedProgress(normalizedProgress);
		return progress;
	}

	private BookProgress createPartialInProgressDocument(String bookId, int chapterNumber, int chunkNumber,
			double progressPercentage) {
		BookProgress progress = createProgressDocument(bookId, false, 0.0);
		progress.setMaxReadChapterNumber(chapterNumber);
		progress.setMaxReadChunkNumber(chapterNumber * 65536 + chunkNumber);
		progress.getChapterProgresses()
			.add(BookProgress.ChapterProgressInfo.builder()
				.bookProgress(progress)
				.chapterNumber(chapterNumber)
				.progressPercentage(progressPercentage)
				.isCompleted(false)
				.build());
		return progress;
	}

}
