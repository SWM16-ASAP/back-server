package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class QuestionNode implements ASTNode {
    private final ASTNode left;
    private final ASTNode right;

    @Override
    public Object evaluate(DslInterpreter interpreter) {
        Object leftValue = left.evaluate(interpreter);
        if (isNullOrEmpty(leftValue)) {
            return right.evaluate(interpreter);
        }
        return leftValue;
    }

    private boolean isNullOrEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            return true;
        }
        if (value instanceof List && ((List<?>) value).isEmpty()) {
            return true;
        }
        return false;
    }
}
