package com.linglevel.api.streak.service;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.streak.dto.CalendarDayResponse;
import com.linglevel.api.streak.dto.CalendarResponse;
import com.linglevel.api.streak.dto.EncouragementMessage;
import com.linglevel.api.streak.dto.FreezeTransactionResponse;
import com.linglevel.api.streak.dto.RewardInfo;
import com.linglevel.api.streak.dto.StreakResponse;
import com.linglevel.api.streak.dto.WeekDayResponse;
import com.linglevel.api.streak.dto.WeekStreakResponse;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.exception.StreakErrorCode;
import com.linglevel.api.streak.exception.StreakException;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.service.TicketService;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.entity.FreezeTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
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

    private final UserStudyReportRepository userStudyReportRepository;
    private final DailyCompletionRepository dailyCompletionRepository;
    private final TicketService ticketService;
    private final FreezeTransactionRepository freezeTransactionRepository;

    @Transactional
    public StreakResponse getStreakInfo(String userId) {
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
                .encouragementMessage(getEncouragementMessage(report.getCurrentStreak()))
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
        report.setUpdatedAt(Instant.now());

        saveDailyCompletion(report, today, contentType, contentId);

        userStudyReportRepository.save(report);

        log.info("Streak updated for user: {}. Current streak: {}", userId, report.getCurrentStreak());
        return true;
    }

    private void checkAndGrantRewards(UserStudyReport report) {
        int currentStreak = report.getCurrentStreak();
        String userId = report.getUserId();

        // 프리즈 지급 (5일 주기, 최대 2개 보유)
        if (currentStreak > 0 && currentStreak % 5 == 0 && report.getAvailableFreezes() < 2) {
            report.setAvailableFreezes(report.getAvailableFreezes() + 1);

            FreezeTransaction freezeTransaction = FreezeTransaction.builder()
                    .userId(userId)
                    .amount(1)
                    .description("Reward for " + currentStreak + "-day streak")
                    .createdAt(Instant.now())
                    .build();
            freezeTransactionRepository.save(freezeTransaction);
            log.info("Granted 1 freeze to user {} for {} day streak. User now has {} freezes.", userId, currentStreak, report.getAvailableFreezes());
        }

        // 티켓 지급 (1, 7, 15, 30, 45...)
        boolean shouldGrantTicket = false;
        if (currentStreak == 1 || currentStreak == 7) {
            shouldGrantTicket = true;
        } else if (currentStreak >= 15 && (currentStreak - 15) % 15 == 0) {
            shouldGrantTicket = true;
        }

        if (shouldGrantTicket) {
            String description = "Reward for " + currentStreak + "-day streak";
            ticketService.grantTicket(userId, 1, description);
            log.info("Granted 1 ticket to user {} for {} day streak.", userId, currentStreak);
        }
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
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
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

    private EncouragementMessage getEncouragementMessage(int currentStreak) {
        String title;
        String body;
        String translation;

        if (currentStreak == 0) {
            title = "시작하세요!";
            body = "Start your first streak!";
            translation = "첫 스트릭을 시작해보세요!";
        } else if (currentStreak < 5) {
            title = currentStreak + "일 연속!";
            body = "Great job! Keep it up!";
            translation = "잘하고 있어요! 계속 꾸준히 학습해보세요.";
        } else if (currentStreak < 10) {
            title = currentStreak + "일 연속!";
            body = "Amazing! " + currentStreak + " days in a row!";
            translation = "벌써 " + currentStreak + "일 연속 스트릭 달성! 대단해요!";
        } else {
            title = currentStreak + "일 연속!";
            body = "Impressive! " + currentStreak + " days in a row!";
            translation = "당신의 꾸준함에 박수를 보냅니다! " + currentStreak + "일 연속 스트릭!";
        }

        return EncouragementMessage.builder()
                .title(title)
                .body(body)
                .translation(translation)
                .build();
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

        // Convert yesterday date to Instant range (00:00:00 ~ 23:59:59 KST)
        ZoneId kst = ZoneId.of("Asia/Seoul");
        Instant yesterdayStart = yesterday.atStartOfDay(kst).toInstant();
        Instant yesterdayEnd = yesterday.plusDays(1).atStartOfDay(kst).toInstant();

        // Check if freeze was consumed (amount = -1) yesterday
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

    /**
     * 특정 날짜에 프리즈 소진 트랜잭션이 이미 생성되었는지 확인
     * (배치 작업과 실시간 처리 간 중복 방지)
     */
    private boolean wasFreezeProcessedForDate(String userId, LocalDate date) {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        Instant dayStart = date.atStartOfDay(kst).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(kst).toInstant();

        return freezeTransactionRepository.existsByUserIdAndAmountAndCreatedAtBetween(
                userId, -1, dayStart, dayEnd
        );
    }

    /**
     * 특정 날짜에 대한 프리즈 소진 트랜잭션 생성
     * description에 날짜를 포함하여 캘린더 API에서 활용
     */
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

        StreakViewData viewData = prepareStreakViewData(userId, firstDay, lastDay);

        Map<LocalDate, Boolean> monthlyFreezeMap = viewData.getAllFreezeDates().stream()
                .filter(d -> d.getYear() == year && d.getMonthValue() == month)
                .collect(Collectors.toMap(d -> d, d -> true));

        List<CalendarDayResponse> days = new ArrayList<>();
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            days.add(buildCalendarDay(date, today, viewData, monthlyFreezeMap));
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

        StreakViewData viewData = prepareStreakViewData(userId, monday, sunday);

        Map<LocalDate, Boolean> weeklyFreezeMap = viewData.getAllFreezeDates().stream()
                .filter(d -> !d.isBefore(monday) && !d.isAfter(sunday))
                .collect(Collectors.toMap(d -> d, d -> true));

        List<WeekDayResponse> weekDays = new ArrayList<>();
        for (LocalDate date = monday; !date.isAfter(sunday); date = date.plusDays(1)) {
            weekDays.add(buildWeekDay(date, today, viewData, weeklyFreezeMap));
        }

        return WeekStreakResponse.builder()
                .currentStreak(viewData.getReport().getCurrentStreak())
                .freezeCount(viewData.getReport().getAvailableFreezes())
                .weekDays(weekDays)
                .build();
    }

    private StreakViewData prepareStreakViewData(String userId, LocalDate startDate, LocalDate endDate) {
        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> createNewUserStudyReport(userId));

        List<DailyCompletion> completions = dailyCompletionRepository
                .findByUserIdAndCompletionDateBetween(userId, startDate, endDate);
        Map<LocalDate, DailyCompletion> completionMap = completions.stream()
                .collect(Collectors.toMap(DailyCompletion::getCompletionDate, c -> c));

        java.util.Set<LocalDate> allFreezeDates = new java.util.HashSet<>();
        if (report.getStreakStartDate() != null) {
            ZoneId kst = ZoneId.of("Asia/Seoul");
            Instant streakStartInstant = report.getStreakStartDate().atStartOfDay(kst).toInstant();
            List<FreezeTransaction> allFreezeTransactions = freezeTransactionRepository
                    .findByUserIdAndAmountAndCreatedAtBetween(userId, -1, streakStartInstant, Instant.now());

            allFreezeDates = allFreezeTransactions.stream()
                    .filter(t -> t.getDescription() != null && t.getDescription().contains("missed day:"))
                    .map(t -> LocalDate.parse(t.getDescription().substring(t.getDescription().lastIndexOf(":") + 2)))
                    .collect(Collectors.toSet());
        }
        return new StreakViewData(report, completionMap, allFreezeDates);
    }

    private StreakDayInfo calculateStreakDayInfo(LocalDate date, LocalDate today, StreakViewData viewData, Map<LocalDate, Boolean> freezeMap) {
        boolean isFuture = date.isAfter(today);
        DailyCompletion completion = viewData.getCompletionMap().get(date);
        UserStudyReport report = viewData.getReport();

        StreakStatus status;
        if (isFuture) {
            status = StreakStatus.FUTURE;
        } else if (completion != null) {
            status = StreakStatus.COMPLETED;
        } else if (freezeMap.containsKey(date)) {
            status = StreakStatus.FREEZE_USED;
        } else {
            status = StreakStatus.MISSED;
        }

        Integer streakCount = null;
        if (!isFuture && report.getStreakStartDate() != null && !date.isBefore(report.getStreakStartDate())) {
            if (status == StreakStatus.COMPLETED || status == StreakStatus.FREEZE_USED) {
                long daysInStreak = ChronoUnit.DAYS.between(report.getStreakStartDate(), date) + 1;
                long freezesUsed = viewData.getAllFreezeDates().stream()
                        .filter(freezeDate -> !freezeDate.isBefore(report.getStreakStartDate()) && !freezeDate.isAfter(date))
                        .count();
                streakCount = (int) (daysInStreak - freezesUsed);
            }
        }

        RewardInfo rewards = null;
        RewardInfo expectedRewards = null;
        if (isFuture) {
            if (report.getCurrentStreak() > 0) {
                int expectedStreak = report.getCurrentStreak() + (int) ChronoUnit.DAYS.between(today, date);
                expectedRewards = calculateRewards(expectedStreak);
            } else {
                expectedRewards = calculateRewards(1);
            }
        } else if (streakCount != null && streakCount > 0) {
            rewards = calculateRewards(streakCount);
        }

        return new StreakDayInfo(status, streakCount, rewards, expectedRewards);
    }

    private CalendarDayResponse buildCalendarDay(LocalDate date, LocalDate today, StreakViewData viewData, Map<LocalDate, Boolean> freezeMap) {
        StreakDayInfo dayInfo = calculateStreakDayInfo(date, today, viewData, freezeMap);
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

    private WeekDayResponse buildWeekDay(LocalDate date, LocalDate today, StreakViewData viewData, Map<LocalDate, Boolean> freezeMap) {
        StreakDayInfo dayInfo = calculateStreakDayInfo(date, today, viewData, freezeMap);

        return WeekDayResponse.builder()
                .dayOfWeek(date.getDayOfWeek().name())
                .date(date)
                .isToday(date.equals(today))
                .status(dayInfo.status)
                .rewards(dayInfo.rewards)
                .expectedRewards(dayInfo.expectedRewards)
                .build();
    }

    private RewardInfo calculateRewards(int streakCount) {
        int freezes = 0;
        int tickets = 0;

        // 프리즈: 5일마다 (최대 2개 보유는 여기서는 체크 안함, 실제 지급 시에만 체크)
        if (streakCount > 0 && streakCount % 5 == 0) {
            freezes = 1;
        }

        // 티켓: 7, 15, 30, 45, 60...
        if (streakCount == 7) {
            tickets = 1;
        } else if (streakCount >= 15 && (streakCount - 15) % 15 == 0) {
            tickets = 1;
        }

        return RewardInfo.builder()
                .tickets(tickets)
                .freezes(freezes)
                .build();
    }

    @lombok.Value
    private static class StreakViewData {
        UserStudyReport report;
        Map<LocalDate, DailyCompletion> completionMap;
        java.util.Set<LocalDate> allFreezeDates;
    }

    @lombok.Value
    private static class StreakDayInfo {
        StreakStatus status;
        Integer streakCount;
        RewardInfo rewards;
        RewardInfo expectedRewards;
    }
}