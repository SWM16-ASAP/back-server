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
                    LanguageCode.KO, "첫 스트릭을 달성했어요. 작은 시작이 큰 변화를 만들어요.",
                    LanguageCode.EN, "First streak achieved. Small starts lead to big changes.",
                    LanguageCode.JA, "最初のストリークを達成しました。小さな始まりが大きな変化を生みます。"
            )
    ),
    DAY_3(3,
            Map.of(
                    LanguageCode.KO, "3일 연속!",
                    LanguageCode.EN, "3 Days in a Row!",
                    LanguageCode.JA, "3日連続！"
            ),
            Map.of(
                    LanguageCode.KO, "3일째 함께하고 있어요. 습관이 만들어지기 시작했어요!",
                    LanguageCode.EN, "Three days together. A habit is starting to form!",
                    LanguageCode.JA, "3日間一緒にやっています。習慣が形成され始めました！"
            )
    ),
    DAY_7(7,
            Map.of(
                    LanguageCode.KO, "일주일 달성!",
                    LanguageCode.EN, "One Week Milestone!",
                    LanguageCode.JA, "一週間達成！"
            ),
            Map.of(
                    LanguageCode.KO, "7일 동안 매일 학습했어요. 이제 루틴이 되어가고 있네요!",
                    LanguageCode.EN, "Seven days of daily learning. It's becoming a routine now!",
                    LanguageCode.JA, "7日間毎日学習しました。もうルーティンになりつつありますね！"
            )
    ),
    DAY_14(14,
            Map.of(
                    LanguageCode.KO, "2주 달성!",
                    LanguageCode.EN, "Two Weeks Strong!",
                    LanguageCode.JA, "2週間達成！"
            ),
            Map.of(
                    LanguageCode.KO, "2주째 꾸준히 해오고 있어요. 이제 습관으로 자리잡았네요.",
                    LanguageCode.EN, "Two weeks of consistency. It's now part of your routine.",
                    LanguageCode.JA, "2週間着実に続けています。もう習慣として定着しましたね。"
            )
    ),
    DAY_30(30,
            Map.of(
                    LanguageCode.KO, "한 달 달성!",
                    LanguageCode.EN, "One Month Achievement!",
                    LanguageCode.JA, "1ヶ月達成！"
            ),
            Map.of(
                    LanguageCode.KO, "30일 동안 하루도 빠짐없이 학습했어요. 대단한 꾸준함이에요!",
                    LanguageCode.EN, "30 days without missing a single day. That's remarkable consistency!",
                    LanguageCode.JA, "30日間一日も欠かさず学習しました。素晴らしい継続力です！"
            )
    ),
    DAY_50(50,
            Map.of(
                    LanguageCode.KO, "50일 돌파!",
                    LanguageCode.EN, "50 Days Breakthrough!",
                    LanguageCode.JA, "50日突破！"
            ),
            Map.of(
                    LanguageCode.KO, "벌써 50일째 함께하고 있어요. 학습이 일상의 한 부분이 됐네요.",
                    LanguageCode.EN, "Already 50 days together. Learning has become part of your daily life.",
                    LanguageCode.JA, "もう50日間一緒にやっています。学習が日常の一部になりましたね。"
            )
    ),
    DAY_100(100,
            Map.of(
                    LanguageCode.KO, "100일 기념!",
                    LanguageCode.EN, "100 Days Celebration!",
                    LanguageCode.JA, "100日記念！"
            ),
            Map.of(
                    LanguageCode.KO, "100일 동안 매일 학습했어요. 누구나 할 수 있는 일은 아니에요.",
                    LanguageCode.EN, "100 days of daily learning. Not everyone can do this.",
                    LanguageCode.JA, "100日間毎日学習しました。誰にでもできることではありません。"
            )
    ),
    DAY_365(365,
            Map.of(
                    LanguageCode.KO, "1년 달성!",
                    LanguageCode.EN, "One Year Achievement!",
                    LanguageCode.JA, "1年達成！"
            ),
            Map.of(
                    LanguageCode.KO, "365일 동안 하루도 빠짐없이 함께했어요. 정말 대단한 여정이었어요.",
                    LanguageCode.EN, "365 days together, never missing a day. What an incredible journey.",
                    LanguageCode.JA, "365日間一日も欠かさず一緒にやってきました。本当に素晴らしい旅でした。"
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
