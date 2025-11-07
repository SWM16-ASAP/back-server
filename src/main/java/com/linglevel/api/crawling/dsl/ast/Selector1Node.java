package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@RequiredArgsConstructor
public class Selector1Node implements ASTNode {
    private final String selector;

    @Override
    public Object evaluate(DslInterpreter interpreter) {
        Object context = interpreter.getCurrentContext();
        if (context instanceof Document) {
            return ((Document) context).selectFirst(selector);
        } else if (context instanceof Element) {
            return ((Element) context).selectFirst(selector);
        }
        return null;
    }
}
