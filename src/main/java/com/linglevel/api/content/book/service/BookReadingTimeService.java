package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.BookImportData;
import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.service.ReadingTimeService;
import com.linglevel.api.content.book.exception.BooksErrorCode;
import com.linglevel.api.content.book.exception.BooksException;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.book.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookReadingTimeService {

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final ReadingTimeService readingTimeService;

    public void updateReadingTimes(String bookId, BookImportData importData) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.BOOK_NOT_FOUND));
        
        List<Chapter> chapters = chapterRepository.findByBookIdOrderByChapterNumber(bookId);
        
        int totalBookReadingTime = 0;
        
        for (Chapter chapter : chapters) {
            int chapterReadingTime = calculateChapterReadingTime(chapter.getChapterNumber(), book.getDifficultyLevel(), importData);
            chapter.setReadingTime(chapterReadingTime);
            totalBookReadingTime += chapterReadingTime;
        }
        
        book.setReadingTime(totalBookReadingTime);
        
        chapterRepository.saveAll(chapters);
        bookRepository.save(book);
    }
    
    private int calculateChapterReadingTime(int chapterNumber, DifficultyLevel difficultyLevel, BookImportData importData) {
        int totalCharacters = importData.getLeveledResults().stream()
            .filter(levelData -> DifficultyLevel.valueOf(levelData.getTextLevel().toUpperCase()) == difficultyLevel)
            .flatMap(levelData -> levelData.getChapters().stream())
            .filter(chapterData -> chapterData.getChapterNum() == chapterNumber)
            .flatMap(chapterData -> chapterData.getChunks().stream())
            .mapToInt(chunkData -> chunkData.getChunkText().length())
            .sum();
        
        return calculateReadingTimeFromCharacters(totalCharacters);
    }
    
    private int calculateReadingTimeFromCharacters(int characterCount) {
        return readingTimeService.calculateReadingTimeFromCharacters(characterCount);
    }
}