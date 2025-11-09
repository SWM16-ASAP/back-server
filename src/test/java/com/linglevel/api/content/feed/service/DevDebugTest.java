package com.linglevel.api.content.feed.service;

import com.linglevel.api.content.feed.entity.FeedSource;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jdom2.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;

@DisplayName("Dev 환경 디버깅용 테스트")
class DevDebugTest {

    @Test
    @DisplayName("extractThumbnailUrl 로직 상세 디버깅")
    void debugExtractThumbnailUrl() throws Exception {
        String rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCsXVk37bltHxD1rDPwtNM8Q";

        System.out.println("===========================================");
        System.out.println("=== extractThumbnailUrl 디버깅 ===");
        System.out.println("===========================================\n");

        // RSS 파싱
        SyndFeedInput input = new SyndFeedInput();
        input.setAllowDoctypes(true);
        SyndFeed rssFeed = input.build(new XmlReader(new URL(rssUrl)));

        SyndEntry entry = rssFeed.getEntries().get(0);

        System.out.println("Entry: " + entry.getTitle());
        System.out.println("URL: " + entry.getLink());
        System.out.println();

        FeedSource feedSource = FeedSource.builder()
            .url(rssUrl)
            .coverImageDsl("D'meta[property=\"og:image\"]'@'content'")
            .build();

        // extractThumbnailUrl 로직 재현 (상세 로그 포함)
        String result = extractThumbnailUrlWithDebugLogs(entry, entry.getLink(), feedSource);

        System.out.println("\n===========================================");
        System.out.println("최종 결과: " + result);
        System.out.println("===========================================");
    }

    private String extractThumbnailUrlWithDebugLogs(SyndEntry entry, String articleUrl, FeedSource feedSource) {
        // 1. Enclosures
        System.out.println("[STEP 1] Enclosures 확인");
        if (entry.getEnclosures() != null && !entry.getEnclosures().isEmpty()) {
            System.out.println("  Enclosures 개수: " + entry.getEnclosures().size());
            String rssThumbnail = entry.getEnclosures().stream()
                .filter(enc -> enc.getType() != null && enc.getType().startsWith("image/"))
                .map(enc -> enc.getUrl())
                .findFirst()
                .orElse(null);
            if (rssThumbnail != null) {
                System.out.println("  ✓ Enclosures에서 발견: " + rssThumbnail);
                return rssThumbnail;
            }
        }
        System.out.println("  ✗ Enclosures 없음");
        System.out.println();

        // 2. Media module
        System.out.println("[STEP 2] Media 모듈 확인");
        try {
            if (entry.getForeignMarkup() != null) {
                System.out.println("  ForeignMarkup 개수: " + entry.getForeignMarkup().size());

                for (int i = 0; i < entry.getForeignMarkup().size(); i++) {
                    Object element = entry.getForeignMarkup().get(i);
                    System.out.println("  [" + i + "] " + element.getClass().getName());

                    if (element instanceof Element) {
                        Element elem = (Element) element;
                        System.out.println("      Name: " + elem.getName());
                        System.out.println("      Namespace: " + elem.getNamespaceURI());
                        System.out.println("      Prefix: " + elem.getNamespacePrefix());

                        if ("group".equals(elem.getName())) {
                            System.out.println("      ✓✓ media:group 발견!");

                            if (elem.getNamespaceURI() != null && elem.getNamespaceURI().contains("media")) {
                                System.out.println("      ✓✓✓ Namespace에 'media' 포함됨");

                                // 방법 1
                                System.out.println("      [방법 1] getChild(\"thumbnail\", namespace)");
                                Element thumbnail = elem.getChild("thumbnail", elem.getNamespace());
                                System.out.println("        결과: " + thumbnail);
                                if (thumbnail != null) {
                                    String url = thumbnail.getAttributeValue("url");
                                    System.out.println("        URL: " + url);
                                    if (url != null) {
                                        System.out.println("        ✓ 성공!");
                                        return url;
                                    }
                                }

                                // 방법 2
                                System.out.println("      [방법 2] getChildren() 순회");
                                System.out.println("        자식 개수: " + elem.getChildren().size());
                                for (Object child : elem.getChildren()) {
                                    if (child instanceof Element) {
                                        Element childElem = (Element) child;
                                        System.out.println("        - " + childElem.getName() +
                                            " (Namespace: " + childElem.getNamespaceURI() + ")");
                                        if ("thumbnail".equals(childElem.getName())) {
                                            String url = childElem.getAttributeValue("url");
                                            System.out.println("          ✓ thumbnail 발견! URL: " + url);
                                            if (url != null) {
                                                return url;
                                            }
                                        }
                                    }
                                }

                                System.out.println("      ✗ thumbnail을 찾지 못함");
                            } else {
                                System.out.println("      ✗ Namespace에 'media'가 없음");
                            }
                        }
                    }
                }
            } else {
                System.out.println("  ✗ ForeignMarkup이 null");
            }
        } catch (Exception e) {
            System.out.println("  ✗ 에러 발생: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();

        // 3. DSL 크롤링
        System.out.println("[STEP 3] DSL 크롤링");
        System.out.println("  coverImageDsl: " + feedSource.getCoverImageDsl());
        if (feedSource.getCoverImageDsl() != null && !feedSource.getCoverImageDsl().trim().isEmpty()) {
            System.out.println("  DSL 크롤링 시뮬레이션 (실제 크롤링은 생략)");
            // 실제로는 Jsoup.connect()를 하지만 여기서는 생략
        } else {
            System.out.println("  ✗ coverImageDsl이 null이거나 비어있음");
        }

        return null;
    }

    @Test
    @DisplayName("Java/Rome 버전 확인")
    void checkVersions() {
        System.out.println("===========================================");
        System.out.println("=== 환경 정보 ===");
        System.out.println("===========================================");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println();

        // Rome 버전 확인
        try {
            Package pkg = com.rometools.rome.feed.synd.SyndFeed.class.getPackage();
            System.out.println("Rome Package: " + pkg.getName());
            System.out.println("Rome Implementation Version: " + pkg.getImplementationVersion());
            System.out.println("Rome Specification Version: " + pkg.getSpecificationVersion());
        } catch (Exception e) {
            System.out.println("Rome 버전 확인 실패: " + e.getMessage());
        }

        System.out.println("===========================================");
    }
}
