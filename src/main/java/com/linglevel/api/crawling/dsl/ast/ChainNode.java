package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ChainNode implements ASTNode {
    private final ASTNode base;
    private final List<ASTNode> postfixes;

    @Override
    public Object evaluate(DslInterpreter interpreter) {
        Object result = base.evaluate(interpreter);

        for (ASTNode postfix : postfixes) {
            Object saved = interpreter.getCurrentContext();
            interpreter.setCurrentContext(result);
            result = postfix.evaluate(interpreter);
            interpreter.setCurrentContext(saved);

            if (result == null) {
                break;
            }
        }

        return result;
    }
}
