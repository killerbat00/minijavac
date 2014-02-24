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
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Parser {
    private Scanner scanner;
    private Token currentToken;
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

    public Package parse() {
    	ClassDeclList cdl;
    	int pstart, pstop;
    	if(verbose)
    		System.out.println("parse()");
        currentToken = scanner.scan();
    	pstart = currentToken.position.start;
        cdl = parseProgram();
        pstop = currentToken.position.start;
        return new Package(cdl, new SourcePosition(pstart, pstop));
    }
    
    /* Program ->
     * 		(ClassDeclaration)* EOT
     */
    private ClassDeclList parseProgram(){
    	ClassDeclList cdl = new ClassDeclList();
    	ClassDecl cdAST;
    	if(verbose)
    		System.out.println("parseProgram()");
    	while(currentToken.type == Token.CLASS) {
    		cdAST = parseClassDeclaration();
    		cdl.add(cdAST);
    	}
    	accept(Token.EOT);
    	return cdl;
    };
    
    /* Class Declaration ->
     * class id {
     * 		(FieldDeclaration | MethodDeclaration)*
     * }
     */
    private ClassDecl parseClassDeclaration(){
    	if(verbose)
    		System.out.println("parseClassDeclaration");
    	ClassDecl classDeclAST;
    	FieldDeclList fieldDecList = new FieldDeclList();
    	MethodDeclList methodDecList = new MethodDeclList();
    	String className;
    
    	accept(Token.CLASS);
    	className = currentToken.spelling;
    	accept(Token.ID);
    	accept(Token.LCURLY);
    	
    	//  (FieldDeclaration | MethodDeclaration)*
    	//  (currentToken.type in starters[FieldDeclaration]
    	//  (currentToken.type in starters[MethodDeclaration]
    	while(inDeclaratorStarterSet(currentToken.type)) {
    		FieldDecl f;
    		MethodDecl m;
    		f = parseDeclarators();
    		f.name = currentToken.spelling;
    		accept(Token.ID);
    		if(currentToken.type == Token.LPAREN) {
    			m = parseMethodDeclaration(f);
    			methodDecList.add(m);
    		// FieldDeclaration
    		} else if(currentToken.type == Token.SEMICOLON){
    			fieldDecList.add(f);
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
    	classDeclAST = new ClassDecl(className, fieldDecList, methodDecList, currentToken.position);
		return classDeclAST;
    };
    
    /* MethodDeclaration ->
     * 		Declarators id ( ParameterList? ) {
     * 			Statement* (return Expression ;)?
     * 		}
     */
    private MethodDecl parseMethodDeclaration(FieldDecl f){
    	ParameterDeclList pl = new ParameterDeclList();
    	StatementList sl = new StatementList();
    	Expression e = null;
    	
    	if(verbose)
    		System.out.println("parseMethodDeclaration");
    	// We've already parsed Declarators and id
    	//    and our currentToken is LPAREN
    	accept(Token.LPAREN);
    	
    	// ParameterList?
    	if (inParameterListStarterSet(currentToken.type))
    		pl = parseParameterList();
    	
    	accept(Token.RPAREN);
    	accept(Token.LCURLY);
    	
    	// Statement*
    	while (inStatementStarterSet(currentToken.type)) {
    		sl.add(parseStatement());
    	}
    	// return statement
    	if(currentToken.type == Token.RETURN) {
    		acceptIt();
    		e = parseExpression();
    		accept(Token.SEMICOLON);
    	}
    	accept(Token.RCURLY);
    	return new MethodDecl(f, pl, sl, e, currentToken.position);
    };
    
    /* Declarators -> 
     * 		(public | private)? static? Type
     */
    private FieldDecl parseDeclarators(){
    	boolean isPriv = false;
    	boolean isStatic;
    	Type typeAST;
    	if(verbose)
    		System.out.println("parseDeclarators");
    	// (public | private)?
    	if(currentToken.type == Token.PUBLIC
    		|| currentToken.type == Token.PRIVATE) {
    		if(currentToken.type == Token.PUBLIC)
    			isPriv = false;
    		else
    			isPriv = true;
    		acceptIt();
    	}
    	// static?
    	if(currentToken.type == Token.STATIC) {
    		isStatic = true;
    		acceptIt();
    	} else {
    		isStatic = false;
    	}
    	typeAST = parseType();
    	return new FieldDecl(isPriv, isStatic, typeAST, null, currentToken.position);
    };
    
    /* Type ->
     * 		PrimType | ClassType | ArrType
     */
    private Type parseType(){
    	Type ty;
    	if(verbose)
    		System.out.println("parseType");
    	switch(currentToken.type){
    	// PrimType
    	case Token.BOOLEAN:
    		ty = new BaseType(TypeKind.BOOLEAN, currentToken.position);
    		acceptIt();
    		return ty;
    	case Token.VOID:
    		ty = new BaseType(TypeKind.VOID, currentToken.position);
    		acceptIt();
    		return ty;
    	// ClassType
    	case Token.ID:
    		String cn = currentToken.spelling;
    		Identifier classname = new Identifier(cn, currentToken.position);
    		acceptIt();
    		/*String cn = currentToken.spelling;
    		Identifier classname = new Identifier(cn, currentToken.position);*/
    		// ArrayType aka ClassType aka id[]
    		if(currentToken.type == Token.LBRACKET) {
    			acceptIt();
    			accept(Token.RBRACKET);
    			ty = new ArrayType(new ClassType(classname, currentToken.position), currentToken.position);
    		} else {
    			ty = new ClassType(classname, currentToken.position);
    		}
    		return ty;
    	// PrimType INT
    	case Token.INT:
    		acceptIt();
    		// ArrType INT aka int[]
    		if(currentToken.type == Token.LBRACKET){
    			acceptIt();
    			accept(Token.RBRACKET);
    			ty = new ArrayType(new BaseType(TypeKind.ARRAY, currentToken.position), currentToken.position);
    		} else {
    			ty = new BaseType(TypeKind.INT, currentToken.position);
    		}
    		return ty;
    	default:
    		syntacticError("Type Declarator expected here\n"
    				+ "\t instead of ", currentToken.spelling);
    		return new BaseType(TypeKind.ERROR, currentToken.position);
    	}
    };
    
    /* ParameterList ->
     * 		Type id (, Type id)*
     */
    private ParameterDeclList parseParameterList(){
    	ParameterDeclList pdl = new ParameterDeclList();
    	Type t;
    	if(verbose)
    		System.out.println("parseParameterList");
    	//Type id 
    	t = parseType();
    	pdl.add(new ParameterDecl(t, currentToken.spelling, currentToken.position));
    	accept(Token.ID);
    	while(currentToken.type == Token.COMMA) {
    		Type t1;
    		acceptIt();
    		t1 = parseType();
    		pdl.add(new ParameterDecl(t1, currentToken.spelling, currentToken.position));
    		accept(Token.ID);
    	}
    	return pdl;
    };
    
    /* ArgumentList ->
     * 		Expression (, Expression)*
     */
    private ExprList parseArgumentList(){
    	if(verbose)
    		System.out.println("parseArgumentList");
    	ExprList exprlist = new ExprList();
    	Expression e;
    	//Expression
    	if(inExpressionStarterSet(currentToken.type)) {
    		e = parseExpression();
    		exprlist.add(e);
    		while(currentToken.type == Token.COMMA){
    			acceptIt();
    			e = parseExpression();
    			exprlist.add(e);
    		}
    	} else {
    		//Malformed Argument List
    		syntacticError("Malformed Argument list, expected Expression", null);
    	}
		return exprlist;
    };

    /* Reference ->
     * 		BaseRef RefTail?
     */
	private Reference parseReference() {
		Reference r;
		if(verbose)
			System.out.println("parseReference");
		// BaseRef
		r = parseBaseRef();
		if(inRefTailStarterSet(currentToken.type)) {
			return parseRefTail(r);
		} else {
			return r;
		}
	};
	
	/* BaseRef ->
	 * 		this | id RefArrID?
	 */
    private Reference parseBaseRef() {
    	if(verbose)
    		System.out.println("parseBaseReference");
    	switch(currentToken.type) {
    	case (Token.THIS):
    		int thisstart = currentToken.position.start;
    		int thisend;
    		acceptIt();
    		thisend = currentToken.position.start;
    		return new ThisRef(new SourcePosition(thisstart, thisend));
		case (Token.ID):
			int idstart = currentToken.position.start;
			int idend;
			String name = currentToken.spelling;
    		acceptIt();
    		idend = currentToken.position.start;
    		Identifier id = new Identifier(name, new SourcePosition(idstart, idend));
    		IdRef rid = new IdRef(id, id.posn);
    		if(inRefArrIDStarterSet(currentToken.type)) {
    			return parseRefArrID(rid);
    		} else {
    			return rid;
    		}
    	default:
    		syntacticError("Malformed BaseReference\n"
    				+"\texpected 'this' or 'id', instead of", currentToken.spelling);
    		// bad
    		System.exit(4);
    		return null;
    	}
    };
    
    /* RefArrID ->
     * 		[ Expression ]
     */
    //IndexedRef
    private IndexedRef parseRefArrID(Reference ref) {
    	//rewind position to beginning of ref (really id)
    	int irefstart = currentToken.position.start - currentToken.spelling.length();
    	int irefend;
    	Expression expr;
    	if(verbose)
    		System.out.println("parseRefArrID");
		accept(Token.LBRACKET);
		expr = parseExpression();
		accept(Token.RBRACKET);
		irefend = currentToken.position.start;
		return new IndexedRef(ref, expr, new SourcePosition(irefstart,irefend));
	}
    
    /* RefTail ->
     * 		. DotFollow RefTail?
     */
	private Reference parseRefTail(Reference ref){
		Reference r;
		if(verbose)
			System.out.println("parseReferenceTail");
		accept(Token.DOT);
		r = parseDotFollow(ref);
		if(inRefTailStarterSet(currentToken.type)) {
			return parseRefTail(r);
		} else {
			return r;
		}
	};
	
	/* DotFollow ->
	 * 		id RefArrID?
	 */
    private Reference parseDotFollow(Reference r){
    	IdRef id;
    	if(verbose)
    		System.out.println("parseDotFollow");
    	int idstart = currentToken.position.start;
    	String name = currentToken.spelling;
    	accept(Token.ID);
    	int idend = currentToken.position.start;
    	Identifier i = new Identifier(name, new SourcePosition(idstart, idend));
    	id = new IdRef(i, i.posn);
    	//Something funky with names here
    	if(inRefArrIDStarterSet(currentToken.type)) {
    		return parseRefArrID(id);
    	} else {
    		return new QualifiedRef(r, i, i.posn);
    	}
    };
    
    /* Statement ->
     * 		{ Statement* }
     *   |	Type id = Expression;
     *   |	Reference	SmtRefTail
     *   |	if ( Expression ) Statement (else Statement)?
     *   |	while ( Expression ) Statement
     *   |  Reference ExparseRefTail? ExpTail?
     */
    private Statement parseStatement() {
    	if(verbose)
    		System.out.println("parseStatement");
    	switch(currentToken.type) {
    	// { Statement* }
    	case(Token.LCURLY):
    		StatementList bsl = new StatementList();
    		acceptIt();
    		while(inStatementStarterSet(currentToken.type))
    			bsl.add(parseStatement());
    		accept(Token.RCURLY);
    		return new BlockStmt(bsl, currentToken.position);
    	// Derivations starting with ID
    	case(Token.ID):
    		int ididstart = currentToken.position.start;
    		int iddeclstart = currentToken.position.start;
    		int ididend, iddeclend;
    		Identifier id;
    		IdRef iref;
    		Expression exp;
    		VarDecl vardec;
    		String name = currentToken.spelling;
    		acceptIt();
    		//Derivations starting with ID [
    		if(currentToken.type == Token.LBRACKET) {
    			acceptIt();
    			//VarDecl statement
    			//Type id = Expression ;
    			//id[] id = Expression ;
    			if(currentToken.type == Token.RBRACKET) {
    				acceptIt();
    				ididend = currentToken.position.start;
    				SourcePosition posn = new SourcePosition(ididstart, ididend);
    				Identifier classid = new Identifier(name, posn);
    				ArrayType intarrtype = new ArrayType(new ClassType(classid, posn), posn);
    				name = currentToken.spelling;
    				accept(Token.ID);
    				accept(Token.ASSIGN);
    				exp = parseExpression();
    				accept(Token.SEMICOLON);
    				iddeclend = currentToken.position.start;
    				vardec = new VarDecl(intarrtype, name, intarrtype.posn);
    				return new VarDeclStmt(vardec, exp, new SourcePosition(iddeclstart, iddeclend));
    			// Reference SmtRefTail
    			// id RefArrID RefTail? SmtRefTail
    			} else {
    				//Indexed Ref
    				IndexedRef indexref;
    				exp = parseExpression();
    				accept(Token.RBRACKET);
    				ididend = currentToken.position.start;
    				id = new Identifier(name, new SourcePosition(ididstart, ididend));
    				iref = new IdRef(id, id.posn);
    				indexref = new IndexedRef(iref, exp, exp.posn);
    				
    				//Qualified Ref
    				if(inRefTailStarterSet(currentToken.type)) {
    					Reference qrr;
    					ididend = currentToken.position.start;
    					qrr = parseRefTail(indexref);
    					
    					return parseSmtRefTail(qrr);
    				} else {
    					//AssignStatement || Call Statement
    					return parseSmtRefTail(indexref);
    				}
    			}
    		// Reference SmtRefTail
    		// id RefTail SmtRefTail
    		} else if(inRefTailStarterSet(currentToken.type)) {
    			QualifiedRef qual;
    			Reference qra;
    			ididend = currentToken.position.start;
    			id = new Identifier(name, new SourcePosition(ididstart, ididend));
    			iref = new IdRef(id, id.posn);
    			qra = parseRefTail(iref);
    			qual = new QualifiedRef(qra, id, iref.posn);
				return parseSmtRefTail(qual);
			// Type id = Expression ;
    		// id id = Expression ;
    		} else if(currentToken.type == Token.ID) {
    			String varname = currentToken.spelling;
    			acceptIt();
    			ididend = currentToken.position.start;
    			SourcePosition posn = new SourcePosition(ididstart, ididend);
    			id = new Identifier(name, posn);
    			ClassType classtype = new ClassType(id, id.posn);
    			accept(Token.ASSIGN);
    			exp = parseExpression();
    			accept(Token.SEMICOLON);
    			iddeclend = currentToken.position.start;
    			vardec = new VarDecl(classtype, varname, classtype.posn);
    			return new VarDeclStmt(vardec, exp, new SourcePosition(iddeclstart, iddeclend));
    		//Assign statement
    		// Reference SmtRefTail
    		// id = Expression;
    		} else if(currentToken.type == Token.ASSIGN) {
    			ididend = currentToken.position.start;
    			id = new Identifier(name, new SourcePosition(ididstart, ididend));
    			iref = new IdRef(id, id.posn);
    			acceptIt();
    			exp = parseExpression();
    			accept(Token.SEMICOLON);
    			iddeclend = currentToken.position.start;
    			return new AssignStmt(iref, exp, new SourcePosition(iddeclstart, iddeclend));
    		// Reference SmtRefTail
    		// id ( ArgumentList? ) ;
    		} else if(currentToken.type == Token.LPAREN) {
    			ExprList exprlist = new ExprList();
    			ididend = currentToken.position.start;
    			id = new Identifier(name, new SourcePosition(ididstart, ididend));
    			iref = new IdRef(id, id.posn);
    			acceptIt();
    			if(inArgumentListStarterSet(currentToken.type))
    				exprlist = parseArgumentList();
    			accept(Token.RPAREN);
    			accept(Token.SEMICOLON);
    			iddeclend = currentToken.position.start;
    			return new CallStmt(iref, exprlist, new SourcePosition(iddeclstart, iddeclend));
    		}
    	/* 
    	 * Variable decls and assignment
    	 */
    	// Type id = Expression ;
    	// int id = Expression;
    	// int[] id = Expression;
    	case(Token.INT):
    		String intname;
    		Type integer;
    		Expression intexp;
    		BaseType intbt;
    		VarDecl intvd;
    		int inttypestart = currentToken.position.start;
    		int inttypeend;
    		int intdeclend;
    		acceptIt();
    		
    		if(currentToken.type == Token.LBRACKET) {
    			acceptIt();
    			accept(Token.RBRACKET);
    			inttypeend = currentToken.position.start;
    			intbt = new BaseType(TypeKind.INT, new SourcePosition(inttypestart, inttypeend));
    			integer = new ArrayType(intbt, intbt.posn);
    		} else {
    			inttypeend = currentToken.position.start;
    			integer = new BaseType(TypeKind.INT, new SourcePosition(inttypestart, inttypeend));
    		}
    		intname = currentToken.spelling;
    		accept(Token.ID);
    		accept(Token.ASSIGN);
    		intexp = parseExpression();
    		accept(Token.SEMICOLON);
    		intdeclend = currentToken.position.start;
    		intvd = new VarDecl(integer, intname, new SourcePosition(inttypestart, intdeclend));
    		return new VarDeclStmt(intvd, intexp, intvd.posn);
    	//boolean id = Expression;
    	//void id = Expression;
    	case(Token.BOOLEAN):
    	case(Token.VOID):
    		String bvtypename;
    		BaseType bv;
    		Expression bvexp;
    		VarDecl bvvd;
    		SourcePosition bvtypepos = new SourcePosition();
    		int bvtypestart = currentToken.position.start, bvdeclstart = currentToken.position.start;
    		int bvtypeend, bvdeclend;
    		if(currentToken.type == Token.BOOLEAN) {
    			acceptIt();
    			bvtypeend = currentToken.position.start;
    			bvtypepos.start = bvtypestart;
    			bvtypepos.finish = bvtypeend;
    			bv = new BaseType(TypeKind.BOOLEAN, bvtypepos);
    		} else {
    			acceptIt();
    			bvtypeend = currentToken.position.start;
    			bvtypepos.start = bvtypestart;
    			bvtypepos.finish = bvtypeend;
    			bv = new BaseType(TypeKind.VOID, bvtypepos);
    		}
    		bvtypename = currentToken.spelling;
    		accept(Token.ID);
    		accept(Token.ASSIGN);
    		bvexp = parseExpression();
    		accept(Token.SEMICOLON);
    		bvdeclend = currentToken.position.start;
    		bvvd = new VarDecl(bv, bvtypename, new SourcePosition(bvdeclstart, bvdeclend));
    		return new VarDeclStmt(bvvd, bvexp, bvvd.posn);
    	// Reference SmtRefTail
    	// this RefTail? SmtRefTail
    	case(Token.THIS):
    		Reference qr;
    		int startThis = currentToken.position.start;
    		acceptIt();
    		int endThis = currentToken.position.finish;
    		ThisRef tr = new ThisRef(new SourcePosition(startThis, endThis));
    		if(inRefTailStarterSet(currentToken.type)) {
    			qr = parseRefTail(tr);
    			return parseSmtRefTail(qr);
    		} else {
    				return parseSmtRefTail(tr);
    		}
    	
    	// if ( Expression ) Statement (else Statement)?
    	case(Token.IF):
    		Expression ifexp;
    		Statement ifstmt;
    		
    		acceptIt();
    		accept(Token.LPAREN);
    		ifexp = parseExpression();
    		accept(Token.RPAREN);
    		ifstmt = parseStatement();
    		if(currentToken.type == Token.ELSE) {
    			Statement e;
    			acceptIt();
    			e = parseStatement();
    			return new IfStmt(ifexp, ifstmt, e, currentToken.position);
    		} else {
    			return new IfStmt(ifexp, ifstmt, currentToken.position);
    		}
    	// while ( Expression ) Statement
    	case(Token.WHILE):
    		int start = currentToken.position.start;
    		int end;
    		Expression whilexp;
    		Statement whilestmt;
    		
    		acceptIt();
    		accept(Token.LPAREN);
    		whilexp = parseExpression();
    		accept(Token.RPAREN);
    		whilestmt = parseStatement();
    		end = currentToken.position.start;
    		return new WhileStmt(whilexp, whilestmt, new SourcePosition(start, end));
    	default:
    		syntacticError("Malformed Statment.\n"
    				+ "\t error with token ", currentToken.spelling);
    		return null;
    	}
    }
    /* SmtRefTail ->
     * 		= Expression ;
     * 	  | ( ArgumentList? ) ;
     */
    private Statement parseSmtRefTail(Reference ref) {
    	if(verbose)
    		System.out.println("parseSmtRefTail");
    	Expression reftailexp;
    	int strt = currentToken.position.start - currentToken.spelling.length();
    	int assignend, callend;
    	switch(currentToken.type) {
    	// = Expression ;
    	case(Token.ASSIGN):
    		acceptIt();
    		reftailexp = parseExpression();
    		accept(Token.SEMICOLON);
    		assignend = currentToken.position.start;
    		return new AssignStmt(ref, reftailexp, new SourcePosition(strt,assignend));
    	// ( ArgumentList? ) ;
    	case(Token.LPAREN):
    		ExprList args = new ExprList();
    		acceptIt();
    		if(inArgumentListStarterSet(currentToken.type)) {
    			args = parseArgumentList();
    		}
    		accept(Token.RPAREN);
    		accept(Token.SEMICOLON);
    		callend = currentToken.position.start;
    		return new CallStmt(ref, args, new SourcePosition(strt,callend));
    	default:
    		syntacticError("Malformed SmtRefTail\n"
    				+ "\tExpected '=' or '(', instead of ", currentToken.spelling);
    		//bad
    		System.exit(4);
    		return null;
    	}
	}

    /* Expression ->
     * 		Reference ExparseRefTail? ExpTail?
     *    | unop Expression ExpTail?
     *    | ( Expression ) ExpTail?
     *    | num ExpTail?
     *    | true
     *    | false
     *    | new ExpDecl ExpTail? // Maybe no ExpTail here
     */
    private Expression parseExpression(){
    	if(verbose)
    		System.out.println("parseExpression");
    	// Reference ExparseRefTail? ExpTail?
    	if(inReferenceStarterSet(currentToken.type)) {
    		Reference r1;
    		Expression e1;
    		Expression e2;
    		RefExpr rxp;
    		r1 = parseReference();
    		rxp = new RefExpr(r1, r1.posn);
    		if(inExparseRefTailStarterSet(currentToken.type)) {
    			e1 = parseExparseRefTail(r1);
    		} else {
    			e1 = rxp;
    		}
    		//Operator precedence
    		//
    		if(inExpTailStarterSet(currentToken.type)) {
    			e2 = parseExpTail(e1);
    		} else {
    			e2 = e1;
    		}
    		return e2;
    	// unop Expression
    	} else if(currentToken.type == Token.MINUS || 
    			currentToken.type == Token.NOT) {
    		Expression e1;
    		Token op = currentToken;
    		Operator o;
    		int opstart, opend, exprend;
    		opstart = currentToken.position.start;
    		acceptIt();
    		opend = currentToken.position.start;
    		o = new Operator(op, new SourcePosition(opstart, opend));
    		e1 = parseExpression();
    		exprend = currentToken.position.start;
    		return new UnaryExpr(o, e1, new SourcePosition(opstart, exprend));
    	// ( Expression ) ExpTail?
    	} else if(currentToken.type == Token.LPAREN) {
    		Expression e;
    		acceptIt();
    		e = parseExpression();
    		accept(Token.RPAREN);
    		if(inExpTailStarterSet(currentToken.type)){
    			return parseExpTail(e);
    		} else {
    			return e;
    		}
    	// num ExpTail?
    	} else if(currentToken.type == Token.INTLITERAL){
    		IntLiteral numlit;
    		String name = currentToken.spelling;
    		int litstart, litstop;
    		litstart = currentToken.position.start;
    		acceptIt();
    		litstop = currentToken.position.start;
    		numlit = new IntLiteral(name, new SourcePosition(litstart, litstop));
    		if(inExpTailStarterSet(currentToken.type)){
    			return parseExpTail(new LiteralExpr(numlit, numlit.posn));
    		} else {
    			return new LiteralExpr(numlit, numlit.posn);
    		}
    	// true | false ExpTail?
    	} else if(currentToken.type == Token.TRUE ||
    			currentToken.type == Token.FALSE) {
    		BooleanLiteral bl;
    		int bstart, bstop;
    		String name = currentToken.spelling;
    		bstart = currentToken.position.start;
    		acceptIt();
    		bstop = currentToken.position.start;
    		bl = new BooleanLiteral(name, new SourcePosition(bstart, bstop));
    		if(inExpTailStarterSet(currentToken.type)) {
    			return parseExpTail(new LiteralExpr(bl, bl.posn));
    		} else {
    			return new LiteralExpr(bl, bl.posn);
    		}
    	// new ExpDecl ExpTail? // Maybe no ExpTail here
    	} else if(currentToken.type == Token.NEW) {
    		Expression e;
    		acceptIt();
    		e = parseExpDecl();
    		if(inExpTailStarterSet(currentToken.type)) {
    			return parseExpTail(e);
    		} else {
    			return e;
    		}
    	} else {
    		syntacticError("Malformed Expression\n"
    				+ "\t epxression cannot begin with ", currentToken.spelling);
    	}
		return null;
    };
    
    /* ExpDecl ->
     * 		int [ Expression ] 
     * 	  | id
     * 		() | [ Expression ]
     */
    private NewExpr parseExpDecl() {
    	int nwxpstart, nwxpstop;
    	nwxpstart = currentToken.position.start;
    	if(verbose)
    		System.out.println("parseExpDecl");
    	// NewArrayExpr
    	// int [ Expression ]
    	switch(currentToken.type) {
    	case(Token.INT):
    		BaseType inttype;
    		Expression intexp;
    		int stint, stopint;
    		stint = currentToken.position.start;
    		acceptIt();
    		stopint = currentToken.position.start;
    		inttype = new BaseType(TypeKind.INT, new SourcePosition(stint, stopint));
    		accept(Token.LBRACKET);
    		intexp = parseExpression();
    		accept(Token.RBRACKET);
    		nwxpstop = currentToken.position.start;
    		return new NewArrayExpr(inttype, intexp, new SourcePosition(nwxpstart, nwxpstop));
    	// NewObjectExpr
    	// id ( () | [ Expression ] )
    	case(Token.ID):
    		ClassType ct;
    		Identifier ci;
    		String cname = currentToken.spelling;
    		int ctstart, ctstop, expstop;
    		ctstart = currentToken.position.start;
    		acceptIt();
    		ctstop = currentToken.position.start;
    		ci = new Identifier(cname, new SourcePosition(ctstart, ctstop));
    		ct = new ClassType(ci, ci.posn);
    		// NewObjectExpr
    		if(currentToken.type == Token.LPAREN) {
    			acceptIt();
    			accept(Token.RPAREN);
    			expstop = currentToken.position.start;
    			return new NewObjectExpr(ct, new SourcePosition(ctstart, expstop));
    		// NewArrayExpr
    		} else if(currentToken.type == Token.LBRACKET){
    			Expression e;
    			acceptIt();
    			e = parseExpression();
    			accept(Token.RBRACKET);
    			expstop = currentToken.position.start;
    			return new NewArrayExpr(ct, e, new SourcePosition(ctstart, expstop));
    		} else {
    			syntacticError("Malformed id\n"
    				+ "\tid cannot be followed by ", currentToken.spelling);
    			//bad
    			System.exit(4);
    			return null;
    		}
    	default:
    		syntacticError("Malformed id\n"
    				+ "\tid cannot be followed by ", currentToken.spelling);
    		//bad
    		System.exit(4);
    		return null;
    	}	
	}

    /* ExparseRefTail ->
     * 		( ArgumentList? )
     */
	private CallExpr parseExparseRefTail(Reference r) {
		ExprList el = new ExprList();
		int cst, stop;
		cst = currentToken.position.start;
		if(verbose)
			System.out.println("parseExparseRefTail");
		accept(Token.LPAREN);
		if(inArgumentListStarterSet(currentToken.type)) {
			el = parseArgumentList();
		}
		accept(Token.RPAREN);
		stop = currentToken.position.start;
		return new CallExpr(r, el, new SourcePosition(cst, stop));
	}

	/* ExpTail ->
	 * 		binop Expression ExpTail?
	 */
	private BinaryExpr parseExpTail(Expression e1) {
		Operator o;
		Token tk;
		Expression e2;
		int estart, estop;
		estart = currentToken.position.start - currentToken.spelling.length();
		int ostart, ostop;
		if(verbose)
			System.out.println("parseExpTail");
		//if(isBinop(currentToken.type)) {
		//Here comes operator precedence
		//Highest to lowest
		// *, /
		// +, -
		// <=, <, >, >=
		// ==, !=
		// &&
		// ||
		if(currentToken.type == Token.OR) {
			ostart = currentToken.position.start;
			tk = currentToken;
			acceptIt();
			ostop = currentToken.position.start;
			o = new Operator(tk, new SourcePosition(ostart, ostop));
			e2 = parseExpression();
			estop = currentToken.position.start;
			return new BinaryExpr(o, e1, e2, new SourcePosition(estart, estop));
		}
		else if(currentToken.type == Token.TIMES || currentToken.type == Token.DIV) {
			ostart = currentToken.position.start;
			tk = currentToken;
			acceptIt();
			ostop = currentToken.position.start;
			o = new Operator(tk, new SourcePosition(ostart, ostop));
			e2 = parseExpression();
			estop = currentToken.position.start;
			return new BinaryExpr(o, e1, e2, new SourcePosition(estart, estop));
		} else {
			ostart = currentToken.position.start;
			tk = currentToken;
			acceptIt();
			ostop = currentToken.position.start;
			o = new Operator(tk, new SourcePosition(ostart, ostop));
			e2 = parseExpression();
			estop = currentToken.position.start;
			return new BinaryExpr(o, e1, e2, new SourcePosition(estart, estop));
		}
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
	private boolean inExparseRefTailStarterSet(int type) {
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