package com.linglevel.api.content.common;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.content.book.entity.*;
import com.linglevel.api.content.book.repository.*;
import com.linglevel.api.content.book.service.*;
import com.linglevel.api.content.book.dto.ProgressUpdateRequest;
import com.linglevel.api.content.article.entity.*;
import com.linglevel.api.content.article.repository.*;
import com.linglevel.api.content.article.service.*;
import com.linglevel.api.content.article.dto.ArticleProgressUpdateRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.entity.CustomContentProgress;
import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.repository.*;
import com.linglevel.api.content.custom.service.*;
import com.linglevel.api.content.custom.dto.CustomContentReadingProgressUpdateRequest;
import com.linglevel.api.content.common.service.*;
import com.linglevel.api.streak.entity.*;
import com.linglevel.api.streak.repository.*;
import com.linglevel.api.streak.service.*;
import com.linglevel.api.user.entity.*;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.user.ticket.entity.*;
import com.linglevel.api.user.ticket.repository.*;
import com.linglevel.api.user.ticket.service.TicketService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ ProgressService.class, ArticleProgressService.class, CustomContentReadingProgressService.class,
		ProgressCalculationService.class, StreakService.class, StudyReportLock.class, TicketService.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReadingProgressPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	ProgressService bookProgressService;

	@Autowired
	ArticleProgressService articleProgressService;

	@Autowired
	CustomContentReadingProgressService customProgressService;

	@Autowired
	UserRepository users;

	@Autowired
	BookRepository books;

	@Autowired
	ChapterRepository chapters;

	@Autowired
	ArticleRepository articles;

	@Autowired
	CustomContentRepository contents;

	@Autowired
	ContentRequestRepository requests;

	@Autowired
	TicketReservationRepository reservations;

	@Autowired
	BookProgressRepository bookProgress;

	@Autowired
	ArticleProgressRepository articleProgress;

	@Autowired
	CustomContentProgressRepository customProgress;

	@Autowired
	UserStudyReportRepository reports;

	@Autowired
	DailyCompletionRepository days;

	@Autowired
	LearningCompletionRepository learning;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	EntityManager em;

	@MockitoSpyBean
	TicketService ticketService;

	@MockitoBean
	BookService bookService;

	@MockitoBean
	ChapterService chapterService;

	@MockitoBean
	ChunkService chunkService;

	@MockitoBean
	ChunkRepository chunks;

	@MockitoBean
	ArticleService articleService;

	@MockitoBean
	ArticleChunkService articleChunkService;

	@MockitoBean
	ArticleChunkRepository articleChunks;

	@MockitoBean
	CustomContentService customService;

	@MockitoBean
	CustomContentChunkService customChunkService;

	@MockitoBean
	CustomContentChunkRepository customChunks;

	@MockitoBean
	ReadingCompletionService readingCompletion;

	@MockitoBean
	ReadingSessionService readingSessions;

	String userId;

	Book book;

	Article article;

	CustomContent custom;

	Chapter first;

	Chapter second;

	TransactionTemplate tx;

	@BeforeEach
	void setUp() {
		tx = new TransactionTemplate(transactionManager);
		userId = users.saveAndFlush(User.builder().username("reader-" + UUID.randomUUID()).role(UserRole.USER).build())
			.getId()
			.toString();
		book = new Book();
		book.setTitle("Book");
		book.setDifficultyLevel(DifficultyLevel.A1);
		books.saveAndFlush(book);
		first = chapter(1);
		second = chapter(2);
		when(bookService.existsById(bookId())).thenReturn(true);
		bookChunk(first, "first-start", 1);
		bookChunk(first, "first-end", 2);
		bookChunk(second, "second-end", 2);
		when(chunks.countByChapterIdAndDifficultyLevel(anyString(), eq(DifficultyLevel.A1))).thenReturn(2L);

		article = new Article();
		article.setTitle("Article");
		article.setDifficultyLevel(DifficultyLevel.A1);
		articles.saveAndFlush(article);
		when(articleService.existsById(articleId())).thenReturn(true);
		when(articleService.findById(articleId())).thenReturn(article);
		articleChunk("article-start", 1);
		articleChunk("article-end", 2);
		when(articleChunks.countByArticleIdAndDifficultyLevel(articleId(), DifficultyLevel.A1)).thenReturn(2L);

		var reservation = reservations.saveAndFlush(TicketReservation.builder()
			.userId(Long.valueOf(userId))
			.amount(1)
			.status(TicketReservationStatus.RESERVED)
			.description("fixture")
			.build());
		var request = requests.saveAndFlush(ContentRequest.builder()
			.requestKey(UUID.randomUUID().toString())
			.userId(Long.valueOf(userId))
			.ticketReservationId(reservation.getId())
			.title("Request")
			.contentType(com.linglevel.api.content.custom.entity.ContentType.TEXT)
			.build());
		custom = contents.saveAndFlush(CustomContent.builder()
			.contentRequestId(request.getId())
			.userId(Long.valueOf(userId))
			.title("Custom")
			.difficultyLevel(DifficultyLevel.A1)
			.build());
		when(customService.existsById(customId())).thenReturn(true);
		customChunk("custom-start", 1);
		customChunk("custom-end", 2);
		when(customChunks.countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(customId(), DifficultyLevel.A1))
			.thenReturn(2L);
	}

	@Test
	void concurrentChapterUpdatesKeepBothRowsAndOneBookProgress() throws Exception {
		parallel(8, i -> updateBook(i % 2 == 0 ? "first-end" : "second-end"));
		var result = storedBook();
		assertThat(bookProgress.findAllByUserId(userId)).hasSize(1);
		assertThat(result.getChapterProgresses()).hasSize(2).allMatch(p -> p.getIsCompleted());
		assertThat(result.getIsCompleted()).isTrue();
		assertThat(result.getNormalizedProgress()).isEqualTo(100);
		assertThat(result.getMaxReadChapterNumber()).isEqualTo(2);
		assertThat(result.getMaxReadChunkNumber()).isEqualTo((2 << 16) | 2);
	}

	@Test
	void rereadPreservesFirstCompletionAndReturnsUpdatedPercentage() {
		var response = updateBook("first-end");
		assertThat(response.getNormalizedProgress()).isEqualTo(50);
		assertThat(response.getId()).matches("[0-9]+");
		Instant completedAt = storedBook().getChapterProgresses().get(0).getCompletedAt();
		updateBook("first-start");
		var result = storedBook();
		assertThat(result.getChapterProgresses().get(0).getIsCompleted()).isTrue();
		assertThat(result.getChapterProgresses().get(0).getCompletedAt()).isEqualTo(completedAt);
		assertThat(result.getNormalizedProgress()).isEqualTo(50);
		assertThat(result.getMaxReadChunkNumber()).isEqualTo((1 << 16) | 2);
	}

	@Test
	void concurrentArticleAndCustomUpdatesPreserveMaximumAndCompletion() throws Exception {
		parallel(8, i -> {
			updateArticle(i % 2 == 0 ? "article-start" : "article-end");
			updateCustom(i % 2 == 0 ? "custom-start" : "custom-end");
		});
		assertThat(articleProgress.findAllByUserId(userId)).hasSize(1);
		assertThat(customProgress.findAllByUserId(userId)).hasSize(1);
		var a = articleProgress.findByUserIdAndArticleId(userId, articleId()).orElseThrow();
		var c = customProgress.findByUserIdAndCustomId(userId, customId()).orElseThrow();
		assertThat(a.getMaxNormalizedProgress()).isEqualTo(100);
		assertThat(c.getMaxNormalizedProgress()).isEqualTo(100);
		assertThat(a.getIsCompleted()).isTrue();
		assertThat(c.getIsCompleted()).isTrue();
		assertThat(a.getCompletedAt()).isNotNull();
		assertThat(c.getCompletedAt()).isNotNull();
	}

	@Test
	void firstGetDoesNotPersistProgressForAnyContentType() {
		assertThat(bookProgressService.getProgress(bookId(), userId).getId()).isNull();
		assertThat(articleProgressService.getProgress(articleId(), userId).getId()).isNull();
		assertThat(customProgressService.getProgress(customId(), userId).getId()).isNull();
		assertThat(bookProgress.findAllByUserId(userId)).isEmpty();
		assertThat(articleProgress.findAllByUserId(userId)).isEmpty();
		assertThat(customProgress.findAllByUserId(userId)).isEmpty();
		verifyNoInteractions(readingCompletion);
	}

	@Test
	void completionCommitsProgressHistoryAndRewardTogether() {
		seedSixDayStreak();
		when(readingCompletion.processReadingCompletion(eq(userId), any(), anyString(), any())).thenReturn(60L);
		ticketService.getTicketBalance(userId);
		updateBook("first-end");
		assertThat(storedBook().getNormalizedProgress()).isEqualTo(50);
		assertThat(reports.findByUserId(userId).orElseThrow().getCurrentStreak()).isEqualTo(7);
		assertThat(learning.countDistinctContents(Long.valueOf(userId))).isEqualTo(1);
		assertThat(ticketService.getTicketBalance(userId).getBalance()).isEqualTo(11);
	}

	@Test
	void downstreamFailureRollsBackFlushedProgressHistoryAndTicket() {
		seedSixDayStreak();
		ticketService.getTicketBalance(userId);
		when(readingCompletion.processReadingCompletion(eq(userId), any(), anyString(), any())).thenReturn(60L);
		assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
			updateBook("first-end");
			updateArticle("article-end");
			updateCustom("custom-end");
			em.flush();
			throw new IllegalStateException("failure after SQL writes");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(bookProgress.findAllByUserId(userId)).isEmpty();
		assertThat(articleProgress.findAllByUserId(userId)).isEmpty();
		assertThat(customProgress.findAllByUserId(userId)).isEmpty();
		assertThat(learning.countDistinctContents(Long.valueOf(userId))).isZero();
		assertThat(days.findByUserIdAndCompletionDate(userId, today())).isEmpty();
		assertThat(reports.findByUserId(userId).orElseThrow().getCurrentStreak()).isEqualTo(6);
		assertThat(reports.findByUserId(userId).orElseThrow().getTotalReadingTimeSeconds()).isZero();
		assertThat(ticketService.getTicketBalance(userId).getBalance()).isEqualTo(10);
	}

	@Test
	void ticketFailureDoesNotChangeExistingProgress() {
		updateBook("first-start");
		seedSixDayStreak();
		when(readingCompletion.processReadingCompletion(eq(userId), any(), anyString(), any())).thenReturn(60L);
		doThrow(new IllegalStateException("ticket failure")).when(ticketService)
			.grantTicket(eq(userId), eq(1), anyString());
		assertThatThrownBy(() -> updateBook("first-end")).isInstanceOf(IllegalStateException.class);
		var result = storedBook();
		assertThat(result.getNormalizedProgress()).isZero();
		assertThat(result.getChapterProgresses().get(0).getIsCompleted()).isFalse();
		assertThat(reports.findByUserId(userId).orElseThrow().getCurrentStreak()).isEqualTo(6);
		assertThat(learning.countDistinctContents(Long.valueOf(userId))).isZero();
	}

	@Test
	void deleteProgressCascadesChapterRowsButKeepsLearningHistory() {
		when(readingCompletion.processReadingCompletion(eq(userId), any(), anyString(), any())).thenReturn(60L);
		updateBook("first-end");
		Long id = storedBook().getId();
		bookProgressService.deleteProgress(bookId(), userId);
		assertThat(bookProgress.findAllByUserId(userId)).isEmpty();
		assertThat(tx.<Long>execute(s -> em
			.createQuery("select count(cp) from BookChapterProgress cp where cp.bookProgress.id = :id", Long.class)
			.setParameter("id", id)
			.getSingleResult())).isZero();
		assertThat(learning.countDistinctContents(Long.valueOf(userId))).isEqualTo(1);
		updateBook("first-start");
		assertThat(storedBook().getChapterProgresses()).hasSize(1);
	}

	@Test
	void articleDeletionCascadesItsProgress() {
		updateArticle("article-end");
		articles.deleteById(article.getId());
		assertThat(articleProgress.findAllByUserId(userId)).isEmpty();
	}

	@Test
	void uniqueAndForeignKeyConstraintsRejectInvalidProgress() {
		updateArticle("article-start");
		ArticleProgress duplicate = new ArticleProgress();
		duplicate.setUserId(Long.valueOf(userId));
		duplicate.setArticleId(article.getId());
		assertThatThrownBy(() -> articleProgress.saveAndFlush(duplicate))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		ArticleProgress orphan = new ArticleProgress();
		orphan.setUserId(Long.valueOf(userId));
		orphan.setArticleId(Long.MAX_VALUE);
		assertThatThrownBy(() -> articleProgress.saveAndFlush(orphan))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		BookProgress p = new BookProgress();
		p.setUserId(Long.valueOf(userId));
		p.setBookId(book.getId());
		for (int i = 0; i < 2; i++)
			p.getChapterProgresses()
				.add(BookProgress.ChapterProgressInfo.builder()
					.bookProgress(p)
					.chapterNumber(1)
					.isCompleted(false)
					.build());
		assertThatThrownBy(() -> bookProgress.saveAndFlush(p))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	private Chapter chapter(int number) {
		Chapter c = new Chapter();
		c.setBookId(book.getId());
		c.setChapterNumber(number);
		c.setTitle("Chapter " + number);
		chapters.saveAndFlush(c);
		when(chapterService.findById(c.getId().toString())).thenReturn(c);
		return c;
	}

	private void bookChunk(Chapter chapter, String id, int number) {
		Chunk c = new Chunk();
		c.setId(id);
		c.setChapterId(chapter.getId().toString());
		c.setChunkNumber(number);
		c.setDifficultyLevel(DifficultyLevel.A1);
		when(chunkService.findById(id)).thenReturn(c);
	}

	private void articleChunk(String id, int number) {
		ArticleChunk c = new ArticleChunk();
		c.setId(id);
		c.setArticleId(articleId());
		c.setChunkNumber(number);
		c.setDifficultyLevel(DifficultyLevel.A1);
		when(articleChunkService.findById(id)).thenReturn(c);
		if (number == 1)
			when(articleChunkService.findFirstByArticleId(articleId())).thenReturn(c);
	}

	private void customChunk(String id, int number) {
		CustomContentChunk c = new CustomContentChunk();
		c.setId(id);
		c.setCustomContentId(customId());
		c.setChunkNum(number);
		c.setDifficultyLevel(DifficultyLevel.A1);
		when(customChunkService.findById(id)).thenReturn(c);
		if (number == 1)
			when(customChunkService.findFirstByCustomContentId(customId())).thenReturn(c);
	}

	private com.linglevel.api.content.book.dto.ProgressResponse updateBook(String chunk) {
		ProgressUpdateRequest request = new ProgressUpdateRequest();
		request.setChunkId(chunk);
		return bookProgressService.updateProgress(bookId(), request, userId);
	}

	private void updateArticle(String chunk) {
		ArticleProgressUpdateRequest request = new ArticleProgressUpdateRequest();
		request.setChunkId(chunk);
		articleProgressService.updateProgress(articleId(), request, userId);
	}

	private void updateCustom(String chunk) {
		CustomContentReadingProgressUpdateRequest request = new CustomContentReadingProgressUpdateRequest();
		request.setChunkId(chunk);
		customProgressService.updateProgress(customId(), request, userId);
	}

	private BookProgress storedBook() {
		return tx.execute(s -> {
			var p = bookProgress.findByUserIdAndBookId(userId, bookId()).orElseThrow();
			p.getChapterProgresses().size();
			return p;
		});
	}

	private void seedSixDayStreak() {
		UserStudyReport r = new UserStudyReport();
		r.setUserId(Long.valueOf(userId));
		r.setCurrentStreak(6);
		r.setLongestStreak(6);
		r.setLastCompletionDate(today().minusDays(1));
		r.setStreakStartDate(today().minusDays(6));
		reports.saveAndFlush(r);
	}

	private LocalDate today() {
		return LocalDate.now(ZoneId.of("Asia/Seoul"));
	}

	private String bookId() {
		return book.getId().toString();
	}

	private String articleId() {
		return article.getId().toString();
	}

	private String customId() {
		return custom.getId().toString();
	}

	private void parallel(int count, java.util.function.IntConsumer action) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(4);
		try {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				int index = i;
				tasks.add(() -> {
					action.accept(index);
					return null;
				});
			}
			for (var result : pool.invokeAll(tasks, 30, TimeUnit.SECONDS))
				result.get();
		}
		finally {
			pool.shutdownNow();
		}
	}

}
