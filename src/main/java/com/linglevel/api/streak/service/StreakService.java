package com.linglevel.api.streak.service;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.streak.dto.CalendarDayResponse;
import com.linglevel.api.streak.dto.CalendarResponse;
import com.linglevel.api.streak.dto.EncouragementMessage;
import com.linglevel.api.streak.dto.FreezeTransactionResponse;
import com.linglevel.api.streak.dto.RewardInfo;
import com.linglevel.api.streak.dto.StreakResponse;
import com.linglevel.api.streak.dto.WeekDayResponse;
import com.linglevel.api.streak.dto.WeekStreakResponse;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.InspirationQuote;
import com.linglevel.api.streak.entity.StreakMilestone;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.service.TicketService;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.entity.FreezeTransaction;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
import com.linglevel.api.user.ticket.entity.TicketTransaction;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
                .encouragementMessage(getEncouragementMessage(report.getCurrentStreak(), languageCode))
                .build();
    }

    @Transactional
    public boolean updateStreak(String userId, ContentType contentType, String contentId) {
        LocalDate today = getKstToday();

        // 오늘 이미 완료했는지 체크 (중복 방지)
        if (hasCompletedStreakToday(userId, today)) {
            log.info("User {} has already completed a streak today.", userId);
            return false;
        }

        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> createNewUserStudyReport(userId));

        if (report.getLastCompletionDate() == null) {
            // 첫 완료
            report.setCurrentStreak(1);
            report.setLongestStreak(1);
            report.setStreakStartDate(today);
        } else {
            long daysBetween = ChronoUnit.DAYS.between(report.getLastCompletionDate(), today);

            if (daysBetween == 1) {
                // 연속 완료 → 스트릭 증가
                report.setCurrentStreak(report.getCurrentStreak() + 1);
            } else if (daysBetween == 0) {
                // 오늘 이미 완료 (hasCompletedStreakToday에서 걸러져야 함)
                log.warn("User {} completed multiple times today. Should have been prevented.", userId);
                return false;
            } else {
                // daysBetween > 1: 여러 날 누락 (배치 실패 대비 방어적 처리)
                log.warn("User {} has {} days gap. Processing defensively.", userId, daysBetween);

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

        saveDailyCompletion(report, today, contentType, contentId);

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
        return dailyCompletionRepository.existsByUserIdAndCompletionDate(userId, today);
    }

    private UserStudyReport createNewUserStudyReport(String userId) {
        UserStudyReport report = new UserStudyReport();
        report.setUserId(userId);
        report.setCreatedAt(Instant.now());
        return report;
    }

    private void saveDailyCompletion(UserStudyReport report, LocalDate today, ContentType contentType, String contentId) {
        DailyCompletion.CompletedContent completedContent = DailyCompletion.CompletedContent.builder()
                .type(contentType)
                .contentId(contentId)
                .completedAt(Instant.now())
                .build();

        // 전체 기간에서 첫 완료인지 확인
        boolean isFirstCompletion = !report.getCompletedContentIds().contains(contentId);

        // 해당 날짜에 이미 DailyCompletion이 있는지 확인
        DailyCompletion dailyCompletion = dailyCompletionRepository
                .findByUserIdAndCompletionDate(report.getUserId(), today)
                .orElse(null);

        if (dailyCompletion != null) {
            // 기존 레코드가 있는 경우
            if (dailyCompletion.getCompletedContents() == null) {
                dailyCompletion.setCompletedContents(new java.util.ArrayList<>());
            }

            // 총 완료 개수 증가 (복습 포함)
            dailyCompletion.setTotalCompletionCount(dailyCompletion.getTotalCompletionCount() + 1);

            // 첫 완료인 경우만 firstCompletionCount 증가
            if (isFirstCompletion) {
                dailyCompletion.setFirstCompletionCount(dailyCompletion.getFirstCompletionCount() + 1);
                report.getCompletedContentIds().add(contentId);
            }

            // 모든 완료 기록을 completedContents에 추가
            dailyCompletion.getCompletedContents().add(completedContent);
        } else {
            // 새로운 레코드 생성
            dailyCompletion = DailyCompletion.builder()
                    .userId(report.getUserId())
                    .completionDate(today)
                    .firstCompletionCount(isFirstCompletion ? 1 : 0)
                    .totalCompletionCount(1)
                    .completedContents(new java.util.ArrayList<>(Collections.singletonList(completedContent)))
                    .createdAt(Instant.now())
                    .build();

            if (isFirstCompletion) {
                report.getCompletedContentIds().add(contentId);
            }
        }

        dailyCompletionRepository.save(dailyCompletion);
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

    private EncouragementMessage getEncouragementMessage(int currentStreak, LanguageCode languageCode) {
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

    private StreakStatus calculateTodayStatus(String userId, LocalDate today) {
        boolean todayCompleted = dailyCompletionRepository.existsByUserIdAndCompletionDate(userId, today);

        if (todayCompleted) {
            return StreakStatus.COMPLETED;
        }

        // Check if yesterday freeze was used
        boolean yesterdayFreezeUsed = checkYesterdayFreezeUsed(userId, today);
        if (yesterdayFreezeUsed) {
            return StreakStatus.FREEZE_USED;
        }

        return StreakStatus.MISSED;
    }

    private boolean checkYesterdayFreezeUsed(String userId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        Instant yesterdayStart = yesterday.atStartOfDay(KST_ZONE).toInstant();
        Instant yesterdayEnd = yesterday.plusDays(1).atStartOfDay(KST_ZONE).toInstant();

        return freezeTransactionRepository.existsByUserIdAndAmountAndCreatedAtBetween(
            userId, -1, yesterdayStart, yesterdayEnd
        );
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
            if (!wasFreezeProcessedForDate(report.getUserId(), missedDate)) {
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
                if (!wasFreezeProcessedForDate(report.getUserId(), missedDate)) {
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
                if (!wasFreezeProcessedForDate(report.getUserId(), missedDate)) {
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
        Instant dayStart = date.atStartOfDay(KST_ZONE).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(KST_ZONE).toInstant();

        return freezeTransactionRepository.existsByUserIdAndAmountAndCreatedAtBetween(
                userId, -1, dayStart, dayEnd
        );
    }

    private void consumeFreezeForDate(UserStudyReport report, LocalDate missedDate) {
        FreezeTransaction transaction = FreezeTransaction.builder()
                .userId(report.getUserId())
                .amount(-1)
                .description("Auto-consumed for missed day: " + missedDate)
                .createdAt(Instant.now())
                .build();
        freezeTransactionRepository.save(transaction);

        log.debug("Created freeze consumption transaction for user {} on date {}", report.getUserId(), missedDate);
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
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        CalendarViewData viewData = prepareCalendarViewData(userId, monday, sunday);

        List<WeekDayResponse> weekDays = new ArrayList<>();
        for (LocalDate date = monday; !date.isAfter(sunday); date = date.plusDays(1)) {
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
        Map<LocalDate, DailyCompletion> completionMap = completions.stream()
                .collect(Collectors.toMap(DailyCompletion::getCompletionDate, c -> c));

        Instant startInstant = startDate.atStartOfDay(KST_ZONE).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(KST_ZONE).toInstant();

        List<FreezeTransaction> freezeUsageTxs = freezeTransactionRepository
                .findByUserIdAndAmountAndCreatedAtBetween(userId, -1, startInstant, endInstant);
        Map<LocalDate, Boolean> freezeUsageMap = freezeUsageTxs.stream()
                .map(t -> t.getCreatedAt().atZone(KST_ZONE).toLocalDate())
                .distinct()
                .collect(Collectors.toMap(date -> date, date -> true));

        List<FreezeTransaction> freezeRewardTxs = freezeTransactionRepository
            .findByUserIdAndAmountAndCreatedAtBetween(userId, 1, startInstant, endInstant);
        Map<LocalDate, Integer> freezeRewardsMap = freezeRewardTxs.stream()
            .collect(Collectors.groupingBy(
                t -> t.getCreatedAt().atZone(KST_ZONE).toLocalDate(),
                Collectors.summingInt(FreezeTransaction::getAmount)
            ));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<TicketTransaction> ticketRewardTxs = ticketTransactionRepository
            .findByUserIdAndAmountAndCreatedAtBetween(userId, TICKET_REWARD_AMOUNT, startDateTime, endDateTime);
        Map<LocalDate, Integer> ticketRewardsMap = ticketRewardTxs.stream()
            .collect(Collectors.groupingBy(
                t -> t.getCreatedAt().toLocalDate(),
                Collectors.summingInt(TicketTransaction::getAmount)
            ));

        return new CalendarViewData(report, completionMap, freezeUsageMap, freezeRewardsMap, ticketRewardsMap);
    }

    private CalendarDayInfo calculateCalendarDayInfo(LocalDate date, LocalDate today, CalendarViewData viewData) {
        boolean isFuture = date.isAfter(today);
        DailyCompletion completion = viewData.getCompletionMap().get(date);

        // Determine StreakStatus
        StreakStatus status;
        if (isFuture) {
            status = StreakStatus.FUTURE;
        } else if (completion != null) {
            status = StreakStatus.COMPLETED;
        } else if (viewData.getFreezeUsageMap().containsKey(date)) {
            status = StreakStatus.FREEZE_USED;
        } else {
            status = StreakStatus.MISSED;
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

        return new CalendarDayInfo(status, rewards, expectedRewards);
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
                .streakCount(null)
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
        Map<LocalDate, Boolean> freezeUsageMap;
        Map<LocalDate, Integer> freezeRewardsMap;
        Map<LocalDate, Integer> ticketRewardsMap;
    }

    @Value
    private static class CalendarDayInfo {
        StreakStatus status;
        RewardInfo rewards;
        RewardInfo expectedRewards;
    }
}

    