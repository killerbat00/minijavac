/*
 * miniJava Token
 * 
 * this file contains the token definitions and logic for our
 * subset of miniJava
 * 
 * @author brian morrow
 * I pledge that I have given nor received any unauthorized help on this
 * assignment and that I have abided by all Honor Code guidelines.
 */
package miniJava.SyntacticAnalyzer;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class Token extends Object{
	public int type;
	public String spelling;
	public SourcePosition position;
	
	public Token(int type, String spelling, SourcePosition position) {
		this.type = type;
		this.spelling = spelling;
		this.position = position;
		
		if(type == ID) {
			for(int i = firstReservedWord; i <= lastReservedWord; i++) {
				if(spelling.equals(spellings[i])) {
					this.type = i;
					break;
				}
			}
		}
	}
	
	public static String spell(int type) {
		return spellings[type];
	}
	
	public String toString() {
		return "Type=" + type + ", spelling=" + spelling +
				", position=" + position;
	}
	
	public static final int
		INTLITERAL	= 0, //num
		ID			= 1,
		BINOP		= 2,
		UNOP		= 3,
		
		GREATER		= 4,
		LESS		= 5,
		EQUAL		= 6,
		LTEQUAL		= 7,
		GTEQUAL		= 8,
		NOTEQUAL	= 9,
		AND			= 10,
		OR			= 11,
		NOT			= 12,
		PLUS		= 13,
		MINUS		= 14,
		TIMES		= 15,
		DIV			= 16,
		ASSIGN		= 17,
		
		CLASS		= 18,
		RETURN		= 19,
		PUBLIC		= 20,
		PRIVATE		= 21,
		STATIC		= 22,
		INT			= 23,
		BOOLEAN		= 24,
		VOID		= 25,
		THIS		= 26,
		IF			= 27,
		ELSE		= 28,
		WHILE		= 29,
		TRUE		= 30,
		FALSE		= 31,
		NEW			= 32,
		
		DOT			= 33,
		COMMA		= 34,
		SEMICOLON	= 35,
		
		LPAREN		= 36,
		RPAREN		= 37,
		LBRACKET	= 38,
		RBRACKET	= 39,
		LCURLY		= 40,
		RCURLY		= 41,
		
		EOT			= 42,
		ERROR		= 43;
	
	private static String[] spellings = {
		"<int>",
		"<identifier>",
		"<binop>",
		"<unop>",
		">",
		"<",
		"==",
		"<=",
		">=",
		"!=",
		"&&",
		"||",
		"!",
		"+",
		"-",
		"*",
		"/",
		"=",
		"class",
		"return",
		"public",
		"private",
		"static",
		"int",
		"boolean",
		"void",
		"this",
		"if",
		"else",
		"while",
		"true",
		"false",
		"new",
		".",
		",",
		";",
		"(",
		")",
		"[",
		"]",
		"{",
		"}",
		"",
		"<error>"
	};
	
	private final static int firstReservedWord = Token.CLASS;
	private final static int lastReservedWord = Token.NEW;
}

