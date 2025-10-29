package com.linglevel.api.streak.entity;

import com.linglevel.api.i18n.LanguageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 스트릭 리마인더 메시지 템플릿 정의
 * 사용자의 학습 상태에 따라 다양한 리마인더 메시지를 제공합니다.
 */
@Getter
@RequiredArgsConstructor
public enum StreakReminderMessage {
    /**
     * 시나리오 1: 일반 스트릭 유지 독려 (5가지 변형)
     */
    REGULAR_REMINDER(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("불꽃🔥을 지켜주세요!", "오늘 학습을 완료하고 %d일 스트릭 불꽃을 계속 타오르게 하세요!"),
                            new Message("오늘의 학습, 잊지 않으셨죠?", "매일의 작은 노력이 큰 결과를 만들어요. %d일 스트릭을 계속 이어가세요!"),
                            new Message("스트릭이 당신을 기다려요!", "오늘의 학습을 완료하고 %d일 스트릭을 유지하는 것, 잊지 마세요."),
                            new Message("오늘의 학습 시간!", "이 기세를 몰아 %d일 스트릭을 달성해 보세요!"),
                            new Message("하루 한 걸음", "꾸준함이 실력이에요! 멋진 %d일 스트릭을 이어가세요.")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Keep the flame🔥 alive!", "Complete your lesson today and keep your %d-day streak flame burning!"),
                            new Message("Don't forget today's lesson!", "A little progress each day adds up to big results. Keep your %d-day streak going!"),
                            new Message("Your streak is waiting!", "Just a quick reminder to complete your daily lesson and continue your %d-day streak."),
                            new Message("Time for today's lesson!", "Keep the momentum going! Secure your %d-day streak today."),
                            new Message("One step a day", "Consistency is key! Keep up your amazing %d-day streak.")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("炎🔥を守ろう！", "今日のレッスンを完了して、%d日のストリークの炎を燃やし続けましょう！"),
                            new Message("今日の学習をお忘れなく！", "日々の小さな一歩が大きな結果に繋がります。%d日のストリークを続けましょう！"),
                            new Message("ストリークが待っています！", "今日のレッスンを完了し、%d日のストリークを維持することを忘れないでください。"),
                            new Message("今日の学習時間です！", "この勢いを維持して、%d日のストリークを達成しましょう！"),
                            new Message("一日一歩", "継続は力なり！素晴らしい%d日のストリークを続けましょう。")
                    )
            )
    ),

    /**
     * 시나리오 2: 프리즈로 스트릭이 보존되었을 때
     */
    STREAK_SAVED_BY_FREEZE(
            Map.of(
                    LanguageCode.KO, List.of(new Message("스트릭이 보존됐어요", "다행이에요! 프리즈가 소중한 %d일 스트릭을 지켜줬어요. 오늘도 학습해 볼까요?")),
                    LanguageCode.EN, List.of(new Message("Your streak was saved!", "Phew! Your Streak Freeze saved your %d-day streak. Ready to learn today?")),
                    LanguageCode.JA, List.of(new Message("ストリークが保存されました！", "よかった！ストリークフリーズが%d日のストリークを守りました。今日も学習しませんか？"))
            )
    ),

    /**
     * 시나리오 3: 스트릭이 깨졌을 때
     */
    STREAK_LOST(
            Map.of(
                    LanguageCode.KO, List.of(new Message("새로운 시작도 멋져요", "스트릭은 잠시 쉬어가도 괜찮아요. 중요한 건 다시 시작하는 용기! 오늘 1일차부터 다시 시작해 볼까요?")),
                    LanguageCode.EN, List.of(new Message("A fresh start is great, too!", "It's okay to rest your streak. What matters is the courage to restart! How about starting Day 1 again today?")),
                    LanguageCode.JA, List.of(new Message("新しいスタートも素敵です！", "ストリークは少し休んでも大丈夫。大切なのは再スタートする勇気です！今日から1日目を始めてみませんか？"))
            )
    ),

    /**
     * 시나리오 4: 긴 스트릭 유지 중 (5가지 변형)
     */
    LONG_STREAK_REMINDER(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("정말 대단한 기록이에요!", "와! %d일 연속 학습 중! 상위 1%% 학습자들의 기록이에요. 이 멋진 기록을 계속 이어가세요!"),
                            new Message("당신은 꾸준함의 대명사!", "%d일 스트릭이라니, 정말 놀라워요! 당신의 열정을 응원합니다."),
                            new Message("스트릭 장인, 바로 당신!", "축하해요! %d일이라는 긴 시간 동안 매일 학습하는 것은 아무나 할 수 없는 일이에요."),
                            new Message("이 구역의 스트릭 마스터!", "%d일 연속 학습! 당신의 꾸준함이 다른 사람들에게도 영감을 주고 있어요."),
                            new Message("멈출 수 없는 학습 열정!", "벌써 %d일 스트릭! 이 기세라면 곧 새로운 기록을 세울 수 있겠어요.")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("What an incredible record!", "Wow! %d-day streak! That's a record for the top 1%% of learners. Keep this amazing record going!"),
                            new Message("You are the definition of consistency!", "A %d-day streak is truly amazing! We're cheering for your passion."),
                            new Message("Streak Artisan, that's you!", "Congratulations! Learning every day for %d days is something not everyone can do."),
                            new Message("The Streak Master of this area!", "%d days of continuous learning! Your consistency is inspiring others."),
                            new Message("Unstoppable learning passion!", "Already a %d-day streak! At this rate, you'll be setting a new record soon.")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("本当に素晴らしい記録です！", "すごい！%d日連続学習中！上位1%%の学習者の記録です。この素晴らしい記録を続けましょう！"),
                            new Message("あなたは継続の代名詞！", "%d日のストリークとは、本当に素晴らしいです！あなたの情熱を応援しています。"),
                            new Message("ストリークの職人、それはあなたです！", "おめでとうございます！%d日間毎日学習するのは、誰にでもできることではありません。"),
                            new Message("このエリアのストリークマスター！", "%d日連続学習！あなたの継続性が他の人にもインスピレーションを与えています。"),
                            new Message("止められない学習熱！", "すでに%d日のストリーク！この勢いなら、すぐに新しい記録を打ち立てられそうですね。")
                    )
            )
    ),

    /**
     * 시나리오 5: 마일스톤 직전
     */
    MILESTONE_APPROACHING(
            Map.of(
                    LanguageCode.KO, List.of(new Message("마일스톤이 코앞!", "현재 %d일! 하루만 더 하면 특별한 마일스톤을 달성해요!")),
                    LanguageCode.EN, List.of(new Message("Milestone ahead!", "You're at %d days! Just one more day to reach a special milestone!")),
                    LanguageCode.JA, List.of(new Message("マイルストーン目前！", "現在%d日！あと1日で特別なマイルストーンを達成します！"))
            )
    );

    private static final Random RANDOM = new Random();

    @Getter
    @RequiredArgsConstructor
    public static class Message {
        private final String title;
        private final String bodyFormat;
    }

    private final Map<LanguageCode, List<Message>> messages;

    /**
     * 지정된 언어의 메시지 목록에서 임의의 메시지(제목+본문 쌍)를 가져옵니다.
     *
     * @param languageCode 언어 코드
     * @return 해당 언어의 임의 메시지 객체, 없으면 영어 메시지
     */
    public Message getRandomMessage(LanguageCode languageCode) {
        List<Message> messageList = messages.getOrDefault(languageCode, messages.get(LanguageCode.EN));
        return messageList.get(RANDOM.nextInt(messageList.size()));
    }
}
