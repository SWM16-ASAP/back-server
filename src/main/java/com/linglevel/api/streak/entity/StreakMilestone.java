package com.linglevel.api.streak.entity;

import com.linglevel.api.i18n.LanguageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

/**
 * 스트릭 마일스톤 정의
 * 특별한 날들(1, 3, 7, 14, 30일 등)에 대한 축하 메시지를 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum StreakMilestone {
    DAY_0(0,
            Map.of(
                    LanguageCode.KO, "새로운 시작",
                    LanguageCode.EN, "A New Start",
                    LanguageCode.JA, "新しい始まり"
            ),
            Map.of(
                    LanguageCode.KO, "첫 학습을 시작하고 스트릭을 만들어보세요! 당신의 도전을 응원합니다.",
                    LanguageCode.EN, "Start your first lesson and build a streak! We're cheering for your challenge.",
                    LanguageCode.JA, "最初の学習を始めて、ストリークを作りましょう！あなたの挑戦を応援します。"
            )
    ),
    DAY_1(1,
            Map.of(
                    LanguageCode.KO, "첫 시작!",
                    LanguageCode.EN, "First Step!",
                    LanguageCode.JA, "初めの一歩！"
            ),
            Map.of(
                    LanguageCode.KO, "축하합니다! 첫 스트릭을 달성했습니다.",
                    LanguageCode.EN, "Congratulations! You've achieved your first streak.",
                    LanguageCode.JA, "おめでとうございます！最初のストリークを達成しました。"
            )
    ),
    DAY_3(3,
            Map.of(
                    LanguageCode.KO, "3일 연속!",
                    LanguageCode.EN, "3 Days in a Row!",
                    LanguageCode.JA, "3日連続！"
            ),
            Map.of(
                    LanguageCode.KO, "멋진 시작입니다! 꾸준함의 힘을 느끼고 있나요?",
                    LanguageCode.EN, "Great start! Can you feel the power of consistency?",
                    LanguageCode.JA, "素晴らしいスタートです！継続の力を感じていますか？"
            )
    ),
    DAY_7(7,
            Map.of(
                    LanguageCode.KO, "일주일 달성!",
                    LanguageCode.EN, "One Week Milestone!",
                    LanguageCode.JA, "一週間達成！"
            ),
            Map.of(
                    LanguageCode.KO, "대단합니다! 첫 주를 완료했습니다. 습관이 만들어지고 있어요!",
                    LanguageCode.EN, "Amazing! You've completed your first week. A habit is forming!",
                    LanguageCode.JA, "素晴らしい！最初の一週間を完了しました。習慣が形成されつつあります！"
            )
    ),
    DAY_14(14,
            Map.of(
                    LanguageCode.KO, "2주 달성!",
                    LanguageCode.EN, "Two Weeks Strong!",
                    LanguageCode.JA, "2週間達成！"
            ),
            Map.of(
                    LanguageCode.KO, "놀라워요! 2주 연속 스트릭! 이제 진짜 습관이 되어가고 있습니다.",
                    LanguageCode.EN, "Incredible! Two weeks in a row! This is becoming a real habit.",
                    LanguageCode.JA, "信じられない！2週間連続！これは本当の習慣になりつつあります。"
            )
    ),
    DAY_30(30,
            Map.of(
                    LanguageCode.KO, "한 달 달성!",
                    LanguageCode.EN, "One Month Achievement!",
                    LanguageCode.JA, "1ヶ月達成！"
            ),
            Map.of(
                    LanguageCode.KO, "축하합니다! 한 달 연속 스트릭 달성! 당신의 헌신이 대단합니다!",
                    LanguageCode.EN, "Congratulations! One month streak achieved! Your dedication is outstanding!",
                    LanguageCode.JA, "おめでとうございます！1ヶ月連続ストリーク達成！あなたの献身は素晴らしい！"
            )
    ),
    DAY_50(50,
            Map.of(
                    LanguageCode.KO, "50일 돌파!",
                    LanguageCode.EN, "50 Days Breakthrough!",
                    LanguageCode.JA, "50日突破！"
            ),
            Map.of(
                    LanguageCode.KO, "놀라운 성취입니다! 50일 연속 스트릭! 진정한 학습자입니다!",
                    LanguageCode.EN, "Remarkable achievement! 50 days in a row! You're a true learner!",
                    LanguageCode.JA, "驚くべき成果！50日連続！真の学習者です！"
            )
    ),
    DAY_100(100,
            Map.of(
                    LanguageCode.KO, "100일 기념!",
                    LanguageCode.EN, "100 Days Celebration!",
                    LanguageCode.JA, "100日記念！"
            ),
            Map.of(
                    LanguageCode.KO, "백일 기념! 대단한 성취입니다! 당신은 정말 특별합니다!",
                    LanguageCode.EN, "100 days milestone! Extraordinary achievement! You are truly special!",
                    LanguageCode.JA, "100日マイルストーン！並外れた成果！あなたは本当に特別です！"
            )
    ),
    DAY_365(365,
            Map.of(
                    LanguageCode.KO, "1년 달성!",
                    LanguageCode.EN, "One Year Achievement!",
                    LanguageCode.JA, "1年達成！"
            ),
            Map.of(
                    LanguageCode.KO, "전설이 탄생했습니다! 365일 연속 스트릭! 당신은 영감 그 자체입니다!",
                    LanguageCode.EN, "A legend is born! 365 days streak! You are an inspiration!",
                    LanguageCode.JA, "伝説が誕生しました！365日連続ストリーク！あなたはインスピレーションそのものです！"
            )
    );

    private final int day;
    private final Map<LanguageCode, String> titles;
    private final Map<LanguageCode, String> messages;

    /**
     * 주어진 날짜에 해당하는 마일스톤을 찾습니다.
     *
     * @param day 스트릭 일수
     * @return 마일스톤이 있으면 해당 마일스톤, 없으면 Optional.empty()
     */
    public static Optional<StreakMilestone> fromDay(int day) {
        for (StreakMilestone milestone : values()) {
            if (milestone.day == day) {
                return Optional.of(milestone);
            }
        }
        return Optional.empty();
    }

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
     * 지정된 언어로 메시지를 가져옵니다.
     *
     * @param languageCode 언어 코드
     * @return 해당 언어의 메시지, 없으면 영어 메시지
     */
    public String getMessage(LanguageCode languageCode) {
        return messages.getOrDefault(languageCode, messages.get(LanguageCode.EN));
    }
}
