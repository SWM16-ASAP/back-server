package com.linglevel.api.content.feed.filter;

import com.rometools.rome.feed.synd.SyndEntry;
import com.linglevel.api.content.feed.entity.FeedSource;

public interface FeedFilter {

    FeedFilterResult filter(SyndEntry entry, FeedSource feedSource);

    String getName();

    default int getOrder() {
        return 100;
    }
}
