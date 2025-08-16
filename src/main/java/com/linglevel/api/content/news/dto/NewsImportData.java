package com.linglevel.api.content.news.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class NewsImportData {
    
    private String id;
    @JsonProperty("content_type")
    private String contentType;
    private String title;
    private String author;
    @JsonProperty("cover_image_url")
    private String coverImageUrl;
    @JsonProperty("original_text_level")
    private String originalTextLevel;
    @JsonProperty("leveled_results")
    private List<TextLevelData> leveledResults;
    
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
        private Boolean isImage;
        private String description;
    }
}