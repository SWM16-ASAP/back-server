package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class SelectorAllNode implements ASTNode {
    private final String selector;

    @Override
    public Object evaluate(DslInterpreter interpreter) {
        Object context = interpreter.getCurrentContext();
        Elements elements = null;

        if (context instanceof Document) {
            elements = ((Document) context).select(selector);
        } else if (context instanceof Element) {
            elements = ((Element) context).select(selector);
        }

        if (elements == null || elements.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(elements);
    }
}
