package com.linglevel.api.content.feed.repository;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.feed.entity.Feed;
import com.linglevel.api.content.feed.entity.FeedContentType;
import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.content.recommendation.entity.UserCategoryPreference;
import com.linglevel.api.content.recommendation.repository.UserCategoryPreferenceRepository;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.entity.UserRole;
import com.linglevel.api.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FeedPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	FeedRepository feeds;

	@Autowired
	FeedSourceRepository feedSources;

	@Autowired
	UserCategoryPreferenceRepository preferences;

	@Autowired
	UserRepository users;

	@Autowired
	EntityManager em;

	@Test
	void duplicateFeedUrlIsRejected() {
		feeds.saveAndFlush(feed("https://example.com/article-1"));
		assertThatThrownBy(() -> feeds.saveAndFlush(feed("https://example.com/article-1")))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void feedUrlUniquenessCoversTheFullUrlNotJustA255CharPrefix() {
		// uk_feeds_url_hash hashes the full url, so two URLs sharing a 255-char prefix
		// must both save.
		String sharedPrefix = "https://example.com/article?" + "a".repeat(250);
		feeds.saveAndFlush(feed(sharedPrefix + "-one"));
		Feed second = feeds.saveAndFlush(feed(sharedPrefix + "-two"));

		assertThat(second.getId()).isNotNull();
	}

	@Test
	void viewCountIncrementsAtomicallyByIdAndByUrl() {
		Feed saved = feeds.saveAndFlush(feed("https://example.com/article-2"));
		feeds.incrementViewCount(saved.getId().toString());
		feeds.incrementViewCountByUrl(saved.getUrl());
		em.clear();
		assertThat(feeds.findById(saved.getId()).orElseThrow().getViewCount()).isEqualTo(2);
	}

	@Test
	void duplicateFeedSourceUrlIsRejected() {
		feedSources.saveAndFlush(feedSource("https://example.com/rss"));
		assertThatThrownBy(() -> feedSources.saveAndFlush(feedSource("https://example.com/rss")))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void feedSourceUrlUniquenessCoversTheFullUrlNotJustA255CharPrefix() {
		String sharedPrefix = "https://example.com/rss?" + "b".repeat(250);
		feedSources.saveAndFlush(feedSource(sharedPrefix + "-one"));
		FeedSource second = feedSources.saveAndFlush(feedSource(sharedPrefix + "-two"));

		assertThat(second.getId()).isNotNull();
	}

	@Test
	void userCategoryPreferenceRequiresExistingUser() {
		UserCategoryPreference orphan = UserCategoryPreference.builder().userId(Long.MAX_VALUE).build();
		assertThatThrownBy(() -> preferences.saveAndFlush(orphan))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void userCategoryPreferenceIsUniquePerUser() {
		User user = users.saveAndFlush(User.builder().username("reader").role(UserRole.USER).build());
		preferences.saveAndFlush(UserCategoryPreference.builder().userId(user.getId()).build());
		UserCategoryPreference duplicate = UserCategoryPreference.builder().userId(user.getId()).build();
		assertThatThrownBy(() -> preferences.saveAndFlush(duplicate))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void categoryScoresJsonRoundTrips() {
		User user = users.saveAndFlush(User.builder().username("reader2").role(UserRole.USER).build());
		UserCategoryPreference preference = UserCategoryPreference.builder()
			.userId(user.getId())
			.primaryCategory(ContentCategory.TECH)
			.categoryScores(Map.of(ContentCategory.TECH, 0.8, ContentCategory.SPORTS, 0.2))
			.rawAccessCounts(Map.of(ContentCategory.TECH, 4))
			.totalAccessCount(5)
			.lastUpdatedAt(Instant.now())
			.build();
		preferences.saveAndFlush(preference);
		em.clear();
		UserCategoryPreference loaded = preferences.findByUserId(user.getId()).orElseThrow();
		assertThat(loaded.getCategoryScores()).containsEntry(ContentCategory.TECH, 0.8);
		assertThat(loaded.getRawAccessCounts()).containsEntry(ContentCategory.TECH, 4);
	}

	private Feed feed(String url) {
		return Feed.builder()
			.contentType(FeedContentType.NEWS)
			.title("Title")
			.url(url)
			.category(ContentCategory.TECH)
			.tags(List.of("tech"))
			.viewCount(0)
			.createdAt(Instant.now())
			.build();
	}

	private FeedSource feedSource(String url) {
		return FeedSource.builder()
			.url(url)
			.name("Source")
			.contentType(FeedContentType.NEWS)
			.isActive(true)
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();
	}

}
