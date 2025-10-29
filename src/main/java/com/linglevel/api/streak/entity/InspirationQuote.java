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
                    LanguageCode.KO, "앞서 나가는 비결은 바로 시작하는 것이다.",
                    LanguageCode.EN, "The secret of getting ahead is getting started.",
                    LanguageCode.JA, "先んずる秘訣は、まず始めることである。"
            )
    ),
    QUOTE_2(
            "A journey of a thousand miles begins with a single step.",
            Map.of(
                    LanguageCode.KO, "천 리 길도 한 걸음부터 시작된다.",
                    LanguageCode.EN, "A journey of a thousand miles begins with a single step.",
                    LanguageCode.JA, "千里の道も一歩から。"
            )
    ),
    QUOTE_3(
            "The beginning is always the hardest.",
            Map.of(
                    LanguageCode.KO, "시작이 언제나 가장 어렵다.",
                    LanguageCode.EN, "The beginning is always the hardest.",
                    LanguageCode.JA, "何事も初めが一番難しい。"
            )
    ),
    QUOTE_4(
            "Don't be afraid to give up the good to go for the great.",
            Map.of(
                    LanguageCode.KO, "위대함을 위해 좋은 것을 포기하는 것을 두려워하지 마라.",
                    LanguageCode.EN, "Don't be afraid to give up the good to go for the great.",
                    LanguageCode.JA, "偉大さのために、良いものを諦めることを恐れるな。"
            )
    ),
    QUOTE_5(
            "Every accomplishment starts with the decision to try.",
            Map.of(
                    LanguageCode.KO, "모든 성취는 '해보기로' 결심하는 것에서 시작된다.",
                    LanguageCode.EN, "Every accomplishment starts with the decision to try.",
                    LanguageCode.JA, "すべての達成は、「やってみよう」と決心することから始まる。"
            )
    ),
    QUOTE_6(
            "The expert in anything was once a beginner.",
            Map.of(
                    LanguageCode.KO, "어떤 분야의 전문가든 처음에는 초보자였다.",
                    LanguageCode.EN, "The expert in anything was once a beginner.",
                    LanguageCode.JA, "どんな分野の専門家も、かつては初心者だった。"
            )
    ),
    QUOTE_7(
            "It does not matter how slowly you go as long as you do not stop.",
            Map.of(
                    LanguageCode.KO, "멈추지만 않는다면 얼마나 천천히 가는지는 중요하지 않다.",
                    LanguageCode.EN, "It does not matter how slowly you go as long as you do not stop.",
                    LanguageCode.JA, "止まらない限り、どれだけゆっくり進んでも問題ない。"
            )
    ),
    QUOTE_8(
            "You don’t have to be great to start, but you have to start to be great.",
            Map.of(
                    LanguageCode.KO, "시작하기 위해 위대할 필요는 없지만, 위대해지기 위해선 시작해야 한다.",
                    LanguageCode.EN, "You don’t have to be great to start, but you have to start to be great.",
                    LanguageCode.JA, "始めるために偉大である必要はないが、偉大になるためには始めなければならない。"
            )
    ),
    QUOTE_9(
            "Do not wait; the time will never be 'just right.' Start where you stand.",
            Map.of(
                    LanguageCode.KO, "기다리지 마라. '딱 맞는' 때는 결코 오지 않는다. 당신이 서 있는 곳에서 시작하라.",
                    LanguageCode.EN, "Do not wait; the time will never be 'just right.' Start where you stand.",
                    LanguageCode.JA, "待っていてはだめだ。「ちょうど良い」時など決して来ない。今いる場所から始めなさい。"
            )
    ),
    QUOTE_10(
            "The first step is you have to say that you can.",
            Map.of(
                    LanguageCode.KO, "첫 번째 단계는 '할 수 있다'고 말하는 것이다.",
                    LanguageCode.EN, "The first step is you have to say that you can.",
                    LanguageCode.JA, "最初のステップは、「できる」と言うことだ。"
            )
    ),
    QUOTE_11(
            "What is not started today is never finished tomorrow.",
            Map.of(
                    LanguageCode.KO, "오늘 시작하지 않은 일은 내일 결코 끝마칠 수 없다.",
                    LanguageCode.EN, "What is not started today is never finished tomorrow.",
                    LanguageCode.JA, "今日始めなかったことは、明日決して終わらない。"
            )
    ),
    QUOTE_12(
            "All great achievements require time.",
            Map.of(
                    LanguageCode.KO, "모든 위대한 성취는 시간을 필요로 한다.",
                    LanguageCode.EN, "All great achievements require time.",
                    LanguageCode.JA, "すべての偉大な達成には時間が必要だ。"
            )
    ),
    QUOTE_13(
            "The journey is the reward.",
            Map.of(
                    LanguageCode.KO, "여정 그 자체가 보상이다.",
                    LanguageCode.EN, "The journey is the reward.",
                    LanguageCode.JA, "旅路そのものが報酬である。"
            )
    ),
    QUOTE_14(
            "Start by doing what's necessary; then do what's possible; and suddenly you are doing the impossible.",
            Map.of(
                    LanguageCode.KO, "필요한 것부터 시작하라. 그다음 가능한 것을 하라. 그러면 불가능한 것을 하고 있는 자신을 발견하게 될 것이다.",
                    LanguageCode.EN, "Start by doing what's necessary; then do what's possible; and suddenly you are doing the impossible.",
                    LanguageCode.JA, "まず必要なことをしなさい。次に可能なことをしなさい。そうすればいつの間にか、不可能なことをしている自分に気づくだろう。"
            )
    ),
    QUOTE_15(
            "A year from now you may wish you had started today.",
            Map.of(
                    LanguageCode.KO, "1년 뒤, 당신은 오늘 시작했기를 바랄지도 모른다.",
                    LanguageCode.EN, "A year from now you may wish you had started today.",
                    LanguageCode.JA, "1年後、あなたは今日始めていればよかったと願うかもしれない。"
            )
    ),
    QUOTE_16(
            "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            Map.of(
                    LanguageCode.KO, "성공은 끝이 아니며, 실패는 치명적이지 않다. 중요한 것은 계속 나아갈 용기다.",
                    LanguageCode.EN, "Success is not final, failure is not fatal: it is the courage to continue that counts.",
                    LanguageCode.JA, "成功は終わりではなく、失敗は致命的ではない。重要なのは続ける勇気だ。"
            )
    ),
    QUOTE_17(
            "It's not whether you get knocked down, it's whether you get up.",
            Map.of(
                    LanguageCode.KO, "쓰러지는 것이 중요한 게 아니라, 다시 일어서는 것이 중요하다.",
                    LanguageCode.EN, "It's not whether you get knocked down, it's whether you get up.",
                    LanguageCode.JA, "打ちのめされたかどうかではなく、立ち上がるかどうかが問題だ。"
            )
    ),
    QUOTE_18(
            "Fall seven times and stand up eight.",
            Map.of(
                    LanguageCode.KO, "일곱 번 넘어지면 여덟 번 일어나라.",
                    LanguageCode.EN, "Fall seven times and stand up eight.",
                    LanguageCode.JA, "七転び八起き。"
            )
    ),
    QUOTE_19(
            "Our greatest weakness lies in giving up. The most certain way to succeed is always to try just one more time.",
            Map.of(
                    LanguageCode.KO, "우리의 가장 큰 약점은 포기하는 것이다. 성공으로 가는 가장 확실한 방법은 언제나 한 번 더 시도해 보는 것이다.",
                    LanguageCode.EN, "Our greatest weakness lies in giving up. The most certain way to succeed is always to try just one more time.",
                    LanguageCode.JA, "我々の最大の弱点は諦めることにある。成功への最も確実な道は、常にもう一度だけ試してみることだ。"
            )
    ),
    QUOTE_20(
            "Perseverance is failing 19 times and succeeding the 20th.",
            Map.of(
                    LanguageCode.KO, "인내란 19번 실패하고 20번째에 성공하는 것이다.",
                    LanguageCode.EN, "Perseverance is failing 19 times and succeeding the 20th.",
                    LanguageCode.JA, "忍耐とは、19回失敗し、20回目に成功することである。"
            )
    ),
    QUOTE_21(
            "I am a slow walker, but I never walk back.",
            Map.of(
                    LanguageCode.KO, "나는 천천히 걷지만, 결코 뒷걸음질 치지 않는다.",
                    LanguageCode.EN, "I am a slow walker, but I never walk back.",
                    LanguageCode.JA, "私は歩みが遅いが、決して後戻りはしない。"
            )
    ),
    QUOTE_22(
            "Smooth seas do not make skillful sailors.",
            Map.of(
                    LanguageCode.KO, "잔잔한 바다는 노련한 뱃사공을 만들지 못한다.",
                    LanguageCode.EN, "Smooth seas do not make skillful sailors.",
                    LanguageCode.JA, "穏やかな海は、熟練した船乗りを育てない。"
            )
    ),
    QUOTE_23(
            "Effort is the great equalizer.",
            Map.of(
                    LanguageCode.KO, "노력은 위대한 평형 장치이다.",
                    LanguageCode.EN, "Effort is the great equalizer.",
                    LanguageCode.JA, "努力は偉大なる平等化装置である。"
            )
    ),
    QUOTE_24(
            "I have not failed. I've just found 10,000 ways that won't work.",
            Map.of(
                    LanguageCode.KO, "나는 실패하지 않았다. 단지 작동하지 않는 1만 가지 방법을 찾았을 뿐이다.",
                    LanguageCode.EN, "I have not failed. I've just found 10,000 ways that won't work.",
                    LanguageCode.JA, "私は失敗したことがない。ただ、1万通りのうまくいかない方法を見つけただけだ。"
            )
    ),
    QUOTE_25(
            "When you feel like quitting, think about why you started.",
            Map.of(
                    LanguageCode.KO, "포기하고 싶어질 때, 당신이 왜 시작했는지를 생각하라.",
                    LanguageCode.EN, "When you feel like quitting, think about why you started.",
                    LanguageCode.JA, "やめたくなった時、なぜ始めたのかを思い出せ。"
            )
    ),
    QUOTE_26(
            "We may encounter many defeats but we must not be defeated.",
            Map.of(
                    LanguageCode.KO, "우리는 수많은 패배를 겪을지라도, 결코 패배해서는 안 된다.",
                    LanguageCode.EN, "We may encounter many defeats but we must not be defeated.",
                    LanguageCode.JA, "我々は多くの敗北に出会うかもしれないが、決して打ち負かされてはならない。"
            )
    ),
    QUOTE_27(
            "The harder the conflict, the more glorious the triumph.",
            Map.of(
                    LanguageCode.KO, "고난이 클수록, 승리는 더욱 영광스럽다.",
                    LanguageCode.EN, "The harder the conflict, the more glorious the triumph.",
                    LanguageCode.JA, "困難が大きければ大きいほど、勝利はより栄光に満ちたものになる。"
            )
    ),
    QUOTE_28(
            "A diamond is a chunk of coal that did well under pressure.",
            Map.of(
                    LanguageCode.KO, "다이아몬드는 압력을 잘 견뎌낸 석탄 덩어리다.",
                    LanguageCode.EN, "A diamond is a chunk of coal that did well under pressure.",
                    LanguageCode.JA, "ダイヤモンドとは、プレッシャーをうまく乗り越えた石炭の塊である。"
            )
    ),
    QUOTE_29(
            "It is hard to fail, but it is worse never to have tried to succeed.",
            Map.of(
                    LanguageCode.KO, "실패하는 것은 힘들지만, 성공하려 시도조차 해보지 않는 것은 더 나쁘다.",
                    LanguageCode.EN, "It is hard to fail, but it is worse never to have tried to succeed.",
                    LanguageCode.JA, "失敗することは辛いが、成功しようと試みないことはもっと悪い。"
            )
    ),
    QUOTE_30(
            "Patience and perseverance have a magical effect before which difficulties disappear and obstacles vanish.",
            Map.of(
                    LanguageCode.KO, "인내와 끈기에는 어려움이 사라지고 장애물이 없어지는 마법 같은 효과가 있다.",
                    LanguageCode.EN, "Patience and perseverance have a magical effect before which difficulties disappear and obstacles vanish.",
                    LanguageCode.JA, "忍耐と不屈の精神には、困難が消え去り、障害がなくなるという魔法のような効果がある。"
            )
    ),
    QUOTE_31(
            "The more that you read, the more things you will know. The more that you learn, the more places you'll go.",
            Map.of(
                    LanguageCode.KO, "더 많이 읽을수록, 더 많은 것을 알게 될 것이다. 더 많이 배울수록, 더 많은 곳에 가게 될 것이다.",
                    LanguageCode.EN, "The more that you read, the more things you will know. The more that you learn, the more places you'll go.",
                    LanguageCode.JA, "読めば読むほど、多くのことを知るようになる。学べば学ぶほど、多くの場所へ行けるようになる。"
            )
    ),
    QUOTE_32(
            "A reader lives a thousand lives before he dies . . . The man who never reads lives only one.",
            Map.of(
                    LanguageCode.KO, "책을 읽는 사람은 죽기 전에 천 번의 삶을 산다. 책을 읽지 않는 사람은 단 한 번의 삶을 살 뿐이다.",
                    LanguageCode.EN, "A reader lives a thousand lives before he dies . . . The man who never reads lives only one.",
                    LanguageCode.JA, "本を読む者は、死ぬ前に千の人生を生きる。本を読まぬ者は、たった一度の人生しか生きない。"
            )
    ),
    QUOTE_33(
            "Live as if you were to die tomorrow. Learn as if you were to live forever.",
            Map.of(
                    LanguageCode.KO, "내일 죽을 것처럼 살고, 영원히 살 것처럼 배워라.",
                    LanguageCode.EN, "Live as if you were to die tomorrow. Learn as if you were to live forever.",
                    LanguageCode.JA, "明日死ぬかのように生きよ。永遠に生きるかのように学べ。"
            )
    ),
    QUOTE_34(
            "Learning is a treasure that will follow its owner everywhere.",
            Map.of(
                    LanguageCode.KO, "배움은 그 주인을 어디든 따라다니는 보물이다.",
                    LanguageCode.EN, "Learning is a treasure that will follow its owner everywhere.",
                    LanguageCode.JA, "学びは、持ち主がどこへ行こうともついてくる宝である。"
            )
    ),
    QUOTE_35(
            "An investment in knowledge pays the best interest.",
            Map.of(
                    LanguageCode.KO, "지식에 대한 투자는 최고의 이자를 낳는다.",
                    LanguageCode.EN, "An investment in knowledge pays the best interest.",
                    LanguageCode.JA, "知識への投資は、常に最高のリターンをもたらす。"
            )
    ),
    QUOTE_36(
            "Today a reader, tomorrow a leader.",
            Map.of(
                    LanguageCode.KO, "오늘 책을 읽는 사람이 내일의 리더가 된다.",
                    LanguageCode.EN, "Today a reader, tomorrow a leader.",
                    LanguageCode.JA, "今日の読書家は、明日の指導者となる。"
            )
    ),
    QUOTE_37(
            "Reading is to the mind what exercise is to the body.",
            Map.of(
                    LanguageCode.KO, "독서는 정신에게 운동이 육체에게 주는 것과 같다.",
                    LanguageCode.EN, "Reading is to the mind what exercise is to the body.",
                    LanguageCode.JA, "読書が精神に与える影響は、運動が身体に与える影響と同じである。"
            )
    ),
    QUOTE_38(
            "To learn a language is to have one more window from which to look at the world.",
            Map.of(
                    LanguageCode.KO, "언어를 배운다는 것은 세상을 바라보는 창문을 하나 더 갖는 것이다.",
                    LanguageCode.EN, "To learn a language is to have one more window from which to look at the world.",
                    LanguageCode.JA, "言語を学ぶことは、世界を見る窓をもう一つ持つことだ。"
            )
    ),
    QUOTE_39(
            "Books are a uniquely portable magic.",
            Map.of(
                    LanguageCode.KO, "책은 독특하게 휴대 가능한 마법이다.",
                    LanguageCode.EN, "Books are a uniquely portable magic.",
                    LanguageCode.JA, "本とは、他に類を見ない、持ち運び可能な魔法である。"
            )
    ),
    QUOTE_40(
            "The man who does not read has no advantage over the man who cannot read.",
            Map.of(
                    LanguageCode.KO, "책을 읽지 않는 사람은 글을 읽지 못하는 사람보다 나을 것이 없다.",
                    LanguageCode.EN, "The man who does not read has no advantage over the man who cannot read.",
                    LanguageCode.JA, "本を読まない人は、字が読めない人よりも優れている点はない。"
            )
    ),
    QUOTE_41(
            "Education is the passport to the future, for tomorrow belongs to those who prepare for it today.",
            Map.of(
                    LanguageCode.KO, "교육은 미래로 가는 여권이다. 내일은 오늘 준비하는 자의 것이기 때문이다.",
                    LanguageCode.EN, "Education is the passport to the future, for tomorrow belongs to those who prepare for it today.",
                    LanguageCode.JA, "教育とは未来へのパスポートである。明日は今日準備した者のものであるからだ。"
            )
    ),
    QUOTE_42(
            "Develop a passion for learning. If you do, you will never cease to grow.",
            Map.of(
                    LanguageCode.KO, "배움에 대한 열정을 키워라. 그렇게 한다면, 당신은 결코 성장을 멈추지 않을 것이다.",
                    LanguageCode.EN, "Develop a passion for learning. If you do, you will never cease to grow.",
                    LanguageCode.JA, "学ぶことへの情熱を育てなさい。そうすれば、あなたは決して成長を止めないだろう。"
            )
    ),
    QUOTE_43(
            "Language is the road map of a culture. It tells you where its people come from and where they are going.",
            Map.of(
                    LanguageCode.KO, "언어는 한 문화의 로드맵이다. 그것은 그 민족이 어디에서 왔고 어디로 가고 있는지를 말해준다.",
                    LanguageCode.EN, "Language is the road map of a culture. It tells you where its people come from and where they are going.",
                    LanguageCode.JA, "言語は文化のロードマップである。それは、その人々がどこから来て、どこへ行こうとしているのかを教えてくれる。"
            )
    ),
    QUOTE_44(
            "Reading is a conversation. All books talk. But a good book listens as well.",
            Map.of(
                    LanguageCode.KO, "독서는 대화다. 모든 책은 말을 걸지만, 좋은 책은 귀 기울여 듣기도 한다.",
                    LanguageCode.EN, "Reading is a conversation. All books talk. But a good book listens as well.",
                    LanguageCode.JA, "読書とは会話である。全ての本は語りかける。しかし、良い本は傾聴もしてくれる。"
            )
    ),
    QUOTE_45(
            "Reading brings us unknown friends.",
            Map.of(
                    LanguageCode.KO, "독서는 우리에게 미지의 친구들을 데려다준다.",
                    LanguageCode.EN, "Reading brings us unknown friends.",
                    LanguageCode.JA, "読書は、我々に見知らぬ友人をもたらしてくれる。"
            )
    ),
    QUOTE_46(
            "Believe you can and you're halfway there.",
            Map.of(
                    LanguageCode.KO, "할 수 있다고 믿으면, 당신은 이미 절반은 온 것이다.",
                    LanguageCode.EN, "Believe you can and you're halfway there.",
                    LanguageCode.JA, "できると信じれば、もう半分は達成している。"
            )
    ),
    QUOTE_47(
            "Success is the sum of small efforts, repeated day in and day out.",
            Map.of(
                    LanguageCode.KO, "성공은 매일 반복되는 작은 노력들의 합이다.",
                    LanguageCode.EN, "Success is the sum of small efforts, repeated day in and day out.",
                    LanguageCode.JA, "成功とは、日々繰り返される小さな努力の積み重ねである。"
            )
    ),
    QUOTE_48(
            "The only way to do great work is to love what you do.",
            Map.of(
                    LanguageCode.KO, "위대한 일을 하는 유일한 방법은 당신이 하는 일을 사랑하는 것이다.",
                    LanguageCode.EN, "The only way to do great work is to love what you do.",
                    LanguageCode.JA, "素晴らしい仕事をする唯一の方法は、自分のしていることを愛することだ。"
            )
    ),
    QUOTE_49(
            "You are never too old to set another goal or to dream a new dream.",
            Map.of(
                    LanguageCode.KO, "또 다른 목표를 세우거나 새로운 꿈을 꾸기에 너무 늦은 나이란 없다.",
                    LanguageCode.EN, "You are never too old to set another goal or to dream a new dream.",
                    LanguageCode.JA, "新しい目標を立てたり、新しい夢を見るのに遅すぎることはない。"
            )
    ),
    QUOTE_50(
            "Our dreams can come true if we have the courage to pursue them.",
            Map.of(
                    LanguageCode.KO, "꿈을 추구할 용기만 있다면, 우리의 모든 꿈은 이루어질 수 있다.",
                    LanguageCode.EN, "Our dreams can come true if we have the courage to pursue them.",
                    LanguageCode.JA, "追い求める勇気さえあれば、夢は必ず叶う。"
            )
    ),
    QUOTE_51(
            "Don't watch the clock; do what it does. Keep going.",
            Map.of(
                    LanguageCode.KO, "시계를 보지 마라. 시계가 하는 것처럼 계속 나아가라.",
                    LanguageCode.EN, "Don't watch the clock; do what it does. Keep going.",
                    LanguageCode.JA, "時計を見るな。時計がするように、進み続けろ。"
            )
    ),
    QUOTE_52(
            "The future belongs to those who believe in the beauty of their dreams.",
            Map.of(
                    LanguageCode.KO, "미래는 자기 꿈의 아름다움을 믿는 사람들의 것이다.",
                    LanguageCode.EN, "The future belongs to those who believe in the beauty of their dreams.",
                    LanguageCode.JA, "未来とは、自分の夢の美しさを信じる者のものである。"
            )
    ),
    QUOTE_53(
            "You miss 100% of the shots you don’t take.",
            Map.of(
                    LanguageCode.KO, "시도하지 않은 슛은 100% 빗나간다.",
                    LanguageCode.EN, "You miss 100% of the shots you don’t take.",
                    LanguageCode.JA, "打たなかったシュートは、100％外れる。"
            )
    ),
    QUOTE_54(
            "It always seems impossible until it's done.",
            Map.of(
                    LanguageCode.KO, "어떤 일이든 그것이 끝나기 전까지는 항상 불가능해 보인다.",
                    LanguageCode.EN, "It always seems impossible until it's done.",
                    LanguageCode.JA, "何事も、成し遂げられるまでは不可能に見える。"
            )
    ),
    QUOTE_55(
            "If you can dream it, you can do it.",
            Map.of(
                    LanguageCode.KO, "당신이 꿈꿀 수 있다면, 당신은 그것을 해낼 수 있다.",
                    LanguageCode.EN, "If you can dream it, you can do it.",
                    LanguageCode.JA, "夢見ることができれば、それは実現できる。"
            )
    ),
    QUOTE_56(
            "What you get by achieving your goals is not as important as what you become by achieving your goals.",
            Map.of(
                    LanguageCode.KO, "목표를 달성함으로써 얻는 것보다, 목표를 달성함으로써 당신이 어떤 사람이 되는지가 더 중요하다.",
                    LanguageCode.EN, "What you get by achieving your goals is not as important as what you become by achieving your goals.",
                    LanguageCode.JA, "目標を達成して得られるものよりも、目標を達成する過程で自分がどう成長するかが重要だ。"
            )
    ),
    QUOTE_57(
            "Act as if what you do makes a difference. It does.",
            Map.of(
                    LanguageCode.KO, "당신의 행동이 변화를 만든다고 생각하며 행동하라. 실제로 그러니까.",
                    LanguageCode.EN, "Act as if what you do makes a difference. It does.",
                    LanguageCode.JA, "自分の行動が変化をもたらすと信じて行動しなさい。実際、その通りなのだから。"
            )
    ),
    QUOTE_58(
            "Don't let what you cannot do interfere with what you can do.",
            Map.of(
                    LanguageCode.KO, "당신이 할 수 없는 일이 당신이 할 수 있는 일을 방해하게 두지 마라.",
                    LanguageCode.EN, "Don't let what you cannot do interfere with what you can do.",
                    LanguageCode.JA, "できないことに、できることの邪魔をさせてはいけない。"
            )
    ),
    QUOTE_59(
            "He who has a why to live can bear almost any how.",
            Map.of(
                    LanguageCode.KO, "살아야 할 '이유'를 아는 사람은 거의 모든 '어떻게'를 견뎌낼 수 있다.",
                    LanguageCode.EN, "He who has a why to live can bear almost any how.",
                    LanguageCode.JA, "生きる「なぜ」を持つ者は、ほとんどあらゆる「どのように」にも耐えられる。"
            )
    ),
    QUOTE_60(
            "Your time is limited, so don’t waste it living someone else’s life.",
            Map.of(
                    LanguageCode.KO, "당신의 시간은 한정되어 있다. 그러니 다른 사람의 삶을 사느라 시간을 낭비하지 마라.",
                    LanguageCode.EN, "Your time is limited, so don’t waste it living someone else’s life.",
                    LanguageCode.JA, "あなたの時間は限られている。だから、他人の人生を生きて無駄にしてはいけない。"
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
