package com.linglevel.api.fcm.dto;

import com.linglevel.api.i18n.CountryCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationMessageTest {

    @Test
    void testContentCompletedMessages() {
        // KR
        String krTitle = NotificationMessage.CONTENT_COMPLETED.getTitle(CountryCode.KR);
        String krBody = NotificationMessage.CONTENT_COMPLETED.getBody(CountryCode.KR, "테스트 콘텐츠");
        assertEquals("콘텐츠 준비 완료", krTitle);
        assertEquals("'테스트 콘텐츠' 처리가 완료되었습니다.", krBody);

        // US
        String usTitle = NotificationMessage.CONTENT_COMPLETED.getTitle(CountryCode.US);
        String usBody = NotificationMessage.CONTENT_COMPLETED.getBody(CountryCode.US, "Test Content");
        assertEquals("Content Ready", usTitle);
        assertEquals("'Test Content' has been successfully processed.", usBody);

        // JP
        String jpTitle = NotificationMessage.CONTENT_COMPLETED.getTitle(CountryCode.JP);
        String jpBody = NotificationMessage.CONTENT_COMPLETED.getBody(CountryCode.JP, "テストコンテンツ");
        assertEquals("コンテンツ準備完了", jpTitle);
        assertEquals("'テストコンテンツ'の処理が完了しました。", jpBody);
    }

    @Test
    void testContentFailedMessages() {
        // KR
        assertEquals("콘텐츠 처리 실패", NotificationMessage.CONTENT_FAILED.getTitle(CountryCode.KR));
        assertEquals("처리 중 오류가 발생했습니다.", NotificationMessage.CONTENT_FAILED.getBody(CountryCode.KR));

        // US
        assertEquals("Content Processing Failed", NotificationMessage.CONTENT_FAILED.getTitle(CountryCode.US));
        assertEquals("An error occurred while processing.", NotificationMessage.CONTENT_FAILED.getBody(CountryCode.US));

        // JP
        assertEquals("コンテンツ処理失敗", NotificationMessage.CONTENT_FAILED.getTitle(CountryCode.JP));
        assertEquals("処理中にエラーが発生しました。", NotificationMessage.CONTENT_FAILED.getBody(CountryCode.JP));
    }

    @Test
    void testNullCountryCodeDefaultsToUS() {
        String title = NotificationMessage.CONTENT_COMPLETED.getTitle(null);
        String body = NotificationMessage.CONTENT_COMPLETED.getBody(null, "Test");

        assertEquals("Content Ready", title);
        assertEquals("'Test' has been successfully processed.", body);
    }
}
