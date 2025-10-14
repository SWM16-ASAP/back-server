package com.linglevel.api.content.book.dto;

import com.linglevel.api.content.common.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkCountByLevelDto {
    private String chapterId;
    private DifficultyLevel difficultyLevel;
    private long count;
}
