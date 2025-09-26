package com.linglevel.api.content.common.service.provider;

import com.linglevel.api.content.book.entity.Book;
import com.linglevel.api.content.book.repository.BookRepository;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.service.ContentInfo;
import com.linglevel.api.content.common.service.ContentInfoProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 도서 콘텐츠 정보 제공자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookContentInfoProvider implements ContentInfoProvider {

    private final BookRepository bookRepository;

    @Override
    public ContentType getSupportedType() {
        return ContentType.BOOK;
    }

    @Override
    public ContentInfo getContentInfo(String contentId) {
        try {
            return bookRepository.findById(contentId)
                    .map(this::convertToContentInfo)
                    .orElse(ContentInfo.builder().build());
        } catch (Exception e) {
            return ContentInfo.builder().build();
        }
    }

    private ContentInfo convertToContentInfo(Book book) {
        return ContentInfo.builder()
                .title(book.getTitle())
                .author(book.getAuthor())
                .coverImageUrl(book.getCoverImageUrl())
                .readingTime(book.getReadingTime())
                .build();
    }
}