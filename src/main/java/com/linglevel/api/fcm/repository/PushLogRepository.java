package com.linglevel.api.fcm.repository;

import com.linglevel.api.fcm.entity.PushLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PushLogRepository extends MongoRepository<PushLog, String> {

    Optional<PushLog> findByCampaignId(String campaignId);

    List<PushLog> findByCampaignGroup(String campaignGroup);

    List<PushLog> findByUserId(String userId);

    List<PushLog> findBySentAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
