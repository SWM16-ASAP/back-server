package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.entity.Feed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Feed> findByDeletedFalseWithProjection() {
        Query query = new Query(Criteria.where("deleted").is(false));

        query.fields()
                .exclude("displayOrder")
                .exclude("deleted")
                .exclude("deletedAt");

        return mongoTemplate.find(query, Feed.class);
    }
}