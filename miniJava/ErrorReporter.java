/*
 * miniJava ErrorReporter
 * 
 * serves as the ErrorReporter utility for our compiler
 * 
 * @author brian morrow
 * I pledge that I have given nor received any unauthorized help on this
 * assignment and that I have abided by all Honor Code guidelines.
 */
package miniJava;

import java.util.ArrayList;

public class ErrorReporter {
	private int numErrors;
	public ArrayList<String> errors = new ArrayList<String>(); 
	
	public ErrorReporter() {
		numErrors = 0;
	}
	
	public void reportError(String msg) {
		errors.add(msg);
		numErrors++;
	}
	
	public boolean hasErrors() {
		return (numErrors > 0);
	}
	
	public int getNumErrors() {
		return numErrors;
	}

}
