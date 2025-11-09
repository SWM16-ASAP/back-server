package com.linglevel.api.crawling.dsl.ast;

import com.linglevel.api.crawling.dsl.DslInterpreter;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PlusNode implements ASTNode {
    private final ASTNode left;
    private final ASTNode right;

    @Override
    public Object evaluate(DslInterpreter interpreter) {
        Object leftValue = left.evaluate(interpreter);
        Object rightValue = right.evaluate(interpreter);

        List<Element> leftList = listify(leftValue);
        List<Element> rightList = listify(rightValue);

        if (leftList != null && rightList != null) {
            List<Element> result = new ArrayList<>(leftList);
            result.addAll(rightList);
            return result;
        }

        throw new RuntimeException("Type error in + operator: both operands must be Element or List<Element>");
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
            if (list.isEmpty() || list.get(0) instanceof Element) {
                return (List<Element>) list;
            }
        }
        return null;
    }
}
