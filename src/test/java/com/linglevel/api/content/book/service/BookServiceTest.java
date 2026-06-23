package com.linglevel.api.content.book.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.book.dto.*;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookProgressRepository;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.content.common.TitleTranslations;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.s3.service.ImageResizeService;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.service.S3TransferService;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.BookPathStrategy;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private BookProgressRepository bookProgressRepository;

	@Mock
	private S3AiService s3AiService;

	@Mock
	private S3TransferService s3TransferService;

	@Mock
	private S3UrlService s3UrlService;

	@Mock
	private BookPathStrategy bookPathStrategy;

	@Mock
	private ImageResizeService imageResizeService;

	@Mock
	private BookReadingTimeService bookReadingTimeService;

	@Mock
	private BookImportService bookImportService;

	@InjectMocks
	private BookService bookService;

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = new User();
		testUser.setId("test-user-id");
		testUser.setUsername("testuser");
		testUser.setEmail("test@example.com");
		testUser.setRole(UserRole.USER);
		testUser.setDeleted(false);
		testUser.setCreatedAt(LocalDateTime.now());
	}

	@Test
	@DisplayName("importBook는 책 저장, 이미지 처리, 챕터/청크 import, reading time 갱신을 순서대로 수행한다")
	void importBook_orchestratesImportFlow() {
		// given
		BookImportRequest request = new BookImportRequest();
		request.setId("request-1");

		BookImportData importData = createImportData();
		Chapter savedChapter = new Chapter();
		savedChapter.setId("chapter-1");
		List<Chapter> savedChapters = List.of(savedChapter);

		when(s3AiService.downloadJsonFile("request-1", BookImportData.class, bookPathStrategy)).thenReturn(importData);
		when(s3UrlService.getCoverImageUrl("request-1", bookPathStrategy)).thenReturn("https://cdn/request-cover.jpg");
		when(s3UrlService.getCoverImageUrl("saved-book-id", bookPathStrategy))
			.thenReturn("https://cdn/original-cover.jpg");
		when(bookPathStrategy.generateCoverImagePath("saved-book-id"))
			.thenReturn("literature/saved-book-id/images/cover.jpg");
		when(imageResizeService.createSmallImage("literature/saved-book-id/images/cover.jpg"))
			.thenReturn("https://cdn/small-cover.webp");
		when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
			Book book = invocation.getArgument(0);
			if (book.getId() == null) {
				book.setId("saved-book-id");
			}
			return book;
		});
		when(bookImportService.createChaptersFromMetadata(importData, "saved-book-id")).thenReturn(savedChapters);

		ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);

		// when
		BookImportResponse response = bookService.importBook(request);

		// then
		verify(bookRepository, org.mockito.Mockito.times(2)).save(bookCaptor.capture());
		List<Book> savedBooks = bookCaptor.getAllValues();
		Book finalSavedBook = savedBooks.get(savedBooks.size() - 1);

		assertThat(response.getId()).isEqualTo("saved-book-id");
		assertThat(finalSavedBook.getId()).isEqualTo("saved-book-id");
		assertThat(finalSavedBook.getTitle()).isEqualTo("Imported title");
		assertThat(finalSavedBook.getDifficultyLevel()).isEqualTo(DifficultyLevel.A1);
		assertThat(finalSavedBook.getChapterCount()).isEqualTo(2);
		assertThat(finalSavedBook.getCoverImageUrl()).isEqualTo("https://cdn/small-cover.webp");

		verify(s3TransferService).transferImagesFromAiToStatic("request-1", "saved-book-id", bookPathStrategy);
		verify(bookImportService).createChaptersFromMetadata(importData, "saved-book-id");
		verify(bookImportService).createChunksFromLeveledResults(importData, savedChapters, "saved-book-id");
		verify(bookReadingTimeService).updateReadingTimes("saved-book-id", importData);
	}

	@Test
	@DisplayName("cover image 리사이즈가 실패하면 원본 cover URL을 유지한다")
	void importBook_keepsOriginalCoverUrlWhenResizeFails() {
		// given
		BookImportRequest request = new BookImportRequest();
		request.setId("request-1");

		BookImportData importData = createImportData();
		List<Chapter> savedChapters = List.of(new Chapter());

		when(s3AiService.downloadJsonFile("request-1", BookImportData.class, bookPathStrategy)).thenReturn(importData);
		when(s3UrlService.getCoverImageUrl("request-1", bookPathStrategy)).thenReturn("https://cdn/request-cover.jpg");
		when(s3UrlService.getCoverImageUrl("saved-book-id", bookPathStrategy))
			.thenReturn("https://cdn/original-cover.jpg");
		when(bookPathStrategy.generateCoverImagePath("saved-book-id"))
			.thenReturn("literature/saved-book-id/images/cover.jpg");
		when(imageResizeService.createSmallImage("literature/saved-book-id/images/cover.jpg"))
			.thenThrow(new RuntimeException("resize failed"));
		when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
			Book book = invocation.getArgument(0);
			if (book.getId() == null) {
				book.setId("saved-book-id");
			}
			return book;
		});
		when(bookImportService.createChaptersFromMetadata(importData, "saved-book-id")).thenReturn(savedChapters);

		ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);

		// when
		BookImportResponse response = bookService.importBook(request);

		// then
		verify(bookRepository, org.mockito.Mockito.times(2)).save(bookCaptor.capture());
		List<Book> savedBooks = bookCaptor.getAllValues();
		Book finalSavedBook = savedBooks.get(savedBooks.size() - 1);

		assertThat(response.getId()).isEqualTo("saved-book-id");
		assertThat(finalSavedBook.getCoverImageUrl()).isEqualTo("https://cdn/original-cover.jpg");

		verify(bookImportService).createChunksFromLeveledResults(importData, savedChapters, "saved-book-id");
		verify(bookReadingTimeService).updateReadingTimes("saved-book-id", importData);
	}

	@Test
	@DisplayName("단일 책 조회 시 요청 언어에 맞는 번역 제목을 우선 사용한다")
	void getBook_selectsTranslatedTitleByLanguage() {
		// given
		Book book = createBook("Original title", "Author", List.of("tag1"));
		book.setTitleTranslations(new TitleTranslations("번역 제목", "Original title"));
		when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

		// when
		BookResponse response = bookService.getBook(book.getId(), testUser.getId(), LanguageCode.KO);

		// then
		assertThat(response.getTitle()).isEqualTo("번역 제목");
	}

	@Test
	@DisplayName("번역 제목이 비어 있으면 원본 제목으로 fallback 한다")
	void getBook_fallsBackToOriginalTitleWhenTranslationMissing() {
		// given
		Book book = createBook("Original title", "Author", List.of("tag1"));
		book.setTitleTranslations(new TitleTranslations(null, "Original title"));
		when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

		// when
		BookResponse response = bookService.getBook(book.getId(), testUser.getId(), LanguageCode.KO);

		// then
		assertThat(response.getTitle()).isEqualTo("Original title");
	}

	@Test
	@DisplayName("단일 책 조회 시 책이 없으면 BOOK_NOT_FOUND 예외를 던진다")
	void getBook_throwsWhenBookMissing() {
		// given
		when(bookRepository.findById("missing-book")).thenReturn(Optional.empty());

		// when
		BooksException exception = assertThrows(BooksException.class,
				() -> bookService.getBook("missing-book", testUser.getId(), LanguageCode.EN));

		// then
		assertThat(exception.getMessage()).isEqualTo(BooksErrorCode.BOOK_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("지원하지 않는 sortBy 값이면 INVALID_SORT_BY 예외를 던진다")
	void getBooks_throwsWhenSortByInvalid() {
		// given
		GetBooksRequest request = GetBooksRequest.builder().sortBy("unknown-sort").page(1).limit(10).build();

		// when
		BooksException exception = assertThrows(BooksException.class,
				() -> bookService.getBooks(request, testUser.getId()));

		// then
		assertThat(exception.getMessage()).isEqualTo(BooksErrorCode.INVALID_SORT_BY.getMessage());
	}

	@Test
	@DisplayName("진도 필터링과 페이지네이션이 함께 동작할 때 - IN_PROGRESS 필터")
	void testProgressFilterWithPagination_InProgress() {
		GetBooksRequest request = GetBooksRequest.builder()
			.progress(ProgressStatus.IN_PROGRESS)
			.page(1)
			.limit(5)
			.build();

		List<Book> books = createBooks(5, "Book", "Author", List.of("tag1"));

		Page<Book> bookPage = new PageImpl<>(books, PageRequest.of(0, 5), 10);

		when(bookRepository.findBooksWithFilters(any(), eq(testUser.getId()), any())).thenReturn(bookPage);

		mockBookProgress(books, false);

		PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getId());

		assertThat(response.getData()).hasSize(5);
		assertThat(response.getTotalCount()).isEqualTo(10);
		assertThat(response.getTotalPages()).isEqualTo(2);

		response.getData().forEach(book -> {
			assertThat(book.getProgressPercentage()).isGreaterThan(0.0);
			assertThat(book.getIsCompleted()).isFalse();
		});
	}

	@Test
	@DisplayName("진도 필터링과 페이지네이션이 함께 동작할 때 - COMPLETED 필터")
	void testProgressFilterWithPagination_Completed() {
		GetBooksRequest request = GetBooksRequest.builder().progress(ProgressStatus.COMPLETED).page(1).limit(5).build();

		List<Book> books = List.of(createBook("Book 1", "Author 1", List.of("tag1")),
				createBook("Book 2", "Author 2", List.of("tag1")), createBook("Book 3", "Author 3", List.of("tag1")),
				createBook("Book 4", "Author 4", List.of("tag1")), createBook("Book 5", "Author 5", List.of("tag1")));

		Page<Book> bookPage = new PageImpl<>(books, PageRequest.of(0, 5), 10);

		when(bookRepository.findBooksWithFilters(any(), eq(testUser.getId()), any())).thenReturn(bookPage);

		mockBookProgress(books, true);

		PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getId());

		assertThat(response.getData()).hasSize(5);
		assertThat(response.getTotalCount()).isEqualTo(10);
		assertThat(response.getTotalPages()).isEqualTo(2);

		response.getData().forEach(book -> {
			assertThat(book.getProgressPercentage()).isEqualTo(100.0);
			assertThat(book.getIsCompleted()).isTrue();
		});
	}

	@Test
	@DisplayName("진도 필터링과 페이지네이션이 함께 동작할 때 - NOT_STARTED 필터")
	void testProgressFilterWithPagination_NotStarted() {
		GetBooksRequest request = GetBooksRequest.builder()
			.progress(ProgressStatus.NOT_STARTED)
			.page(1)
			.limit(5)
			.build();

		List<Book> books = List.of(createBook("Book 1", "Author 1", List.of("tag1")),
				createBook("Book 2", "Author 2", List.of("tag1")), createBook("Book 3", "Author 3", List.of("tag1")),
				createBook("Book 4", "Author 4", List.of("tag1")), createBook("Book 5", "Author 5", List.of("tag1")));

		Page<Book> bookPage = new PageImpl<>(books, PageRequest.of(0, 5), 10);

		when(bookRepository.findBooksWithFilters(any(), eq(testUser.getId()), any())).thenReturn(bookPage);

		PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getId());

		assertThat(response.getData()).hasSize(5);
		assertThat(response.getTotalCount()).isEqualTo(10);
		assertThat(response.getTotalPages()).isEqualTo(2);

		response.getData().forEach(book -> {
			assertThat(book.getProgressPercentage()).isEqualTo(0.0);
			assertThat(book.getIsCompleted()).isFalse();
		});
	}

	@Test
	@DisplayName("태그 필터링과 페이지네이션")
	void testTagsFilterWithPagination() {
		GetBooksRequest request = GetBooksRequest.builder().tags("technology").page(1).limit(10).build();

		List<Book> books = createBooks(10, "Tech Book", "Author", List.of("technology"));

		Page<Book> bookPage = new PageImpl<>(books, PageRequest.of(0, 10), 15);

		when(bookRepository.findBooksWithFilters(any(), eq(testUser.getId()), any())).thenReturn(bookPage);

		PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getId());

		assertThat(response.getData()).hasSize(10);
		assertThat(response.getTotalCount()).isEqualTo(15);
		assertThat(response.getTotalPages()).isEqualTo(2);

		response.getData().forEach(book -> assertThat(book.getTags()).contains("technology"));
	}

	@Test
	@DisplayName("키워드 검색과 페이지네이션")
	void testKeywordSearchWithPagination() {
		GetBooksRequest request = GetBooksRequest.builder().keyword("java").page(1).limit(10).build();

		List<Book> books = createBooks(10, "Java Programming", "Author", List.of("tag1"));

		Page<Book> bookPage = new PageImpl<>(books, PageRequest.of(0, 10), 12);

		when(bookRepository.findBooksWithFilters(any(), eq(testUser.getId()), any())).thenReturn(bookPage);

		PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getId());

		assertThat(response.getData()).hasSize(10);
		assertThat(response.getTotalCount()).isEqualTo(12);
		assertThat(response.getTotalPages()).isEqualTo(2);

		response.getData().forEach(book -> assertThat(book.getTitle().toLowerCase()).contains("java"));
	}

	@Test
	@DisplayName("복합 필터링 - 태그 + 진도 + 페이지네이션")
	void testCombinedFiltersWithPagination() {
		GetBooksRequest request = GetBooksRequest.builder()
			.tags("technology")
			.progress(ProgressStatus.IN_PROGRESS)
			.page(1)
			.limit(5)
			.build();

		List<Book> books = List.of(createBook("Tech Book 1", "Author 1", List.of("technology")),
				createBook("Tech Book 2", "Author 2", List.of("technology")),
				createBook("Tech Book 3", "Author 3", List.of("technology")),
				createBook("Tech Book 4", "Author 4", List.of("technology")),
				createBook("Tech Book 5", "Author 5", List.of("technology")));

		Page<Book> bookPage = new PageImpl<>(books, PageRequest.of(0, 5), 10);

		when(bookRepository.findBooksWithFilters(any(), eq(testUser.getId()), any())).thenReturn(bookPage);

		mockBookProgress(books, false);

		PageResponse<BookResponse> response = bookService.getBooks(request, testUser.getId());

		assertThat(response.getData()).hasSize(5);
		assertThat(response.getTotalCount()).isEqualTo(10);
		assertThat(response.getTotalPages()).isEqualTo(2);

		response.getData().forEach(book -> {
			assertThat(book.getTags()).contains("technology");
			assertThat(book.getProgressPercentage()).isGreaterThan(0.0);
			assertThat(book.getIsCompleted()).isFalse();
		});
	}

	private List<Book> createBooks(int count, String titlePrefix, String authorPrefix, List<String> tags) {
		List<Book> books = new java.util.ArrayList<>();
		for (int i = 1; i <= count; i++) {
			books.add(createBook(titlePrefix + " " + i, authorPrefix + " " + i, tags));
		}
		return books;
	}

	private Book createBook(String title, String author, List<String> tags) {
		Book book = new Book();
		book.setId("book-" + title.hashCode());
		book.setTitle(title);
		book.setAuthor(author);
		book.setTags(tags);
		book.setDifficultyLevel(DifficultyLevel.A1);
		book.setChapterCount(20);
		book.setReadingTime(300);
		book.setAverageRating(4.5);
		book.setReviewCount(100);
		book.setViewCount(1000);
		book.setCreatedAt(Instant.now());
		return book;
	}

	private void mockBookProgress(List<Book> books, boolean isCompleted) {
		List<String> bookIds = books.stream().map(Book::getId).toList();
		List<BookProgress> progresses = books.stream()
			.map(book -> createBookProgress(testUser.getId(), book.getId(), isCompleted))
			.toList();

		when(bookProgressRepository.findByUserIdAndBookIdIn(testUser.getId(), bookIds)).thenReturn(progresses);
	}

	private BookProgress createBookProgress(String userId, String bookId, boolean isCompleted) {
		BookProgress progress = new BookProgress();
		progress.setUserId(userId);
		progress.setBookId(bookId);
		progress.setCurrentReadChapterNumber(isCompleted ? 20 : 10);
		progress.setMaxReadChapterNumber(isCompleted ? 20 : 10);
		progress.setNormalizedProgress(isCompleted ? 100.0 : 50.0);
		progress.setMaxNormalizedProgress(isCompleted ? 100.0 : 50.0);
		progress.setIsCompleted(isCompleted);
		progress.setUpdatedAt(Instant.now());
		return progress;
	}

	private BookImportData createImportData() {
		BookImportData importData = new BookImportData();
		importData.setTitle("Imported title");
		importData.setTitleTranslations(new TitleTranslations("가져온 제목", "Imported title"));
		importData.setAuthor("Imported author");
		importData.setOriginalTextLevel("a1");
		importData.setLeveledResults(List.of(createTextLevelData("a1", 2), createTextLevelData("b1", 1)));
		return importData;
	}

	private BookImportData.TextLevelData createTextLevelData(String level, int chapterCount) {
		BookImportData.TextLevelData textLevelData = new BookImportData.TextLevelData();
		textLevelData.setTextLevel(level);
		List<BookImportData.ChapterData> chapters = new java.util.ArrayList<>();
		for (int i = 1; i <= chapterCount; i++) {
			BookImportData.ChapterData chapterData = new BookImportData.ChapterData();
			chapterData.setChapterNum(i);
			chapterData.setChunks(List.of());
			chapters.add(chapterData);
		}
		textLevelData.setChapters(chapters);
		return textLevelData;
	}

}
