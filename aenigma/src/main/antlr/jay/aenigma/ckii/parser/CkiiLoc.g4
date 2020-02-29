grammar CkiiLoc;

@header {
    package jay.aenigma.ckii.parser;
}

// localisation file

//Theoretically, localisation files should have the most well defined formatting of them all. Theoretically.

// PARSER RULES

unit : (localisation|comment_line)*;

comment_line : COMMENT (EOL|EOF);

localisation : KEY SEMICOLON (value|SEMICOLON)* (EOL|EOF);

value : KEY | STRING | COMMENT;

// LEXER RULES
KEY : ~(' '|'\t'|'\r'|'\n'|'"'|'='|'{'|'}'|'#'|';')+;

COMMENT : '#' ~('\r'|'\n')*;

STRING : ~('\r'|'\n'|';')+;

SEMICOLON : ';';

EOL : '\r'?'\n';
