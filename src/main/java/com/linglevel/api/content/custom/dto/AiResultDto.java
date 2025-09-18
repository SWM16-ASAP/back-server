package com.linglevel.api.content.custom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiResultDto {
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
    private List<LeveledResult> leveledResults;
    
    @JsonProperty("chapter_metadata")
    private List<ChapterMetadata> chapterMetadata;
    
    @Getter
    @Setter
    public static class LeveledResult {
        @JsonProperty("textLevel")
        private String textLevel;
        
        @JsonProperty("chapters")
        private List<Chapter> chapters;
    }
    
    @Getter
    @Setter
    public static class Chapter {
        @JsonProperty("chapterNum")
        private Integer chapterNum;
        
        @JsonProperty("chunks")
        private List<Chunk> chunks;
    }
    
    @Getter
    @Setter
    public static class Chunk {
        @JsonProperty("chunkNum")
        private Integer chunkNum;
        
        @JsonProperty("isImage")
        private Boolean isImage;
        
        @JsonProperty("chunkText")
        private String chunkText;
        
        @JsonProperty("description")
        private String description;
    }
    
    @Getter
    @Setter
    public static class ChapterMetadata {
        @JsonProperty("chapterNum")
        private Integer chapterNum;
        
        @JsonProperty("summary")
        private String summary;
    }
}