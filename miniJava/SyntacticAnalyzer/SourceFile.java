/*
 * miniJava SourceFile
 * 
 * this class handles opening a new sourcefile for the compiler.
 * 
 * @author brian morrow
 * I pledge that I have given nor received any unauthorized help on this
 * assignment and that I have abided by all Honor Code guidelines.
 */

package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

public class SourceFile {
	public static final char eolUnix = '\n';
	public static final char eolWindows = '\r';
	public static final char EOT = '\u0000';
	
	private ErrorReporter reporter;
	
	public java.io.File sourceFile;
	public java.io.FileInputStream source;

	private boolean checkFilename(String file) {
		if(file.indexOf('.') == -1) {
			reporter.reportError("Filename has no extension");
			return false;
		} else {
			String ext = file.substring(file.indexOf('.'));
			return (ext.equals(".java") || ext.equals(".mjava"));
		}
	}
	
	public SourceFile(String filename, ErrorReporter reporter) {
		this.reporter = reporter;
		if (checkFilename(filename)) {
			try {
				sourceFile = new java.io.File(filename);
				source = new java.io.FileInputStream(sourceFile);
			} catch (java.io.IOException e) {
				reporter.reportError(e.toString());
				sourceFile = null;
				source = null;
			}
		} else {
			reporter.reportError("Bad filename: " + filename);
		}
	}
	
	public char getSource() {
		int c;
		try {
			c = source.read();
			if (c == -1) {
				c = EOT;
			}
			return (char) c;
			
		} catch (java.io.IOException e) {
			reporter.reportError(e.toString());
			return EOT;
		}
	}
}
