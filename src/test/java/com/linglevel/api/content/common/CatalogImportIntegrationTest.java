package com.linglevel.api.content.common;

import com.linglevel.api.content.book.dto.*;
import com.linglevel.api.content.book.entity.*;
import com.linglevel.api.content.book.repository.*;
import com.linglevel.api.content.book.service.*;
import com.linglevel.api.content.article.dto.*;
import com.linglevel.api.content.article.repository.*;
import com.linglevel.api.content.article.service.*;
import com.linglevel.api.content.common.service.ReadingTimeService;
import com.linglevel.api.s3.service.*;
import com.linglevel.api.s3.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import({ BookService.class, BookImportService.class, BookReadingTimeService.class, ArticleService.class,
		ArticleImportService.class, ArticleReadingTimeService.class, ArticleChunkService.class,
		ReadingTimeService.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogImportIntegrationTest extends AbstractCatalogTest {

	@Autowired
	BookService bookService;

	@Autowired
	ArticleService articleService;

	@Autowired
	ArticleChunkService articleChunkService;

	@Autowired
	BookRepository books;

	@Autowired
	ChapterRepository chapters;

	@Autowired
	ChunkRepository chunks;

	@Autowired
	ArticleRepository articles;

	@Autowired
	ArticleChunkRepository articleChunks;

	@MockitoBean
	S3AiService ai;

	@MockitoBean
	S3TransferService transfer;

	@MockitoBean
	S3UrlService urls;

	@MockitoBean
	ImageResizeService images;

	@MockitoBean
	BookPathStrategy bookPath;

	@MockitoBean
	ArticlePathStrategy articlePath;

	@BeforeEach
	void clean() {
		chapters.deleteAll();
		books.deleteAll();
		articles.deleteAll();
		chunks.deleteAll();
		articleChunks.deleteAll();
	}

	@Test
	void importsBookMetadataAndBodyUsingSqlChapterId() {
		when(ai.downloadJsonFile("book-source", BookImportData.class, bookPath)).thenReturn(bookData());
		BookImportRequest request = new BookImportRequest();
		request.setId("book-source");
		String id = bookService.importBook(request).getId();
		assertThat(Long.parseLong(id)).isPositive();
		Book book = books.findById(id).orElseThrow();
		assertThat(book.getChapterCount()).isEqualTo(1);
		assertThat(book.getReadingTime()).isEqualTo(1);
		Chapter chapter = chapters.findByBookIdOrderByChapterNumber(id).get(0);
		assertThat(chapter.getBookId()).isEqualTo(book.getId());
		assertThat(chunks.findByChapterIdOrderByChunkNumber(chapter.getId().toString())).extracting(Chunk::getContent)
			.containsExactly("A short book.");
		verify(transfer).transferImagesFromAiToStatic("book-source", id, bookPath);
	}

	@Test
	void s3FailureRollsBackBookCatalog() {
		when(ai.downloadJsonFile("book-source", BookImportData.class, bookPath)).thenReturn(bookData());
		doThrow(new IllegalStateException("S3 unavailable")).when(transfer)
			.transferImagesFromAiToStatic(eq("book-source"), anyString(), eq(bookPath));
		BookImportRequest request = new BookImportRequest();
		request.setId("book-source");
		assertThatThrownBy(() -> bookService.importBook(request)).isInstanceOf(IllegalStateException.class);
		assertThat(books.count()).isZero();
		assertThat(chapters.count()).isZero();
		assertThat(chunks.count()).isZero();
	}

	@Test
	void malformedBodyRollsBackBookAndChapterTogether() {
		BookImportData data = bookData();
		data.getLeveledResults().get(0).setTextLevel("invalid");
		when(ai.downloadJsonFile("book-source", BookImportData.class, bookPath)).thenReturn(data);
		BookImportRequest request = new BookImportRequest();
		request.setId("book-source");
		assertThatThrownBy(() -> bookService.importBook(request)).isInstanceOf(IllegalArgumentException.class);
		assertThat(books.count()).isZero();
		assertThat(chapters.count()).isZero();
	}

	@Test
	void importsArticleAndBodyUsingSqlId() {
		when(ai.downloadJsonFile("article-source", ArticleImportData.class, articlePath)).thenReturn(articleData());
		ArticleImportRequest request = new ArticleImportRequest();
		request.setId("article-source");
		String id = articleService.importArticle(request).getId();
		var article = articles.findById(id).orElseThrow();
		assertThat(article.getReadingTime()).isEqualTo(1);
		assertThat(article.getTargetLanguageCode()).contains(com.linglevel.api.i18n.LanguageCode.KO);
		var query = new GetArticleChunksRequest();
		query.setDifficultyLevel(DifficultyLevel.A1);
		assertThat(articleChunkService.getArticleChunks(id, query, null).getData()).hasSize(1);
		assertThat(articleService.getArticle(id, null).getId()).isEqualTo(id);
		verify(transfer).transferImagesFromAiToStatic("article-source", id, articlePath);
	}

	@Test
	void malformedArticleDoesNotPublishMetadata() {
		ArticleImportData data = articleData();
		data.getLeveledResults().get(0).setTextLevel("invalid");
		when(ai.downloadJsonFile("article-source", ArticleImportData.class, articlePath)).thenReturn(data);
		ArticleImportRequest request = new ArticleImportRequest();
		request.setId("article-source");
		assertThatThrownBy(() -> articleService.importArticle(request)).isInstanceOf(IllegalArgumentException.class);
		assertThat(articles.count()).isZero();
	}

	@Test
	void orphanArticleChunkIsRejectedByForeignKeyConstraint() {
		var chunk = new com.linglevel.api.content.article.entity.ArticleChunk();
		chunk.setArticleId(999999L);
		chunk.setChunkNumber(1);
		chunk.setDifficultyLevel(DifficultyLevel.A1);
		chunk.setType(com.linglevel.api.content.common.ChunkType.TEXT);
		chunk.setContent("orphan");
		assertThatThrownBy(() -> articleChunks.saveAndFlush(chunk))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void concurrentViewUpdatesDoNotLoseIncrements() throws Exception {
		Book b = new Book();
		b.setTitle("Concurrent");
		b.setDifficultyLevel(DifficultyLevel.A1);
		books.save(b);
		ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			var tasks = new java.util.ArrayList<Callable<Void>>();
			for (int i = 0; i < 20; i++)
				tasks.add(() -> {
					books.incrementViewCount(b.getId().toString());
					return null;
				});
			for (Future<Void> result : executor.invokeAll(tasks))
				result.get(10, TimeUnit.SECONDS);
		}
		finally {
			executor.shutdownNow();
		}
		assertThat(books.findById(b.getId()).orElseThrow().getViewCount()).isEqualTo(20);
	}

	@Test
	void emptyArticleBodyRollsBackMetadata() {
		ArticleImportData data = articleData();
		data.setLeveledResults(List.of());
		when(ai.downloadJsonFile("article-source", ArticleImportData.class, articlePath)).thenReturn(data);
		ArticleImportRequest request = new ArticleImportRequest();
		request.setId("article-source");
		assertThatThrownBy(() -> articleService.importArticle(request)).isInstanceOf(IllegalArgumentException.class);
		assertThat(articles.count()).isZero();
		assertThat(articleChunks.count()).isZero();
	}

	@Test
	void incompleteBookBodyRollsBackMetadataAndChapters() {
		BookImportData data = bookData();
		data.getLeveledResults().get(0).setChapters(List.of());
		when(ai.downloadJsonFile("book-source", BookImportData.class, bookPath)).thenReturn(data);
		BookImportRequest request = new BookImportRequest();
		request.setId("book-source");
		assertThatThrownBy(() -> bookService.importBook(request)).isInstanceOf(IllegalArgumentException.class);
		assertThat(books.count()).isZero();
		assertThat(chapters.count()).isZero();
		assertThat(chunks.count()).isZero();
	}

	private BookImportData bookData() {
		var data = new BookImportData();
		data.setTitle("Book");
		data.setOriginalTextLevel("A1");
		var metadata = new BookImportData.ChapterMetadata();
		metadata.setChapterNum(1);
		metadata.setTitle("Chapter");
		data.setChapterMetadata(List.of(metadata));
		var chunk = new BookImportData.ChunkData();
		chunk.setChunkNum(1);
		chunk.setChunkText("A short book.");
		var chapter = new BookImportData.ChapterData();
		chapter.setChapterNum(1);
		chapter.setChunks(List.of(chunk));
		var level = new BookImportData.TextLevelData();
		level.setTextLevel("A1");
		level.setChapters(List.of(chapter));
		data.setLeveledResults(List.of(level));
		return data;
	}

	private ArticleImportData articleData() {
		var data = new ArticleImportData();
		data.setTitle("Article");
		data.setOriginalTextLevel("A1");
		var chunk = new ArticleImportData.ChunkData();
		chunk.setChunkNum(1);
		chunk.setChunkText("A short article.");
		var chapter = new ArticleImportData.ChapterData();
		chapter.setChapterNum(1);
		chapter.setChunks(List.of(chunk));
		var level = new ArticleImportData.TextLevelData();
		level.setTextLevel("A1");
		level.setChapters(List.of(chapter));
		data.setLeveledResults(List.of(level));
		return data;
	}

}
