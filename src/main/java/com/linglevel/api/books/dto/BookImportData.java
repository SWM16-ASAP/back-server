package com.linglevel.api.books.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookImportData {
    
    private String id;
    private String title;
    private String author;
    private String originalLevel;
    private String imgUrl;
    private List<TextLevelData> result;
    
    @Data
    public static class TextLevelData {
        private String textLevel;
        private List<ChapterData> chapters;
    }
    
    @Data
    public static class ChapterData {
        private int chapterNum;
        private List<ChunkData> chunks;
    }
    
    @Data
    public static class ChunkData {
        private int chunkNum;
        private String chunkText;
    }
} 