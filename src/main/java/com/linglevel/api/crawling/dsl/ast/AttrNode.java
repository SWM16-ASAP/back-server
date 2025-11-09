package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Element;

@RequiredArgsConstructor
public class AttrNode implements ASTNode {
    private final String attribute;

    @Override
    public Object evaluate(DslInterpreter interpreter) {
        Object context = interpreter.getCurrentContext();
        if (context instanceof Element) {
            Element element = (Element) context;
            String value = element.attr(attribute);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
