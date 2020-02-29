grammar Ckii;

@header {
    package jay.aenigma.ckii.parser;
}
// script file

// PARSER RULES

unit : statement* EOF;

statement
    : lhs=expression OPERATOR rhs=block #BlockStatement
    | lhs=expression OPERATOR rhs=expression #ExpressionStatement
    ;

block
    : LBRACE RBRACE #EmptyBlock
    | LBRACE statement+ RBRACE #StatementBlock
    | LBRACE expression+ RBRACE #ExpressionBlock
    | LBRACE block+ RBRACE #BlockBlock
    ;

expression
    : STRING_QUOTED #StringExpr
    | IDENTIFIER #IdenExpr
    | NAMESPACED_ID #NamespExpr
    | NUMBER #NumberExpr
    | DATE #DateExpr
    | BOOL #BoolExpr
    ;

// LEXER RULES
COMMENT : '#' ~('\r'|'\n')* (CRLF|EOF) -> skip;


OPERATOR
    : '=='
    | '>='
    | '<='
    | '>'
    | '<'
    | '='
    ;

BOOL : 'yes' | 'no';

DATE : DIGIT+ DOT DIGIT+ DOT DIGIT+;

NAMESPACED_ID : (LETTER|UNDERSCORE)+ DOT DIGIT+;

LBRACE : '{';
RBRACE : '}';

NUMBER : (PLUS|MINUS)? DIGIT+ (DOT DIGIT+)?;

STRING_QUOTED : '"' ~('"')* '"';

WHITESPACE : (SPACE|CRLF)+ -> skip;

IDENTIFIER : ~(' '|'\t'|'\r'|'\n'|'"'|'='|'{'|'}'|'#'|';')+;


//fragments
fragment DOT : '.';
fragment DIGIT : [0-9];
fragment LETTER : 'a'..'z' |'A'..'Z';
fragment UNDERSCORE : '_';
fragment MINUS : '-';
fragment PLUS : '+';
fragment SPACE : ' '|'\t';
fragment CRLF : '\r'?'\n';
