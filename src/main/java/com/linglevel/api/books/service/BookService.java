package com.linglevel.api.books.service;

import com.linglevel.api.books.dto.*;
import com.linglevel.api.books.entity.Book;
import com.linglevel.api.books.entity.Chapter;
import com.linglevel.api.books.entity.Chunk;
import com.linglevel.api.books.entity.ChunkType;
import com.linglevel.api.books.entity.DifficultyLevel;
import com.linglevel.api.s3.service.S3StaticService;
import com.linglevel.api.books.exception.BooksErrorCode;
import com.linglevel.api.books.exception.BooksException;
import com.linglevel.api.books.repository.BookRepository;
import com.linglevel.api.books.repository.ChapterRepository;
import com.linglevel.api.books.repository.ChunkRepository;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.s3.service.S3AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final ChunkRepository chunkRepository;
    private final S3AiService s3AiService;
    private final S3StaticService s3StaticService;

    private final int AVERAGE_READING_SPEED_PER_MINUTE = 500;


    @Transactional
    public BookImportResponse importBook(BookImportRequest request) {
        log.info("Starting book import for file: {}", request.getId());
        BookImportData importData = s3AiService.downloadJsonFile(request.getId() + "/" + request.getId(), BookImportData.class);

        Book book = createBook(importData, request.getId());
        Book savedBook = bookRepository.save(book);
        
        uploadImagesFromAiToStatic(request.getId(), savedBook.getId());

        String coverImageUrl = getCoverImageUrl(savedBook.getId());
        savedBook.setCoverImageUrl(coverImageUrl);
        bookRepository.save(savedBook);
        
        List<Chapter> savedChapters = createChaptersFromMetadata(importData, savedBook.getId());
        
        createChunksFromLeveledResults(importData, savedChapters, savedBook.getId());
        
        updateReadingTimes(savedBook.getId(), importData);
        
        log.info("Successfully imported book with id: {}", savedBook.getId());
        return new BookImportResponse(savedBook.getId());
    }


    private Book createBook(BookImportData importData, String requestId) {
        Book book = new Book();
        book.setTitle(importData.getTitle());
        book.setAuthor(importData.getAuthor());
        DifficultyLevel difficultyLevel = DifficultyLevel.valueOf(importData
                .getOriginalTextLevel().toUpperCase());
        book.setDifficultyLevel(difficultyLevel);

        String coverImageUrl = getCoverImageUrl(requestId);
        book.setCoverImageUrl(coverImageUrl);
        
        book.setViewCount(0);
        book.setAverageRating(0.0);
        book.setReviewCount(0);
        book.setReadingTime(0);
        book.setCreatedAt(LocalDateTime.now());
        
        int chapterCount = importData.getLeveledResults().isEmpty() ? 0 :
                          importData.getLeveledResults().get(0).getChapters().size();

        book.setChapterCount(chapterCount);
        
        return book;
    }

    private List<Chapter> createChaptersFromMetadata(BookImportData importData, String bookId) {
        List<Chapter> chapters = importData.getChapterMetadata().stream()
                .map(metadata -> {
                    Chapter chapter = new Chapter();
                    chapter.setBookId(bookId);
                    chapter.setChapterNumber(metadata.getChapterNum());
                    chapter.setTitle(metadata.getTitle());
                    chapter.setDescription(metadata.getSummary());
                    chapter.setReadingTime(0);
                    
                    int chunkCount = getChunkCountForChapter(importData, metadata.getChapterNum());
                    chapter.setChunkCount(chunkCount);
                    
                    return chapter;
                })
                .collect(Collectors.toList());
        
        return chapterRepository.saveAll(chapters);
    }
    
    private int getChunkCountForChapter(BookImportData importData, int chapterNum) {
        return importData.getLeveledResults().stream()
                .findFirst()
                .flatMap(firstLevel -> 
                    firstLevel.getChapters().stream()
                        .filter(chapterData -> chapterData.getChapterNum() == chapterNum)
                        .findFirst()
                        .map(chapterData -> chapterData.getChunks().size())
                )
                .orElse(0);
    }
    
    private void createChunksFromLeveledResults(BookImportData importData, List<Chapter> savedChapters, String databaseBookId) {
        Map<Integer, Chapter> chapterMap = savedChapters.stream()
                .collect(Collectors.toMap(Chapter::getChapterNumber, chapter -> chapter));
        
        List<Chunk> allChunks = importData.getLeveledResults().stream()
                .flatMap(textLevelData ->
                    textLevelData.getChapters().stream()
                        .flatMap(chapterData ->
                            chapterData.getChunks().stream()
                                .map(chunkData -> createChunk(
                                    chunkData,
                                    chapterMap.get(chapterData.getChapterNum()),
                                    textLevelData.getTextLevel(),
                                    databaseBookId
                                ))
                        )
                )
                .collect(Collectors.toList());
        
        chunkRepository.saveAll(allChunks);
    }
    
    private Chunk createChunk(BookImportData.ChunkData chunkData, Chapter chapter, String difficultyLevel, String bookId) {
        Chunk chunk = new Chunk();
        chunk.setChapterId(chapter.getId());
        chunk.setChunkNumber(chunkData.getChunkNum());
        chunk.setDifficulty(DifficultyLevel.valueOf(difficultyLevel.toUpperCase()));
        
        if (Boolean.TRUE.equals(chunkData.getIsImage())) {
            chunk.setType(ChunkType.IMAGE);
            String imageUrl = buildImageUrl(bookId, chunkData.getChunkText()); 
            chunk.setContent(imageUrl);
            chunk.setDescription(chunkData.getDescription());
        } else {
            chunk.setType(ChunkType.TEXT);
            chunk.setContent(chunkData.getChunkText());
            chunk.setDescription(null);
        }
        
        return chunk;
    }
    
    private String buildImageUrl(String bookId, String imageFileName) {
        return getImageUrl(bookId, imageFileName);
    }
    
    private String getCoverImageUrl(String bookId) {
        return s3StaticService.getPublicUrl("books/" + bookId + "/images/cover.jpg");
    }
    
    private String getImageUrl(String bookId, String imageFileName) {
        return s3StaticService.getPublicUrl("books/" + bookId + "/images/" + imageFileName);
    }
    
    private String getBookImagePath(String bookId) {
        return "books/" + bookId;
    }

    private void uploadImagesFromAiToStatic(String requestId, String bookId) {
        try {
            log.info("Starting image upload from AI bucket to Static bucket for requestId: {} to bookId: {}", requestId, bookId);
            
            List<String> imageKeys = s3AiService.listImagesInFolder(requestId);
            
            for (String imageKey : imageKeys) {
                byte[] imageBytes = s3AiService.downloadImageFile(imageKey);
                String contentType = getContentTypeFromKey(imageKey);

                String newKey = imageKey.replace(requestId, getBookImagePath(bookId));
                s3StaticService.uploadFileFromBytes(imageBytes, newKey, contentType);
            }
            
            log.info("Successfully uploaded {} images to Static bucket with bookId path", imageKeys.size());
            
        } catch (Exception e) {
            log.error("Failed to upload images from AI to Static bucket: {}", e.getMessage());
            throw new RuntimeException("Image upload failed", e);
        }
    }

    private String getContentTypeFromKey(String key) {
        String lowerKey = key.toLowerCase();
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerKey.endsWith(".png")) {
            return "image/png";
        } else if (lowerKey.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerKey.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    public PageResponse<BookResponse> getBooks(GetBooksRequest request) {
        Sort sort = createSort(request.getSortBy());

        Pageable pageable = PageRequest.of(
            request.getPage() - 1, 
            Math.min(request.getLimit(), 50),
            sort
        );

        // 전체 책 조회 (TODO: 필터링 로직 추가)
        Page<Book> bookPage = bookRepository.findAll(pageable);

        List<BookResponse> bookResponses = bookPage.getContent().stream()
            .map(this::convertToBookResponse)
            .collect(Collectors.toList());

        return new PageResponse<>(bookResponses, bookPage);
    }

    public BookResponse getBook(String bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.BOOK_NOT_FOUND));
        
        return convertToBookResponse(book);
    }

    public boolean existsById(String bookId) { return bookRepository.existsById(bookId); }

    public Book findById(String bookId) {
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new BooksException(BooksErrorCode.BOOK_NOT_FOUND));
    }

    private Sort createSort(String sortBy) {
        if (sortBy == null) {
            sortBy = "created_at";
        }

        return switch (sortBy.toLowerCase()) {
            case "view_count" -> Sort.by("viewCount").descending();
            case "average_rating" -> Sort.by("averageRating").descending();
            case "created_at" -> Sort.by("createdAt").descending();
            default -> throw new BooksException(BooksErrorCode.INVALID_SORT_BY);
        };
    }

    private void updateReadingTimes(String bookId, BookImportData importData) {
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
        return (int) Math.ceil((double) characterCount / AVERAGE_READING_SPEED_PER_MINUTE);
    }

    private BookResponse convertToBookResponse(Book book) {
        return BookResponse.builder()
            .id(book.getId())
            .title(book.getTitle())
            .author(book.getAuthor())
            .coverImageUrl(book.getCoverImageUrl())
            .difficultyLevel(book.getDifficultyLevel())
            .chapterCount(book.getChapterCount())
            .currentReadChapterNumber(0) // TODO: 실제 진도 계산
            .progressPercentage(0.0) // TODO: 실제 진도 계산
            .readingTime(book.getReadingTime())
            .averageRating(book.getAverageRating())
            .reviewCount(book.getReviewCount())
            .viewCount(book.getViewCount())
            .tags(book.getTags())
            .createdAt(book.getCreatedAt())
            .build();
    }
} 