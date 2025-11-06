package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;

public class DocumentNode implements ASTNode {
    @Override
    public Object evaluate(DslInterpreter interpreter) {
        return interpreter.getDocument();
    }
}
