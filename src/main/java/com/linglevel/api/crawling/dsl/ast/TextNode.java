package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import org.jsoup.nodes.Element;

public class TextNode implements ASTNode {
    @Override
    public Object evaluate(DslInterpreter interpreter) {
        Object context = interpreter.getCurrentContext();
        if (context instanceof Element) {
            String text = ((Element) context).text().trim();
            return text.isEmpty() ? null : text;
        }
        return null;
    }
}
