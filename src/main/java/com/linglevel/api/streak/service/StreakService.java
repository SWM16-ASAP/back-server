package com.linglevel.api.streak.service;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.streak.dto.*;
import com.linglevel.api.streak.entity.*;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.entity.TicketTransaction;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
import com.linglevel.api.user.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private static final int FREEZE_REWARD_CYCLE = 5;
    private static final int MAX_FREEZE_COUNT = 2;
    private static final int FIRST_TICKET_REWARD_DAY = 7;
    private static final int TICKET_REWARD_CYCLE = 15;
    private static final int TICKET_REWARD_AMOUNT = 1;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final UserStudyReportRepository userStudyReportRepository;
    private final DailyCompletionRepository dailyCompletionRepository;
    private final TicketService ticketService;
    private final FreezeTransactionRepository freezeTransactionRepository;
    private final TicketTransactionRepository ticketTransactionRepository;
    private final ReadingSessionService readingSessionService;

    @Transactional
    public StreakResponse getStreakInfo(String userId, LanguageCode languageCode) {
        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserStudyReport newReport = createNewUserStudyReport(userId);
                    userStudyReportRepository.save(newReport);
                    log.info("Created new UserStudyReport for user: {}", userId);
                    return newReport;
                });

        LocalDate today = getKstToday();
        StreakStatus todayStatus = calculateTodayStatus(userId, today);
        StreakStatus yesterdayStatus = calculateTodayStatus(userId, today.minusDays(1));
        long totalStudyDays = dailyCompletionRepository.countByUserId(userId);
        long totalContentsRead = report.getCompletedContentIds() != null ? report.getCompletedContentIds().size() : 0;

        // Calculate expected rewards for today (if not completed yet)
        RewardInfo expectedRewards = null;
        if (todayStatus != StreakStatus.COMPLETED) {
            int expectedStreak = report.getCurrentStreak() + 1;
            expectedRewards = calculateExpectedRewards(expectedStreak);
        }

        return StreakResponse.builder()
                .currentStreak(report.getCurrentStreak())
                .todayStatus(todayStatus)
                .yesterdayStatus(yesterdayStatus)
                .longestStreak(report.getLongestStreak())
                .streakStartDate(report.getStreakStartDate())
                .totalStudyDays(totalStudyDays)
                .totalContentsRead(totalContentsRead)
                .availableFreezes(report.getAvailableFreezes())
                .totalReadingTimeSeconds(report.getTotalReadingTimeSeconds())
                .percentile(calculatePercentile(report))
                .encouragementMessage(getEncouragementMessage(report.getCurrentStreak(), todayStatus, languageCode))
                .expectedRewards(expectedRewards)
                .build();
    }

    @Transactional
    public boolean updateStreak(String userId, ContentType contentType, String contentId) {
        LocalDate today = getKstToday();

        // 유효성 검사
        if (hasCompletedStreakToday(userId, today)
            || !readingSessionService.isReadingSessionValid(userId, contentType, contentId)) {
            return false;
        }

        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> createNewUserStudyReport(userId));

        if (report.getLastCompletionDate() == null) {
            report.setCurrentStreak(1);
            report.setLongestStreak(1);
            report.setStreakStartDate(today);
            report.setLastCompletionDate(today);
        }

        long daysBetween = ChronoUnit.DAYS.between(report.getLastCompletionDate(), today);

        if (daysBetween == 1) {
            // 연속 완료 → 스트릭 증가
            report.setCurrentStreak(report.getCurrentStreak() + 1);
        } else if (daysBetween > 1) {
            boolean streakWasReset = processMissedDays(report, today);

            if (streakWasReset) {
                // 스트릭이 리셋됨 -> 오늘부터 다시 시작
                report.setCurrentStreak(1);
                report.setStreakStartDate(today);
            } else {
                // 프리즈로 스트릭 유지됨 또는 이미 배치 처리됨 -> 오늘 완료로 스트릭 증가
                if (report.getCurrentStreak() > 0) {
                    report.setCurrentStreak(report.getCurrentStreak() + 1);
                } else {
                    // 배치에서 이미 리셋됨 -> 새로 시작
                    report.setCurrentStreak(1);
                    report.setStreakStartDate(today);
                }
            }
        }

        // 최장 기록 갱신
        if (report.getCurrentStreak() > report.getLongestStreak()) {
            report.setLongestStreak(report.getCurrentStreak());
        }

        // 보상 지급 확인 및 적용
        checkAndGrantRewards(report);

        report.setLastCompletionDate(today);
        report.setLastLearningTimestamp(Instant.now());
        report.setUpdatedAt(Instant.now());
        userStudyReportRepository.save(report);

        log.info("Streak updated for user: {}. Current streak: {}", userId, report.getCurrentStreak());
        return true;
    }

    private void checkAndGrantRewards(UserStudyReport report) {
        int currentStreak = report.getCurrentStreak();
        String userId = report.getUserId();

        grantFreezeIfEligible(report, currentStreak, userId);
        grantTicketIfEligible(currentStreak, userId);
    }

    private void grantFreezeIfEligible(UserStudyReport report, int currentStreak, String userId) {
        boolean isEligible = currentStreak > 0
                && currentStreak % FREEZE_REWARD_CYCLE == 0
                && report.getAvailableFreezes() < MAX_FREEZE_COUNT;

        if (!isEligible) {
            return;
        }

        report.setAvailableFreezes(report.getAvailableFreezes() + 1);

        FreezeTransaction freezeTransaction = FreezeTransaction.builder()
                .userId(userId)
                .amount(1)
                .description("Reward for " + currentStreak + "-day streak")
                .createdAt(Instant.now())
                .build();
        freezeTransactionRepository.save(freezeTransaction);

        log.info("Granted 1 freeze to user {} for {} day streak. User now has {} freezes.",
                userId, currentStreak, report.getAvailableFreezes());
    }

    private void grantTicketIfEligible(int currentStreak, String userId) {
        if (!shouldGrantTicket(currentStreak)) {
            return;
        }

        String description = "Reward for " + currentStreak + "-day streak";
        ticketService.grantTicket(userId, TICKET_REWARD_AMOUNT, description);

        log.info("Granted {} ticket to user {} for {} day streak.",
                TICKET_REWARD_AMOUNT, userId, currentStreak);
    }

    private boolean shouldGrantTicket(int streakCount) {
        if (streakCount == FIRST_TICKET_REWARD_DAY) {
            return true;
        }
        return streakCount >= TICKET_REWARD_CYCLE && (streakCount - TICKET_REWARD_CYCLE) % TICKET_REWARD_CYCLE == 0;
    }

    public boolean hasCompletedStreakToday(String userId, LocalDate today) {
        return dailyCompletionRepository
                .findByUserIdAndCompletionDate(userId, today)
                .map(completion -> completion.getStreakStatus() == StreakStatus.COMPLETED)
                .orElse(false);
    }

    private UserStudyReport createNewUserStudyReport(String userId) {
        UserStudyReport report = new UserStudyReport();
        report.setUserId(userId);
        report.setCreatedAt(Instant.now());
        return report;
    }

    private LocalDate getKstToday() {
        return LocalDate.now(KST_ZONE);
    }

    private double calculatePercentile(UserStudyReport report) {
        if (report.getCurrentStreak() == 0) {
            return 0.0;
        }

        long totalUsers = userStudyReportRepository.count();
        if (totalUsers <= 1) {
            return 100.0;
        }

        // Calculate "top X%" - users with higher or equal streak
        long usersWithHigherOrEqualStreak = userStudyReportRepository.countByCurrentStreakGreaterThanEqual(report.getCurrentStreak());

        double percentile = ((double) usersWithHigherOrEqualStreak / totalUsers) * 100;

        // 소수점 첫째 자리까지 반올림
        return Math.round(percentile * 10.0) / 10.0;
    }

    private EncouragementMessage getEncouragementMessage(int currentStreak, StreakStatus todayStatus, LanguageCode languageCode) {
        if (todayStatus != StreakStatus.COMPLETED) {
            return EncouragementMessage.builder().build();
        }

        // Default to EN if null
        if (languageCode == null) {
            languageCode = LanguageCode.EN;
        }

        // Check if current streak is a milestone day
        var milestone = StreakMilestone.fromDay(currentStreak);

        if (milestone.isPresent()) {
            // Milestone day - show celebration message
            StreakMilestone streakMilestone = milestone.get();
            return EncouragementMessage.builder()
                    .title(streakMilestone.getTitle(languageCode))
                    .body(null)
                    .translation(streakMilestone.getMessage(languageCode))
                    .build();
        } else {
            // Non-milestone day - show random inspirational quote
            InspirationQuote quote = InspirationQuote.random();
            return EncouragementMessage.builder()
                    .title(null)
                    .body(quote.getOriginal())
                    .translation(quote.getTranslation(languageCode))
                    .build();
        }
    }

    private StreakStatus calculateTodayStatus(String userId, LocalDate date) {
        DailyCompletion completion = dailyCompletionRepository
                .findByUserIdAndCompletionDate(userId, date)
                .map(this::ensureStreakStatus)  // Lazy 업데이트
                .orElse(null);

        if (completion != null && completion.getStreakStatus() != null) {
            return completion.getStreakStatus();
        }

        return StreakStatus.MISSED;
    }

    @Transactional(readOnly = true)
    public Page<FreezeTransactionResponse> getFreezeTransactions(String userId, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        Page<FreezeTransaction> transactions = freezeTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        return transactions.map(this::toFreezeTransactionResponse);
    }

    private FreezeTransactionResponse toFreezeTransactionResponse(FreezeTransaction transaction) {
        return FreezeTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    @Transactional
    public void addStudyTime(String userId, long studyTimeSeconds) {
        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserStudyReport newReport = createNewUserStudyReport(userId);
                    userStudyReportRepository.save(newReport);
                    return newReport;
                });

        report.setTotalReadingTimeSeconds(report.getTotalReadingTimeSeconds() + studyTimeSeconds);
        userStudyReportRepository.save(report);
    }

    @Transactional
    public void addCompletedContent(String userId, ContentType contentType, String contentId, boolean streakUpdated) {
        LocalDate today = getKstToday();

        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> createNewUserStudyReport(userId));

        // completedContentIds 초기화
        if (report.getCompletedContentIds() == null) {
            report.setCompletedContentIds(new HashSet<>());
        }

        // DailyCompletion 업데이트
        DailyCompletion.CompletedContent completedContent = DailyCompletion.CompletedContent.builder()
                .type(contentType)
                .contentId(contentId)
                .completedAt(Instant.now())
                .build();

        DailyCompletion dailyCompletion = dailyCompletionRepository
                .findByUserIdAndCompletionDate(userId, today)
                .orElse(DailyCompletion.builder()
                        .userId(userId)
                        .completionDate(today)
                        .firstCompletionCount(0)
                        .totalCompletionCount(0)
                        .streakCount(report.getCurrentStreak())
                        .streakStatus(StreakStatus.MISSED)
                        .createdAt(Instant.now())
                        .build()
                );

        if (dailyCompletion.getCompletedContents() == null) {
            dailyCompletion.setCompletedContents(new ArrayList<>());
        }

        dailyCompletion.getCompletedContents().add(completedContent);
        dailyCompletion.setTotalCompletionCount(dailyCompletion.getTotalCompletionCount() + 1);

        if (!report.getCompletedContentIds().contains(contentId)) {
            report.getCompletedContentIds().add(contentId);
            dailyCompletion.setFirstCompletionCount(dailyCompletion.getFirstCompletionCount() + 1);
        }

        if (streakUpdated) {
            dailyCompletion.setStreakStatus(StreakStatus.COMPLETED);
        }

        userStudyReportRepository.save(report);
        dailyCompletionRepository.save(dailyCompletion);
    }

    /**
     * 여러 날 누락 처리
     *
     * @param report UserStudyReport
     * @param today 오늘 날짜
     * @return 스트릭이 리셋되었는지 여부
     */
    @Transactional
    public boolean processMissedDays(UserStudyReport report, LocalDate today) {
        if (report.getLastCompletionDate() == null) {
            log.warn("Cannot process missed days: lastCompletionDate is null for user {}", report.getUserId());
            return false;
        }

        long daysSinceLastCompletion = ChronoUnit.DAYS.between(report.getLastCompletionDate(), today);

        if (daysSinceLastCompletion <= 1) {
            return false;
        }

        int daysMissed = (int) daysSinceLastCompletion - 1;
        log.warn("User {} missed {} days. Processing gap.", report.getUserId(), daysMissed);

        // 각 누락일에 대해 배치가 이미 처리했는지 확인
        int unprocessedDays = 0;
        for (int i = 1; i <= daysMissed; i++) {
            LocalDate missedDate = report.getLastCompletionDate().plusDays(i);
            if (wasFreezeProcessedForDate(report.getUserId(), missedDate)) {
                unprocessedDays++;
            }
        }

        if (unprocessedDays == 0) {
            log.info("All missed days already processed for user {}", report.getUserId());
            return false;
        }

        log.info("User {} has {} unprocessed missed days.", report.getUserId(), unprocessedDays);

        if (report.getAvailableFreezes() >= unprocessedDays) {
            // 프리즈 충분 -> 소진하고 스트릭 유지
            report.setAvailableFreezes(report.getAvailableFreezes() - unprocessedDays);

            // 각 누락일에 대해 FreezeTransaction 기록
            for (int i = 1; i <= daysMissed; i++) {
                LocalDate missedDate = report.getLastCompletionDate().plusDays(i);
                if (wasFreezeProcessedForDate(report.getUserId(), missedDate)) {
                    consumeFreezeForDate(report, missedDate);
                }
            }

            log.info("Consumed {} freezes for user {}. Streak maintained at {}.",
                    unprocessedDays, report.getUserId(), report.getCurrentStreak());
            return false;
        } else {
            // 프리즈 부족 -> 남은 프리즈 모두 소진하고 스트릭 리셋
            int remainingFreezes = report.getAvailableFreezes();
            report.setAvailableFreezes(0);

            // 소진 가능한 프리즈에 대해 트랜잭션 기록
            int processedDays = 0;
            for (int i = 1; i <= daysMissed && processedDays < remainingFreezes; i++) {
                LocalDate missedDate = report.getLastCompletionDate().plusDays(i);
                if (wasFreezeProcessedForDate(report.getUserId(), missedDate)) {
                    consumeFreezeForDate(report, missedDate);
                    processedDays++;
                }
            }

            // 스트릭 리셋
            int previousStreak = report.getCurrentStreak();
            report.setCurrentStreak(0);
            report.setLastCompletionDate(null);
            report.setStreakStartDate(null);

            log.warn("Insufficient freezes for user {}. Streak reset from {} to 0. Consumed {} freezes.",
                    report.getUserId(), previousStreak, remainingFreezes);
            return true;
        }
    }

    private boolean wasFreezeProcessedForDate(String userId, LocalDate date) {
        return dailyCompletionRepository
                .findByUserIdAndCompletionDate(userId, date)
                .map(completion -> completion.getStreakStatus() == StreakStatus.FREEZE_USED)
                .orElse(false);
    }

    private boolean hasFreezeTransaction(String userId, LocalDate date) {
        Instant dayStart = date.atStartOfDay(KST_ZONE).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(KST_ZONE).toInstant();

        return freezeTransactionRepository.existsByUserIdAndAmountAndCreatedAtBetween(
                userId, -1, dayStart, dayEnd
        );
    }

    @Transactional
    protected DailyCompletion ensureStreakStatus(DailyCompletion completion) {
        if (completion == null || completion.getStreakStatus() != null) {
            return completion;
        }

        StreakStatus status;

        if (completion.getTotalCompletionCount() != null && completion.getTotalCompletionCount() > 0) {
            status = StreakStatus.COMPLETED;
        }
        else if (completion.getStreakCount() != null && completion.getStreakCount() > 0) {
            boolean hasFreeze = hasFreezeTransaction(completion.getUserId(), completion.getCompletionDate());
            status = hasFreeze ? StreakStatus.FREEZE_USED : StreakStatus.COMPLETED;
        }
        else {
            status = StreakStatus.MISSED;
        }

        completion.setStreakStatus(status);
        dailyCompletionRepository.save(completion);

        log.debug("Lazy updated streakStatus for user {} on {}: {}",
                completion.getUserId(), completion.getCompletionDate(), status);

        return completion;
    }

    private void consumeFreezeForDate(UserStudyReport report, LocalDate missedDate) {
        FreezeTransaction transaction = FreezeTransaction.builder()
                .userId(report.getUserId())
                .amount(-1)
                .description("Auto-consumed for missed day: " + missedDate)
                .createdAt(Instant.now())
                .build();
        freezeTransactionRepository.save(transaction);

        // 프리즈 사용 시에도 DailyCompletion 생성 (streakStatus=FREEZE_USED로 구분)
        DailyCompletion freezeCompletion = DailyCompletion.builder()
                .userId(report.getUserId())
                .completionDate(missedDate)
                .firstCompletionCount(0)
                .totalCompletionCount(0)
                .completedContents(new ArrayList<>())
                .streakCount(report.getCurrentStreak())
                .streakStatus(StreakStatus.FREEZE_USED)
                .createdAt(Instant.now())
                .build();
        dailyCompletionRepository.save(freezeCompletion);

        log.debug("Created freeze consumption transaction and DailyCompletion for user {} on date {} with streak count {}",
                report.getUserId(), missedDate, report.getCurrentStreak());
    }

    @Transactional(readOnly = true)
    public CalendarResponse getCalendar(String userId, int year, int month) {
        LocalDate today = getKstToday();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        CalendarViewData viewData = prepareCalendarViewData(userId, firstDay, lastDay);

        List<CalendarDayResponse> days = new ArrayList<>();
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            days.add(buildCalendarDay(date, today, viewData));
        }

        return CalendarResponse.builder()
                .year(year)
                .month(month)
                .today(today.getDayOfMonth())
                .currentStreak(viewData.getReport().getCurrentStreak())
                .days(days)
                .build();
    }

    @Transactional(readOnly = true)
    public WeekStreakResponse getThisWeekStreak(String userId) {
        LocalDate today = getKstToday();
        LocalDate sunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate saturday = sunday.plusDays(6);

        CalendarViewData viewData = prepareCalendarViewData(userId, sunday, saturday);

        List<WeekDayResponse> weekDays = new ArrayList<>();
        for (LocalDate date = sunday; !date.isAfter(saturday); date = date.plusDays(1)) {
            weekDays.add(buildWeekDay(date, today, viewData));
        }

        return WeekStreakResponse.builder()
                .currentStreak(viewData.getReport().getCurrentStreak())
                .freezeCount(viewData.getReport().getAvailableFreezes())
                .weekDays(weekDays)
                .build();
    }

    private CalendarViewData prepareCalendarViewData(String userId, LocalDate startDate, LocalDate endDate) {
        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> createNewUserStudyReport(userId));

        List<DailyCompletion> completions = dailyCompletionRepository
                .findByUserIdAndCompletionDateBetween(userId, startDate, endDate);

        completions = completions.stream()
                .map(this::ensureStreakStatus)
                .toList();

        Map<LocalDate, DailyCompletion> completionMap = completions.stream()
                .collect(Collectors.toMap(DailyCompletion::getCompletionDate, c -> c));

        Instant startInstant = startDate.atStartOfDay(KST_ZONE).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(KST_ZONE).toInstant();

        List<FreezeTransaction> freezeRewardTxs = freezeTransactionRepository
            .findByUserIdAndAmountAndCreatedAtBetween(userId, 1, startInstant, endInstant);
        Map<LocalDate, Integer> freezeRewardsMap = freezeRewardTxs.stream()
            .collect(Collectors.groupingBy(
                t -> t.getCreatedAt().atZone(KST_ZONE).toLocalDate(),
                Collectors.summingInt(FreezeTransaction::getAmount)
            ));

        LocalDateTime startDateTime = startDate.atStartOfDay(KST_ZONE).toLocalDateTime();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay(KST_ZONE).toLocalDateTime();

        List<TicketTransaction> ticketRewardTxs = ticketTransactionRepository
            .findByUserIdAndAmountAndCreatedAtBetween(userId, TICKET_REWARD_AMOUNT, startDateTime, endDateTime);
        Map<LocalDate, Integer> ticketRewardsMap = ticketRewardTxs.stream()
            .collect(Collectors.groupingBy(
                t -> t.getCreatedAt().atZone(ZoneId.systemDefault()).withZoneSameInstant(KST_ZONE).toLocalDate(),
                Collectors.summingInt(TicketTransaction::getAmount)
            ));

        // null인 streakCount를 가진 날짜들을 채우기
        backfillMissingStreakCountsInRange(userId, startDate, endDate, completionMap);

        return new CalendarViewData(report, completionMap, freezeRewardsMap, ticketRewardsMap);
    }

    @Transactional
    protected void backfillMissingStreakCountsInRange(
            String userId,
            LocalDate startDate,
            LocalDate endDate,
            Map<LocalDate, DailyCompletion> completionMap) {

        List<DailyCompletion> allUpdates = new ArrayList<>();
        Set<LocalDate> processedDates = new HashSet<>();

        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            if (processedDates.contains(currentDate)) {
                continue;
            }

            DailyCompletion completion = completionMap.get(currentDate);
            boolean hasStreakActivity = completion != null && completion.getStreakStatus() != null;
            boolean needsBackfill = hasStreakActivity && completion.getStreakCount() == null;

            if (!needsBackfill) {
                continue;
            }

            log.info("Found missing streakCount for user {} on date {}. Starting backfill.", userId, currentDate);

            LocalDate streakStartDate = findStreakStartDate(userId, currentDate, completionMap);

            Map<LocalDate, DailyCompletion> extendedCompletionMap = loadExtendedCompletions(
                    userId, streakStartDate, endDate);

            List<DailyCompletion> updates = calculateAndCollectUpdates(
                    streakStartDate, endDate, extendedCompletionMap, userId, processedDates);

            allUpdates.addAll(updates);
        }

        saveUpdatesAndUpdateMap(allUpdates, completionMap, userId);
    }

    private Map<LocalDate, DailyCompletion> loadExtendedCompletions(
            String userId,
            LocalDate streakStartDate,
            LocalDate endDate) {

        List<DailyCompletion> extendedCompletions = dailyCompletionRepository
                .findByUserIdAndCompletionDateBetween(userId, streakStartDate, endDate);

        return extendedCompletions.stream()
                .collect(Collectors.toMap(DailyCompletion::getCompletionDate, c -> c));
    }

    private List<DailyCompletion> calculateAndCollectUpdates(
            LocalDate startDate,
            LocalDate endDate,
            Map<LocalDate, DailyCompletion> completionMap,
            String userId,
            Set<LocalDate> processedDates) {

        List<DailyCompletion> toUpdate = new ArrayList<>();
        int streakCount = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            DailyCompletion completion = completionMap.get(currentDate);
            boolean hasStreakActivity = completion != null && completion.getStreakStatus() != null;

            if (hasStreakActivity) {
                // COMPLETED 상태일 때만 증가, FREEZE_USED는 유지만
                if (completion.getStreakStatus() == StreakStatus.COMPLETED) {
                    streakCount++;
                }

                if (completion.getStreakCount() == null) {
                    completion.setStreakCount(streakCount);
                    toUpdate.add(completion);
                    log.debug("Scheduled streakCount update for user {} on date {} to {}", userId, currentDate, streakCount);
                }

                processedDates.add(currentDate);
                currentDate = currentDate.plusDays(1);
            } else {
                break;
            }
        }

        return toUpdate;
    }

    private void saveUpdatesAndUpdateMap(
            List<DailyCompletion> toUpdate,
            Map<LocalDate, DailyCompletion> completionMap,
            String userId) {

        if (toUpdate.isEmpty()) {
            return;
        }

        dailyCompletionRepository.saveAll(toUpdate);
        log.info("Backfilled {} streakCounts for user {}", toUpdate.size(), userId);

        toUpdate.forEach(c -> completionMap.put(c.getCompletionDate(), c));
    }

    private LocalDate findStreakStartDate(
            String userId,
            LocalDate fromDate,
            Map<LocalDate, DailyCompletion> completionMap) {

        LocalDate currentDate = fromDate.minusDays(1);
        LocalDate searchLimit = fromDate.minusDays(1000); // 안전장치: 최대 1000일 전까지만

        while (!currentDate.isBefore(searchLimit)) {
            DailyCompletion completion = completionMap.get(currentDate);
            boolean hasStreakActivity = completion != null && completion.getStreakStatus() != null;

            if (!hasStreakActivity) {
                completion = dailyCompletionRepository
                        .findByUserIdAndCompletionDate(userId, currentDate)
                        .map(this::ensureStreakStatus)
                        .orElse(null);
                hasStreakActivity = completion != null && completion.getStreakStatus() != null;
            }

            if (!hasStreakActivity) {
                return currentDate.plusDays(1);
            }

            currentDate = currentDate.minusDays(1);
        }

        log.warn("Streak search went back to limit {} for user {}", searchLimit, userId);
        return searchLimit;
    }

    private CalendarDayInfo calculateCalendarDayInfo(LocalDate date, LocalDate today, CalendarViewData viewData) {
        boolean isFuture = date.isAfter(today);
        DailyCompletion completion = viewData.getCompletionMap().get(date);

        // Determine StreakStatus
        StreakStatus status;
        if (isFuture) {
            status = StreakStatus.FUTURE;
        } else if (completion != null && completion.getStreakStatus() != null) {
            status = completion.getStreakStatus();
        } else {
            status = StreakStatus.MISSED;
        }

        // 저장된 streakCount 가져오기
        Integer streakCount = 0;
        if (completion != null && completion.getStreakCount() != null) {
            streakCount = completion.getStreakCount();
        }

        RewardInfo rewards = null;
        RewardInfo expectedRewards = null;

        if (!isFuture) {
            rewards = createRewardInfo(
                viewData.getTicketRewardsMap().getOrDefault(date, 0),
                viewData.getFreezeRewardsMap().getOrDefault(date, 0)
            );
        }

        if (!date.isBefore(today)) {
            UserStudyReport report = viewData.getReport();
            boolean isTodayCompleted = viewData.getCompletionMap().containsKey(today);

            int baseStreak = report.getCurrentStreak();
            if (isTodayCompleted) {
                baseStreak--;
            }
            if (baseStreak < 0) {
                baseStreak = 0;
            }

            int daysFromToday = (int) ChronoUnit.DAYS.between(today, date);
            int expectedStreak = baseStreak + 1 + daysFromToday;

            expectedRewards = calculateExpectedRewards(expectedStreak);
        }

        return new CalendarDayInfo(status, streakCount, rewards, expectedRewards);
    }

    private CalendarDayResponse buildCalendarDay(LocalDate date, LocalDate today, CalendarViewData viewData) {
        CalendarDayInfo dayInfo = calculateCalendarDayInfo(date, today, viewData);
        DailyCompletion completion = viewData.getCompletionMap().get(date);

        Integer firstCompletionCount = (completion != null) ? completion.getFirstCompletionCount() : 0;
        Integer totalCompletionCount = (completion != null) ? completion.getTotalCompletionCount() : 0;

        return CalendarDayResponse.builder()
                .date(date)
                .dayOfMonth(date.getDayOfMonth())
                .isToday(date.equals(today))
                .status(dayInfo.status)
                .streakCount(dayInfo.streakCount)
                .firstCompletionCount(firstCompletionCount)
                .totalCompletionCount(totalCompletionCount)
                .rewards(dayInfo.rewards)
                .expectedRewards(dayInfo.expectedRewards)
                .build();
    }

    private WeekDayResponse buildWeekDay(LocalDate date, LocalDate today, CalendarViewData viewData) {
        CalendarDayInfo dayInfo = calculateCalendarDayInfo(date, today, viewData);

        return WeekDayResponse.builder()
                .dayOfWeek(date.getDayOfWeek().name())
                .date(date)
                .isToday(date.equals(today))
                .status(dayInfo.status)
                .rewards(dayInfo.rewards)
                .expectedRewards(dayInfo.expectedRewards)
                .build();
    }

    private RewardInfo calculateExpectedRewards(int streakCount) {
        int freezes = 0;
        int tickets = 0;

        if (streakCount > 0 && streakCount % FREEZE_REWARD_CYCLE == 0) {
            freezes = 1;
        }

        if (shouldGrantTicket(streakCount)) {
            tickets = TICKET_REWARD_AMOUNT;
        }

        return createRewardInfo(tickets, freezes);
    }

    private RewardInfo createRewardInfo(int tickets, int freezes) {
        return RewardInfo.builder()
                .tickets(tickets)
                .freezes(freezes)
                .build();
    }

    @Value
    private static class CalendarViewData {
        UserStudyReport report;
        Map<LocalDate, DailyCompletion> completionMap;
        Map<LocalDate, Integer> freezeRewardsMap;
        Map<LocalDate, Integer> ticketRewardsMap;
    }

    @Value
    private static class CalendarDayInfo {
        StreakStatus status;
        Integer streakCount;
        RewardInfo rewards;
        RewardInfo expectedRewards;
    }
}

    