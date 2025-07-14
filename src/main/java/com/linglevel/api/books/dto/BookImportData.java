package com.linglevel.api.books.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BookImportData {
    
    @JsonProperty("novel_id")
    private String novelId;
    private String title;
    private String author;
    @JsonProperty("original_text_level")
    private String originalTextLevel;
    @JsonProperty("chapter_metadata")
    private List<ChapterMetadata> chapterMetadata;
    @JsonProperty("leveled_results")
    private List<TextLevelData> leveledResults;
    
    @Data
    public static class ChapterMetadata {
        private int chapterNum;
        private String title;
        private String summary;
    }
    
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