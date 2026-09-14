package com.linglevel.api.content.common;

import com.linglevel.api.content.book.dto.GetBooksRequest;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.repository.*;
import com.linglevel.api.content.article.dto.*;
import com.linglevel.api.content.article.entity.*;
import com.linglevel.api.content.article.repository.*;
import com.linglevel.api.i18n.LanguageCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class CatalogPersistenceIntegrationTest extends AbstractCatalogTest {

	@Autowired
	BookRepository books;

	@Autowired
	ChapterRepository chapters;

	@Autowired
	ArticleRepository articles;

	@Autowired
	ArticleProgressRepository progress;

	@Autowired
	EntityManager em;

	@BeforeEach
	void resetProgress() {
		progress.deleteAll();
	}

	@Test
	void generatedIdsAndJsonRoundTrip() {
		Book first = book("First");
		first.setTitleTranslations(new TitleTranslations("첫 번째", "最初"));
		first.setTags(List.of("history", "adventure"));
		books.saveAndFlush(first);
		Book second = books.saveAndFlush(book("Second"));
		em.clear();
		Book loaded = books.findById(first.getId()).orElseThrow();
		assertThat(first.getId()).isPositive().isLessThan(second.getId());
		assertThat(loaded.getTitleTranslations().getKo()).isEqualTo("첫 번째");
		assertThat(loaded.getTags()).containsExactly("history", "adventure");
		assertThat(loaded.getViewCount()).isZero();
		assertThat(loaded.getCreatedAt()).isNotNull();
	}

	@Test
	void chapterRequiresExistingBook() {
		Chapter c = chapter(Long.MAX_VALUE, 1);
		assertThatThrownBy(() -> chapters.saveAndFlush(c))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void chapterNumberIsUniqueWithinBook() {
		Book first = books.save(book("First"));
		Book second = books.save(book("Second"));
		chapters.saveAndFlush(chapter(first.getId(), 1));
		chapters.saveAndFlush(chapter(second.getId(), 1));
		assertThat(chapters.countByBookId(first.getId())).isEqualTo(1);
		assertThatThrownBy(() -> chapters.saveAndFlush(chapter(first.getId(), 1)))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void chapterNumberMustBePositive() {
		Book b = books.save(book("First"));
		assertThatThrownBy(() -> chapters.saveAndFlush(chapter(b.getId(), 0)))
			.isInstanceOf(org.springframework.dao.DataAccessException.class)
			.hasMessageContaining("chk_chapters_number");
	}

	@Test
	void booksFilterAnyTagLiteralKeywordAndCreatedAfter() {
		Book first = book("A 100% story");
		first.setTags(List.of("history"));
		first.setCreatedAt(Instant.parse("2026-01-02T00:00:00Z"));
		books.save(first);
		Book second = book("A 1000 story");
		second.setTags(List.of("science"));
		second.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		books.save(second);
		var request = GetBooksRequest.builder().tags("history, science").build();
		assertThat(books.findBooksWithFilters(request, null, page()).getTotalElements()).isEqualTo(2);
		request.setKeyword("100%");
		assertThat(books.findBooksWithFilters(request, null, page())).extracting(Book::getId)
			.containsExactly(first.getId());
		request.setKeyword(null);
		request.setCreatedAfter(LocalDateTime.parse("2026-01-02T00:00:00"));
		assertThat(books.findBooksWithFilters(request, null, page())).extracting(Book::getId)
			.containsExactly(first.getId());
	}

	@Test
	void tiedSortUsesIdForStablePages() {
		Book a = book("A");
		Book b = book("B");
		Book c = book("C");
		Instant same = Instant.parse("2026-01-01T00:00:00Z");
		for (Book book : List.of(a, b, c)) {
			book.setCreatedAt(same);
			books.save(book);
		}
		var request = GetBooksRequest.builder().build();
		assertThat(books.findBooksWithFilters(request, null, PageRequest.of(0, 2, Sort.by("createdAt"))))
			.extracting(Book::getId)
			.containsExactly(c.getId(), b.getId());
		var next = books.findBooksWithFilters(request, null, PageRequest.of(1, 2, Sort.by("createdAt")));
		assertThat(next).extracting(Book::getId).containsExactly(a.getId());
		assertThat(next.getTotalElements()).isEqualTo(3);
	}

	@Test
	void articlesFilterCategoryLanguageTagsAndOrigins() {
		Article match = article("Technology");
		match.setOriginUrl("https://example.com/story");
		articles.save(match);
		Article noOrigin = article("Other");
		articles.save(noOrigin);
		Article wrongCategory = article("Science");
		wrongCategory.setCategory(ContentCategory.SCIENCE);
		articles.save(wrongCategory);
		GetArticlesRequest request = new GetArticlesRequest();
		request.setCategory(ContentCategory.TECH);
		request.setTargetLanguageCode(LanguageCode.KO);
		request.setTags("absent,technology");
		assertThat(articles.findArticlesWithFilters(request, null, page()).getTotalElements()).isEqualTo(2);
		request.setTargetLanguageCode(LanguageCode.JA);
		assertThat(articles.findArticlesWithFilters(request, null, page())).isEmpty();
		GetArticleOriginsRequest origins = new GetArticleOriginsRequest();
		origins.setCategory("Technology");
		origins.setTargetLanguageCode(LanguageCode.KO);
		assertThat(articles.findArticleOriginsWithFilters(origins, page())).extracting(Article::getId)
			.containsExactly(match.getId());
		em.flush();
		em.clear();
		assertThat(articles.findById(match.getId()).orElseThrow().getTargetLanguageCode())
			.containsExactly(LanguageCode.EN, LanguageCode.KO);
	}

	@Test
	void articleProgressUsesStoredNormalizedProgressAndUserScope() {
		Article unread = articles.save(article("Unread"));
		Article reading = articles.save(article("Reading"));
		Article complete = articles.save(article("Complete"));
		ArticleProgress p = new ArticleProgress();
		p.setUserId("1");
		p.setArticleId(reading.getId().toString());
		p.setNormalizedProgress(30.0);
		progress.save(p);
		ArticleProgress done = new ArticleProgress();
		done.setUserId("1");
		done.setArticleId(complete.getId().toString());
		done.setIsCompleted(true);
		progress.save(done);
		GetArticlesRequest request = new GetArticlesRequest();
		request.setProgress(ProgressStatus.IN_PROGRESS);
		assertThat(articles.findArticlesWithFilters(request, "1", page())).extracting(Article::getId)
			.containsExactly(reading.getId());
		assertThat(articles.findArticlesWithFilters(request, "2", page())).isEmpty();
		request.setProgress(ProgressStatus.COMPLETED);
		assertThat(articles.findArticlesWithFilters(request, "1", page())).extracting(Article::getId)
			.containsExactly(complete.getId());
		request.setProgress(ProgressStatus.NOT_STARTED);
		assertThat(articles.findArticlesWithFilters(request, "1", page())).extracting(Article::getId)
			.containsExactly(unread.getId());
		assertThat(articles.findArticlesWithFilters(request, "2", page()).getTotalElements()).isEqualTo(3);
	}

	@Test
	void viewCountUpdatesAccumulateForBooksAndArticles() {
		Book b = books.saveAndFlush(book("Book"));
		Article a = articles.saveAndFlush(article("Article"));
		for (int i = 0; i < 3; i++) {
			books.incrementViewCount(b.getId().toString());
			articles.incrementViewCount(a.getId().toString());
		}
		em.clear();
		assertThat(books.findById(b.getId()).orElseThrow().getViewCount()).isEqualTo(3);
		assertThat(articles.findById(a.getId()).orElseThrow().getViewCount()).isEqualTo(3);
	}

	private Pageable page() {
		return PageRequest.of(0, 20, Sort.by("createdAt").descending());
	}

	private Book book(String title) {
		Book b = new Book();
		b.setTitle(title);
		b.setDifficultyLevel(DifficultyLevel.A1);
		return b;
	}

	private Chapter chapter(Long bookId, int number) {
		Chapter c = new Chapter();
		c.setBookId(bookId);
		c.setChapterNumber(number);
		c.setTitle("Chapter");
		return c;
	}

	private Article article(String title) {
		Article a = new Article();
		a.setTitle(title);
		a.setDifficultyLevel(DifficultyLevel.A1);
		a.setTags(List.of("technology"));
		a.setCategory(ContentCategory.TECH);
		a.setTargetLanguageCode(List.of(LanguageCode.EN, LanguageCode.KO));
		return a;
	}

}
