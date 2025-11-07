package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GroupNode implements ASTNode {
    private final ASTNode expr;

    @Override
    public Object evaluate(DslInterpreter interpreter) {
        return expr.evaluate(interpreter);
    }
}
