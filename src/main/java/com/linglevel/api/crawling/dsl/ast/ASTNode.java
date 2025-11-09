package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;

public interface ASTNode {
    Object evaluate(DslInterpreter interpreter);
}
