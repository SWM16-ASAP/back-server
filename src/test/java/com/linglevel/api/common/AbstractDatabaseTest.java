package com.linglevel.api.common;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

/**
 * 데이터베이스 TestContainers를 사용하는 테스트의 추상 클래스
 *
 * 모든 테스트가 하나의 MongoDB 컨테이너를 공유하여 안정성과 성능을 보장합니다.
 *
 * 사용법:
 * @DataMongoTest
 * class MyRepositoryTest extends AbstractDatabaseTest {
 *     // TestContainers 설정 불필요, 상속받음
 * }
 */
public abstract class AbstractDatabaseTest {

    private static MongoDBContainer mongoContainer;

    static {
        mongoContainer = new MongoDBContainer("mongo:6.0").withReuse(true);
        mongoContainer.start();
    }

    @DynamicPropertySource
    static void configureDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);

        // 추가 데이터베이스 설정이 필요한 경우 여기서 일괄 관리
        // registry.add("spring.data.mongodb.database", () -> "test-db");
    }

    // 컨테이너 정리를 위한 메서드 (필요 시)
    public static MongoDBContainer getMongoContainer() {
        return mongoContainer;
    }
}