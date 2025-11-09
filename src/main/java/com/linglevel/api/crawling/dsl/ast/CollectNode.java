package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CollectNode implements ASTNode {
    private final List<CollectStatement> statements;

    @Override
    @SuppressWarnings("unchecked")
    public Object evaluate(DslInterpreter interpreter) {
        List<Object> collected = new ArrayList<>();

        for (CollectStatement stmt : statements) {
            Object value = stmt.getExpr().evaluate(interpreter);
            collect(value, collected);
        }

        return collected;
    }

    private void collect(Object value, List<Object> collected) {
        if (value == null) {
            return;
        }

        if (value instanceof String) {
            String trimmed = ((String) value).trim();
            if (!trimmed.isEmpty()) {
                collected.add(trimmed);
            }
        } else if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    if (item instanceof String) {
                        String trimmed = ((String) item).trim();
                        if (!trimmed.isEmpty()) {
                            collected.add(trimmed);
                        }
                    } else {
                        collected.add(item);
                    }
                }
            }
        } else {
            collected.add(value);
        }
    }
}
