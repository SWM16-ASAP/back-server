package com.linglevel.api.streak.entity;

import com.linglevel.api.i18n.LanguageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 스트릭 리마인더 메시지 템플릿 정의
 * 사용자의 학습 상태에 따라 다양한 리마인더 메시지를 제공합니다.
 */
@Getter
@RequiredArgsConstructor
public enum StreakReminderMessage {
    /**
     * 시나리오 1: 일반 스트릭 유지 독려 (수정: 게이미피케이션 요소 강화)
     * 사용자가 활성 스트릭을 가지고 있으며, 오늘 학습을 완료하지 않은 경우
     */
    REGULAR_REMINDER(
            Map.of(
                    LanguageCode.KO, "불꽃🔥을 지켜주세요!",
                    LanguageCode.EN, "Keep the flame🔥 alive!",
                    LanguageCode.JA, "炎🔥を守ろう！"
            ),
            Map.of(
                    LanguageCode.KO, "오늘 학습을 완료하고 %d일 스트릭 불꽃을 계속 타오르게 하세요!",
                    LanguageCode.EN, "Complete your lesson today and keep your %d-day streak flame burning!",
                    LanguageCode.JA, "今日のレッスンを完了して、%d日のストリークの炎を燃やし続けましょう！"
            )
    ),

    /**
     * 시나리오 2: 프리즈로 스트릭이 보존되었을 때 (유지)
     * 어제 학습하지 않았지만 프리즈가 스트릭을 보호한 경우
     */
    STREAK_SAVED_BY_FREEZE(
            Map.of(
                    LanguageCode.KO, "스트릭이 보존됐어요",
                    LanguageCode.EN, "Your streak was saved!",
                    LanguageCode.JA, "ストリークが保存されました！"
            ),
            Map.of(
                    LanguageCode.KO, "다행이에요! 프리즈가 소중한 %d일 스트릭을 지켜줬어요. 오늘도 학습해 볼까요?",
                    LanguageCode.EN, "Phew! Your Streak Freeze saved your %d-day streak. Ready to learn today?",
                    LanguageCode.JA, "よかった！ストリークフリーズが%d日のストリークを守りました。今日も学習しませんか？"
            )
    ),

    /**
     * 시나리오 3: 스트릭이 깨졌을 때 (수정: 부정적 감정 최소화)
     * 어제 스트릭이 끊겼으며, 새롭게 시작할 수 있도록 격려
     */
    STREAK_LOST(
            Map.of(
                    LanguageCode.KO, "새로운 시작도 멋져요",
                    LanguageCode.EN, "A fresh start is great, too!",
                    LanguageCode.JA, "新しいスタートも素敵です！"
            ),
            Map.of(
                    LanguageCode.KO, "스트릭은 잠시 쉬어가도 괜찮아요. 중요한 건 다시 시작하는 용기! 오늘 1일차부터 다시 시작해 볼까요?",
                    LanguageCode.EN, "It's okay to rest your streak. What matters is the courage to restart! How about starting Day 1 again today?",
                    LanguageCode.JA, "ストリークは少し休んでも大丈夫。大切なのは再スタートする勇気です！今日から1日目を始めてみませんか？"
            )
    ),

    /**
     * 시나리오 4: 긴 스트릭 유지 중 (수정: 사회적 증명 활용)
     * 일주일 이상의 스트릭을 유지하고 있는 사용자에게 특별한 동기부여
     */
    LONG_STREAK_REMINDER(
            Map.of(
                    LanguageCode.KO, "정말 대단한 기록이에요!",
                    LanguageCode.EN, "What an incredible record!",
                    LanguageCode.JA, "本当に素晴らしい記録です！"
            ),
            Map.of(
                    LanguageCode.KO, "와! %d일 연속 학습 중! 상위 1% 학습자들의 기록이에요. 이 멋진 기록을 계속 이어가세요!",
                    LanguageCode.EN, "Wow! %d-day streak! That's a record for the top 1% of learners. Keep this amazing record going!",
                    LanguageCode.JA, "すごい！%d日連続学習中！上位1%の学習者の記録です。この素晴らしい記録を続けましょう！"
            )
    ),

    /**
     * 시나리오 5: 마일스톤 직전 (유지)
     * 다음 마일스톤 달성이 임박한 경우
     */
    MILESTONE_APPROACHING(
            Map.of(
                    LanguageCode.KO, "마일스톤이 코앞!",
                    LanguageCode.EN, "Milestone ahead!",
                    LanguageCode.JA, "マイルストーン目前！"
            ),
            Map.of(
                    LanguageCode.KO, "현재 %d일! 하루만 더 하면 특별한 마일스톤을 달성해요!",
                    LanguageCode.EN, "You're at %d days! Just one more day to reach a special milestone!",
                    LanguageCode.JA, "現在%d日！あと1日で特別なマイルストーンを達成します！"
            )
    );

    private final Map<LanguageCode, String> titles;
    private final Map<LanguageCode, String> bodies;

    /**
     * 지정된 언어로 제목을 가져옵니다.
     *
     * @param languageCode 언어 코드
     * @return 해당 언어의 제목, 없으면 영어 제목
     */
    public String getTitle(LanguageCode languageCode) {
        return titles.getOrDefault(languageCode, titles.get(LanguageCode.EN));
    }

    /**
     * 지정된 언어로 본문을 가져옵니다.
     *
     * @param languageCode 언어 코드
     * @return 해당 언어의 본문, 없으면 영어 본문
     */
    public String getBody(LanguageCode languageCode) {
        return bodies.getOrDefault(languageCode, bodies.get(LanguageCode.EN));
    }

    /**
     * 포맷팅된 본문 메시지를 가져옵니다.
     * String.format을 사용하여 동적 값을 삽입합니다.
     *
     * @param languageCode 언어 코드
     * @param args 포맷 인자 (예: 스트릭 일수)
     * @return 포맷팅된 메시지
     */
    public String getFormattedBody(LanguageCode languageCode, Object... args) {
        String format = bodies.getOrDefault(languageCode, bodies.get(LanguageCode.EN));
        return String.format(format, args);
    }
}
