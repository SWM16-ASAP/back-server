package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.BookImportData;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.service.ReadingTimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookReadingTimeServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ReadingTimeService readingTimeService;

    @InjectMocks
    private BookReadingTimeService bookReadingTimeService;

    @Test
    @DisplayName("책이 없으면 BOOK_NOT_FOUND 예외를 던진다.")
    void updateReadingTimes_throwsWhenBookNotFound() {
        // given
        BookImportData importData = new BookImportData();
        when(bookRepository.findById("missing-book")).thenReturn(Optional.empty());

        // when
        BooksException exception = assertThrows(
            BooksException.class,
            () -> bookReadingTimeService.updateReadingTimes("missing-book", importData)
        );

        // then
        assertEquals(BooksErrorCode.BOOK_NOT_FOUND.getMessage(), exception.getMessage());
        verify(chapterRepository, never()).findByBookIdOrderByChapterNumber("missing-book");
        verify(chapterRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(bookRepository, never()).save(org.mockito.ArgumentMatchers.any(Book.class));
    }

    @Test
    @DisplayName("책 난이도와 일치하는 leveled results를 사용해 chapter와 book readingTime을 저장한다.")
    void updateReadingTimes_updatesChapterAndBookReadingTimes() {
        // given
        Book book = new Book();
        book.setId("book-1");
        book.setDifficultyLevel(DifficultyLevel.A1);

        Chapter firstChapter = new Chapter();
        firstChapter.setId("chapter-1");
        firstChapter.setBookId("book-1");
        firstChapter.setChapterNumber(1);

        Chapter secondChapter = new Chapter();
        secondChapter.setId("chapter-2");
        secondChapter.setBookId("book-1");
        secondChapter.setChapterNumber(2);

        BookImportData importData = createImportData();

        when(bookRepository.findById("book-1")).thenReturn(Optional.of(book));
        when(chapterRepository.findByBookIdOrderByChapterNumber("book-1"))
            .thenReturn(List.of(firstChapter, secondChapter));
        when(readingTimeService.calculateReadingTimeFromCharacters(5)).thenReturn(3);
        when(readingTimeService.calculateReadingTimeFromCharacters(4)).thenReturn(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Chapter>> chaptersCaptor =
            ArgumentCaptor.forClass((Class) Iterable.class);
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);

        // when
        bookReadingTimeService.updateReadingTimes("book-1", importData);

        // then
        verify(chapterRepository).saveAll(chaptersCaptor.capture());
        verify(bookRepository).save(bookCaptor.capture());

        List<Chapter> savedChapters =
            StreamSupport.stream(chaptersCaptor.getValue().spliterator(), false).toList();
        Book savedBook = bookCaptor.getValue();

        assertEquals(2, savedChapters.size());
        assertEquals(3, savedChapters.get(0).getReadingTime());
        assertEquals(2, savedChapters.get(1).getReadingTime());
        assertEquals(5, savedBook.getReadingTime());

        verify(readingTimeService).calculateReadingTimeFromCharacters(5);
        verify(readingTimeService).calculateReadingTimeFromCharacters(4);
    }

    private BookImportData createImportData() {
        BookImportData.ChunkData firstA1Chunk = createChunkData("abc");
        BookImportData.ChunkData secondA1Chunk = createChunkData("de");
        BookImportData.ChunkData chapterTwoA1Chunk = createChunkData("wxyz");
        BookImportData.ChunkData ignoredB1Chunk = createChunkData("ignored-text");

        BookImportData.ChapterData firstA1Chapter = createChapterData(1, List.of(firstA1Chunk, secondA1Chunk));
        BookImportData.ChapterData secondA1Chapter = createChapterData(2, List.of(chapterTwoA1Chunk));
        BookImportData.ChapterData ignoredB1Chapter = createChapterData(1, List.of(ignoredB1Chunk));

        BookImportData.TextLevelData a1Level = createTextLevelData("a1", List.of(firstA1Chapter, secondA1Chapter));
        BookImportData.TextLevelData b1Level = createTextLevelData("b1", List.of(ignoredB1Chapter));

        BookImportData importData = new BookImportData();
        importData.setLeveledResults(List.of(a1Level, b1Level));
        return importData;
    }

    private BookImportData.TextLevelData createTextLevelData(String textLevel, List<BookImportData.ChapterData> chapters) {
        BookImportData.TextLevelData levelData = new BookImportData.TextLevelData();
        levelData.setTextLevel(textLevel);
        levelData.setChapters(chapters);
        return levelData;
    }

    private BookImportData.ChapterData createChapterData(int chapterNum, List<BookImportData.ChunkData> chunks) {
        BookImportData.ChapterData chapterData = new BookImportData.ChapterData();
        chapterData.setChapterNum(chapterNum);
        chapterData.setChunks(chunks);
        return chapterData;
    }

    private BookImportData.ChunkData createChunkData(String chunkText) {
        BookImportData.ChunkData chunkData = new BookImportData.ChunkData();
        chunkData.setChunkText(chunkText);
        return chunkData;
    }
}
