package com.linglevel.api.bookmark.service;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.bookmark.entity.WordBookmark;
import com.linglevel.api.bookmark.repository.WordBookmarkRepository;
import com.linglevel.api.bookmark.exception.BookmarksException;
import com.linglevel.api.user.entity.*;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.word.dto.*;
import com.linglevel.api.word.entity.Word;
import com.linglevel.api.word.repository.WordRepository;
import com.linglevel.api.word.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ BookmarkService.class, BookmarkWriter.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookmarkPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	BookmarkService service;

	@Autowired
	BookmarkWriter writer;

	@Autowired
	WordBookmarkRepository bookmarks;

	@Autowired
	UserRepository users;

	@Autowired
	PlatformTransactionManager transactionManager;

	@MockitoBean
	WordService words;

	@MockitoBean
	WordVariantService variants;

	@MockitoBean
	WordRepository wordRepository;

	String userId;

	TransactionTemplate tx;

	@BeforeEach
	void setUp() {
		userId = users
			.saveAndFlush(User.builder().username("bookmark-" + UUID.randomUUID()).role(UserRole.USER).build())
			.getId()
			.toString();
		tx = new TransactionTemplate(transactionManager);
	}

	@Test
	void concurrentAddsCreateOneBookmarkAndReturnDomainConflict() throws Exception {
		AtomicInteger added = new AtomicInteger();
		AtomicInteger conflicts = new AtomicInteger();
		parallel(8, () -> {
			try {
				writer.add(userId, "run");
				added.incrementAndGet();
			}
			catch (BookmarksException e) {
				conflicts.incrementAndGet();
			}
		});
		assertThat(added.get()).isEqualTo(1);
		assertThat(conflicts.get()).isEqualTo(7);
		assertThat(service.getBookmarkedWords(userId, 1, 10, null).getTotalElements()).isEqualTo(1);
	}

	@Test
	void concurrentTogglesApplyEveryTransition() throws Exception {
		AtomicInteger added = new AtomicInteger();
		parallel(8, () -> {
			if (writer.toggle(userId, "run"))
				added.incrementAndGet();
		});
		assertThat(added.get()).isEqualTo(4);
		assertThat(bookmarks.existsByUserIdAndWord(userId, "run")).isFalse();
		assertThat(writer.toggle(userId, "run")).isTrue();
	}

	@Test
	void wordResolutionRunsOutsideSqlTransactionEvenWithOuterCaller() {
		when(words.getOrCreateWords(eq(userId), eq("ran"), any())).thenAnswer(invocation -> {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
			return response("run", "other");
		});
		tx.executeWithoutResult(status -> service.addWordBookmark(userId, "ran"));
		assertThat(bookmarks.existsByUserIdAndWord(userId, "run")).isTrue();
		assertThat(bookmarks.existsByUserIdAndWord(userId, "other")).isFalse();
	}

	@Test
	void failedOrEmptyWordResolutionDoesNotSaveBookmark() {
		when(words.getOrCreateWords(eq(userId), eq("broken"), any()))
			.thenThrow(new IllegalStateException("AI failure"));
		assertThatThrownBy(() -> service.addWordBookmark(userId, "broken")).isInstanceOf(IllegalStateException.class);
		when(words.getOrCreateWords(eq(userId), eq("missing"), any())).thenReturn(response());
		assertThatThrownBy(() -> service.toggleWordBookmark(userId, "missing")).isInstanceOf(BookmarksException.class);
		assertThat(service.getBookmarkedWords(userId, 1, 10, null)).isEmpty();
	}

	@Test
	void searchUsesSqlBookmarksWithoutMongoAndTreatsWildcardsLiterally() {
		for (String word : List.of("Run", "runner", "a%b", "a_b", "axb"))
			writer.add(userId, word);
		assertThat(service.getBookmarkedWords(userId, 1, 10, " RUN ")).extracting(r -> r.getWord())
			.containsExactly("runner", "Run");
		assertThat(service.getBookmarkedWords(userId, 1, 10, "%")).extracting(r -> r.getWord()).containsExactly("a%b");
		assertThat(service.getBookmarkedWords(userId, 1, 10, "_")).extracting(r -> r.getWord()).containsExactly("a_b");
		verifyNoInteractions(wordRepository, words, variants);
	}

	@Test
	void sameTimestampPaginationIsStableAndUserScoped() {
		LocalDateTime time = LocalDateTime.of(2026, 1, 1, 0, 0);
		var first = bookmarks
			.saveAndFlush(WordBookmark.builder().userId(Long.valueOf(userId)).word("first").bookmarkedAt(time).build());
		var second = bookmarks.saveAndFlush(
				WordBookmark.builder().userId(Long.valueOf(userId)).word("second").bookmarkedAt(time).build());
		assertThat(service.getBookmarkedWords(userId, 1, 1, null).getContent().get(0).getId())
			.isEqualTo(second.getId().toString());
		assertThat(service.getBookmarkedWords(userId, 2, 1, null).getContent().get(0).getId())
			.isEqualTo(first.getId().toString());
		assertThat(service.getBookmarkedWords("999999", 1, 10, null)).isEmpty();
	}

	@Test
	void removePrefersExactWordThenVariantAndDoesNotTouchOtherUsers() {
		writer.add(userId, "saw");
		writer.add(userId, "see");
		when(variants.getOriginalForms("saw")).thenReturn(List.of("see", "saw"));
		service.removeWordBookmark(userId, "saw");
		assertThat(bookmarks.existsByUserIdAndWord(userId, "saw")).isFalse();
		assertThat(bookmarks.existsByUserIdAndWord(userId, "see")).isTrue();
		service.removeWordBookmark(userId, "saw");
		assertThat(bookmarks.existsByUserIdAndWord(userId, "see")).isFalse();
		assertThatThrownBy(() -> service.removeWordBookmark(userId, "saw")).isInstanceOf(BookmarksException.class);
	}

	@Test
	void toggleByMongoIdResolvesWordBeforeSqlMutation() {
		Word word = new Word();
		word.setWord("run");
		when(wordRepository.findById("mongo-word-id")).thenReturn(Optional.of(word));
		assertThat(service.toggleWordBookmarkById(userId, "mongo-word-id")).isTrue();
		assertThat(service.toggleWordBookmarkById(userId, "mongo-word-id")).isFalse();
		assertThatThrownBy(() -> service.toggleWordBookmarkById(userId, "missing"))
			.isInstanceOf(BookmarksException.class);
	}

	@Test
	void sqlRollbackPreservesBookmarkState() {
		writer.add(userId, "run");
		assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
			writer.toggle(userId, "run");
			bookmarks.flush();
			throw new IllegalStateException("rollback");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(bookmarks.existsByUserIdAndWord(userId, "run")).isTrue();
	}

	@Test
	void databaseEnforcesUniqueUserWordAndForeignKeyButKeepsExactWordIdentity() {
		writer.add(userId, "run");
		writer.add(userId, "Run");
		assertThatThrownBy(
				() -> bookmarks.saveAndFlush(WordBookmark.builder().userId(Long.valueOf(userId)).word("run").build()))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(
				() -> bookmarks.saveAndFlush(WordBookmark.builder().userId(Long.MAX_VALUE).word("run").build()))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThat(service.getBookmarkedWords(userId, 1, 10, null).getTotalElements()).isEqualTo(2);
	}

	@Test
	void normalizationUpdatesOrRemovesOnlyTheDuplicateInItsOwnTransaction() {
		writer.add(userId, "ran");
		var b = service.getBookmarkedWords(userId, 1, 10, null).getContent().get(0);
		assertThat(writer.normalize(userId, Long.valueOf(b.getId()), "run"))
			.isEqualTo(BookmarkWriter.NormalizationResult.UPDATED);
		assertThat(writer.normalize(userId, Long.valueOf(b.getId()), "run"))
			.isEqualTo(BookmarkWriter.NormalizationResult.UNCHANGED);
		writer.add(userId, "ran");
		Long duplicateId = Long.valueOf(service.getBookmarkedWords(userId, 1, 10, "ran").getContent().get(0).getId());
		assertThat(writer.normalize(userId, duplicateId, "run"))
			.isEqualTo(BookmarkWriter.NormalizationResult.DUPLICATE_REMOVED);
		assertThat(service.getBookmarkedWords(userId, 1, 10, null)).hasSize(1);
		assertThat(bookmarks.findById(Long.valueOf(b.getId()))).isPresent();
	}

	private WordSearchResponse response(String... forms) {
		return WordSearchResponse.builder()
			.results(Arrays.stream(forms).map(f -> WordResponse.builder().originalForm(f).build()).toList())
			.build();
	}

	private void parallel(int count, Runnable action) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(4);
		try {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int i = 0; i < count; i++)
				tasks.add(() -> {
					action.run();
					return null;
				});
			for (var result : pool.invokeAll(tasks, 30, TimeUnit.SECONDS))
				result.get();
		}
		finally {
			pool.shutdownNow();
		}
	}

}
