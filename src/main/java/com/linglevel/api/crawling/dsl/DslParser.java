package com.linglevel.api.crawling.dsl;

import com.linglevel.api.crawling.dsl.ast.*;

import java.util.ArrayList;
import java.util.List;

public class DslParser {
    private final List<Token> tokens;
    private int position;

    public DslParser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    private Token current() {
        return tokens.get(position);
    }

    public ASTNode parse() {
        ASTNode result = parseExpr();
        if (current().getType() != TokenType.EOF) {
            throw new RuntimeException("Unexpected token at position " + current().getPosition() + ": " + current().getValue());
        }
        return result;
    }

    private ASTNode parseExpr() {
        return parseOr();
    }

    private ASTNode parseOr() {
        ASTNode left = parseAdd();

        while (current().getType() == TokenType.QUESTION) {
            position++;
            ASTNode right = parseAdd();
            left = new QuestionNode(left, right);
        }

        return left;
    }

    private ASTNode parseAdd() {
        ASTNode left = parseChain();

        while (current().getType() == TokenType.PLUS) {
            position++;
            ASTNode right = parseChain();
            left = new PlusNode(left, right);
        }

        return left;
    }

    private ASTNode parseChain() {
        ASTNode primary = parsePrimary();
        List<ASTNode> items = new ArrayList<>();
        items.add(primary);

        while (true) {
            if (current().getType() == TokenType.GT) {
                position++;
                List<ASTNode> actionChain = parseActionChain();
                ASTNode source = items.size() == 1 ? items.get(0) : new ChainNode(items.get(0), new ArrayList<>(items.subList(1, items.size())));
                items.clear();
                items.add(new MapEachNode(source, actionChain));
            } else if (isPostfix()) {
                items.add(parsePostfix());
            } else {
                break;
            }
        }

        if (items.size() == 1) {
            return items.get(0);
        }
        return new ChainNode(items.get(0), new ArrayList<>(items.subList(1, items.size())));
    }

    private List<ASTNode> parseActionChain() {
        List<ASTNode> actions = new ArrayList<>();

        if (current().getType() == TokenType.LPAREN) {
            position++;
            while (current().getType() != TokenType.RPAREN && current().getType() != TokenType.EOF) {
                if (!isPostfix()) {
                    throw new RuntimeException("Only postfix operations allowed in > action chain at position " + current().getPosition());
                }
                actions.add(parsePostfix());
            }
            if (current().getType() != TokenType.RPAREN) {
                throw new RuntimeException("Expected ) at position " + current().getPosition());
            }
            position++;
        } else {
            while (isPostfix()) {
                actions.add(parsePostfix());
            }
        }

        if (actions.isEmpty()) {
            throw new RuntimeException("Empty action chain after > at position " + current().getPosition());
        }

        return actions;
    }

    private boolean isPostfix() {
        return current().getType() == TokenType.SINGLE_QUOTE ||
               current().getType() == TokenType.TRIPLE_QUOTE ||
               current().getType() == TokenType.AT ||
               current().getType() == TokenType.HASH;
    }

    private ASTNode parsePostfix() {
        if (current().getType() == TokenType.SINGLE_QUOTE) {
            String selector = current().getValue();
            position++;
            return new Selector1Node(selector);
        } else if (current().getType() == TokenType.TRIPLE_QUOTE) {
            String selector = current().getValue();
            position++;
            return new SelectorAllNode(selector);
        } else if (current().getType() == TokenType.AT) {
            position++;
            if (current().getType() != TokenType.SINGLE_QUOTE) {
                throw new RuntimeException("Expected attribute name after @ at position " + current().getPosition());
            }
            String attr = current().getValue();
            position++;
            return new AttrNode(attr);
        } else if (current().getType() == TokenType.HASH) {
            position++;
            return new TextNode();
        } else {
            throw new RuntimeException("Expected postfix operation at position " + current().getPosition());
        }
    }

    private ASTNode parsePrimary() {
        if (current().getType() == TokenType.D) {
            position++;
            return new DocumentNode();
        } else if (current().getType() == TokenType.LBRACKET) {
            return parseCollect();
        } else if (current().getType() == TokenType.LPAREN) {
            position++;
            ASTNode expr = parseExpr();
            if (current().getType() != TokenType.RPAREN) {
                throw new RuntimeException("Expected ) at position " + current().getPosition());
            }
            position++;
            return new GroupNode(expr);
        } else {
            throw new RuntimeException("Unexpected token at position " + current().getPosition() + ": " + current().getType());
        }
    }

    private ASTNode parseCollect() {
        if (current().getType() != TokenType.LBRACKET) {
            throw new RuntimeException("Expected [ at position " + current().getPosition());
        }
        position++;

        List<CollectStatement> statements = new ArrayList<>();

        while (current().getType() != TokenType.RBRACKET && current().getType() != TokenType.EOF) {
            ASTNode expr = parseExpr();

            if (current().getType() == TokenType.CARET) {
                position++;
                statements.add(new CollectStatement(expr));
            } else if (current().getType() == TokenType.RBRACKET) {
                // Last expression without ^
                break;
            } else {
                throw new RuntimeException("Expected ^ or ] at position " + current().getPosition());
            }
        }

        if (current().getType() != TokenType.RBRACKET) {
            throw new RuntimeException("Expected ] at position " + current().getPosition());
        }
        position++;

        return new CollectNode(statements);
    }
}
