package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.scribble.main.ScribbleException;

import ast.ScribProtocolTranslator;
import ast.global.GlobalType;
import ast.local.LocalType;
import ast.binary.Type;
import ast.name.Role;

public class Main
{
	public static void main(String[] args) throws ScribbleException
	{
		Path mainmod = Paths.get(args[0]);
		String proto = "Proto";  // Hardcoded to look for protocol named "Proto"
		
		ScribProtocolTranslator sbp = new ScribProtocolTranslator();
		
		GlobalType g = sbp.parse(mainmod, proto);
		System.out.println("Translated:\n" + "    " + g);
		
		GlobalType gs = ast.global.ops.Sanitize.apply(g);
		System.out.println("\nSanitized:\n" + "    " + gs);
		
		Map<Role, LocalType> projs = ast.global.ops.Project.apply(gs, ast.local.ops.Merge::full);
		for (Entry<Role, LocalType> rl: projs.entrySet())
		{
			LocalType l = rl.getValue();
			System.out.println("\nLocal projection for " + rl.getKey() + ":\n    " + l);
			Map<Role, Type> p = ast.local.ops.Project.apply(l, ast.binary.ops.Merge::full);
			for (Role r: l.roles())
			{
				Type b = p.get(r);
				System.out.println("Binary type towards " + r + ":\n" + "    " + b);
			}
		}
		
	}
}
