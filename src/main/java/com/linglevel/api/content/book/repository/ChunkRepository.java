package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.common.DifficultyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import com.linglevel.api.content.book.dto.ChunkCountByLevelDto;
import org.springframework.data.mongodb.repository.Aggregation;

import java.util.Optional;

public interface ChunkRepository extends MongoRepository<Chunk, String> {
    Page<Chunk> findByChapterId(String chapterId, Pageable pageable);

    Page<Chunk> findByChapterIdAndDifficultyLevel(String chapterId, DifficultyLevel difficultyLevel, Pageable pageable);

    Optional<Chunk> findFirstByChapterIdOrderByChunkNumberAsc(String chapterId);

    Optional<Chunk> findById(String chunkId);

    List<Chunk> findByChapterIdOrderByChunkNumber(String chapterId);

    // V2 Progress: Count chunks by difficulty level
    long countByChapterIdAndDifficultyLevel(String chapterId, DifficultyLevel difficultyLevel);

    @Aggregation(pipeline = {
        """
        {
            $match: {
                chapterId: { $in: ?0 }
            }
        }
        """,
        """
        {
            $group: {
                _id: {
                    chapterId: '$chapterId',
                    difficultyLevel: '$difficultyLevel'
                },
                count: { $sum: 1 }
            }
        }
        """,
        """
        {
            $project: {
                chapterId: '$_id.chapterId',
                difficultyLevel: '$_id.difficultyLevel',
                count: 1,
                _id: 0
            }
        }
        """
    })
    List<ChunkCountByLevelDto> findChunkCountsByChapterIds(List<String> chapterIds);

}