package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.scribble.main.MainContext;
import org.scribble.main.ScribbleException;
import org.scribble.main.resource.DirectoryResourceLocator;
import org.scribble.main.resource.ResourceLocator;
import org.scribble.sesstype.name.GProtocolName;
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
		String simpname = (args.length < 2) ? "Proto" : args[1];  // Looks for protocol named "Proto" as default if unspecified
		
		ScribProtocolTranslator sbp = new ScribProtocolTranslator();
		GlobalType g = null;
		try
		{
			//g = sbp.parseAndCheck(mainmod, proto);
			g = sbp.parseAndCheck(newMainContext(mainmod), new GProtocolName(simpname));
			System.out.println("Translated:\n" + "    " + g);
		}
		catch (ScribParserException | ScribbleException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
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

	// Duplicated from CommandLine for convenience
	private static MainContext newMainContext(Path mainmod) throws ScribParserException
	{
		boolean debug = false;
		boolean useOldWF = false;
		boolean noLiveness = false;
		boolean minEfsm = false;
		boolean fair = false;

		Path mainpath = mainmod;
		/*List<Path> impaths = this.args.containsKey(ArgFlag.PATH)
				? CommandLine.parseImportPaths(this.args.get(ArgFlag.PATH)[0])
				: Collections.emptyList();*/
		List<Path> impaths = Collections.emptyList();  // FIXME: get from Main args
		ResourceLocator locator = new DirectoryResourceLocator(impaths);
		return new MainContext(debug, locator, mainpath, useOldWF, noLiveness, minEfsm, fair);
	}
}
