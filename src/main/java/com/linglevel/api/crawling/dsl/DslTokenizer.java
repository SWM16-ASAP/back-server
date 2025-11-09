package com.linglevel.api.crawling.dsl;

import java.util.ArrayList;
import java.util.List;

public class DslTokenizer {
    private final String input;
    private int position;
    private final List<Token> tokens;

    public DslTokenizer(String input) {
        this.input = input;
        this.position = 0;
        this.tokens = new ArrayList<>();
    }

    public List<Token> tokenize() {
        while (position < input.length()) {
            skipWhitespace();
            if (position >= input.length()) {
                break;
            }

            int startPos = position;
            char ch = input.charAt(position);

            switch (ch) {
                case 'D':
                    tokens.add(new Token(TokenType.D, "D", startPos));
                    position++;
                    break;
                case '@':
                    tokens.add(new Token(TokenType.AT, "@", startPos));
                    position++;
                    break;
                case '#':
                    tokens.add(new Token(TokenType.HASH, "#", startPos));
                    position++;
                    break;
                case '?':
                    tokens.add(new Token(TokenType.QUESTION, "?", startPos));
                    position++;
                    break;
                case '+':
                    tokens.add(new Token(TokenType.PLUS, "+", startPos));
                    position++;
                    break;
                case '>':
                    tokens.add(new Token(TokenType.GT, ">", startPos));
                    position++;
                    break;
                case '^':
                    tokens.add(new Token(TokenType.CARET, "^", startPos));
                    position++;
                    break;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, "(", startPos));
                    position++;
                    break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, ")", startPos));
                    position++;
                    break;
                case '[':
                    tokens.add(new Token(TokenType.LBRACKET, "[", startPos));
                    position++;
                    break;
                case ']':
                    tokens.add(new Token(TokenType.RBRACKET, "]", startPos));
                    position++;
                    break;
                case '\'':
                    parseQuote(startPos);
                    break;
                default:
                    throw new RuntimeException("Unexpected character at position " + position + ": " + ch);
            }
        }

        tokens.add(new Token(TokenType.EOF, "", position));
        return tokens;
    }

    private void skipWhitespace() {
        while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
            position++;
        }
    }

    private void parseQuote(int startPos) {
        if (position + 2 < input.length() && input.substring(position, position + 3).equals("'''")) {
            position += 3;
            String content = extractQuotedContent("'''");
            tokens.add(new Token(TokenType.TRIPLE_QUOTE, content, startPos));
        } else if (input.charAt(position) == '\'') {
            position++;
            String content = extractQuotedContent("'");
            tokens.add(new Token(TokenType.SINGLE_QUOTE, content, startPos));
        }
    }

    private String extractQuotedContent(String quote) {
        StringBuilder buffer = new StringBuilder();

        while (position < input.length()) {
            if (quote.equals("'''") && position + 2 < input.length() &&
                    input.substring(position, position + 3).equals("'''")) {
                position += 3;
                return buffer.toString();
            } else if (quote.equals("'") && input.charAt(position) == '\'') {
                position++;
                return buffer.toString();
            } else if (input.charAt(position) == '\\' && position + 1 < input.length()) {
                position++;
                buffer.append(input.charAt(position));
                position++;
            } else {
                buffer.append(input.charAt(position));
                position++;
            }
        }

        throw new RuntimeException("Unterminated quote at position " + (position - buffer.length()));
    }
}
