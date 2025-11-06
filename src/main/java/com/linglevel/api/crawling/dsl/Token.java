package com.linglevel.api.crawling.dsl;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Token {
    private final TokenType type;
    private final String value;
    private final int position;

    @Override
    public String toString() {
        return type + "('" + value + "')@" + position;
    }
}
