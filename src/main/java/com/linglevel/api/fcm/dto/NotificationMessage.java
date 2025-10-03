package com.linglevel.api.fcm.dto;

import com.linglevel.api.i18n.CountryCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum NotificationMessage {
    // Custom Content 메시지
    CONTENT_COMPLETED(
        Map.of(
            CountryCode.KR, new Message("콘텐츠 준비 완료", "'%s' 처리가 완료되었습니다."),
            CountryCode.US, new Message("Content Ready", "'%s' has been successfully processed."),
            CountryCode.JP, new Message("コンテンツ準備完了", "'%s'の処理が完了しました。")
        )
    ),
    CONTENT_FAILED(
        Map.of(
            CountryCode.KR, new Message("콘텐츠 처리 실패", "처리 중 오류가 발생했습니다."),
            CountryCode.US, new Message("Content Processing Failed", "An error occurred while processing."),
            CountryCode.JP, new Message("コンテンツ処理失敗", "処理中にエラーが発生しました。")
        )
    );

    private final Map<CountryCode, Message> translations;

    /**
     * 국가 코드에 해당하는 제목을 반환합니다.
     * 해당 국가의 번역이 없으면 기본값(US)을 반환합니다.
     */
    public String getTitle(CountryCode countryCode) {
        CountryCode code = countryCode != null ? countryCode : CountryCode.US;
        return translations.getOrDefault(code, translations.get(CountryCode.US)).title;
    }

    /**
     * 국가 코드에 해당하는 본문을 반환합니다.
     * 파라미터를 사용하여 메시지 템플릿을 포맷팅합니다.
     * 해당 국가의 번역이 없으면 기본값(US)을 반환합니다.
     */
    public String getBody(CountryCode countryCode, Object... params) {
        CountryCode code = countryCode != null ? countryCode : CountryCode.US;
        String template = translations.getOrDefault(code, translations.get(CountryCode.US)).body;
        return params.length > 0 ? String.format(template, params) : template;
    }

    @Getter
    @AllArgsConstructor
    public static class Message {
        private final String title;
        private final String body;
    }
}
