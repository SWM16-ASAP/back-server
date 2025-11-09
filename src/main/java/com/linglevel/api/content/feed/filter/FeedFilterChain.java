package com.linglevel.api.content.feed.filter;

import com.rometools.rome.feed.synd.SyndEntry;
import com.linglevel.api.content.feed.entity.FeedSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 피드 필터 체인
 * 등록된 모든 필터를 순차적으로 실행하여 피드를 검증
 */
@Component
@Slf4j
public class FeedFilterChain {

    private final List<FeedFilter> filters;

    /**
     * ObjectProvider를 사용하여 @Order에 따라 자동 정렬된 필터 주입
     * Spring이 orderedStream()을 통해 자동으로 정렬해줌
     */
    public FeedFilterChain(ObjectProvider<FeedFilter> filterProvider) {
        this.filters = filterProvider.orderedStream()
            .collect(Collectors.toList());

        log.info("FeedFilterChain initialized with {} filters (auto-sorted by @Order)", filters.size());
        filters.forEach(filter ->
            log.info("  - {} (order: {})", filter.getName(), filter.getOrder())
        );
    }

    /**
     * 모든 필터를 순차적으로 실행
     * 하나라도 실패하면 즉시 중단하고 실패 결과 반환 (Fail-Fast)
     *
     * @param entry RSS Entry
     * @param feedSource Feed Source
     * @return 최종 필터링 결과
     */
    public FeedFilterResult executeFilters(SyndEntry entry, FeedSource feedSource) {
        log.debug("Executing {} filters for entry: {}", filters.size(), entry.getLink());

        // 이미 정렬되어 있음!
        for (FeedFilter filter : filters) {
            FeedFilterResult result = filter.filter(entry, feedSource);

            if (!result.isPassed()) {
                log.info("Feed filtered out by {}: {} - {}",
                    filter.getName(), entry.getLink(), result.getReason());
                return result;
            }
        }

        log.debug("Feed passed all filters: {}", entry.getLink());
        return FeedFilterResult.pass();
    }
}
