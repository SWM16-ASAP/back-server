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
     * 매시간 발송되므로 가장 다양한 메시지 필요
     */
    LEARNING_ENCOURAGEMENT(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("평소 이 시간에 학습하시잖아요?", "습관의 힘으로 오늘도 시작해 볼까요?"),
                            new Message("언제나처럼 이 시간!", "익숙한 루틴으로 오늘의 학습을 시작해요"),
                            new Message("당신의 학습 시간이에요", "매일 이 시간을 지켜온 당신, 오늘도 해냅시다!"),
                            new Message("골든 타임이에요!", "평소처럼 집중력 좋은 이 시간, 놓치지 마세요"),
                            new Message("학습할 시간이에요", "오늘도 작은 한 걸음을 시작해 볼까요?"),
                            new Message("매일 이 시간, 당신을 기다려요", "꾸준함이 실력이 되는 순간이에요!"),
                            new Message("오늘도 시작해볼까요?", "언제나처럼 이 시간, 편안하게 학습해요"),
                            new Message("평소 학습 시간입니다", "루틴의 힘을 믿고 오늘도 함께해요!")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Your usual study time!", "Let's start with the power of habit today?"),
                            new Message("As always, it's time!", "Start today's learning with your familiar routine"),
                            new Message("It's your learning time", "You've kept this time every day, let's do it today!"),
                            new Message("Your golden hour!", "Don't miss this focused time as usual"),
                            new Message("Time to learn!", "How about taking a small step today?"),
                            new Message("This time waits for you daily", "The moment consistency becomes skill!"),
                            new Message("Shall we start today?", "Like always, let's learn comfortably at this time"),
                            new Message("Your regular study time", "Trust the power of routine and let's go today!")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("いつもこの時間に学習してますよね？", "習慣の力で今日も始めてみませんか？"),
                            new Message("いつものこの時間！", "慣れ親しんだルーティンで今日の学習を始めましょう"),
                            new Message("あなたの学習時間です", "毎日この時間を守ってきたあなた、今日もやりましょう！"),
                            new Message("ゴールデンタイムです！", "いつものように集中力の良いこの時間、逃さないで"),
                            new Message("学習の時間です！", "今日も小さな一歩を始めてみませんか？"),
                            new Message("毎日この時間、あなたを待っています", "継続が実力になる瞬間です！"),
                            new Message("今日も始めましょうか？", "いつものようにこの時間、気楽に学習しましょう"),
                            new Message("いつもの学習時間です", "ルーティンの力を信じて今日も一緒に！")
                    )
            )
    ),

    /**
     * 스트릭 보호 (밤 9시 고정, currentStreak > 0 && 오늘 학습 미완료)
     * 긴박하지만 친근하게, 부담 줄이기
     */
    STREAK_PROTECTION(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("자기 전 5분만요!", "%d일의 노력이 사라지기 전에 짧은 학습 한 번 어때요?"),
                            new Message("오늘 하루만 남았어요", "잠들기 전 %d일 스트릭을 지키고 푹 자요!"),
                            new Message("%d일 스트릭이 기다려요", "자기 전에 간단히 끝내고 마음 편히 주무세요"),
                            new Message("거의 다 왔어요!", "하루를 멋지게 마무리하고 %d일 스트릭을 지켜요"),
                            new Message("불꽃🔥을 지켜주세요", "오늘만 완료하면 %d일 스트릭이 계속돼요!"),
                            new Message("아직 늦지 않았어요", "지금 시작하면 %d일의 기록을 이어갈 수 있어요"),
                            new Message("마지막 기회!", "오늘이 가기 전에 %d일 스트릭을 완성하세요")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Just 5 minutes before bed!", "How about a quick lesson before losing %d days of effort?"),
                            new Message("Only today left", "Keep your %d-day streak before bed and sleep well!"),
                            new Message("Your %d-day streak is waiting", "Finish quickly before bed and sleep peacefully"),
                            new Message("Almost there!", "End the day beautifully and keep your %d-day streak"),
                            new Message("Keep the flame🔥 alive", "Complete today and your %d-day streak continues!"),
                            new Message("Not too late yet", "Start now and continue your %d-day record"),
                            new Message("Last chance!", "Complete your %d-day streak before the day ends")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("寝る前に5分だけ！", "%d日の努力が消える前に短い学習一回どうですか？"),
                            new Message("今日一日だけ残っています", "寝る前に%d日のストリークを守ってぐっすり寝ましょう！"),
                            new Message("%d日のストリークが待っています", "寝る前に簡単に終えて安心して眠りましょう"),
                            new Message("もうすぐです！", "一日を素敵に締めくくって%d日のストリークを守りましょう"),
                            new Message("炎🔥を守ろう", "今日だけ完了すれば%d日のストリークが続きます！"),
                            new Message("まだ遅くありません", "今始めれば%d日の記録を続けられます"),
                            new Message("最後のチャンス！", "今日が終わる前に%d日のストリークを完成させましょう")
                    )
            )
    ),

    /**
     * 어제 프리즈로 스트릭이 유지됨 (밤 9시 알림에서 사용)
     * 어제 프리즈 덕분에 살았으니 오늘은 꼭 학습해야 함을 강조
     */
    STREAK_SAVED_BY_FREEZE(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("프리즈가 지켜줬어요!", "어제는 프리즈 덕분에 %d일 스트릭이 유지됐어요. 오늘은 꼭 학습해야 해요!"),
                            new Message("위기 넘겼어요", "프리즈로 %d일 스트릭을 살렸어요. 오늘은 반드시 학습해 주세요!"),
                            new Message("한 번 살았어요", "어제는 프리즈가 %d일 스트릭을 지켜줬어요. 오늘은 꼭 이어가요!"),
                            new Message("프리즈 덕분이에요", "어제 프리즈로 %d일 스트릭 유지! 오늘 학습하고 다시 증가시켜요"),
                            new Message("세이프! 하지만", "프리즈로 %d일 스트릭은 지켰지만, 오늘은 직접 학습해야 해요!")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Freeze saved you!", "Yesterday, Freeze kept your %d-day streak alive. You must study today!"),
                            new Message("Crisis averted", "Freeze saved your %d-day streak. Please study today for sure!"),
                            new Message("Got a second chance", "Yesterday, Freeze protected your %d-day streak. Let's continue today!"),
                            new Message("Thanks to Freeze", "Freeze kept your %d-day streak yesterday! Study today to grow it again"),
                            new Message("Safe! But", "Freeze protected your %d-day streak, but today you need to study yourself!")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("フリーズが守りました！", "昨日はフリーズのおかげで%d日のストリークが維持されました。今日は必ず学習してください！"),
                            new Message("危機を乗り越えました", "フリーズで%d日のストリークを救いました。今日は必ず学習してください！"),
                            new Message("一度助かりました", "昨日はフリーズが%d日のストリークを守りました。今日は必ず続けましょう！"),
                            new Message("フリーズのおかげです", "昨日フリーズで%d日のストリーク維持！今日学習して再び増やしましょう"),
                            new Message("セーフ！でも", "フリーズで%d日のストリークは守りましたが、今日は自分で学習する必要があります！")
                    )
            )
    ),

    /**
     * 시나리오 3: 스트릭이 깨진 후 Day 1 (23-24시간) - 즉시 재시작 독려
     * 위로하고 재시작의 부담 낮추기
     */
    STREAK_LOST_DAY1(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("완벽하지 않아도 괜찮아요", "스트릭이 끊겼지만, 오늘부터 새로운 기록을 만들어요!"),
                            new Message("0일부터 다시 시작", "전에도 해냈으니 이번에도 할 수 있어요!"),
                            new Message("리셋은 새로운 기회", "과거 기록은 경험으로, 오늘은 1일차로 출발해요"),
                            new Message("새로운 시작!", "스트릭이 리셋됐어도 괜찮아요. 오늘부터 다시 쌓아가요"),
                            new Message("다시 일어설 시간", "넘어졌다면 다시 일어서면 되죠! 오늘 1일차 시작해요"),
                            new Message("경험은 사라지지 않아요", "스트릭은 리셋돼도 당신의 실력은 그대로예요")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("It's okay not to be perfect", "Your streak ended, but let's create a new record from today!"),
                            new Message("Starting from day 0 again", "You did it before, you can do it again!"),
                            new Message("Reset is a new opportunity", "Past records become experience, today starts as day 1"),
                            new Message("A fresh start!", "Even if your streak reset, it's okay. Let's build again from today"),
                            new Message("Time to get back up", "If you fall, just get back up! Starting day 1 today"),
                            new Message("Experience doesn't disappear", "Even if streak resets, your skills remain")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("完璧じゃなくても大丈夫", "ストリークが途切れましたが、今日から新しい記録を作りましょう！"),
                            new Message("0日からまた始める", "前にもできたから今回もできます！"),
                            new Message("リセットは新しいチャンス", "過去の記録は経験として、今日は1日目として出発しましょう"),
                            new Message("新しいスタート！", "ストリークがリセットされても大丈夫。今日からまた積み上げましょう"),
                            new Message("立ち上がる時間", "転んだらまた起き上がればいいんです！今日1日目を始めます"),
                            new Message("経験は消えません", "ストリークがリセットされてもあなたのスキルはそのままです")
                    )
            )
    ),

    /**
     * 시나리오 3-2: 스트릭이 깨진 후 Day 2 (47-48시간) - 부드러운 복귀 유도
     */
    STREAK_LOST_DAY2(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("어제 못했어도 괜찮아요", "오늘이 진짜 재시작의 날이 될 수 있어요"),
                            new Message("두 번째 기회", "한 번 더 도전할 용기, 오늘 보여주세요!"),
                            new Message("함께 다시 시작해요", "어제 못했지만, 오늘이 바로 그날이에요!"),
                            new Message("기다리고 있어요", "언제든 돌아올 수 있어요. 오늘 시작해 볼까요?"),
                            new Message("완벽하지 않아도 돼요", "중요한 건 다시 시작하는 것. 오늘 해봐요!")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("It's okay you missed yesterday", "Today can be your real restart day"),
                            new Message("Second chance", "Show your courage to try once more today!"),
                            new Message("Let's start together", "You didn't yesterday, but today is the day!"),
                            new Message("We're waiting", "You can come back anytime. Shall we start today?"),
                            new Message("You don't have to be perfect", "What matters is restarting. Try today!")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("昨日できなくても大丈夫", "今日が本当の再スタートの日になれます"),
                            new Message("2度目のチャンス", "もう一度挑戦する勇気、今日見せてください！"),
                            new Message("一緒に再スタート", "昨日はできませんでしたが、今日がその日です！"),
                            new Message("待っています", "いつでも戻ってこれます。今日始めてみませんか？"),
                            new Message("完璧じゃなくていい", "大切なのは再スタートすること。今日やってみて！")
                    )
            )
    ),

    /**
     * 시나리오 3-3: 스트릭이 깨진 후 Day 3 (71-72시간) - 강한 복귀 유도
     */
    STREAK_LOST_DAY3(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("그동안의 노력이 사라지지 않아요", "경험은 남습니다. 오늘 다시 1일차 시작해요"),
                            new Message("당신을 믿어요", "전에 해냈던 것처럼 다시 할 수 있어요"),
                            new Message("당신을 기억해요", "쌓아온 실력은 그대로예요. 오늘 다시 시작해 보세요"),
                            new Message("아직 늦지 않았어요", "지금 돌아오면 다시 성장할 수 있어요"),
                            new Message("마지막 기회일지도", "오늘이 재시작하기 좋은 타이밍일 수 있어요")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("Your past effort doesn't disappear", "Experience remains. Let's start day 1 again today"),
                            new Message("We believe in you", "You can do it again like you did before"),
                            new Message("We remember you", "Your skills remain intact. Start again today"),
                            new Message("It's not too late", "Come back now and you can grow again"),
                            new Message("Maybe the last chance", "Today might be a good timing to restart")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("これまでの努力は消えません", "経験は残ります。今日また1日目を始めましょう"),
                            new Message("あなたを信じています", "前にできたように、また できます"),
                            new Message("あなたを覚えています", "積み上げたスキルはそのままです。今日再スタートしてみてください"),
                            new Message("まだ遅くありません", "今戻れば、また成長できます"),
                            new Message("最後のチャンスかも", "今日が再スタートに良いタイミングかもしれません")
                    )
            )
    ),

    /**
     * 시나리오 3-4: 스트릭이 깨진 후 Day 4 (95-96시간) - 최후의 메시지
     * 강요하지 않고 따뜻하게 배웅
     */
    STREAK_LOST_DAY4(
            Map.of(
                    LanguageCode.KO, List.of(
                            new Message("언제든 돌아올 수 있어요", "준비됐을 때 다시 만나요. 기다릴게요"),
                            new Message("쉬어가도 돼요", "학습은 언제나 여기 있어요. 편할 때 돌아오세요"),
                            new Message("마지막 인사", "언제든 돌아오고 싶으면 여기 있을게요"),
                            new Message("문은 열려 있어요", "준비되면 언제든 다시 시작할 수 있어요"),
                            new Message("당신의 페이스로", "서두를 필요 없어요. 준비됐을 때 다시 만나요")
                    ),
                    LanguageCode.EN, List.of(
                            new Message("You can always come back", "See you again when you're ready. We'll wait"),
                            new Message("It's okay to take a break", "Learning is always here. Come back when it's comfortable"),
                            new Message("Final goodbye", "We'll be here whenever you want to come back"),
                            new Message("The door is open", "You can restart anytime when you're ready"),
                            new Message("At your own pace", "No need to rush. See you when you're ready")
                    ),
                    LanguageCode.JA, List.of(
                            new Message("いつでも戻ってこれます", "準備ができたらまた会いましょう。待っています"),
                            new Message("休んでもいいです", "学習はいつもここにあります。楽な時に戻ってきてください"),
                            new Message("最後の挨拶", "戻りたくなったらいつでもここにいます"),
                            new Message("ドアは開いています", "準備ができたらいつでも再スタートできます"),
                            new Message("自分のペースで", "急ぐ必要はありません。準備ができたらまた会いましょう")
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
