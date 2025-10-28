package com.linglevel.api.streak.entity;

import com.linglevel.api.i18n.LanguageCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Random;

/**
 * 영감을 주는 명언 정의
 * 마일스톤이 아닌 일반 날짜에 랜덤으로 보여줄 명언을 관리합니다.
 */
@Getter
@RequiredArgsConstructor
public enum InspirationQuote {
    QUOTE_1(
            "The secret of getting ahead is getting started.",
            Map.of(
                    LanguageCode.KO, "앞서가는 비결은 시작하는 것입니다.",
                    LanguageCode.EN, "The secret of getting ahead is getting started.",
                    LanguageCode.JA, "前進する秘訣は、始めることです。"
            )
    ),
    QUOTE_2(
            "Success is the sum of small efforts repeated day in and day out.",
            Map.of(
                    LanguageCode.KO, "성공은 날마다 반복되는 작은 노력의 합입니다.",
                    LanguageCode.EN, "Success is the sum of small efforts repeated day in and day out.",
                    LanguageCode.JA, "成功とは、日々繰り返される小さな努力の総和です。"
            )
    ),
    QUOTE_3(
            "The only way to do great work is to love what you do.",
            Map.of(
                    LanguageCode.KO, "위대한 일을 하는 유일한 방법은 당신이 하는 일을 사랑하는 것입니다.",
                    LanguageCode.EN, "The only way to do great work is to love what you do.",
                    LanguageCode.JA, "素晴らしい仕事をする唯一の方法は、自分のしていることを愛することです。"
            )
    ),
    QUOTE_4(
            "Don't watch the clock; do what it does. Keep going.",
            Map.of(
                    LanguageCode.KO, "시계를 보지 말고, 시계가 하는 것을 하세요. 계속 가세요.",
                    LanguageCode.EN, "Don't watch the clock; do what it does. Keep going.",
                    LanguageCode.JA, "時計を見るのではなく、時計がすることをしなさい。進み続けなさい。"
            )
    ),
    QUOTE_5(
            "Learning never exhausts the mind.",
            Map.of(
                    LanguageCode.KO, "배움은 결코 마음을 지치게 하지 않습니다.",
                    LanguageCode.EN, "Learning never exhausts the mind.",
                    LanguageCode.JA, "学びは決して心を疲れさせません。"
            )
    ),
    QUOTE_6(
            "The beautiful thing about learning is that no one can take it away from you.",
            Map.of(
                    LanguageCode.KO, "배움의 아름다운 점은 아무도 그것을 빼앗을 수 없다는 것입니다.",
                    LanguageCode.EN, "The beautiful thing about learning is that no one can take it away from you.",
                    LanguageCode.JA, "学びの美しい点は、誰もそれをあなたから奪うことができないということです。"
            )
    ),
    QUOTE_7(
            "Education is the most powerful weapon which you can use to change the world.",
            Map.of(
                    LanguageCode.KO, "교육은 세상을 바꾸는 데 사용할 수 있는 가장 강력한 무기입니다.",
                    LanguageCode.EN, "Education is the most powerful weapon which you can use to change the world.",
                    LanguageCode.JA, "教育は、世界を変えるために使える最も強力な武器です。"
            )
    ),
    QUOTE_8(
            "The expert in anything was once a beginner.",
            Map.of(
                    LanguageCode.KO, "모든 분야의 전문가도 한때는 초보자였습니다.",
                    LanguageCode.EN, "The expert in anything was once a beginner.",
                    LanguageCode.JA, "どんな分野の専門家も、かつては初心者でした。"
            )
    ),
    QUOTE_9(
            "Consistency is what transforms average into excellence.",
            Map.of(
                    LanguageCode.KO, "일관성이야말로 평범함을 탁월함으로 변화시키는 것입니다.",
                    LanguageCode.EN, "Consistency is what transforms average into excellence.",
                    LanguageCode.JA, "一貫性こそが、平凡さを卓越性に変えるものです。"
            )
    ),
    QUOTE_10(
            "A journey of a thousand miles begins with a single step.",
            Map.of(
                    LanguageCode.KO, "천 리 길도 한 걸음부터 시작됩니다.",
                    LanguageCode.EN, "A journey of a thousand miles begins with a single step.",
                    LanguageCode.JA, "千里の道も一歩から。"
            )
    ),
    QUOTE_11(
            "You are never too old to set another goal or to dream a new dream.",
            Map.of(
                    LanguageCode.KO, "새로운 목표를 세우거나 새로운 꿈을 꾸기에 너무 늦은 나이는 없습니다.",
                    LanguageCode.EN, "You are never too old to set another goal or to dream a new dream.",
                    LanguageCode.JA, "新しい目標を立てたり、新しい夢を見るのに遅すぎることはありません。"
            )
    ),
    QUOTE_12(
            "Believe you can and you're halfway there.",
            Map.of(
                    LanguageCode.KO, "할 수 있다고 믿으면 이미 반은 이룬 것입니다.",
                    LanguageCode.EN, "Believe you can and you're halfway there.",
                    LanguageCode.JA, "できると信じれば、半分は達成したも同然です。"
            )
    ),
    QUOTE_13(
            "The only impossible journey is the one you never begin.",
            Map.of(
                    LanguageCode.KO, "불가능한 여정은 시작하지 않은 여정뿐입니다.",
                    LanguageCode.EN, "The only impossible journey is the one you never begin.",
                    LanguageCode.JA, "唯一不可能な旅は、始めない旅だけです。"
            )
    ),
    QUOTE_14(
            "Your limitation—it's only your imagination.",
            Map.of(
                    LanguageCode.KO, "당신의 한계는 오직 당신의 상상 속에만 있습니다.",
                    LanguageCode.EN, "Your limitation—it's only your imagination.",
                    LanguageCode.JA, "あなたの限界は、あなたの想像の中にのみ存在します。"
            )
    ),
    QUOTE_15(
            "Small daily improvements over time lead to stunning results.",
            Map.of(
                    LanguageCode.KO, "시간이 지나면서 매일의 작은 발전이 놀라운 결과를 가져옵니다.",
                    LanguageCode.EN, "Small daily improvements over time lead to stunning results.",
                    LanguageCode.JA, "日々の小さな改善が、時間とともに驚くべき結果をもたらします。"
            )
    );

    private final String original;
    private final Map<LanguageCode, String> translations;

    private static final Random RANDOM = new Random();

    /**
     * 랜덤으로 명언을 선택합니다.
     *
     * @return 랜덤하게 선택된 명언
     */
    public static InspirationQuote random() {
        InspirationQuote[] quotes = values();
        return quotes[RANDOM.nextInt(quotes.length)];
    }

    /**
     * 원문 명언을 가져옵니다.
     *
     * @return 명언 원문 (영어)
     */
    public String getOriginal() {
        return original;
    }

    /**
     * 지정된 언어로 번역된 명언을 가져옵니다.
     *
     * @param languageCode 언어 코드
     * @return 해당 언어의 번역, 없으면 영어 원문
     */
    public String getTranslation(LanguageCode languageCode) {
        return translations.getOrDefault(languageCode, original);
    }
}
