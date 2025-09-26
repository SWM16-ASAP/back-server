package com.linglevel.api.content.common.service;

import com.linglevel.api.content.common.ContentType;

public interface ContentInfoProvider {

    ContentType getSupportedType();

    /**
     * 콘텐츠 ID로 콘텐츠 정보를 조회
     * @param contentId 콘텐츠 ID
     * @return 콘텐츠 정보 (존재하지 않으면 빈 ContentInfo)
     */
    ContentInfo getContentInfo(String contentId);
}