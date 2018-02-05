grammar NugetODataFilter;

@header {
    package org.carlspring.strongbox.nuget.filter;
}

filter
:
    filterExp
;

filterExp
:
    '(' filterExp ')'
    | left = filterExp op = logicalOp right = filterExp
    | tokenExp
;

tokenExp
:
    left = tokenExpLeft op = filterOp right = tokenExpRight
    | TAG
;

tokenExpRight
:
    '\'' VALUE '\''
;

tokenExpLeft
:
    ATTRIBUTE
    | tokenExpFunction '(' ATTRIBUTE ')'
;

tokenExpFunction
:
    'tolower'
;

filterOp
:
    EQ
    | GE
;

logicalOp
:
    AND
    | OR
;

TAG
:
    'IsLatestVersion'
;

ATTRIBUTE
:
    'Id'
    | 'Version'
;

VALUE
:
    [a-zA-Z_] [a-zA-Z_0-9.]*
;

EQ
:
    'eq'
;

GE
:
    'ge'
;

AND
:
    'and'
;

OR
:
    'or'
;

NOT
:
    'not'
;

WHITESPACE
:
    ' ' -> skip
;