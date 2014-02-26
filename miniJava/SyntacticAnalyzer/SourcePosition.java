/*
 * miniJava SourcePosition
 * 
 * This class keeps track of a token's source position
 * 
 * @author brian morrow
 * I pledge that I have given nor received any unauthorized help on this
 * assignment and that I have abided by all Honor Code guidelines.
 */
package miniJava.SyntacticAnalyzer;

public class SourcePosition {
	public int start, finish, linestart, linefinish;
	
	public SourcePosition() {
		linestart = 1;
		linefinish = 1;
		start = 1;
		finish = 1;
	}
	
	//Keeps track of Token's position
	public SourcePosition(int line, int st, int fi) {
		linestart = line;
		linefinish = 0;
		start = st;
		finish = fi;
	}
	
	//Keeps track of any AST related positioning that may span
	//multiple lines
	public SourcePosition(int ls, int lf, int s, int f) {
		linestart = ls;
		linefinish = lf;
		start = s;
		finish = f;
	}

	public String toString() {
		String lines = linefinish == 0 ? 
				"Line: " + linestart :
				"Lines: (" + linestart + "," + linefinish +")";
		return lines + " (" + start + ", " + finish + ")";
	}
}
