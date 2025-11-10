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
     * 시나리오 0: 학습 권장 (활성 유저 대상, 평소 학습 시간에 발송)
     */
    LEARNING_ENCOURAGEMENT(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("학습할 시간이에요!", "오늘도 작은 한 걸음을 시작해 볼까요?"),
                            new Message("평소 학습 시간이에요", "매일의 꾸준함이 실력을 만들어요. 지금 시작해 보세요!"),
                            new Message("학습 시간입니다", "익숙한 이 시간, 오늘도 함께해요!"),
                            new Message("준비되셨나요?", "평소처럼 학습할 시간이에요. 시작해 볼까요?")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Time to learn!", "How about taking a small step today?"),
                            new Message("Your usual study time", "Daily consistency builds skills. Start now!"),
                            new Message("Study time", "This familiar time, let's do it together today!"),
                            new Message("Ready?", "It's your usual study time. Shall we start?")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("学習の時間です！", "今日も小さな一歩を始めてみませんか？"),
                            new Message("いつもの学習時間です", "毎日の継続が実力を作ります。今すぐ始めましょう！"),
                            new Message("学習時間です", "慣れ親しんだこの時間、今日も一緒に！"),
                            new Message("準備はいいですか？", "いつもの学習時間です。始めてみませんか？")
                    )
            )
    ),

    /**
     * 스트릭 보호 (밤 9시 고정, currentStreak > 0 && 오늘 학습 미완료)
     */
    STREAK_PROTECTION(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("불꽃🔥을 지켜주세요!", "오늘 학습을 완료하고 %d일 스트릭을 유지하세요!"),
                            new Message("스트릭이 위험해요!", "자기 전에 학습을 완료하고 %d일 스트릭을 지켜주세요."),
                            new Message("마지막 기회예요", "오늘이 가기 전에 %d일 스트릭을 이어가세요!"),
                            new Message("아직 늦지 않았어요!", "지금 학습하고 소중한 %d일 스트릭을 보호하세요.")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Keep the flame🔥 alive!", "Complete today's lesson and maintain your %d-day streak!"),
                            new Message("Your streak is at risk!", "Complete your lesson before bed and protect your %d-day streak."),
                            new Message("Last chance!", "Continue your %d-day streak before the day ends!"),
                            new Message("Not too late yet!", "Study now and protect your precious %d-day streak.")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("炎🔥を守ろう！", "今日のレッスンを完了して%d日のストリークを維持しましょう！"),
                            new Message("ストリークが危険です！", "寝る前にレッスンを完了して%d日のストリークを守りましょう。"),
                            new Message("最後のチャンスです", "今日が終わる前に%d日のストリークを続けましょう！"),
                            new Message("まだ遅くありません！", "今学習して大切な%d日のストリークを守りましょう。")
                    )
            )
    ),

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
     * 시나리오 3: 스트릭이 깨진 후 Day 1 (23-24시간) - 즉시 재시작 독려
     */
    STREAK_LOST_DAY1(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("새로운 시작!", "스트릭이 끊겼지만 괜찮아요. 오늘부터 다시 시작하면 돼요!"),
                            new Message("다시 일어설 시간", "넘어졌다면 다시 일어서면 되죠! 오늘 1일차를 시작해 볼까요?"),
                            new Message("괜찮아요!", "스트릭이 깨졌어도 괜찮아요. 중요한 건 다시 시작하는 용기예요.")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("A fresh start!", "Your streak ended, but that's okay. Let's start again today!"),
                            new Message("Time to get back up", "If you fall, just get back up! How about starting Day 1 today?"),
                            new Message("It's okay!", "It's okay your streak ended. What matters is the courage to restart.")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("新しいスタート！", "ストリークが途切れましたが、大丈夫です。今日からまた始めましょう！"),
                            new Message("立ち上がる時間", "転んだら起き上がればいいんです！今日から1日目を始めてみませんか？"),
                            new Message("大丈夫です！", "ストリークが途切れても大丈夫。大切なのは再スタートする勇気です。")
                    )
            )
    ),

    /**
     * 시나리오 3-2: 스트릭이 깨진 후 Day 2 (47-48시간) - 부드러운 복귀 유도
     */
    STREAK_LOST_DAY2(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("함께 다시 시작해요", "어제 새로 시작하지 못했지만, 오늘이 바로 그날이에요!"),
                            new Message("기다리고 있어요", "언제든 돌아올 수 있어요. 오늘 작은 한 걸음을 시작해 볼까요?"),
                            new Message("두 번째 기회", "완벽하지 않아도 괜찮아요. 오늘 다시 도전해 보세요!")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Let's start together", "You didn't restart yesterday, but today is the day!"),
                            new Message("We're waiting", "You can come back anytime. How about taking a small step today?"),
                            new Message("Second chance", "You don't have to be perfect. Try again today!")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("一緒に再スタート", "昨日は始められませんでしたが、今日がその日です！"),
                            new Message("待っています", "いつでも戻ってこれます。今日、小さな一歩を始めてみませんか？"),
                            new Message("2度目のチャンス", "完璧でなくても大丈夫。今日また挑戦してみてください！")
                    )
            )
    ),

    /**
     * 시나리오 3-3: 스트릭이 깨진 후 Day 3 (71-72시간) - 강한 복귀 유도
     */
    STREAK_LOST_DAY3(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("당신을 기억해요", "그동안 쌓아온 노력이 아깝지 않나요? 오늘 다시 시작해 보세요."),
                            new Message("아직 늦지 않았어요", "지금 돌아오면 과거의 당신처럼 다시 성장할 수 있어요."),
                            new Message("마지막 기회일지도", "오늘이 다시 시작하기에 마지막으로 좋은 타이밍일 수 있어요.")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("We remember you", "Don't you miss the effort you've built up? Start again today."),
                            new Message("It's not too late", "Come back now and you can grow like you used to."),
                            new Message("Maybe the last chance", "Today might be the last good timing to restart.")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("あなたを覚えています", "これまで積み重ねた努力がもったいなくないですか？今日再スタートしてみてください。"),
                            new Message("まだ遅くありません", "今戻れば、以前のように成長できます。"),
                            new Message("最後のチャンスかも", "今日が再スタートするのに最後の良いタイミングかもしれません。")
                    )
            )
    ),

    /**
     * 시나리오 3-4: 스트릭이 깨진 후 Day 4 (95-96시간) - 최후의 메시지
     */
    STREAK_LOST_DAY4(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("마지막 인사", "언제든 돌아오고 싶으면 여기 있을게요. 당신의 선택을 응원해요."),
                            new Message("문은 열려 있어요", "학습은 언제나 여기서 기다리고 있어요. 준비되면 돌아오세요."),
                            new Message("당신의 페이스로", "서두를 필요 없어요. 준비됐을 때 다시 만나요.")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Final goodbye", "We'll be here whenever you want to come back. We support your choice."),
                            new Message("The door is open", "Learning is always waiting here. Come back when you're ready."),
                            new Message("At your own pace", "No need to rush. See you again when you're ready.")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("最後の挨拶", "戻りたくなったらいつでもここにいます。あなたの選択を応援しています。"),
                            new Message("ドアは開いています", "学習はいつもここで待っています。準備ができたら戻ってきてください。"),
                            new Message("自分のペースで", "急ぐ必要はありません。準備ができたらまた会いましょう。")
                    )
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
    ),

    /**
     * 시나리오 6: 어제 학습 놓침 - 첫 번째 복귀 유도 (Day 2)
     */
    COMEBACK_DAY2(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("어제는 쉬어갔죠?", "괜찮아요! 오늘 다시 시작하면 %d일 스트릭을 되찾을 수 있어요."),
                            new Message("오늘이라면 가능해요!", "어제는 빼먹었지만, 오늘 학습하고 %d일 스트릭을 이어가세요!"),
                            new Message("다시 시작할 시간!", "하루쯤은 괜찮아요. 오늘 학습하고 %d일의 기록을 계속 쌓아가세요.")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Took a break yesterday?", "That's okay! Start again today and get back to your %d-day streak."),
                            new Message("Today's your chance!", "You missed yesterday, but today you can continue your %d-day streak!"),
                            new Message("Time for a fresh start!", "One day off is fine. Study today and keep building on your %d-day record.")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("昨日は休憩しましたか？", "大丈夫です！今日再開すれば、%d日のストリークを取り戻せます。"),
                            new Message("今日ならできます！", "昨日は休んでしまいましたが、今日学習して%d日のストリークを続けましょう！"),
                            new Message("再スタートの時間！", "1日くらいは大丈夫。今日学習して%d日の記録を積み重ねていきましょう。")
                    )
            )
    ),

    /**
     * 시나리오 7: 이틀 연속 놓침 - 두 번째 복귀 유도 (Day 3)
     */
    COMEBACK_DAY3(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("다시 돌아올 때예요", "%d일이나 쌓은 소중한 기록이 기다리고 있어요. 오늘 다시 시작해 볼까요?"),
                            new Message("아직 늦지 않았어요!", "지금 돌아오면 %d일의 노력이 헛되지 않아요. 오늘 한번 해보세요!"),
                            new Message("여기서 멈추긴 아쉬워요", "%d일 동안의 여정이 그리울 거예요. 오늘 다시 시작하는 건 어떨까요?")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Time to come back", "Your precious %d-day record is waiting. How about starting again today?"),
                            new Message("It's not too late!", "Come back now and your %d days of effort won't be wasted. Give it a try today!"),
                            new Message("Too good to stop here", "You'll miss your %d-day journey. Why not restart today?")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("戻ってくる時です", "大切な%d日の記録が待っています。今日再開してみませんか？"),
                            new Message("まだ遅くありません！", "今戻れば、%d日の努力が無駄になりません。今日やってみましょう！"),
                            new Message("ここで止めるのはもったいない", "%d日間の旅が恋しくなるでしょう。今日再スタートしませんか？")
                    )
            )
    ),

    /**
     * 시나리오 8: 사흘 연속 놓침 - 마지막 복귀 유도 (Day 4)
     */
    COMEBACK_DAY4(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("마지막으로 부탁드려요", "%d일의 추억을 간직하고 새로운 시작을 해보는 건 어떨까요?"),
                            new Message("언제든 돌아올 수 있어요", "완벽하지 않아도 괜찮아요. %d일의 경험을 바탕으로 다시 도전해 보세요."),
                            new Message("당신을 기다리고 있어요", "학습은 언제나 여기 있어요. %d일 동안 함께했던 시간을 기억하며 다시 시작해 볼까요?")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("One last reminder", "How about treasuring your %d-day memory and starting fresh?"),
                            new Message("You can always come back", "It's okay not to be perfect. Try again building on your %d-day experience."),
                            new Message("We're waiting for you", "Learning is always here. Remember your %d days together and start again?")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("最後にお願いします", "%d日の思い出を胸に、新しいスタートを切ってみませんか？"),
                            new Message("いつでも戻ってこれます", "完璧でなくても大丈夫。%d日の経験を基に、再チャレンジしてみましょう。"),
                            new Message("お待ちしています", "学習はいつもここにあります。%d日間一緒に過ごした時間を思い出して、また始めてみませんか？")
                    )
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
