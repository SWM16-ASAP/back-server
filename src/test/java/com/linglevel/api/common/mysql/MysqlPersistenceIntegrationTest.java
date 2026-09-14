package com.linglevel.api.common.mysql;

import com.linglevel.api.common.AbstractMysqlTest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.bson.Document;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = MysqlPersistenceIntegrationTest.Config.class)
@ImportAutoConfiguration({ MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MysqlPersistenceIntegrationTest extends AbstractMysqlTest {

	@Container
	static final MongoDBContainer mongo = new MongoDBContainer("mongo:6.0");

	@DynamicPropertySource
	static void configureMongo(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
	}

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JpaTransactionManager transactionManager;

	@Autowired
	private Flyway flyway;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Test
	void appliesMigrationOnlyOnce() {
		assertThat(flyway.info().applied()).hasSize(1);
		assertThat(flyway.migrate().migrationsExecuted).isZero();
	}

	@Test
	void commitsAndReadsJpaEntity() {
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		tx.executeWithoutResult(status -> entityManager.persist(new Probe(1L, "committed")));
		assertThat(tx.<String>execute(status -> entityManager.find(Probe.class, 1L).value)).isEqualTo("committed");
	}

	@Test
	void rollsBackOnFailure() {
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
			entityManager.persist(new Probe(2L, "rolled-back"));
			entityManager.flush();
			throw new IllegalStateException("simulated failure");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(tx.<Probe>execute(status -> entityManager.find(Probe.class, 2L))).isNull();
	}

	@Test
	void uniqueViolationRollsBackWholeTransaction() {
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
			entityManager.persist(new Probe(3L, "duplicate"));
			entityManager.persist(new Probe(4L, "duplicate"));
			entityManager.flush();
		})).isInstanceOf(RuntimeException.class);
		assertThat(tx.<Probe>execute(status -> entityManager.find(Probe.class, 3L))).isNull();
		assertThat(tx.<Probe>execute(status -> entityManager.find(Probe.class, 4L))).isNull();
	}

	@Test
	void mongoRemainsIndependentOfMysqlRollback() {
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		try {
			tx.executeWithoutResult(status -> {
				entityManager.persist(new Probe(5L, "mysql-only"));
				mongoTemplate.insert(new Document("_id", "independent"), "persistence_probe");
				status.setRollbackOnly();
			});
			assertThat(tx.<Probe>execute(status -> entityManager.find(Probe.class, 5L))).isNull();
			assertThat(mongoTemplate.findById("independent", Document.class, "persistence_probe")).isNotNull();
		}
		finally {
			mongoTemplate.dropCollection("persistence_probe");
		}
	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigurationPackage
	@EntityScan(basePackageClasses = Probe.class)
	static class Config {

	}

	@Entity
	@Table(name = "persistence_probe")
	static class Probe {

		@Id
		private Long id;

		@Column(name = "value_text", nullable = false, length = 100)
		private String value;

		protected Probe() {
		}

		Probe(Long id, String value) {
			this.id = id;
			this.value = value;
		}

	}

}
