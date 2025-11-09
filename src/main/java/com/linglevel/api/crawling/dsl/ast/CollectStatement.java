package com.linglevel.api.crawling.dsl.ast;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CollectStatement {
    private final ASTNode expr;
}
