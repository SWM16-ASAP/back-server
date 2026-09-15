package com.linglevel.api.common.config;

import com.linglevel.api.banner.entity.ContentBanner;
import com.linglevel.api.banner.repository.ContentBannerRepository;
import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.crawling.entity.CrawlingDsl;
import com.linglevel.api.crawling.repository.CrawlingDslRepository;
import com.linglevel.api.i18n.CountryCode;
import com.linglevel.api.version.entity.AppVersion;
import com.linglevel.api.version.repository.AppVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConfigPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	CrawlingDslRepository crawlingDsls;

	@Autowired
	ContentBannerRepository banners;

	@Autowired
	AppVersionRepository appVersions;

	@Test
	void crawlingDslDomainIsUnique() {
		crawlingDsls.saveAndFlush(dsl("coupang.com"));
		assertThatThrownBy(() -> crawlingDsls.saveAndFlush(dsl("coupang.com")))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void nextDisplayOrderUsesHighestExistingOrderPerCountry() {
		banners.saveAndFlush(banner(CountryCode.KR, 1));
		banners.saveAndFlush(banner(CountryCode.KR, 5));
		banners.saveAndFlush(banner(CountryCode.US, 9));

		assertThat(
				banners.findFirstByCountryCodeOrderByDisplayOrderDesc(CountryCode.KR).orElseThrow().getDisplayOrder())
			.isEqualTo(5);
		assertThat(
				banners.findFirstByCountryCodeOrderByDisplayOrderDesc(CountryCode.US).orElseThrow().getDisplayOrder())
			.isEqualTo(9);
		assertThat(banners.findFirstByCountryCodeOrderByDisplayOrderDesc(CountryCode.JP)).isEmpty();
	}

	@Test
	void activeBannersAreOrderedByDisplayOrderAscending() {
		banners.saveAndFlush(banner(CountryCode.KR, 3));
		banners.saveAndFlush(banner(CountryCode.KR, 1));
		ContentBanner inactive = banner(CountryCode.KR, 2);
		inactive.setIsActive(false);
		banners.saveAndFlush(inactive);

		assertThat(banners.findByCountryCodeAndIsActiveTrueOrderByDisplayOrderAsc(CountryCode.KR))
			.extracting(ContentBanner::getDisplayOrder)
			.containsExactly(1, 3);
	}

	@Test
	void appVersionLookupReturnsMostRecentlyUpdatedRow() {
		AppVersion older = new AppVersion();
		older.setLatestVersion("1.0.0");
		older.setMinimumVersion("1.0.0");
		older.setUpdatedAt(LocalDateTime.now().minusDays(1));
		appVersions.saveAndFlush(older);

		AppVersion newer = new AppVersion();
		newer.setLatestVersion("2.0.0");
		newer.setMinimumVersion("1.5.0");
		newer.setUpdatedAt(LocalDateTime.now());
		appVersions.saveAndFlush(newer);

		assertThat(appVersions.findTopByOrderByUpdatedAtDesc().orElseThrow().getLatestVersion()).isEqualTo("2.0.0");
	}

	private CrawlingDsl dsl(String domain) {
		return CrawlingDsl.builder()
			.domain(domain)
			.name("Coupang")
			.titleDsl("h1.title")
			.contentDsl(".content")
			.createdAt(Instant.now())
			.updatedAt(Instant.now())
			.build();
	}

	private ContentBanner banner(CountryCode countryCode, int displayOrder) {
		ContentBanner banner = new ContentBanner();
		banner.setCountryCode(countryCode);
		banner.setContentId(1L);
		banner.setContentType(ContentType.BOOK);
		banner.setTitle("Banner");
		banner.setDescription("Description");
		banner.setDisplayOrder(displayOrder);
		banner.setCreatedAt(LocalDateTime.now());
		return banner;
	}

}
