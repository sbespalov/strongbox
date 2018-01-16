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
	ATTRIBUTE filterOp VALUE
	| TAG
;

filterOp
:
	EQ
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
	[a-zA-Z_] [a-zA-Z_0-9]*
;

EQ
:
	'eq'
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

WS
:
	' '
;
