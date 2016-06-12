package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.scribble.main.ScribbleException;
import org.scribble.util.ScribParserException;

import ast.ScribProtocolTranslator;
import ast.global.GlobalType;
import ast.local.LocalType;
// import ast.binary.Type;
import ast.name.Role;

public class Main
{
	public static void main(String[] args) throws ScribbleException, ScribParserException
	{
		Path mainmod = Paths.get(args[0]);
		String proto = "Proto";  // Hardcoded to look for protocol named "Proto"
		
		ScribProtocolTranslator sbp = new ScribProtocolTranslator();
		try
		{
			GlobalType g = sbp.parseAndCheck(mainmod, proto);
			System.out.println("Translated:\n" + "    " + g);
			
			GlobalType gs = ast.global.ops.Sanitizer.apply(g);
			System.out.println("\nSanitized:\n" + "    " + gs);
			
			Map<Role, LocalType> projs = ast.global.ops.Projector.apply(gs, ast.local.ops.Merge::full);
			for (Entry<Role, LocalType> rl: projs.entrySet())
			{
				LocalType l = rl.getValue();
				System.out.println("\nLocal projection for " + rl.getKey() + ":\n    " + l);
	//			Map<Role, Type> p = ast.local.ops.Projector.apply(l, ast.binary.ops.Merge::full);
	//			for (Role r: l.roles())
	//			{
	//				Type b = p.get(r);
	//				System.out.println("Binary type towards " + r + ":\n    " + b);
	//				ast.linear.Type bl = ast.binary.ops.LinearEncoder.apply(b);
	//				System.out.println("    Linear encoding:\n        " + bl);
	//				String scalaProt = ast.linear.ops.ScalaProtocolExtractor.apply(bl);
	//				System.out.println("    Scala protocol classes:\n" + scalaProt);
	//			}
				String scalaMPProt = ast.local.ops.ScalaProtocolExtractor.apply(l);
				System.out.println("    Scala protocol classes for local type:\n" + scalaMPProt);
			}
		}
		catch (ScribParserException | ScribbleException e)
		{
			System.err.println(e.getMessage());
		}
	}
}
