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

public class Parser {
    private Scanner scanner;
    private Token currentToken;
    private Token previousToken;
    private ErrorReporter errorReporter;
    private SourcePosition previousTokenPosition;
    private boolean verbose = false;

    public Parser(Scanner lexer, ErrorReporter reporter) {
        scanner = lexer;
        errorReporter = reporter;
        previousTokenPosition = new SourcePosition();
    }
    
    public Parser(Scanner lexer, ErrorReporter reporter, boolean verbo) {
    	if(verbo) {
    		verbose = true;
    	}
    	scanner = lexer;
    	errorReporter = reporter;
    	previousTokenPosition = new SourcePosition();
    }

    void syntacticError(String messageTemplate,
                        String tokenQuoted) {
        SourcePosition pos = currentToken.position;
        errorReporter.reportError(messageTemplate + " " + tokenQuoted + " " + pos);
    }

    void accept(int tokenExpected) {
    	String msg = null;
        if(currentToken.type == tokenExpected) {
        	if(verbose) {
        		msg = currentToken.type == Token.EOT ? "End of Text" :
        			"Accept: " + currentToken.spelling;
        		System.out.println(msg);
        	}
            previousTokenPosition = currentToken.position;
            previousToken = currentToken;
            currentToken = scanner.scan();
        } else {
        	//Sometimes tokenExpected is whitespace
        	//and the message should be different
        	// class id {*} error for example
        	syntacticError(Token.spell(tokenExpected) + " expected, instead of \'" +
        		currentToken.spelling + "\'", null);
        }
    }

    void acceptIt() {
    	if(verbose)
    		System.out.println("AcceptIt: " + currentToken.spelling);
        previousTokenPosition = currentToken.position;
        previousToken = currentToken;
        currentToken = scanner.scan();
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
    	if(verbose)
    		System.out.println("parse()");
        currentToken = scanner.scan();
        pProgram();
    }
    
    /* Program ->
     * 		(ClassDeclaration)* EOT
     */
    private void pProgram(){
    	if(verbose)
    		System.out.println("parseProgram()");
    	while(currentToken.type == Token.CLASS) {
    		pClassDeclaration();
    	}
    	accept(Token.EOT);
    };
    
    /* Class Declaration ->
     * class id {
     * 		(FieldDeclaration | MethodDeclaration)*
     * }
     */
    private void pClassDeclaration(){
    	if(verbose)
    		System.out.println("parseClassDeclaration");
    	accept(Token.CLASS);
    	accept(Token.ID);
    	accept(Token.LCURLY);
    	
    	//  (FieldDeclaration | MethodDeclaration)*
    	//  (currentToken.type in starters[FieldDeclaration]
    	//  (currentToken.type in starters[MethodDeclaration]
    	while(inDeclaratorStarterSet(currentToken.type)) {
    		pDeclarators();
    		accept(Token.ID);
    		if(currentToken.type == Token.LPAREN) {
    			pMethodDeclaration();
    		} else if(currentToken.type == Token.SEMICOLON){
    			acceptIt();
    		} else {
    			syntacticError("( OR ; expected, instead of " + 
    					currentToken.spelling, currentToken.spelling);
    		}
    	}
    	/* Class declaration body
    	 * 		can be empty -> accept }
    	 * 		can be a semicolon -> accept ; then accept }
    	 */
    	if(currentToken.type == Token.SEMICOLON) {
    		acceptIt();
    		accept(Token.RCURLY);
    	} else if(currentToken.type == Token.RCURLY) {
    		acceptIt();
    	} else {
    		syntacticError("Empty class declarations are allowed only with \n"
    				+ "\t';' or ' ' instead of", currentToken.spelling);
    	}
    };
    
    /* MethodDeclaration ->
     * 		Declarators id ( ParameterList? ) {
     * 			Statement* (return Expression ;)?
     * 		}
     */
    private void pMethodDeclaration(){
    	if(verbose)
    		System.out.println("parseMethodDeclaration");
    	// We've already parsed Declarators and id
    	//    and our currentToken is LPAREN
    	accept(Token.LPAREN);
    	
    	// ParameterList?
    	if (inParameterListStarterSet(currentToken.type))
    		pParameterList();
    	
    	accept(Token.RPAREN);
    	accept(Token.LCURLY);
    	
    	// Statement*
    	while (inStatementStarterSet(currentToken.type)) {
    		newPStatement();
    	}
    	
    	// return statement
    	if(currentToken.type == Token.RETURN) {
    		acceptIt();
    		pExpression();
    		accept(Token.SEMICOLON);
    	}
    	accept(Token.RCURLY);
    };
    
    /* Declarators -> 
     * 		(public | private)? static? Type
     */
    private void pDeclarators(){
    	if(verbose)
    		System.out.println("parseDeclarators");
    	// (public | private)?
    	if(currentToken.type == Token.PUBLIC
    		|| currentToken.type == Token.PRIVATE) {
    		acceptIt();
    	}
    	// static?
    	if(currentToken.type == Token.STATIC) {
    		acceptIt();
    	}
    	// Type
    	if(inTypeStarterSet(currentToken.type)) {
    		pType();
    	} else {
    		syntacticError("Declarator expected here\n"
    				+ "instead of ", currentToken.spelling);
    	}
    };
    
    /* Type ->
     * 		PrimType | ClassType | ArrType
     */
    private void pType(){
    	if(verbose)
    		System.out.println("parseType");
    	switch(currentToken.type){
    	// PrimType
    	case Token.BOOLEAN:
    	case Token.VOID:
    		acceptIt();
    		break;
    	// ClassType
    	case Token.ID:
    		acceptIt();
    		// ArrayType aka ClassType aka id[]
    		if(currentToken.type == Token.LBRACKET) {
    			acceptIt();
    			accept(Token.RBRACKET);
    		}
    		break;
    	// PrimType INT
    	case Token.INT:
    		acceptIt();
    		// ArrType INT aka int[]
    		if(currentToken.type == Token.LBRACKET){
    			acceptIt();
    			accept(Token.RBRACKET);
    		}
    		break;
    	default:
    		syntacticError("Type Declarator expected here\n"
    				+ "\t instead of ", currentToken.spelling);
    		break;
    	}
    };
    
    /* ParameterList ->
     * 		Type id (, Type id)*
     */
    private void pParameterList(){
    	if(verbose)
    		System.out.println("parseParameterList");
    	//Type id 
    	if(inTypeStarterSet(currentToken.type)) {
    		pType();
    		accept(Token.ID);
    		while(currentToken.type == Token.COMMA) {
    			acceptIt();
    			pType();
    			accept(Token.ID);
    		}
    	} else {
    		//Malformed Parameter List
    		syntacticError("Malformed Paramter list, expected Type", null);
    	}
    };
    
    /* ArgumentList ->
     * 		Expression (, Expression)*
     */
    private void pArgumentList(){
    	if(verbose)
    		System.out.println("parseArgumentList");
    	//Expression
    	if(inExpressionStarterSet(currentToken.type)) {
    		pExpression();
    		while(currentToken.type == Token.COMMA){
    			acceptIt();
    			pExpression();
    		}
    	} else {
    		//Malformed Argument List
    		syntacticError("Malformed Argument list, expected Expression", null);
    	}
    };

    /* Reference ->
     * 		BaseRef RefTail?
     */
	private void pReference() {
		if(verbose)
			System.out.println("parseReference");
		// BaseRef
		pBaseRef();
		if(inRefTailStarterSet(currentToken.type)) {
			pRefTail();
		} 
		
	};
	
	/* BaseRef ->
	 * 		this | id RefArrID?
	 */
    private void pBaseRef() {
    	if(verbose)
    		System.out.println("parseBaseReference");
    	switch(currentToken.type) {
    	case (Token.THIS):
    		acceptIt();
    		break;
    	case (Token.ID):
    		acceptIt();
    		if(inRefArrIDStarterSet(currentToken.type)) {
    			pRefArrID();
    		}
    		break;
    	default:
    		syntacticError("Malformed BaseReference\n"
    				+"\texpected 'this' or 'id', instead of", currentToken.spelling);
    	}
    };
    
    /* RefArrID ->
     * 		[ Expression? ]
     */
    private void pRefArrID() {
    	if(verbose)
    		System.out.println("parseRefArrID");
		accept(Token.LBRACKET);
		if(inExpressionStarterSet(currentToken.type)) {
			pExpression();
		}
		accept(Token.RBRACKET);
	}
    
    /* RefTail ->
     * 		. DotFollow RefTail?
     */
	private void pRefTail(){
		if(verbose)
			System.out.println("parseReferenceTail");
		accept(Token.DOT);
		pDotFollow();
		if(inRefTailStarterSet(currentToken.type)) {
			pRefTail();
		}
	};
	
	/* DotFollow ->
	 * 		id RefArrID?
	 */
    private void pDotFollow(){
    	if(verbose)
    		System.out.println("parseDotFollow");
    	accept(Token.ID);
    	if(inRefArrIDStarterSet(currentToken.type)) {
    		pRefArrID();
    	}
    };
    
    /* Statement ->
     * 		{ Statement* }
     *   |	Type id = Expression;
     *   |	Reference	SmtRefTail
     *   |	if ( Expression ) Statement (else Statement)?
     *   |	while ( Expression ) Statement
     *   |  Reference ExpRefTail? ExpTail?
     */
    private void newPStatement() {
    	if(verbose)
    		System.out.println("newPStatement");
    	switch(currentToken.type) {
    	// { Statement* }
    	case(Token.LCURLY):
    		acceptIt();
    		while(inStatementStarterSet(currentToken.type))
    			newPStatement();
    		accept(Token.RCURLY);
    		break;
    	// Derivations starting with ID
    	case(Token.ID):
    		acceptIt();
    		//Derivatons starting with ID [
    		if(currentToken.type == Token.LBRACKET) {
    			acceptIt();
    			//id[] id = Expression ;
    			if(currentToken.type == Token.RBRACKET) {
    				acceptIt();
    				accept(Token.ID);
    				accept(Token.ASSIGN);
    				pExpression();
    				accept(Token.SEMICOLON);
    			// id RefArrID RefTail? SmtRefTail
    			} else {
    				pExpression();
    				accept(Token.RBRACKET);
    				if(inRefTailStarterSet(currentToken.type)) {
    					pRefTail();
    				}
    				pSmtRefTail();
    			}
    		// Reference SmtRefTail
    		// id RefTail? SmtRefTail
    		} else if(inRefTailStarterSet(currentToken.type)) {
    			pRefTail();
				pSmtRefTail();
    		// id id = Expression ;
    		} else if(currentToken.type == Token.ID) {
    			acceptIt();
    			accept(Token.ASSIGN);
    			pExpression();
    			accept(Token.SEMICOLON);
    		// id = Expression;
    		} else if(currentToken.type == Token.ASSIGN) {
    			acceptIt();
    			pExpression();
    			accept(Token.SEMICOLON);
    		// id ( ArgumentList? ) ;
    		} else if(currentToken.type == Token.LPAREN) {
    			acceptIt();
    			if(inArgumentListStarterSet(currentToken.type))
    				pArgumentList();
    			accept(Token.RPAREN);
    			accept(Token.SEMICOLON);
    		}
    		break;
    	/* 
    	 * Variable decls and assignment
    	 */
    	// int id = Expression;
    	// int[] id = Expression;
    	case(Token.INT):
    		acceptIt();
    		if(currentToken.type == Token.LBRACKET) {
    			acceptIt();
    			accept(Token.RBRACKET);
    		}
    		accept(Token.ID);
    		accept(Token.ASSIGN);
    		pExpression();
    		accept(Token.SEMICOLON);
    		break;
    	//boolean id = Expression;
    	//void id = Expression;
    	case(Token.BOOLEAN):
    	case(Token.VOID):
    		acceptIt();
    		accept(Token.ID);
    		accept(Token.ASSIGN);
    		pExpression();
    		accept(Token.SEMICOLON);
    		break;
    	// Reference SmtRefTail
    	// this RefTail? SmtRefTail
    	case(Token.THIS):
    		acceptIt();
    		if(inRefTailStarterSet(currentToken.type))
    			pRefTail();
			pSmtRefTail();
    		break;
    	
    	// if ( Expression ) Statement (else Statement)?
    	case(Token.IF):
    		acceptIt();
    		accept(Token.LPAREN);
    		pExpression();
    		accept(Token.RPAREN);
    		newPStatement();
    		if(currentToken.type == Token.ELSE) {
    			acceptIt();
    			newPStatement();
    		}
    		break;
    	// while ( Expression ) Statement
    	case(Token.WHILE):
    		acceptIt();
    		accept(Token.LPAREN);
    		pExpression();
    		accept(Token.RPAREN);
    		newPStatement();
    		break;
    	default:
    		syntacticError("Malformed Statment.\n"
    				+ "\t error with token ", currentToken.spelling);
    		break;
    	}
    }
    /* SmtRefTail ->
     * 		= Expression ;
     * 	  | ( ArgumentList? ) ;
     */
    private void pSmtRefTail() {
    	if(verbose)
    		System.out.println("parseSmtRefTail");
    	switch(currentToken.type) {
    	// = Expression ;
    	case(Token.ASSIGN):
    		acceptIt();
    		pExpression();
    		accept(Token.SEMICOLON);
    		break;
    	// Handles a derivation from Statement
    	// and a derivation from Expression
    	// ( ArgumentList? ) ExpTail?
    	// ( ArgumentList? ) ;
    	case(Token.LPAREN):
    		acceptIt();
    		if(inArgumentListStarterSet(currentToken.type)) {
    			pArgumentList();
    		}
    		accept(Token.RPAREN);
    		accept(Token.SEMICOLON);
    		break;
    	default:
    		syntacticError("Malformed SmtRefTail\n"
    				+ "\tExpected '=' or '(', instead of ", currentToken.spelling);
    	}
	}

    /* Expression ->
     * 		Reference ExpRefTail? ExpTail?
     *    | unop Expression ExpTail?
     *    | ( Expression ) ExpTail?
     *    | num ExpTail?
     *    | true
     *    | false
     *    | new ExpDecl ExpTail? // Maybe no ExpTail here
     */
    private void pExpression(){
    	if(verbose)
    		System.out.println("parseExpression");
    	// Reference ExpRefTail? ExpTail?
    	if(inReferenceStarterSet(currentToken.type)) {
    		pReference();
    		if(inExpRefTailStarterSet(currentToken.type)) {
    			pExpRefTail();
    		}
    		if(inExpTailStarterSet(currentToken.type)) {
    			pExpTail();
    		} 
    	// unop Expression ExpTail?
    	} else if(currentToken.type == Token.MINUS || 
    			currentToken.type == Token.NOT || currentToken.type == Token.DOT) {
    		acceptIt();
    		pExpression();
    		if(inExpTailStarterSet(currentToken.type)) {
    			pExpTail();
    		}
    	// ( Expression ) ExpTail?
    	} else if(currentToken.type == Token.LPAREN) {
    		acceptIt();
    		pExpression();
    		accept(Token.LPAREN);
    		if(inExpTailStarterSet(currentToken.type)){
    			pExpTail();
    		}
    	// num ExpTail?
    	} else if(currentToken.type == Token.INTLITERAL){
    		acceptIt();
    		if(inExpTailStarterSet(currentToken.type)){
    			pExpTail();
    		}
    	// true | false
    	} else if(currentToken.type == Token.TRUE ||
    			currentToken.type == Token.FALSE) {
    		acceptIt();
    		if(inExpTailStarterSet(currentToken.type)) {
    			pExpTail();
    		}
    	// new ExpDecl ExpTail? // Maybe no ExpTail here
    	} else if(currentToken.type == Token.NEW) {
    		acceptIt();
    		pExpDecl();
    		if(inExpTailStarterSet(currentToken.type)) {
    			pExpTail();
    		}
    	} else {
    		syntacticError("Malformed Expression\n"
    				+ "\t epxression cannot begin with ", currentToken.spelling);
    	}
    };
    
    /* ExpDecl ->
     * 		int [ Expression ] 
     * 	  | id
     * 		() | [ Expression ]
     */
    private void pExpDecl() {
    	if(verbose)
    		System.out.println("parseExpDecl");
    	// int [ Expression ]
    	switch(currentToken.type) {
    	case(Token.INT):
    		acceptIt();
    		accept(Token.LBRACKET);
    		pExpression();
    		accept(Token.RBRACKET);
    		break;
    	// id ( () | [ Expression ] )
    	case(Token.ID):
    		acceptIt();
    		if(currentToken.type == Token.LPAREN) {
    			acceptIt();
    			accept(Token.RPAREN);
    		} else if(currentToken.type == Token.LBRACKET){
    			acceptIt();
    			pExpression();
    			accept(Token.RBRACKET);
    		} else {
    			syntacticError("Malformed id\n"
    				+ "\tid cannot be followed by ", currentToken.spelling);
    		}
    	}	
	}

    /* ExpRefTail ->
     * 		( ArgumentList? )
     */
	private void pExpRefTail() {
		if(verbose)
			System.out.println("parseExpRefTail");
		accept(Token.LPAREN);
		if(inArgumentListStarterSet(currentToken.type)) {
			pArgumentList();
		}
		accept(Token.RPAREN);
	}

	/* ExpTail ->
	 * 		binop Expression ExpTail?
	 */
	private void pExpTail() {
		if(verbose)
			System.out.println("parseExpTail");
		if(isBinop(currentToken.type))
			acceptIt();
		pExpression();
		/*if(inExpTailStarterSet(currentToken.type)) {
			pExpTail();
		}*/
	}

	private boolean inDeclaratorStarterSet(int type) {
    	return (type == Token.PUBLIC ||
    			type == Token.PRIVATE ||
    			type == Token.STATIC ||
    			inTypeStarterSet(type));
    }
    
    private boolean inTypeStarterSet(int type) {
    	return (type == Token.INT ||
    			type == Token.BOOLEAN ||
    			type == Token.VOID ||
    			type == Token.ID);
    }
    
    private boolean inStatementStarterSet(int type) {
    	return (type == Token.LCURLY ||
    			inTypeStarterSet(type) ||
    			inReferenceStarterSet(type) ||
    			type == Token.IF ||
    			type == Token.WHILE
    			);
    };
    
    private boolean inParameterListStarterSet(int type) {
    	return (inTypeStarterSet(type));
    }
    
    private boolean inExpressionStarterSet(int type) {
		return (inReferenceStarterSet(type) ||
				type == Token.MINUS || type == Token.NOT || //unop 
				type == Token.LPAREN ||
				type == Token.INTLITERAL ||
				type == Token.NEW
				);
	}
    
    private boolean inRefTailStarterSet(int type) {
		return (type == Token.DOT);
	}

	private boolean inBaseRefStarterSet(int type) {
		return (type == Token.THIS);
	}
	
	private boolean inRefArrIDStarterSet(int type) {
		return (type == Token.LBRACKET);
	}
	
	private boolean inArgumentListStarterSet(int type) {
		return (inExpressionStarterSet(type));
	}

	private boolean inReferenceStarterSet(int type) {
		return (inBaseRefStarterSet(type) || type == Token.ID);
	}
	private boolean inExpRefTailStarterSet(int type) {
		return (type == Token.LPAREN);
	}

	private boolean inExpTailStarterSet(int type) {
		return (isBinop(type));
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