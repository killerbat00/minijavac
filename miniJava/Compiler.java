/*
 * miniJava Compiler
 * 
 * this file contains the main compilation logic for the
 * subset of miniJava
 * 
 * @author brian morrow
 * I pledge that I have given nor received any unauthorized help on this
 * assignment and that I have abided by all Honor Code guidelines.
 */
package miniJava;

import miniJava.SyntacticAnalyzer.ParserOld;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.SyntacticAnalyzer.SourceFile;

public class Compiler {
	public static ErrorReporter reporter;
	static Parser parser;
	static Scanner scanner;
	static SourceFile inputFile;
	
	public static boolean checkArgs(String[] args) {
		if (args.length > 0) {
			return true;
		} else {
			reporter.reportError("0 arguments");
			return false;
		}
	}
	
	static boolean compile(String filename) {
		reporter = new ErrorReporter();
		SourceFile source = new SourceFile(filename, reporter);
		scanner = new Scanner(source, reporter);
		parser = new Parser(scanner, reporter/*, true*/);
		parser.parse();
		return reporter.getNumErrors() == 0;
	}
	
	public static void main(String[] args) {
		if (checkArgs(args)) {
			String filename = args[0];
			if(compile(filename)) {
				System.exit(0);
			} else {
				for (String e : reporter.errors) {
					System.out.println(e);
				}
				System.exit(4);
			}
		} else {
			for (String e : reporter.errors) {
				System.out.println(e);
			}
			System.exit(4);
		}
	}
}
