package com.linglevel.api.streak.service;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.streak.entity.*;
import com.linglevel.api.streak.repository.*;
import com.linglevel.api.user.entity.*;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.user.ticket.service.TicketService;
import com.linglevel.api.user.ticket.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ StreakService.class, StudyReportLock.class, TicketService.class, StudyTimeAnalysisService.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StreakPersistenceIntegrationTest extends AbstractMysqlTest {

	@Autowired
	StreakService service;

	@Autowired
	UserRepository users;

	@Autowired
	UserStudyReportRepository reports;

	@Autowired
	DailyCompletionRepository days;

	@Autowired
	LearningCompletionRepository learning;

	@Autowired
	FreezeTransactionRepository freezes;

	@Autowired
	TicketTransactionRepository tickets;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	StudyTimeAnalysisService studyTime;

	@MockitoSpyBean
	TicketService ticketService;

	@MockitoBean
	ReadingSessionService readingSessionService;

	private String userId;

	private TransactionTemplate tx;

	private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

	@BeforeEach
	void createUser() {
		userId = users.save(User.builder().username("streak-" + UUID.randomUUID()).role(UserRole.USER).build())
			.getId()
			.toString();
		tx = new TransactionTemplate(transactionManager);
	}

	@Test
	void createsReportOnceForConcurrentFirstRequests() throws Exception {
		parallel(8, () -> service.getStreakInfo(userId, LanguageCode.KO));
		assertThat(reports.findByUserId(userId)).isPresent();
		assertThat(reports.findAll().stream().filter(r -> r.getUserId().toString().equals(userId)).count())
			.isEqualTo(1);
	}

	@Test
	void recordsRepeatReadsSeparatelyButCountsUniqueContentByType() {
		service.addCompletedContent(userId, ContentType.BOOK, "1", false);
		service.addCompletedContent(userId, ContentType.BOOK, "1", false);
		service.addCompletedContent(userId, ContentType.ARTICLE, "1", false);
		var day = day(today);
		assertThat(day.getTotalCompletionCount()).isEqualTo(3);
		assertThat(day.getFirstCompletionCount()).isEqualTo(2);
		assertThat(day.getCompletedContents()).hasSize(3);
		assertThat(service.getStreakInfo(userId, LanguageCode.EN).getTotalContentsRead()).isEqualTo(2);
	}

	@Test
	void concurrentSameContentDoesNotDoubleCountFirstCompletion() throws Exception {
		parallel(8, () -> service.addCompletedContent(userId, ContentType.BOOK, "1", false));
		var day = day(today);
		assertThat(day.getFirstCompletionCount()).isEqualTo(1);
		assertThat(day.getTotalCompletionCount()).isEqualTo(8);
		assertThat(day.getCompletedContents()).hasSize(8);
	}

	@Test
	void concurrentSeventhDayGrantsOneTicket() throws Exception {
		seedReport(6, 0, today.minusDays(1));
		ticketService.getTicketBalance(userId);
		parallel(8, () -> service.updateStreak(userId, ContentType.BOOK, "1"));
		assertThat(report().getCurrentStreak()).isEqualTo(7);
		assertThat(day(today).getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
		assertThat(ticketService.getTicketBalance(userId).getBalance()).isEqualTo(11);
		assertThat(tickets.findAll()
			.stream()
			.filter(t -> t.getUserId().toString().equals(userId)
					&& t.getDescription().equals("Reward for 7-day streak"))
			.count()).isEqualTo(1);
	}

	@Test
	void concurrentFifthDayGrantsOneFreeze() throws Exception {
		seedReport(4, 0, today.minusDays(1));
		parallel(8, () -> service.updateStreak(userId, ContentType.BOOK, "1"));
		assertThat(report().getAvailableFreezes()).isEqualTo(1);
		assertThat(freezeLedger()).hasSize(1);
		assertThat(report().getCurrentStreak()).isEqualTo(5);
	}

	@Test
	void failedTicketGrantRollsBackEntireLearningTransaction() {
		seedReport(6, 0, today.minusDays(1));
		doThrow(new IllegalStateException("ticket failure")).when(ticketService)
			.grantTicket(eq(userId), eq(1), anyString());
		assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
			service.addStudyTime(userId, 60);
			service.addCompletedContent(userId, ContentType.BOOK, "1", false);
			service.updateStreak(userId, ContentType.BOOK, "1");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(report().getCurrentStreak()).isEqualTo(6);
		assertThat(report().getTotalReadingTimeSeconds()).isZero();
		assertThat(days.findByUserIdAndCompletionDate(userId, today)).isEmpty();
		assertThat(learning.countDistinctContents(Long.valueOf(userId))).isZero();
	}

	@Test
	void concurrentStudyTimeUpdatesAccumulate() throws Exception {
		parallel(8, () -> service.addStudyTime(userId, 30));
		assertThat(report().getTotalReadingTimeSeconds()).isEqualTo(240);
		assertThatThrownBy(() -> service.addStudyTime(userId, -1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void concurrentMissedDayProcessingConsumesFreezeOnceAndIgnoresStaleReport() throws Exception {
		seedReport(4, 1, today.minusDays(2));
		UserStudyReport stale = report();
		parallel(8, () -> service.processMissedDays(stale, today));
		assertThat(report().getAvailableFreezes()).isZero();
		assertThat(report().getCurrentStreak()).isEqualTo(4);
		assertThat(day(today.minusDays(1)).getStreakStatus()).isEqualTo(StreakStatus.FREEZE_USED);
		assertThat(freezeLedger()).hasSize(1);
		assertThat(freezeLedger().get(0).getEffectiveDate()).isEqualTo(today.minusDays(1));
	}

	@Test
	void recoveryIsAtomicAndRepeatedCompensationIsIgnored() {
		seedReport(3, 0, today.minusDays(2));
		days.save(DailyCompletion.builder()
			.userId(Long.valueOf(userId))
			.completionDate(today.minusDays(1))
			.streakStatus(StreakStatus.FREEZE_USED)
			.streakCount(3)
			.build());
		service.recoverStreak(userId, today.minusDays(1), today.minusDays(1));
		service.recoverStreak(userId, today.minusDays(1), today.minusDays(1));
		assertThat(report().getAvailableFreezes()).isEqualTo(1);
		assertThat(freezeLedger()).hasSize(1);
		assertThat(day(today.minusDays(1)).getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
	}

	@Test
	void dailyUniquenessAndUserForeignKeyAreEnforced() {
		days.save(DailyCompletion.builder().userId(Long.valueOf(userId)).completionDate(today).build());
		assertThatThrownBy(() -> days
			.saveAndFlush(DailyCompletion.builder().userId(Long.valueOf(userId)).completionDate(today).build()))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(
				() -> days.saveAndFlush(DailyCompletion.builder().userId(Long.MAX_VALUE).completionDate(today).build()))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void dateRangeIncludesBothCalendarEdgesAndLedgerExcludesNextDay() {
		LocalDate start = today.minusDays(2);
		for (LocalDate d : List.of(start.minusDays(1), start, today, today.plusDays(1)))
			days.save(DailyCompletion.builder().userId(Long.valueOf(userId)).completionDate(d).build());
		assertThat(days.findByUserIdAndCompletionDateBetween(userId, start, today))
			.extracting(DailyCompletion::getCompletionDate)
			.containsExactlyInAnyOrder(start, today);
		Instant midnight = today.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
		freezes.save(FreezeTransaction.builder().userId(Long.valueOf(userId)).amount(1).createdAt(midnight).build());
		freezes.save(FreezeTransaction.builder()
			.userId(Long.valueOf(userId))
			.amount(1)
			.createdAt(midnight.plusSeconds(86400))
			.build());
		assertThat(freezes.findByUserIdAndAmountAndCreatedAtBetween(userId, 1, midnight, midnight.plusSeconds(86400)))
			.hasSize(1);
	}

	@Test
	void deletingDailySummaryDeletesItsHistoryAndUniqueCountIsDerived() {
		service.addCompletedContent(userId, ContentType.BOOK, "1", false);
		tx.executeWithoutResult(status -> days.delete(days.findByUserIdAndCompletionDate(userId, today).orElseThrow()));
		assertThat(learning.countDistinctContents(Long.valueOf(userId))).isZero();
	}

	@Test
	void preferredHourReadsNormalizedHistoryWithinTransaction() {
		service.addCompletedContent(userId, ContentType.BOOK, "1", false);
		assertThat(studyTime.calculateAndSavePreferredStudyHour(userId)).isPresent();
		assertThat(report().getPreferredStudyHour()).isNotNull();
	}

	@Test
	void completionUsesCurrentRowsEvenWhenOuterTransactionAlreadyRead() throws Exception {
		seedReport(6, 0, today.minusDays(1));
		ticketService.getTicketBalance(userId);
		CountDownLatch snapshots = new CountDownLatch(4);
		parallel(4, () -> tx.executeWithoutResult(status -> {
			users.findById(userId).orElseThrow();
			snapshots.countDown();
			try {
				if (!snapshots.await(10, TimeUnit.SECONDS))
					throw new IllegalStateException("barrier timeout");
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
			service.updateStreak(userId, ContentType.BOOK, "1");
			service.addCompletedContent(userId, ContentType.BOOK, "1", false);
		}));
		assertThat(ticketService.getTicketBalance(userId).getBalance()).isEqualTo(11);
		assertThat(day(today).getFirstCompletionCount()).isEqualTo(1);
		assertThat(day(today).getTotalCompletionCount()).isEqualTo(4);
	}

	@Test
	void freezeUpdatesExistingMissedDayInsteadOfInsertingDuplicate() {
		seedReport(4, 1, today.minusDays(2));
		days.save(DailyCompletion.builder()
			.userId(Long.valueOf(userId))
			.completionDate(today.minusDays(1))
			.streakStatus(StreakStatus.MISSED)
			.build());
		service.processMissedDays(report(), today);
		assertThat(day(today.minusDays(1)).getStreakStatus()).isEqualTo(StreakStatus.FREEZE_USED);
		assertThat(freezes.existsByUserIdAndAmountAndEffectiveDate(userId, -1, today.minusDays(1))).isTrue();
	}

	@Test
	void recoveryCapAdjustmentKeepsLedgerConsistent() {
		seedReport(3, 2, today.minusDays(2));
		days.save(DailyCompletion.builder()
			.userId(Long.valueOf(userId))
			.completionDate(today.minusDays(1))
			.streakStatus(StreakStatus.FREEZE_USED)
			.streakCount(3)
			.build());
		service.recoverStreak(userId, today.minusDays(1), today.minusDays(1));
		assertThat(report().getAvailableFreezes()).isEqualTo(2);
		assertThat(freezeLedger().stream().mapToInt(FreezeTransaction::getAmount).sum()).isZero();
	}

	private UserStudyReport report() {
		return reports.findByUserId(userId).orElseThrow();
	}

	private DailyCompletion day(LocalDate date) {
		return tx.execute(s -> {
			var d = days.findByUserIdAndCompletionDate(userId, date).orElseThrow();
			d.getCompletedContents().size();
			return d;
		});
	}

	private List<FreezeTransaction> freezeLedger() {
		return freezes.findByUserIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.PageRequest.of(0, 100))
			.getContent();
	}

	private void seedReport(int streak, int balance, LocalDate last) {
		UserStudyReport report = new UserStudyReport();
		report.setUserId(Long.valueOf(userId));
		report.setCurrentStreak(streak);
		report.setLongestStreak(streak);
		report.setAvailableFreezes(balance);
		report.setLastCompletionDate(last);
		report.setStreakStartDate(last.minusDays(Math.max(0, streak - 1)));
		reports.save(report);
	}

	private void parallel(int count, Runnable action) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(4);
		try {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int i = 0; i < count; i++)
				tasks.add(() -> {
					action.run();
					return null;
				});
			for (var result : pool.invokeAll(tasks, 30, TimeUnit.SECONDS))
				result.get();
		}
		finally {
			pool.shutdownNow();
		}
	}

}
