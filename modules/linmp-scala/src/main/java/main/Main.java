package main;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.scribble.main.ScribbleException;

import ast.ScribProtocolTranslator;
import ast.global.GlobalType;

public class Main
{
	public static void main(String[] args) throws ScribbleException
	{
		Path mainmod = Paths.get(args[0]);
		String proto = "Proto";  // Hardcoded to look for protocol named "Proto"
		
		ScribProtocolTranslator sbp = new ScribProtocolTranslator();
		
		GlobalType g = sbp.parse(mainmod, proto);
		System.out.println("Translated:\n" + g);
		
		GlobalType gs = ast.global.GlobalTypeSanitizer.apply(g);
		System.out.println("Sanitized:\n" + gs);
	}
}
