/*
 * miniJava Parser
 * 
 * this class parses tokens to determine syntactic correctness.
 * 
 * @author brian morrow
 * I pledge that I have given nor received any unauthorized help on this
 * assignment and that I have abided by all Honor Code guidelines.
 */

package miniJava.SyntacticAnalyzer;

import miniJava.*;

public class ParserOld {

    private Scanner lexicalAnalyzer;
    private Token currentToken;
    private ErrorReporter errorReporter;
    private SourcePosition previousTokenPosition;

    public ParserOld(Scanner lexer, ErrorReporter reporter) {
        lexicalAnalyzer = lexer;
        errorReporter = reporter;
        previousTokenPosition = new SourcePosition();
    }

    void syntacticError(String messageTemplate,
                        String tokenQuoted) {
        SourcePosition pos = currentToken.position;
        errorReporter.reportError(messageTemplate + " " + tokenQuoted + " " + pos);
    }

    void accept(int tokenExpected) {
    	System.out.println("Accepting Token: " + tokenExpected);
        if(currentToken.type == tokenExpected) {
            previousTokenPosition = currentToken.position;
            currentToken = lexicalAnalyzer.scan();
        } else {
        	syntacticError(Token.spell(tokenExpected) + " expected, instead of \'" +
        		currentToken.spelling + "\'", Token.spell(tokenExpected));

        }
    }

    void acceptIt() {
    	System.out.println("AcceptIting Token: " + currentToken.type);
        previousTokenPosition = currentToken.position;
        currentToken = lexicalAnalyzer.scan();
    }

    // start records the position of the 1st char of 1st token of phrase
    void start(SourcePosition position) {
        position.start = currentToken.position.start;
    }

    // finish records the position of last char of last token of phrase
    void finish(SourcePosition position) {
        position.finish = previousTokenPosition.finish;
    }

    public void parse() {
        currentToken = lexicalAnalyzer.scan();
        parseProgram();
    }

    private void parseProgram(){
    	System.out.println("ParseProgram");
        while(currentToken.type == Token.CLASS) {
            parseClassDeclaration();
        }
        accept(Token.EOT);
    }

    private void parseClassDeclaration(){
    	System.out.println("ParseClassDeclaration");
        accept(Token.CLASS);
        parseID();
        accept(Token.LCURLY);

        while(isStarterDeclarators(currentToken.type)) {
            parseDeclarators();
            parseID();

            switch(currentToken.type) {
            case Token.VOID:
            	parseDeclarators();
            	parseID();
            	break;
            case Token.SEMICOLON:
                acceptIt();
                break;

            case Token.LPAREN:
                acceptIt();

                if(isStarterParameterList(currentToken.type))
                    parseParameterList();

                accept(Token.RPAREN);
                accept(Token.LCURLY);

                while(isStarterStatement(currentToken.type))
                    parseStatement();

                if(currentToken.type == Token.RETURN) {
                    acceptIt();
                    parseExpression();
                    accept(Token.SEMICOLON);
                }

                accept(Token.RCURLY);
                break;
            default:
                syntacticError("\";\" or \"(\" expected, instead of " +
                    "\"%\"", currentToken.spelling);
                break;
            }
        }
        accept(Token.RCURLY);
    }

    private void parseDeclarators(){
    	System.out.println("ParseDeclarators");
        if(currentToken.type == Token.PUBLIC
            || currentToken.type == Token.PRIVATE
        	|| currentToken.type == Token.STATIC)
            acceptIt();
        else 
        	parseType();
    }

    private void parseType(){
    	System.out.println("ParseType");
        switch(currentToken.type) {
        case Token.BOOLEAN:
        case Token.VOID:
            acceptIt();
            break;

        case Token.ID:
            parseID();
            if(currentToken.type == Token.LBRACKET) {
                acceptIt();
                accept(Token.RBRACKET);
            }
            break;

        case Token.INT:
            acceptIt();
            if(currentToken.type == Token.LBRACKET) {
                acceptIt();
                accept(Token.RBRACKET);
            }
            break;

        default:
            syntacticError("\"%\" cannot start a type", currentToken.spelling);
            break;
        }
    }

    private void parseParameterList(){
    	System.out.println("parseParameterList");
        parseType();
        parseID();

        while(currentToken.type == Token.COMMA) {
            acceptIt();
            parseType();
            parseID();
        }
    }

    private void parseArgumentList(){
    	System.out.println("parseArgumentList");
        parseExpression();

        while(currentToken.type == Token.COMMA) {
            acceptIt();
            parseExpression();
        }
    }

    private void parseReference() {
    	System.out.println("parseReference");
        if(currentToken.type == Token.THIS)
            acceptIt();
        else if(currentToken.type == Token.ID)
            acceptIt();
        else
            syntacticError("\"%\" cannot start a reference",
                currentToken.spelling);

        while(currentToken.type == Token.DOT) {
        	System.out.println(currentToken.type);
            acceptIt();
            parseID();
        }
    }

    private void parseStatement() {
    	System.out.println("parseStatement");
        switch(currentToken.type) {
        case Token.LCURLY:
            acceptIt();
            while(isStarterStatement(currentToken.type))
                parseStatement();
            accept(Token.RCURLY);
            break;

        case Token.IF:
            acceptIt();
            accept(Token.LPAREN);
            parseExpression();
            accept(Token.RPAREN);
            parseStatement();
            if(currentToken.type == Token.ELSE) {
                acceptIt();
                parseStatement();
            }
            break;

        case Token.WHILE:
            acceptIt();
            accept(Token.LPAREN);
            parseExpression();
            accept(Token.RPAREN);
            parseStatement();
            break;

        case Token.BOOLEAN: // Statement ::= Type id = Expression;
        case Token.VOID:
        case Token.INT:
            parseType();
            parseID();
            accept(Token.ASSIGN);
            parseExpression();
            accept(Token.SEMICOLON);
            break;

        case Token.THIS: // Statement ::= Reference ([Expression])? =
            parseReference(); // Expression; | (ArgumentList?);
            switch(currentToken.type) {
            case Token.LBRACKET:
            case Token.ASSIGN:
                if(currentToken.type == Token.LBRACKET) {
                    acceptIt();
                    parseExpression();
                    accept(Token.RBRACKET);
                }
                if(currentToken.type == Token.DOT){
                	parseReference();
                }

                accept(Token.ASSIGN);
                parseExpression();
                accept(Token.SEMICOLON);
                break;

            case Token.LPAREN:
                acceptIt();
                if(isStarterArgumentList(currentToken.type))
                    parseArgumentList();
                accept(Token.RPAREN);
                accept(Token.SEMICOLON);
                break;

            default:
                syntacticError("\"[\", \"=\", or \"(\" expected, " +
                    "instead of \"%\"", currentToken.spelling);
                break;
            }
            break;

        case Token.ID:
            parseID();

            switch(currentToken.type) {
            case Token.LBRACKET:
                acceptIt();

                switch(currentToken.type) {
                case Token.RBRACKET: // Statement ::= id [] id = Expression;
                    acceptIt();
                    parseID();
                    accept(Token.ASSIGN);
                    parseExpression();
                    accept(Token.SEMICOLON);
                    break;

                case Token.THIS: // Statement ::= id [Expression] = Expression;
                case Token.ID: // Starters of Expression
                case Token.NOT:
                case Token.MINUS:
                case Token.LPAREN:
                case Token.INT:
                case Token.TRUE:
                case Token.FALSE:
                case Token.NEW:
                    parseExpression();
                    accept(Token.RBRACKET);
                    accept(Token.ASSIGN);
                    parseExpression();
                    accept(Token.SEMICOLON);
                    break;

                default:
                    syntacticError("\"]\" or an expression expected, " +
                        "instead of \"%\"", currentToken.spelling);
                    break;
                }
                break;

            case Token.ID: // Statement ::= id id = Expression;
                parseID();
                accept(Token.ASSIGN);
                parseExpression();
                accept(Token.SEMICOLON);
                break;

            case Token.DOT: // Statement ::= id (. id)* ([Expression])? =
                while(currentToken.type == Token.DOT) { // Expression; | (Ar?);
                    acceptIt();
                    parseID();
                }

                switch(currentToken.type) {
                case Token.LBRACKET:
                case Token.ASSIGN:
                    if(currentToken.type == Token.LBRACKET) {
                        acceptIt();
                        parseExpression();
                        accept(Token.RBRACKET);
                    }
                    accept(Token.ASSIGN);
                    parseExpression();
                    accept(Token.SEMICOLON);
                    break;

                case Token.LPAREN:
                    acceptIt();
                    if(isStarterArgumentList(currentToken.type))
                        parseArgumentList();
                    accept(Token.RPAREN);
                    accept(Token.SEMICOLON);
                    break;

                default:
                    syntacticError("\"[\", \"=\", or \"(\" expected, " +
                        "instead of \"%\"", currentToken.spelling);
                    break;
                }
                break;

            case Token.ASSIGN: // Statement ::= id = Expression;
                acceptIt();
                parseExpression();
                accept(Token.SEMICOLON);
                break;

            case Token.LPAREN: // Statement ::= id (ArgumentList?);
                acceptIt();
                if(isStarterArgumentList(currentToken.type))
                    parseArgumentList();
                accept(Token.RPAREN);
                accept(Token.SEMICOLON);
                break;

            default:
                syntacticError("Type or reference expected, instead " +
                    "of \"%\"", currentToken.spelling);
                break;
            }
            break;

        default:
            syntacticError("\"%\" cannot start a statement",
                currentToken.spelling);
            break;
        }
    }

    private void parseExpression() {
        switch(currentToken.type) {
        case Token.THIS: // Reference
        case Token.ID:
            parseReference();
            if(currentToken.type == Token.LBRACKET) {
                acceptIt();
                parseExpression();
                accept(Token.RBRACKET);
            } else if(currentToken.type == Token.LPAREN) {
                acceptIt();
                if(isStarterArgumentList(currentToken.type))
                    parseArgumentList();
                accept(Token.RPAREN);
            }
            break;

        case Token.NOT:
        case Token.MINUS:
            acceptIt();
            parseExpression();
            break;

        case Token.LPAREN:
            acceptIt();
            parseExpression();
            accept(Token.RPAREN);
            break;

        case Token.INTLITERAL:
        case Token.TRUE:
        case Token.FALSE:
            acceptIt();
            break;

        case Token.NEW:
            acceptIt();
            if(currentToken.type == Token.INT) {
                acceptIt();
                accept(Token.LBRACKET);
                parseExpression();
                accept(Token.RBRACKET);
            } else if(currentToken.type == Token.ID) {
                acceptIt();
                if(currentToken.type == Token.LBRACKET) {
                    acceptIt();
                    parseExpression();
                    accept(Token.RBRACKET);
                } else if(currentToken.type == Token.LPAREN) {
                    acceptIt();
                    accept(Token.RPAREN);
                }
            }
            break;

        default:
            syntacticError("\"%\" cannot start an expression",
                currentToken.spelling);
            break;
        }

        while(isBinop(currentToken.type)) {
            acceptIt();
            parseExpression();
        }
    }

    private void parseID() {
        if(currentToken.type == Token.ID) {
            previousTokenPosition = currentToken.position;
            currentToken = lexicalAnalyzer.scan();
        } else {
            syntacticError("\"%\" expected, instead of \"" +
                currentToken.spelling + "\"", Token.spell(Token.ID));
        }
    }

    private boolean isStarterDeclarators(int type) {
        return type == Token.PUBLIC
                || type == Token.PRIVATE
                || type == Token.STATIC
                || isStarterType(type);
    }

    private boolean isStarterType(int type) {
        return type == Token.BOOLEAN
                || type == Token.VOID
                || type == Token.INT
                || type == Token.ID;
    }

    private boolean isStarterParameterList(int type) {
        return isStarterType(type);
    }

    private boolean isStarterStatement(int type) {
        return type == Token.LCURLY
                || type == Token.IF
                || type == Token.WHILE
                || isStarterType(type)
                || type == Token.THIS;
    }

    private boolean isStarterArgumentList(int type) {
        return isStarterReference(type)
                || type == Token.NOT // unop
                || type == Token.MINUS // maybe should add Token NEGATIVE
                || type == Token.LPAREN
                || type == Token.INTLITERAL
                || type == Token.TRUE
                || type == Token.FALSE
                || type == Token.NEW;
    }

    private boolean isStarterReference(int type) {
        return type == Token.THIS || type == Token.ID;
    }

    private boolean isBinop(int type) {
        return type == Token.GREATER
                || type == Token.LESS
                || type == Token.EQUAL
                || type == Token.LTEQUAL
                || type == Token.GTEQUAL
                || type == Token.NOTEQUAL
                || type == Token.AND
                || type == Token.OR
                || type == Token.PLUS
                || type == Token.MINUS
                || type == Token.TIMES
                || type == Token.DIV;
    }
}