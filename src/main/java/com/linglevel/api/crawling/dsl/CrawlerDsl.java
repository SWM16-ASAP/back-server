package com.linglevel.api.crawling.dsl;

import com.linglevel.api.crawling.dsl.ast.ASTNode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;

/**
 * Crawler DSL 인터프리터
 *
 * 사용 예시:
 * - D'h1'#                           : h1 요소의 텍스트
 * - D'img'@'src'                     : img 요소의 src 속성
 * - D'''p'''>#                       : 모든 p 요소의 텍스트 (map each)
 * - D'h1'#?D'title'#                 : h1 텍스트, 없으면 title 텍스트 (null coalescing)
 * - D'article h1'# ? D'meta[property="og:title"]'@'content'
 */
@Slf4j
public class CrawlerDsl {
    private final Document document;

    public CrawlerDsl(String html) {
        this(Jsoup.parse(html));
    }

    public CrawlerDsl(Document document) {
        this.document = document;
    }

    /**
     * DSL 표현식을 실행하여 결과 반환
     *
     * @param dsl DSL 표현식
     * @return 추출된 값 (String, List, Element 등)
     */
    public Object execute(String dsl) {
        if (dsl == null || dsl.trim().isEmpty()) {
            return null;
        }

        try {
            // Tokenize
            DslTokenizer tokenizer = new DslTokenizer(dsl);
            List<Token> tokens = tokenizer.tokenize();

            // Parse
            DslParser parser = new DslParser(tokens);
            ASTNode ast = parser.parse();

            // Interpret
            DslInterpreter interpreter = new DslInterpreter(document);
            return interpreter.evaluate(ast);

        } catch (Exception e) {
            log.error("Error executing DSL: {} - {}: {}", dsl, e.getClass().getSimpleName(), e.getMessage());
            log.error("Stack trace:", e);
            return null;
        }
    }

    /**
     * DSL 표현식을 실행하여 문자열 결과 반환
     *
     * @param dsl DSL 표현식
     * @return 추출된 문자열 (null 가능)
     */
    public String executeAsString(String dsl) {
        Object result = execute(dsl);
        if (result == null) {
            return null;
        }

        if (result instanceof String) {
            return (String) result;
        }

        if (result instanceof List) {
            List<?> list = (List<?>) result;
            if (list.isEmpty()) {
                return null;
            }
            // Join list elements with newline
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (item != null) {
                    if (sb.length() > 0) {
                        sb.append("\n\n");
                    }
                    sb.append(item.toString());
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        }

        return result.toString();
    }
}
