package com.linglevel.api.crawling.dsl;

import com.linglevel.api.crawling.dsl.ast.ASTNode;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;

@Getter
@Setter
public class DslInterpreter {
    private final Document document;
    private Object currentContext;

    public DslInterpreter(Document document) {
        this.document = document;
        this.currentContext = document;
    }

    public Object evaluate(ASTNode node) {
        return node.evaluate(this);
    }
}
