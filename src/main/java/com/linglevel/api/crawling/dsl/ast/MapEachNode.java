package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class MapEachNode implements ASTNode {
    private final ASTNode source;
    private final List<ASTNode> actionChain;

    @Override
    @SuppressWarnings("unchecked")
    public Object evaluate(DslInterpreter interpreter) {
        Object sourceValue = source.evaluate(interpreter);
        List<Element> elementList = listify(sourceValue);

        if (elementList == null) {
            return new ArrayList<>();
        }

        List<Object> results = new ArrayList<>();

        for (Element element : elementList) {
            Object saved = interpreter.getCurrentContext();

            Object current = element;

            // Apply action chain sequentially
            for (ASTNode action : actionChain) {
                interpreter.setCurrentContext(current);
                current = action.evaluate(interpreter);
                if (current == null) {
                    break;
                }
            }

            // Add result if not null
            if (current != null) {
                if (current instanceof List) {
                    // Flatten list
                    for (Object item : (List<?>) current) {
                        if (item != null) {
                            results.add(item);
                        }
                    }
                } else {
                    results.add(current);
                }
            }

            interpreter.setCurrentContext(saved);
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Element> listify(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Element) {
            List<Element> list = new ArrayList<>();
            list.add((Element) value);
            return list;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return new ArrayList<>();
            }
            if (list.get(0) instanceof Element) {
                // Always create a copy to avoid ConcurrentModificationException
                return new ArrayList<>((List<Element>) list);
            }
        }
        return null;
    }
}
