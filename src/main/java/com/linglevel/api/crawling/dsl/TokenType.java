package com.linglevel.api.crawling.dsl;

public enum TokenType {
    D,              // Document
    SINGLE_QUOTE,   // 'selector'
    TRIPLE_QUOTE,   // '''selector'''
    AT,             // @
    HASH,           // #
    QUESTION,       // ?
    PLUS,           // +
    GT,             // >
    CARET,          // ^
    LPAREN,         // (
    RPAREN,         // )
    LBRACKET,       // [
    RBRACKET,       // ]
    EOF
}
