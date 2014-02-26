/*
 * miniJava Scanner
 * 
 * this file contains the scanner for the miniJava
 * compiler
 * 
 * @author brian morrow
 * I pledge that I have given nor received any unauthorized help on this
 * assignment and that I have abided by all Honor Code guidelines.
 */
package miniJava.SyntacticAnalyzer;
import miniJava.SyntacticAnalyzer.SourceFile;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.ErrorReporter;

public class Scanner {
	private SourceFile sourceFile;
	private char currentChar;
	private char previousChar;
	private StringBuffer currentSpelling;
	private boolean currentlyScanningToken;
	
	private int comStart;
	private int comFinish;
	int currentCharNum = 1;
	int currentLineNum = 1;
	
	private ErrorReporter reporter;
	
	public Scanner(SourceFile source, ErrorReporter r) {
		sourceFile = source;
		reporter = r;
		currentChar = sourceFile.getSource();
	}
	
	private void consume() {
		previousChar = currentChar;
		if(currentChar == '\t')
			currentCharNum += 4;
		else
			currentCharNum++;
		
		if(currentChar == '\n' || (previousChar == '\r' && currentChar == '\n'))
			currentLineNum++;
		if(currentlyScanningToken) {
			currentSpelling.append(currentChar);
		}
		currentChar = sourceFile.getSource();
	}
	
	private int scanToken() {
		switch(currentChar) {
		case 'a': case 'b': case 'c': case 'd': case 'e':
	    case 'f': case 'g': case 'h': case 'i': case 'j':
	    case 'k': case 'l': case 'm': case 'n': case 'o':
	    case 'p': case 'q': case 'r': case 's': case 't':
	    case 'u': case 'v': case 'w': case 'x': case 'y':
	    case 'z':
	    case 'A': case 'B': case 'C': case 'D': case 'E':
	    case 'F': case 'G': case 'H': case 'I': case 'J':
	    case 'K': case 'L': case 'M': case 'N': case 'O':
	    case 'P': case 'Q': case 'R': case 'S': case 'T':
	    case 'U': case 'V': case 'W': case 'X': case 'Y':
	    case 'Z':
	    	consume();
	    	while(Character.isLetter(currentChar) ||
	    			Character.isDigit(currentChar) || currentChar == '_')
	    		consume();
	    	return Token.ID;
	    
	    case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
            consume();
            while(Character.isDigit(currentChar))
                consume();
            return Token.INTLITERAL;
            
        case '>':
        	consume();
        	if(currentChar == '=') {
        		consume();
        		return Token.GTEQUAL;
        	}
        	return Token.GREATER;
        	
        case '<':
        	consume();
        	if(currentChar == '=') {
        		consume();
        		return Token.LTEQUAL;
        	}
        	return Token.LESS;
        	
        case '=':
        	consume();
        	if(currentChar == '=') {
        		consume();
        		return Token.EQUAL;
        	}
        	return Token.ASSIGN;
      
	    case '&':
	    	consume();
	        if(currentChar != '&') {
	        	scanError("Must have 2 ampersands.");
	        }
	        consume();
	        return Token.AND;
	
	    case '|':
	    	consume();
	        if(currentChar != '|')
	        	scanError("Must have 2 | for a valid or.");
	        consume();
	        return Token.OR;
	
	    case '!':
	        consume();
	        if(currentChar == '=') {
	            consume();
	            return Token.NOTEQUAL;
	        }
	        return Token.NOT;
	
	    case '+':
	        consume();
	        return Token.PLUS;
	
	    case '-':
	        consume();
	        return Token.MINUS;
	
	    case '*':
	        consume();
	        return Token.TIMES;
	
	    case '/':
	        consume();
	        return Token.DIV;
	
	    case '.':
	        consume();
	        return Token.DOT;
	
	    case ',':
	        consume();
	        return Token.COMMA;
	
	    case ';':
	        consume();
	        return Token.SEMICOLON;
	
	    case '(':
	        consume();
	        return Token.LPAREN;
	
	    case ')':
	        consume();
	        return Token.RPAREN;
	
	    case '[':
	        consume();
	        return Token.LBRACKET;
	
	    case ']':
	        consume();
	        return Token.RBRACKET;
	
	    case '{':
	        consume();
	        return Token.LCURLY;
	
	    case '}':
	        consume();
	        return Token.RCURLY;
	
	    case SourceFile.EOT:
	        return Token.EOT;
	
	    default:
	        consume();
	        return Token.ERROR;
		}
	}
	
	private boolean scanSeparator() {
		switch(currentChar) {
		case '/':
			comStart = currentCharNum;
			consume();
			comFinish = currentCharNum;
			
			if(currentChar == '/') 
				SLComment();
			 else if(currentChar == '*') 
				MLComment();
			 else
				return true;
			break;
			
		case ' ': case '\n': case '\r': case '\t':
			consume();
			break;
		}
		return false;
	}
	
	private void SLComment() {
		while(currentChar != '\n'
				&& currentChar != '\r'
				&& currentChar != SourceFile.EOT)
			consume();
		consume();
	}
	
	private void MLComment() {
		currentChar = sourceFile.getSource();
		char nextChar = sourceFile.getSource();
		
		while(currentChar != '*' || nextChar != '/') {
			if(currentChar == SourceFile.EOT) {
				System.exit(4);
			}
			currentChar = nextChar;
			nextChar = sourceFile.getSource();
		}
		currentChar = sourceFile.getSource();
	}
	
	public Token scan() {
		Token tok;
		SourcePosition pos;
		int type;
		boolean isDiv;
		
		currentlyScanningToken = false;
		while(currentChar == '/'
				|| currentChar == ' '
				|| currentChar == '\n'
				|| currentChar == '\r'
				|| currentChar == '\t') {
			if (currentChar == '\n' || currentChar == '\r')
				currentCharNum = 0;
			isDiv = scanSeparator();
			if(isDiv) {
				pos = new SourcePosition();
				pos.start = comStart;
				pos.finish = comFinish;
				tok = new Token(Token.DIV, "/", pos);
				return tok;
			}
		}
		
		currentlyScanningToken = true;
		currentSpelling = new StringBuffer("");
		pos = new SourcePosition(currentLineNum, currentCharNum, 0);
		type = scanToken();
		pos.finish = currentCharNum;
		tok = new Token(type, currentSpelling.toString(), pos);
		return tok;
	}
	
	private void scanError(String m) {
		reporter.reportError(m);
	}
}