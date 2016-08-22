package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.scribble.main.MainContext;
import org.scribble.main.ScribbleException;
import org.scribble.main.resource.DirectoryResourceLocator;
import org.scribble.main.resource.ResourceLocator;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.util.ScribParserException;

import ast.ScribProtocolTranslator;
import ast.global.GlobalType;
import ast.local.LocalType;
import ast.binary.Type;
import ast.name.Role;

public class BinarySessionType
{
	public static void main(String[] args) throws ScribbleException, ScribParserException
	{
		Path mainmod = Paths.get(args[0]);
		
		if (args.length < 4)
		{
			throw new IllegalArgumentException("Required arguments: <FILE> <PROTO> <ROLE1> <ROLE2>");
		}
		String simpname = args[1]; // Protocol name
		Role role1 = new Role(args[2]);  // Which role to project from global type?
		Role role2 = new Role(args[3]);  // Which role to project from local type?
		
		ScribProtocolTranslator sbp = new ScribProtocolTranslator();
		GlobalType g = null;
		try
		{
			//g = sbp.parseAndCheck(mainmod, proto);
			g = sbp.parseAndCheck(newMainContext(mainmod), new GProtocolName(simpname), ast.local.ops.Merge::full);
			//System.out.println("Translated:\n" + "    " + g);
		}
		catch (ScribParserException | ScribbleException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		GlobalType gs = ast.global.ops.Sanitizer.apply(g);
		//System.out.println("\nSanitized:\n" + "    " + gs);
		
		LocalType lt = ast.global.ops.Projector.apply(gs, role1, ast.local.ops.Merge::full);
		Type pt = ast.local.ops.Projector.apply(lt, role2, ast.binary.ops.Merge::full);
		
		System.out.println(pt);
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
