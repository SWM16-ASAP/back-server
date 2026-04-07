package com.linglevel.api.content.book.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.book.dto.ChunkResponse;
import com.linglevel.api.content.book.dto.GetChunksRequest;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkServiceTest {

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private BookService bookService;

    @InjectMocks
    private ChunkService chunkService;

    @Test
    @DisplayName("청크 목록 조회 시 페이지 정보와 ChunkResponse 매핑을 반환한다.")
    void getChunks_returnsPagedChunkResponses() {
        // given
        GetChunksRequest request = GetChunksRequest.builder()
            .difficultyLevel(DifficultyLevel.A1)
            .page(1)
            .limit(300)
            .build();

        Chapter chapter = createChapter("chapter-1", "book-1");
        Chunk firstChunk = createChunk("chunk-1", "chapter-1", 1, ChunkType.TEXT, "first", null);
        Chunk secondChunk = createChunk("chunk-2", "chapter-1", 2, ChunkType.IMAGE, "https://cdn/image.png", "image");
        Page<Chunk> chunkPage = new PageImpl<>(List.of(firstChunk, secondChunk));

        when(bookService.existsById("book-1")).thenReturn(true);
        when(chapterRepository.findById("chapter-1")).thenReturn(Optional.of(chapter));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(chunkRepository.findByChapterIdAndDifficultyLevel(
            ArgumentMatchers.eq("chapter-1"),
            ArgumentMatchers.eq(DifficultyLevel.A1),
            pageableCaptor.capture()
        )).thenReturn(chunkPage);

        // when
        PageResponse<ChunkResponse> response = chunkService.getChunks("book-1", "chapter-1", request, "user-1");

        // then
        assertEquals(2, response.getData().size());
        assertEquals("chunk-1", response.getData().get(0).getId());
        assertEquals(ChunkType.TEXT, response.getData().get(0).getType());
        assertEquals("https://cdn/image.png", response.getData().get(1).getContent());
        assertEquals("image", response.getData().get(1).getDescription());

        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(200, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("책이 없으면 BOOK_NOT_FOUND 예외를 던진다.")
    void getChunks_throwsWhenBookNotFound() {
        // given
        GetChunksRequest request = GetChunksRequest.builder()
            .difficultyLevel(DifficultyLevel.A1)
            .build();
        when(bookService.existsById("missing-book")).thenReturn(false);

        // when
        BooksException exception = assertThrows(
            BooksException.class,
            () -> chunkService.getChunks("missing-book", "chapter-1", request, "user-1")
        );

        // then
        assertEquals(BooksErrorCode.BOOK_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("챕터가 다른 책에 속하면 CHAPTER_NOT_FOUND_IN_BOOK 예외를 던진다.")
    void getChunks_throwsWhenChapterDoesNotBelongToBook() {
        // given
        GetChunksRequest request = GetChunksRequest.builder()
            .difficultyLevel(DifficultyLevel.A1)
            .build();

        when(bookService.existsById("book-1")).thenReturn(true);
        when(chapterRepository.findById("chapter-1"))
            .thenReturn(Optional.of(createChapter("chapter-1", "another-book")));

        // when
        BooksException exception = assertThrows(
            BooksException.class,
            () -> chunkService.getChunks("book-1", "chapter-1", request, "user-1")
        );

        // then
        assertEquals(BooksErrorCode.CHAPTER_NOT_FOUND_IN_BOOK.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("단일 청크 조회 시 ChunkResponse로 변환해 반환한다.")
    void getChunk_returnsChunkResponse() {
        // given
        Chapter chapter = createChapter("chapter-1", "book-1");
        Chunk chunk = createChunk("chunk-1", "chapter-1", 3, ChunkType.TEXT, "body", null);

        when(bookService.existsById("book-1")).thenReturn(true);
        when(chapterRepository.findById("chapter-1")).thenReturn(Optional.of(chapter));
        when(chunkRepository.findById("chunk-1")).thenReturn(Optional.of(chunk));

        // when
        ChunkResponse response = chunkService.getChunk("book-1", "chapter-1", "chunk-1");

        // then
        assertEquals("chunk-1", response.getId());
        assertEquals(3, response.getChunkNumber());
        assertEquals(ChunkType.TEXT, response.getType());
        assertEquals("body", response.getContent());
    }

    @Test
    @DisplayName("청크가 다른 챕터에 속하면 CHUNK_NOT_FOUND 예외를 던진다.")
    void getChunk_throwsWhenChunkDoesNotBelongToChapter() {
        // given
        Chapter chapter = createChapter("chapter-1", "book-1");
        Chunk chunk = createChunk("chunk-1", "chapter-2", 1, ChunkType.TEXT, "body", null);

        when(bookService.existsById("book-1")).thenReturn(true);
        when(chapterRepository.findById("chapter-1")).thenReturn(Optional.of(chapter));
        when(chunkRepository.findById("chunk-1")).thenReturn(Optional.of(chunk));

        // when
        BooksException exception = assertThrows(
            BooksException.class,
            () -> chunkService.getChunk("book-1", "chapter-1", "chunk-1")
        );

        // then
        assertEquals(BooksErrorCode.CHUNK_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("findById는 청크가 없으면 CHUNK_NOT_FOUND 예외를 던진다.")
    void findById_throwsWhenChunkNotFound() {
        // given
        when(chunkRepository.findById("missing-chunk")).thenReturn(Optional.empty());

        // when
        BooksException exception = assertThrows(
            BooksException.class,
            () -> chunkService.findById("missing-chunk")
        );

        // then
        assertEquals(BooksErrorCode.CHUNK_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("findFirstByChapterId는 첫 번째 청크를 반환한다.")
    void findFirstByChapterId_returnsFirstChunk() {
        // given
        Chunk chunk = createChunk("chunk-1", "chapter-1", 1, ChunkType.TEXT, "body", null);
        when(chunkRepository.findFirstByChapterIdOrderByChunkNumberAsc("chapter-1"))
            .thenReturn(Optional.of(chunk));

        // when
        Chunk result = chunkService.findFirstByChapterId("chapter-1");

        // then
        assertEquals("chunk-1", result.getId());
        assertEquals(1, result.getChunkNumber());
    }

    private Chapter createChapter(String chapterId, String bookId) {
        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setBookId(bookId);
        return chapter;
    }

    private Chunk createChunk(
        String chunkId,
        String chapterId,
        int chunkNumber,
        ChunkType type,
        String content,
        String description
    ) {
        Chunk chunk = new Chunk();
        chunk.setId(chunkId);
        chunk.setChapterId(chapterId);
        chunk.setChunkNumber(chunkNumber);
        chunk.setDifficultyLevel(DifficultyLevel.A1);
        chunk.setType(type);
        chunk.setContent(content);
        chunk.setDescription(description);
        return chunk;
    }
}
