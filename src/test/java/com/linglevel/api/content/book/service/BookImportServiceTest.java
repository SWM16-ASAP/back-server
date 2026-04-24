package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.BookImportData;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.BookPathStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookImportServiceTest {
    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private S3UrlService s3UrlService;

    @Mock
    private BookPathStrategy bookPathStrategy;

    @InjectMocks
    private BookImportService bookImportService;

    private BookImportData bookImportData;
    private BookImportData.ChapterMetadata chapterMetadata;

    @BeforeEach
    void setUp() {
        bookImportData = new BookImportData();
        chapterMetadata = new BookImportData.ChapterMetadata();
        BookImportData.TextLevelData textLevelData = new BookImportData.TextLevelData();
        BookImportData.ChapterData chapterData = new BookImportData.ChapterData();
        BookImportData.ChunkData textChunkData = new BookImportData.ChunkData();
        BookImportData.ChunkData imageChunkData = new BookImportData.ChunkData();

        textChunkData.setChunkNum(1);
        textChunkData.setChunkText("내용");
        textChunkData.setIsImage(false);
        imageChunkData.setChunkNum(2);
        imageChunkData.setChunkText("주소");
        imageChunkData.setIsImage(true);
        imageChunkData.setDescription("이미지 설명");

        chapterData.setChapterNum(1);
        chapterData.setChunks(List.of(textChunkData, imageChunkData));

        textLevelData.setTextLevel("a1");
        textLevelData.setChapters(List.of(chapterData));

        chapterMetadata.setChapterNum(1);
        chapterMetadata.setTitle("제목");
        chapterMetadata.setSummary("요약");
        
        bookImportData.setChapterMetadata(List.of(chapterMetadata));
        bookImportData.setLeveledResults(List.of(textLevelData));
    }

    @Test
    @DisplayName("chapter metadata를 Chapter 엔티티로 변환해 저장한다.")
    void importChapters() {
        // given
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Chapter>> captor = ArgumentCaptor.forClass((Class) Iterable.class);

        when(chapterRepository.saveAll(ArgumentMatchers.anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<Chapter> chapters = bookImportService.createChaptersFromMetadata(bookImportData, "bookId");

        // then
        verify(chapterRepository).saveAll(captor.capture());

        List<Chapter> savedChapters = StreamSupport.stream(captor.getValue().spliterator(), false).toList();

        assertEquals(1, savedChapters.size());

        Chapter savedChapter = savedChapters.get(0);
        assertEquals("bookId", savedChapter.getBookId());
        assertEquals(1, savedChapter.getChapterNumber());
        assertEquals("제목", savedChapter.getTitle());
        assertEquals("요약", savedChapter.getDescription());
        assertEquals(0, savedChapter.getReadingTime());
        assertEquals(savedChapters, chapters);
    }

    @Test
    @DisplayName("leveled results를 텍스트와 이미지 Chunk 엔티티로 변환해 저장한다.")
    void importChunks() {
        // given
        Chapter savedChapter = new Chapter();
        savedChapter.setId("chapter-1");
        List<Chapter> chapters = List.of(savedChapter);

        when(s3UrlService.buildImageUrl("bookId", "주소", bookPathStrategy))
                .thenReturn("https://cdn.example.com/image.png");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Chunk>> captor =
                ArgumentCaptor.forClass((Class) Iterable.class);

        // when
        bookImportService.createChunksFromLeveledResults(bookImportData, chapters, "bookId");

        // then
        verify(chunkRepository).saveAll(captor.capture());

        List<Chunk> savedChunks =
                StreamSupport.stream(captor.getValue().spliterator(), false).toList();

        assertEquals(2, savedChunks.size());

        Chunk textChunk = savedChunks.get(0);
        assertEquals("chapter-1", textChunk.getChapterId());
        assertEquals(1, textChunk.getChunkNumber());
        assertEquals(DifficultyLevel.A1, textChunk.getDifficultyLevel());
        assertEquals(ChunkType.TEXT, textChunk.getType());
        assertEquals("내용", textChunk.getContent());
        assertNull(textChunk.getDescription());

        Chunk imageChunk = savedChunks.get(1);
        assertEquals("chapter-1", imageChunk.getChapterId());
        assertEquals(2, imageChunk.getChunkNumber());
        assertEquals(DifficultyLevel.A1, imageChunk.getDifficultyLevel());
        assertEquals(ChunkType.IMAGE, imageChunk.getType());
        assertEquals("https://cdn.example.com/image.png", imageChunk.getContent());
        assertEquals("이미지 설명", imageChunk.getDescription());

        verify(s3UrlService).buildImageUrl("bookId", "주소", bookPathStrategy);
    }

    @Test
    @DisplayName("여러 chapter metadata가 주어지면 chapterNumber를 1부터 순차 증가시켜 저장한다.")
    void importChapters_assignSequentialChapterNumbers() {
        // given
        BookImportData.ChapterMetadata secondMetadata = new BookImportData.ChapterMetadata();
        secondMetadata.setChapterNum(2);
        secondMetadata.setTitle("두번째 제목");
        secondMetadata.setSummary("두번째 요약");
        bookImportData.setChapterMetadata(List.of(chapterMetadata, secondMetadata));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Chapter>> captor = ArgumentCaptor.forClass((Class) Iterable.class);

        when(chapterRepository.saveAll(ArgumentMatchers.anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        bookImportService.createChaptersFromMetadata(bookImportData, "bookId");

        // then
        verify(chapterRepository).saveAll(captor.capture());

        List<Chapter> savedChapters = StreamSupport.stream(captor.getValue().spliterator(), false).toList();

        assertEquals(2, savedChapters.size());
        assertEquals(1, savedChapters.get(0).getChapterNumber());
        assertEquals("제목", savedChapters.get(0).getTitle());
        assertEquals(2, savedChapters.get(1).getChapterNumber());
        assertEquals("두번째 제목", savedChapters.get(1).getTitle());
    }

    @Test
    @DisplayName("여러 챕터를 저장할 때 각 챕터의 chunkNumber는 1부터 다시 시작한다.")
    void importChunks_resetsChunkNumberPerChapter() {
        // given
        Chapter firstChapter = new Chapter();
        firstChapter.setId("chapter-1");
        Chapter secondChapter = new Chapter();
        secondChapter.setId("chapter-2");
        List<Chapter> chapters = List.of(firstChapter, secondChapter);

        BookImportData.ChunkData firstTextChunk = createChunkData("첫 챕터 1", false, null);
        BookImportData.ChunkData firstImageChunk = createChunkData("first.png", true, "첫 이미지");
        BookImportData.ChunkData secondTextChunk = createChunkData("둘째 챕터 1", false, null);

        BookImportData.ChapterData firstChapterData = createChapterData(List.of(firstTextChunk, firstImageChunk));
        BookImportData.ChapterData secondChapterData = createChapterData(List.of(secondTextChunk));

        BookImportData.TextLevelData textLevelData = new BookImportData.TextLevelData();
        textLevelData.setTextLevel("a1");
        textLevelData.setChapters(List.of(firstChapterData, secondChapterData));
        bookImportData.setLeveledResults(List.of(textLevelData));

        when(s3UrlService.buildImageUrl("bookId", "first.png", bookPathStrategy))
                .thenReturn("https://cdn.example.com/first.png");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Chunk>> captor = ArgumentCaptor.forClass((Class) Iterable.class);

        // when
        bookImportService.createChunksFromLeveledResults(bookImportData, chapters, "bookId");

        // then
        verify(chunkRepository).saveAll(captor.capture());

        List<Chunk> savedChunks = StreamSupport.stream(captor.getValue().spliterator(), false).toList();

        assertEquals(3, savedChunks.size());
        assertEquals("chapter-1", savedChunks.get(0).getChapterId());
        assertEquals(1, savedChunks.get(0).getChunkNumber());
        assertEquals("chapter-1", savedChunks.get(1).getChapterId());
        assertEquals(2, savedChunks.get(1).getChunkNumber());
        assertEquals("chapter-2", savedChunks.get(2).getChapterId());
        assertEquals(1, savedChunks.get(2).getChunkNumber());
    }

    @Test
    @DisplayName("AI chapter 수가 savedChapters보다 적으면 남은 챕터는 건너뛴다.")
    void importChunks_skipsRemainingSavedChaptersWhenAiChaptersAreShorter() {
        // given
        Chapter firstChapter = new Chapter();
        firstChapter.setId("chapter-1");
        Chapter secondChapter = new Chapter();
        secondChapter.setId("chapter-2");
        List<Chapter> chapters = List.of(firstChapter, secondChapter);

        BookImportData.ChunkData onlyChunk = createChunkData("첫 챕터만 저장", false, null);
        BookImportData.ChapterData onlyChapterData = createChapterData(List.of(onlyChunk));

        BookImportData.TextLevelData textLevelData = new BookImportData.TextLevelData();
        textLevelData.setTextLevel("a1");
        textLevelData.setChapters(List.of(onlyChapterData));
        bookImportData.setLeveledResults(List.of(textLevelData));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Chunk>> captor = ArgumentCaptor.forClass((Class) Iterable.class);

        // when
        bookImportService.createChunksFromLeveledResults(bookImportData, chapters, "bookId");

        // then
        verify(chunkRepository).saveAll(captor.capture());

        List<Chunk> savedChunks = StreamSupport.stream(captor.getValue().spliterator(), false).toList();

        assertEquals(1, savedChunks.size());
        assertEquals("chapter-1", savedChunks.get(0).getChapterId());
        assertEquals("첫 챕터만 저장", savedChunks.get(0).getContent());
    }

    private BookImportData.ChunkData createChunkData(String chunkText, boolean isImage, String description) {
        BookImportData.ChunkData chunkData = new BookImportData.ChunkData();
        chunkData.setChunkText(chunkText);
        chunkData.setIsImage(isImage);
        chunkData.setDescription(description);
        return chunkData;
    }

    private BookImportData.ChapterData createChapterData(List<BookImportData.ChunkData> chunks) {
        BookImportData.ChapterData chapterData = new BookImportData.ChapterData();
        chapterData.setChunks(chunks);
        return chapterData;
    }
}
