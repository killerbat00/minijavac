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
	public int start, finish, line;
	
	public SourcePosition() {
		line = 0;
		start = 0;
		finish = 0;
	}
	
	public SourcePosition(int l, int st, int fin) {
		line = l;
		start = st;
		finish = fin;
	}
	
	public SourcePosition(int start2, int end) {
		start = start2;
		finish = end;
	}

	public String toString() {
		return "Line: " + line + " (" + start + ", " + finish + ")";
	}
}
