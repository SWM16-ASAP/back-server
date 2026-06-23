package com.linglevel.api.content.feed.filter.filters;

import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.content.feed.filter.FeedFilterResult;
import com.rometools.rome.feed.synd.SyndEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("LanguageFilter 테스트")
class LanguageFilterTest {

	private LanguageFilter filter;

	private FeedSource mockFeedSource;

	@BeforeEach
	void setUp() {
		filter = new LanguageFilter();
		mockFeedSource = mock(FeedSource.class);
	}

	@Test
	@DisplayName("영어 제목은 통과")
	void testEnglishTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("How to Build a REST API with Spring Boot");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertTrue(result.isPassed(), "영어 제목은 통과해야 함");
	}

	@Test
	@DisplayName("영어 + 숫자 + 특수문자 제목은 통과")
	void testEnglishWithNumbersAndSpecialChars() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("Top 10 JavaScript Libraries in 2024! (Must-Know)");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertTrue(result.isPassed(), "영어 + 숫자 + 특수문자는 통과해야 함");
	}

	@Test
	@DisplayName("영어 + 이모지 제목은 통과")
	void testEnglishWithEmoji() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("AI is Amazing 🚀🤖 - The Future of Technology 💡");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertTrue(result.isPassed(), "영어 + 이모지는 통과해야 함");
	}

	@Test
	@DisplayName("한글 제목은 필터링")
	void testKoreanTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("스프링 부트로 REST API 만들기");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "한글 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
		assertTrue(result.getReason().contains("Non-English title detected"));
	}

	@Test
	@DisplayName("일본어 제목은 필터링")
	void testJapaneseTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("プログラミングの基礎");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "일본어 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
	}

	@Test
	@DisplayName("중국어 제목은 필터링")
	void testChineseTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("学习编程的基础知识");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "중국어 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
	}

	@Test
	@DisplayName("힌디어 제목은 필터링")
	void testHindiTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("प्रोग्रामिंग सीखना");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "힌디어 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
	}

	@Test
	@DisplayName("아랍어 제목은 필터링")
	void testArabicTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("تعلم البرمجة");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "아랍어 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
	}

	@Test
	@DisplayName("페르시아어 제목은 필터링")
	void testPersianTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("خاله# تهران #شماره خاله# اصفهان");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "페르시아어 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
	}

	@Test
	@DisplayName("페르시아어 + 한글 혼합 제목은 필터링")
	void testPersianKoreanMixedTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("خاله# تهران #شماره خاله# اصفهان이런글자도");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "페르시아어/한글 혼합 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
	}

	@Test
	@DisplayName("러시아어 제목은 필터링")
	void testRussianTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("Основы программирования");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "러시아어 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
	}

	@Test
	@DisplayName("태국어 제목은 필터링")
	void testThaiTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("การเขียนโปรแกรม");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "태국어 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
	}

	@Test
	@DisplayName("영어 + 한글 혼합 제목은 필터링")
	void testMixedEnglishKorean() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("Spring Boot 튜토리얼");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "한글이 포함되어 있으면 필터링되어야 함");
	}

	@Test
	@DisplayName("빈 제목은 필터링")
	void testEmptyTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn("");

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "빈 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
		assertEquals("Empty title", result.getReason());
	}

	@Test
	@DisplayName("null 제목은 필터링")
	void testNullTitle() {
		// given
		SyndEntry entry = mock(SyndEntry.class);
		when(entry.getTitle()).thenReturn(null);

		// when
		FeedFilterResult result = filter.filter(entry, mockFeedSource);

		// then
		assertFalse(result.isPassed(), "null 제목은 필터링되어야 함");
		assertEquals("LanguageFilter", result.getFilterName());
		assertEquals("Empty title", result.getReason());
	}

	@Test
	@DisplayName("필터 이름 확인")
	void testFilterName() {
		// when
		String filterName = filter.getName();

		// then
		assertEquals("LanguageFilter", filterName);
	}

	@Test
	@DisplayName("필터 순서 확인 - URL 체크 다음")
	void testFilterOrder() {
		// when
		int order = filter.getOrder();

		// then
		assertEquals(20, order, "빠른 문자 체크이므로 우선순위가 높아야 함");
	}

	@Test
	@DisplayName("실제 영어 뉴스 제목들 테스트")
	void testRealEnglishNewsTitles() {
		String[] englishTitles = { "OpenAI Releases GPT-5 with Revolutionary Features",
				"How AI is Transforming Healthcare in 2024", "NASA Discovers New Exoplanet That Could Support Life",
				"The Future of Quantum Computing: A Deep Dive", "Breaking: Tech Giants Announce Major Collaboration",
				"10 Tips for Better Code Reviews 🚀", "Why JavaScript Still Dominates Web Development",
				"Understanding Machine Learning Basics (Part 1)" };

		for (String title : englishTitles) {
			SyndEntry entry = mock(SyndEntry.class);
			when(entry.getTitle()).thenReturn(title);

			FeedFilterResult result = filter.filter(entry, mockFeedSource);

			assertTrue(result.isPassed(), "영어 뉴스 제목은 통과해야 함: " + title);
		}
	}

	@Test
	@DisplayName("실제 비영어 뉴스 제목들 테스트")
	void testRealNonEnglishNewsTitles() {
		String[][] nonEnglishTitles = { { "한국어", "인공지능이 바꾸는 미래 사회" }, { "日本語", "新しいテクノロジーの世界" }, { "中文", "人工智能的未来发展" },
				{ "हिन्दी", "तकनीकी विकास की नई दिशा" }, { "العربية", "مستقبل الذكاء الاصطناعي" },
				{ "فارسی", "خاله# تهران #شماره خاله# اصفهان" }, { "Русский", "Будущее искусственного интеллекта" },
				{ "ไทย", "อนาคตของปัญญาประดิษฐ์" } };

		for (String[] titlePair : nonEnglishTitles) {
			String language = titlePair[0];
			String title = titlePair[1];

			SyndEntry entry = mock(SyndEntry.class);
			when(entry.getTitle()).thenReturn(title);

			FeedFilterResult result = filter.filter(entry, mockFeedSource);

			assertFalse(result.isPassed(), language + " 제목은 필터링되어야 함: " + title);
		}
	}

}
