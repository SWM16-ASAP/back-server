package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.repository.dto.ArticleChunkCount;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.article.entity.ArticleChunk;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleChunkRepositoryImpl implements ArticleChunkRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ArticleChunkCount> countChunksByArticleIds(List<String> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return List.of();
        }

        MatchOperation match = Aggregation.match(Criteria.where("articleId").in(articleIds));
        GroupOperation group = Aggregation.group("articleId", "difficultyLevel").count().as("count");

        Aggregation aggregation = Aggregation.newAggregation(match, group);

        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation,
                ArticleChunk.class,
                Document.class
        );

        List<ArticleChunkCount> counts = new ArrayList<>();

        for (Document document : results) {
            Document idDoc = document.get("_id", Document.class);
            if (idDoc == null) {
                continue;
            }

            String articleId = idDoc.getString("articleId");
            String difficultyLevelValue = idDoc.getString("difficultyLevel");

            if (articleId == null || difficultyLevelValue == null) {
                continue;
            }

            DifficultyLevel difficultyLevel = DifficultyLevel.valueOf(difficultyLevelValue);
            long count = document.get("count", Number.class).longValue();

            counts.add(new ArticleChunkCount(articleId, difficultyLevel, count));
        }

        return counts;
    }
}
