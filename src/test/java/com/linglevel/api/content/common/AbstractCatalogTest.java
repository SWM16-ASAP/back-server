package com.linglevel.api.content.common;

import com.linglevel.api.common.AbstractMysqlTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration({ MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
		MongoRepositoriesAutoConfiguration.class })
public abstract class AbstractCatalogTest extends AbstractMysqlTest {

	@Container
	protected static final MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

	@DynamicPropertySource
	static void configureMongo(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
	}

}
