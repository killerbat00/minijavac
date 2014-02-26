/*
 * miniJava Parser
 * 
 * this class parses tokens to determine syntactic correctness
 * it then builds an abstract syntax tree of these tokens.
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

    void syntacticError(String tmpl, String tk) {
        SourcePosition pos = currentToken.position;
        String msg = tk != null ? 
        	tmpl + " " + tk + " " + pos : // include token name
        	tmpl + " " + pos; // without token name
        errorReporter.reportError(msg);
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
        	syntacticError(Token.spell(tokenExpected) + " expected, instead of '" +
        		currentToken.spelling + "'", null);
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
        position.linestart = currentToken.position.linestart;
    }

    // finish records the position of last char of last token of phrase
    void finish(SourcePosition position) {
        position.finish = previousTokenPosition.finish;
        position.linefinish = previousTokenPosition.linestart;
    }

    public Package parse() {
    	ClassDeclList cdl;
    	SourcePosition pkgpos = new SourcePosition();
    	
    	if(verbose) System.out.println("parse()");
        currentToken = scanner.scan();
    	start(pkgpos);
        cdl = parseProgram();
        finish(pkgpos);
        return new Package(cdl, pkgpos);
    }
    
    /*
     * Program -> (ClassDeclaration)* EOT
     */
    private ClassDeclList parseProgram() {
    	ClassDeclList cdl = new ClassDeclList();
    	ClassDecl cdAST;
    	
    	if(verbose) System.out.println("parseProgram()");
    	while(currentToken.type == Token.CLASS) {
    		cdAST = parseClassDeclaration();
    		cdl.add(cdAST);
    	}
    	accept(Token.EOT);
    	return cdl;
    }
    
    /*
     * Class Declaration -> class id {
     * 		(FieldDeclaration | MethodDeclaration)*
     * }
     */
    private ClassDecl parseClassDeclaration() {
    	FieldDeclList fdl = new FieldDeclList();
    	MethodDeclList mdl = new MethodDeclList();
    	SourcePosition classpos = new SourcePosition();
    	String className;
    	
    	if(verbose) System.out.println("parseClassDeclaration()");
    	start(classpos);
    	accept(Token.CLASS);
    	className = currentToken.spelling;
    	accept(Token.ID);
    	accept(Token.LCURLY);
    	// (FieldDeclaration | MethodDeclaration)*
    	while(inDeclaratorStarterSet(currentToken.type)) {
    		FieldDecl f;
    		MethodDecl m;
    		f = parseDeclarators();
    		f.name = currentToken.spelling;
    		accept(Token.ID);
    		if(currentToken.type == Token.LPAREN) {
    			m = parseMethodDeclaration(f);
    			mdl.add(m);
    		} else if(currentToken.type == Token.SEMICOLON) {
    			fdl.add(f);
    			acceptIt();
    		} else {
    			syntacticError("'(' or ';' expected\n "+
    					"instead of ", currentToken.spelling);
    		}
    	}
    	/* 
    	 * Class declaration body
    	 * 		can be empty -> accept }
    	 */
    	if(currentToken.type == Token.RCURLY) {
    		acceptIt();
    	} else {
    		syntacticError("Empty class declaration not empty.\n\t'"+
    				currentToken.spelling + "' not allowed.", null);
    	}
    	finish(classpos);
    	return new ClassDecl(className, fdl, mdl, classpos);
    };
    
    /* 
     * MethodDeclaration ->
     * 		Declarators id ( ParameterList? ) {
     * 			Statement* (return Expression ;)?
     * 		}
     */
    private MethodDecl parseMethodDeclaration(FieldDecl f) {
    	ParameterDeclList pdl = new ParameterDeclList();
    	StatementList sl = new StatementList();
    	SourcePosition mthddeclpos = f.posn;
    	Expression e = null;
    	
    	if(verbose) System.out.println("parseMethodDeclaration");
    	// previously parsed Declarators & id
    	// currentToken == LPAREN
    	accept(Token.LPAREN);
    	if(inParameterListStarterSet(currentToken.type))
    		pdl = parseParameterList();
    	accept(Token.RPAREN);
    	accept(Token.LCURLY);
    	
    	while(inStatementStarterSet(currentToken.type))
    		sl.add(parseStatement());
    	
    	if(currentToken.type == Token.RETURN) {
    		acceptIt();
    		e = parseExpression();
    		accept(Token.SEMICOLON);
    	}
    	accept(Token.RCURLY);
    	finish(mthddeclpos);
    	return new MethodDecl(f, pdl, sl, e, mthddeclpos);
    }
    
    /* Declarators -> 
     * 		(public | private)? static? Type
     */
    private FieldDecl parseDeclarators() {
    	boolean isPriv, isStatic;
    	isPriv = false;
    	Type typeAST;
    	SourcePosition declpos = new SourcePosition();
    	
    	if(verbose) System.out.println("parseDeclarators()");
    	start(declpos);
    	isPriv = currentToken.type == Token.PRIVATE;
    	acceptIt();
    	isStatic = currentToken.type == Token.STATIC;
    	acceptIt();
    	typeAST = parseType();
    	finish(declpos);
    	return new FieldDecl(isPriv, isStatic, typeAST, typeAST.typeKind.name(), declpos);
    }
    
    /* Type ->
     * 		PrimType | ClassType | ArrType
     */
    private Type parseType() {
    	SourcePosition typepos = new SourcePosition();
    	if(verbose) System.out.println("parseType()");
    	start(typepos);
    	switch(currentToken.type) {
    	case Token.BOOLEAN:
    		acceptIt();
    		finish(typepos);
    		return new BaseType(TypeKind.BOOLEAN, typepos);
    	case Token.VOID:
    		acceptIt();
    		finish(typepos);
    		return new BaseType(TypeKind.VOID, typepos);
    	case Token.ID:
    		String cn = currentToken.spelling;
    		Identifier classname = new Identifier(cn, currentToken.position);
    		acceptIt();
    		if(currentToken.type == Token.LBRACKET){
    			acceptIt();
    			accept(Token.RBRACKET);
    			finish(typepos);
    			return new ArrayType(
    					new ClassType(classname, classname.posn), typepos);
    		} else {
    			finish(typepos);
    			return new ClassType(classname, typepos);
    		}
    	case Token.INT:
    		acceptIt();
    		if(currentToken.type == Token.LBRACKET) {
    			acceptIt();
    			accept(Token.RBRACKET);
    			finish(typepos);
    			return new ArrayType(
    					new BaseType(TypeKind.INT, currentToken.position), typepos);
    		} else {
    			finish(typepos);
    			return new BaseType(TypeKind.INT, typepos);
    		}
    	default:
    		syntacticError("Type Declarator expected here\n\t"
    			+ "instead of ", currentToken.spelling);
    		finish(typepos);
    		return new BaseType(TypeKind.ERROR, typepos);
    	}
    }
    
    /*
     * ParameterList ->
     * 		Type id ParameterListTail*
     */
    private ParameterDeclList parseParameterList() {
    	ParameterDeclList pdl = new ParameterDeclList();
    	SourcePosition pdpos = new SourcePosition();
    	String declName;
    	start(pdpos);
    	Type t = parseType();
    	declName = currentToken.spelling;
    	accept(Token.ID);
    	finish(pdpos);
    	pdl.add(new ParameterDecl(t, declName, pdpos));
    	/*
    	 * ParameterListTail ->
    	 * 		, Type id
    	 */
    	while(currentToken.type == Token.COMMA) {
    		Type tt;
    		acceptIt();
    		start(pdpos);
    		tt = parseType();
    		declName = currentToken.spelling;
    		finish(pdpos);
    		pdl.add(new ParameterDecl(tt, declName, pdpos));
    	}
    	return pdl;
    }
    
    /* ArgumentList ->
     * 		Expression ArgumentListTail*
     */
    private ExprList parseArgumentList() {
    	ExprList el = new ExprList();
    	Expression e;
    	
    	if(verbose) System.out.println("parseArgumentList()");
    	if(inExpressionStarterSet(currentToken.type)) {
    		e = parseExpression();
    		el.add(e);
    		while(currentToken.type == Token.COMMA) {
    			acceptIt();
    			e = parseExpression();
    			el.add(e);
    		}
    	} else {
    		syntacticError("Expression expected here.\n\t"
    				+"argument list is malformed", null);
    	}
    	return el;
    }
 
    /* Reference ->
     * 		BaseRef RefTail?
     */
	private Reference parseReference() {
		Reference r;
		if(verbose) System.out.println("parseReference()");
		// BaseRef
		r = parseBaseRef();
		return (inRefTailStarterSet(currentToken.type) ? parseRefTail(r) : r);
	};
	
	/* BaseRef ->
	 * 		this | DotFollow
	 */
	private Reference parseBaseRef() {
		SourcePosition refpos = new SourcePosition();
		Identifier id;
		start(refpos);
		
		if(verbose) System.out.println("parseBaseReference()");
		switch(currentToken.type) {
		case(Token.THIS):
			SourcePosition thispos = new SourcePosition();
			start(thispos);
			acceptIt();
			finish(thispos);
			return new ThisRef(thispos);
		// DotFollow
		// id | id RefArrID
		case (Token.ID):
			SourcePosition idpos = new SourcePosition();
			String name = currentToken.spelling;
			start(idpos);
			acceptIt();
			finish(idpos);
			id = new Identifier(name, idpos);
			if(currentToken.type == Token.LBRACKET) {
				finish(refpos);
				return parseRefArrID(new IdRef(id, refpos));
			} else {
				finish(refpos);
				return new IdRef(id, refpos);
			}
		default:
			syntacticError("Malformed Reference\n\t"+
					"expected 'this' or an 'id', instead of ", currentToken.spelling);
			return null;
		}
	}
    
    /* RefArrID ->
     * 		[ Expression ]
     */
    //IndexedRef
    private IndexedRef parseRefArrID(Reference ref) {
    	SourcePosition refpos = ref.posn;
    	Expression e;
    	if(verbose) System.out.println("parseRefArrID()");
		accept(Token.LBRACKET);
		e = parseExpression();
		accept(Token.RBRACKET);
		finish(refpos);
		return new IndexedRef(ref, e, refpos);
	}
    
    /* RefTail ->
     * 		. DotFollow RefTail?
     */
	private Reference parseRefTail(Reference ref){
		Reference r;
		if(verbose) System.out.println("parseRefTail()");
		accept(Token.DOT);
		r = parseDotFollow(ref);
		if(inRefTailStarterSet(currentToken.type)) {
			return parseRefTail(r);
		} else {
			finish(ref.posn);
			r.posn = ref.posn;
			return r;
		}
	};
	
	/* DotFollow ->
	 * 		id RefArrID?
	 */
    private Reference parseDotFollow(Reference r){
    	IdRef id;
    	SourcePosition idpos = new SourcePosition();
    	String name;
    	if(verbose) System.out.println("parseDotFollow()");
    	start(idpos);
    	name = currentToken.spelling;
    	accept(Token.ID);
    	finish(idpos);
    	Identifier i = new Identifier(name, idpos);
    	id = new IdRef(i, i.posn);
    	if(inRefArrIDStarterSet(currentToken.type)) {
    		return parseRefArrID(id);
    	} else {
    		finish(r.posn);
    		return new QualifiedRef(r, i, r.posn);
    	}
    };
    /*
     * Statement ->
     * 		BlockStmt
     * 	|	VarDecl
     * 	|	Assgn
     * 	|	FuncCall
     * 	|	IfStmt
     * 	|	WhileStmt
     */
    private Statement parseStatement() {
		SourcePosition idpos = new SourcePosition();
		start(idpos);
    	if(verbose) System.out.println("parseStatement()");
    	switch(currentToken.type) {
    	case(Token.LCURLY):
    		StatementList bsl = new StatementList();
    		SourcePosition bspos = new SourcePosition();
    		start(bspos);
    		acceptIt();
    		while(inStatementStarterSet(currentToken.type))
    			bsl.add(parseStatement());
    		accept(Token.RCURLY);
    		finish(bspos);
    		return new BlockStmt(bsl, bspos);
    	case(Token.ID):
    		Identifier id, cid;
    		IdRef ir;
    		Expression e;
    		VarDecl vd;
    		String name = currentToken.spelling;
    		SourcePosition declpos = new SourcePosition();
    		start(declpos);
    		acceptIt();
    		
    		finish(idpos);
    		cid = new Identifier(name, idpos);
    		if(currentToken.type == Token.LBRACKET) {
    			acceptIt();
    			// VarDecl
    			// Type id = Expression ;
    			// id[] id = Expression ;
    			if(currentToken.type == Token.RBRACKET) {
    				acceptIt();
    				finish(idpos);
    				ArrayType cat = new ArrayType(new ClassType(cid, cid.posn), idpos);
    				name = currentToken.spelling;
    				accept(Token.ID);
    				accept(Token.ASSIGN);
    				e = parseExpression();
    				finish(idpos);
    				accept(Token.SEMICOLON);
    				vd = new VarDecl(cat, name, idpos);
    				finish(idpos);
    				return new VarDeclStmt(vd, e, idpos);
    			// Reference SmtRefTail
    			// id RefArrId RefTail? SmtRefTail
    			} else {
    				//Indexed Ref
    				IndexedRef inr;
    				e = parseExpression();
    				accept(Token.RBRACKET);
    				finish(idpos);
    				id = new Identifier(name, idpos);
    				ir = new IdRef(id, idpos);
    				inr = new IndexedRef(ir, e, idpos);
    				
    				//Qualified Ref
    				if(inRefTailStarterSet(currentToken.type)) {
    					Reference qrr = parseRefTail(inr);
    					return parseSmtRefTail(qrr);
    				} else {
    					return parseSmtRefTail(inr);
    				}
    			}
    		// Reference SmtRefTail
    		// id RefTail SmtRefTail
    		} else if(inRefTailStarterSet(currentToken.type)) {
    			QualifiedRef qr = null;
    			Reference r = null;
    			finish(idpos);
    			id = new Identifier(name, idpos);
    			ir = new IdRef(id, id.posn);
    			r = parseRefTail(ir);
    			finish(idpos);
    			qr = new QualifiedRef(r, id, idpos);
    			return parseSmtRefTail(qr);
    		// Type id = Expression ;
    		// id id = Expression ;
    		} else if(currentToken.type == Token.ID) {
    			ClassType ct = null;
    			String varname = currentToken.spelling;
    			acceptIt();
    			finish(idpos);
    			id = new Identifier(name, idpos);
    			ct = new ClassType(id, idpos);
    			accept(Token.ASSIGN);
    			e = parseExpression();
    			accept(Token.SEMICOLON);
    			finish(idpos);
    			vd = new VarDecl(ct, varname, idpos);
    			return new VarDeclStmt(vd, e, idpos);
    		// Assign Statement
    		// Reference StmRefTail
    		// id = Expression;
    		} else if(currentToken.type == Token.ASSIGN){
    			finish(idpos);
    			id = new Identifier(name, idpos);
    			ir = new IdRef(id, idpos);
    			acceptIt();
    			e = parseExpression();
    			accept(Token.SEMICOLON);
    			finish(idpos);
    			return new AssignStmt(ir, e, idpos);
    		// Reference SmtRefTail
    		// id ( ArgumentList? ) ;
    		} else if(currentToken.type == Token.LPAREN) {
    			ExprList el = new ExprList();
    			finish(idpos);
    			id = new Identifier(name, idpos);
    			ir = new IdRef(id, idpos);
    			acceptIt();
    			if(inArgumentListStarterSet(currentToken.type))
    				el = parseArgumentList();
    			accept(Token.RPAREN);
    			accept(Token.SEMICOLON);
    			finish(idpos);
    			return new CallStmt(ir, el, idpos);
    		}
    	/*
    	 * VarDecl
    	 * Type id SmtRefTail
    	 * int id = Expression;
    	 * int[] id = Expression;	
    	 */
    	case(Token.INT):
    		String iname;
    		Type i;
    		Expression e1;
    		BaseType bt = null;
    		VarDecl vd1 = null;
    		acceptIt();
    		if(currentToken.type == Token.LBRACKET){
    			acceptIt();
    			accept(Token.RBRACKET);
    			finish(idpos);
    			bt = new BaseType(TypeKind.INT, idpos);
    			i = new ArrayType(bt, idpos);
    		} else {
    			finish(idpos);
    			i = new BaseType(TypeKind.INT, idpos);
    		}
    		iname = currentToken.spelling;
    		accept(Token.ID);
    		accept(Token.ASSIGN);
    		e1 = parseExpression();
    		accept(Token.SEMICOLON);
    		finish(idpos);
    		vd1 = new VarDecl(i, iname, idpos);
    		return new VarDeclStmt(vd1, e1, idpos);
    	/*
    	 * Boolean id = Expression;
    	 * void id = Expression;
    	 */
    	case(Token.BOOLEAN):
    	case(Token.VOID):
    		String bvname;
    		BaseType bv;
    		Expression e2;
    		VarDecl vd2;
    		if(currentToken.type == Token.BOOLEAN) {
    			acceptIt();
    			finish(idpos);
    			bv = new BaseType(TypeKind.BOOLEAN, idpos);
    		} else {
    			acceptIt();
    			finish(idpos);
    			bv = new BaseType(TypeKind.VOID, idpos);
    		}
    		bvname = currentToken.spelling;
    		accept(Token.ID);
    		accept(Token.ASSIGN);
    		e2 = parseExpression();
    		accept(Token.SEMICOLON);
    		finish(idpos);
    		vd2 = new VarDecl(bv, bvname, idpos);
    		return new VarDeclStmt(vd2, e2, idpos);
    	/*
    	 * Reference SmtRefTail
    	 * this RefTail? SmtRefTail
    	 */
    	case(Token.THIS):
    		Reference r1;
    		ThisRef tr;
    		acceptIt();
    		finish(idpos);
    		tr = new ThisRef(idpos);
    		if(inRefTailStarterSet(currentToken.type)) {
    			r1 = parseRefTail(tr);
    			return parseSmtRefTail(r1);
    		} else {
    			return parseSmtRefTail(tr);
    		}
    	/*
    	 * IfStmt
    	 * if ( Expression ) Statement (else Statement)?
    	 */
    	case(Token.IF):
    		Expression ie;
    		Statement is;
    		acceptIt();
    		accept(Token.LPAREN);
    		ie = parseExpression();
    		accept(Token.RPAREN);
    		is = parseStatement();
    		if(currentToken.type == Token.ELSE){
    			Statement s;
    			acceptIt();
    			s = parseStatement();
    			finish(idpos);
    			return new IfStmt(ie, is, s, idpos);
    		} else {
    			finish(idpos);
    			return new IfStmt(ie, is, idpos);
    		}
    	// while ( Expression ) Statement
    	case(Token.WHILE):
    		Expression we;
    		Statement ws;
    		acceptIt();
    		accept(Token.LPAREN);
    		we = parseExpression();
    		accept(Token.RPAREN);
    		ws = parseStatement();
    		finish(idpos);
    		return new WhileStmt(we, ws, idpos);
    	default:
    		syntacticError("Malformed Statement.\n\t"
    				+ "Error with the token ", currentToken.spelling);
    		return null;
    	}
    }

    /* SmtRefTail ->
     * 		= Expression ;
     * 	  | ( ArgumentList? ) ;
     */
    private Statement parseSmtRefTail(Reference r) {
    	Expression re;
    	ExprList args = new ExprList();
    	SourcePosition spos = r.posn;
    	if(verbose) System.out.println("oldparseSmtRefTail");
    	switch(currentToken.type) {
    	// = Expression ;
    	case(Token.ASSIGN):
    		acceptIt();
    		re = parseExpression();
    		accept(Token.SEMICOLON);
    		finish(spos);
    		return new AssignStmt(r, re, spos);
    	// ( ArgumentList? ) ;
    	case(Token.LPAREN):
    		acceptIt();
    		if(inArgumentListStarterSet(currentToken.type)) {
    			args = parseArgumentList();
    		}
    		accept(Token.RPAREN);
    		accept(Token.SEMICOLON);
    		finish(spos);
    		return new CallStmt(r, args, spos);
    	default:
    		syntacticError("Malformed SmtRefTail\n\t"
    				+ "expected '=' or '(', instead of ", currentToken.spelling);
    		return null;
    	}
	}
    /*
     * Expression ->
     * 		A (|| Expression)*
     */
    private Expression parseExpression() {
    	Expression l, tmp;
    	Operator o;
    	SourcePosition epos = new SourcePosition();
    	start(epos);
    	if(verbose) System.out.println("parseExpression()");
    	l = parseA();
    	while(currentToken.type == Token.OR) {
    		//becomes left operator
    		o = new Operator(currentToken, currentToken.position);
    		tmp = parseExpression();
    		finish(epos);
    		l = new BinaryExpr(o, l, tmp, epos);
    	}
    	return l;
    }
    /*
     * A -> B (&& A)*
     */
    private Expression parseA() {
    	Expression l, tmp;
    	Expression r = null;
    	Operator o;
    	SourcePosition epos = new SourcePosition();
    	start(epos);
    	if(verbose) System.out.println("parseA()");
    	l = parseB();
    	while(currentToken.type == Token.AND) {
    		o = new Operator(currentToken, currentToken.position);
    		tmp = parseA();
    		finish(epos);
    		r = new BinaryExpr(o, l, tmp, epos);
    	}
    	System.out.println(r);
    	System.out.println(currentToken.spelling);
    	if(r == null)
    		return l;
    	else
    		return r;
    }
    /* 
     * B -> C (( == | != ) B)*
     */
    private Expression parseB() {
    	Expression l, tmp;
    	Expression r = null;
    	Operator o;
    	SourcePosition epos = new SourcePosition();
    	start(epos);
    	if(verbose) System.out.println("parseB()");
    	l = parseC();
    	while(currentToken.type == Token.EQUAL ||
    			currentToken.type == Token.NOTEQUAL) {
    		o = new Operator(currentToken, currentToken.position);
    		tmp = parseB();
    		finish(epos);
    		r = new BinaryExpr(o, l, tmp, epos);
    	}
    	System.out.println(r);
    	System.out.println(currentToken.spelling);
    	if(r == null)
    		return l;
    	else
    		return r;
    }
    /*
     * C -> D ((<= | < | > | >=) C)*
     */
    private Expression parseC() {
    	Expression l, tmp;
    	Expression r = null;
    	Operator o;
    	SourcePosition epos = new SourcePosition();
    	start(epos);
    	if(verbose) System.out.println("parseC()");
    	l = parseD();
    	while(currentToken.type == Token.LTEQUAL ||
    			currentToken.type == Token.LESS ||
    			currentToken.type == Token.GREATER ||
    			currentToken.type == Token.GTEQUAL) {
    		o = new Operator(currentToken, currentToken.position);
    		tmp = parseC();
    		finish(epos);
    		r = new BinaryExpr(o, l, tmp, epos);
    	}
    	System.out.println(r);
    	System.out.println(currentToken.spelling);
    	if(r == null)
    		return l;
    	else
    		return r;
    }
    /*
     * D -> E ((+ | -) D)*
     */
    private Expression parseD() {
    	Expression l, tmp;
    	Expression r = null;
    	Operator o;
    	SourcePosition epos = new SourcePosition();
    	start(epos);
    	if(verbose) System.out.println("parseD()");
    	l = parseE();
    	while(currentToken.type == Token.PLUS ||
    			currentToken.type == Token.MINUS) {
    		o = new Operator(currentToken, currentToken.position);
    		tmp = parseD();
    		finish(epos);
    		r = new BinaryExpr(o, l, tmp, epos);
    	}
    	System.out.println(r);
    	System.out.println(currentToken.spelling);
    	if(r == null)
    		return l;
    	else
    		return r;
    }
    /*
     * E -> F ((* | /) E)*
     */
    private Expression parseE() {
    	Expression l, tmp;
    	Expression r = null;
    	Operator o;
    	SourcePosition epos = new SourcePosition();
    	start(epos);
    	if(verbose) System.out.println("parseE()");
    	l = parseF();
    	while(currentToken.type == Token.TIMES ||
    			currentToken.type == Token.DIV) {
    		o = new Operator(currentToken, currentToken.position);
    		tmp = parseE();
    		finish(epos);
    		r = new BinaryExpr(o, l, tmp, epos);
    	}
    	System.out.println(r);
    	System.out.println(currentToken.spelling);
    	if(r == null)
    		return l;
    	else
    		return r;
    }
    /*
     * F ->
     * 		Reference RefExp?
     * 	|	( Expression )
     * 	|	unop Expression
     * 	|	num | true | false
     * 	|	new ExpDecl
     */
    private Expression parseF() {
    	if(verbose) System.out.println("parseF()");
    	SourcePosition epos = new SourcePosition();
    	start(epos);
    	// Reference RefExp?
    	if(inReferenceStarterSet(currentToken.type)) {
    		Reference r1 = parseReference();
    		finish(epos);
    		if(inRefExpStarterSet(currentToken.type)) {
    			return parseRefExp(r1);
    		} else {
    			return new RefExpr(r1, epos);
    		}
    	// unop Expression
    	} else if(currentToken.type == Token.MINUS || 
    			currentToken.type == Token.NOT) {
    		Expression e1;
    		Token op = currentToken;
    		Operator o = new Operator(op, op.position);
    		acceptIt();
    		e1 = parseExpression();
    		finish(epos);
    		return new UnaryExpr(o, e1, epos);
    	// ( Expression )
    	} else if(currentToken.type == Token.LPAREN) {
    		Expression e;
    		acceptIt();
    		e = parseExpression();
    		accept(Token.RPAREN);
    		return e;
    	// num
    	} else if(currentToken.type == Token.INTLITERAL){
    		IntLiteral numlit;
    		String name = currentToken.spelling;
    		numlit = new IntLiteral(name, currentToken.position);
    		acceptIt();
    		finish(epos);
    		return new LiteralExpr(numlit, epos);
    	// true | false
    	} else if(currentToken.type == Token.TRUE ||
    			currentToken.type == Token.FALSE) {
    		BooleanLiteral bl;
    		String name = currentToken.spelling;
    		bl = new BooleanLiteral(name, currentToken.position);
    		acceptIt();
    		finish(epos);
    		return new LiteralExpr(bl, epos);
    	// new ExpDecl
    	} else if(currentToken.type == Token.NEW) {
    		Expression e;
    		acceptIt();
    		e = parseExpDecl();
    		return e;
    	} else {
    		syntacticError("Malformed Expression\n"
    				+ "\t epxression cannot begin with ", currentToken.spelling);
    		return null;
    	}	
    }
    
    /* ExpDecl ->
     * 		int [ Expression ] 
     * 	  | id (() | [ Expression ])
     */
    private NewExpr parseExpDecl() {
    	SourcePosition newpos = new SourcePosition();
    	if(verbose) System.out.println("oldparseExpDecl");
    	start(newpos);
    	// NewArrayExpr
    	// int [ Expression ]
    	switch(currentToken.type) {
    	case(Token.INT):
    		BaseType it;
    		Expression ie;
    		it = new BaseType(TypeKind.INT, currentToken.position);
    		acceptIt();
    		accept(Token.LBRACKET);
    		ie = parseExpression();
    		accept(Token.RBRACKET);
    		finish(newpos);
    		return new NewArrayExpr(it, ie, newpos);
    	// NewObjectExpr
    	// id ( () | [ Expression ] )
    	case(Token.ID):
    		ClassType ct;
    		Identifier ci;
    		String cname = currentToken.spelling;
    		ci = new Identifier(cname, currentToken.position);
    		acceptIt();
    		finish(newpos);
    		ct = new ClassType(ci, newpos);
    		// NewObjectExpr
    		if(currentToken.type == Token.LPAREN) {
    			acceptIt();
    			accept(Token.RPAREN);
    			finish(newpos);
    			return new NewObjectExpr(ct, newpos);
    		// NewArrayExpr
    		} else if(currentToken.type == Token.LBRACKET){
    			Expression e;
    			acceptIt();
    			e = parseExpression();
    			accept(Token.RBRACKET);
    			finish(newpos);
    			return new NewArrayExpr(ct, e, newpos);
    		} else {
    			syntacticError("Malformed id\n"
    				+ "\tid cannot be followed by ", currentToken.spelling);
    			return null;
    		}
    	default:
    		syntacticError("Malformed id\n"
    				+ "\tid cannot be followed by ", currentToken.spelling);
    		return null;
    	}	
	}

    /* ExpRefTail ->
     * 		( ArgumentList? )
     */
	private CallExpr parseRefExp(Reference r) {
		ExprList el = new ExprList();
		SourcePosition cexpr = r.posn;
		if(verbose) System.out.println("oldparseExoldparseRefTail");
		accept(Token.LPAREN);
		if(inArgumentListStarterSet(currentToken.type)) {
			el = parseArgumentList();
		}
		accept(Token.RPAREN);
		finish(cexpr);
		return new CallExpr(r, el, cexpr);
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
    }
    
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
	private boolean inRefExpStarterSet(int type) {
		return (type == Token.LPAREN);
	}
}