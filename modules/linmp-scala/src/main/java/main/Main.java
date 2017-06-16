package main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
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
import ast.local.ops.Merge;
// import ast.binary.Type;
import ast.name.Role;

// Args: one of
//   <-a | -junit> main-mod-path
//   -inline "..inline module def.." [proto-name]
//   main-mod-path [proto-name]
public class Main
{
	public static void main(String[] args) throws ScribbleException, ScribParserException
	{
		ScribProtocolTranslator spt = new ScribProtocolTranslator();
		Merge.Operator merge = ast.local.ops.Merge::full;

		List<GlobalType> gs = new LinkedList<>();
		boolean junit =  args[0].equals("-junit");
		try
		{
			Path mainpath = null;
			if (args[0].equals("-a") || junit)
			{
				mainpath = Paths.get(args[1]);
				gs.addAll(spt.parseAndCheckAll(Main.newMainContext(null, mainpath), merge));
			}
			else 
			{
				String inline = null;
				String simpname;
				if (args[0].equals("-inline"))
				{
					inline = args[1];
					simpname = (args.length < 3) ? "Proto" : args[2];  // Looks for protocol named "Proto" as default if unspecified
				}
				else
				{
					mainpath = Paths.get(args[0]);
					simpname = (args.length < 2) ? "Proto" : args[1];
				}
				gs.add(spt.parseAndCheck(Main.newMainContext(inline, mainpath), new GProtocolName(simpname), merge));  // merge is for projection of "delegation payload types"
			}
			for (GlobalType g : gs)
			{
				runLinMP(junit, mainpath, g, merge);
			}
		}
		catch (ScribParserException | ScribbleException e)
		{
			if (!junit)
			{
				System.err.println(e.getMessage());
				System.exit(1);
			}
		}
		// System.out.println("Translated:\n" + "    " + g);
	}
	
	private static void runLinMP(boolean junit, Path mainpath, GlobalType g, Merge.Operator merge) throws ScribbleException, ScribParserException
	{
		GlobalType gs = ast.global.ops.Sanitizer.apply(g);
		if (!junit)
		{
			System.out.println("// Global type (from " + mainpath + ")");
			System.out.println("//    " + gs + "\n");
		}
		
		Map<Role, LocalType> projs = ast.global.ops.Projector.apply(gs, merge);
		for (Entry<Role, LocalType> rl: projs.entrySet())
		{
			LocalType l = rl.getValue();
			Role r = rl.getKey();
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
			if (!junit)
			{
				System.out.println("// -----------------------------------------------------");
				System.out.println("// Local type for role " + r + ":\n//    " + l);
			}
				String scalaMPProt = ast.local.ops.ScalaEncoder.apply(l, "test.proto." + r);
			if (!junit)
			{
				System.out.println(//"// Scala protocol class definitions:\n" +
						scalaMPProt +
						"// -----------------------------------------------------\n");
			}
		}
	}

	// Duplicated from CommandLine for convenience
	// Pre: one of inline/mainpath is null
	protected static MainContext newMainContext(String inline, Path mainpath) throws ScribParserException, ScribbleException
	{
		boolean debug = false;
		boolean useOldWF = false;
		boolean noLiveness = false;
		boolean minEfsm = false;
		boolean fair = false;
		boolean noLocalChoiceSubjectCheck = false;
		boolean noAcceptCorrelationCheck = true;
		boolean noValidation = true;  // FIXME: deprecate -- redundant due to hardcoded Job.checkLinearMPScalaWellFormedness
		boolean noModuleNameCheck = true;  // For webapp to bypass MainContext.checkMainModuleName

		/*List<Path> impaths = this.args.containsKey(ArgFlag.PATH)
				? CommandLine.parseImportPaths(this.args.get(ArgFlag.PATH)[0])
				: Collections.emptyList();*/
		List<Path> impaths = Collections.emptyList();  // FIXME: get from Main args
		ResourceLocator locator = new DirectoryResourceLocator(impaths);
		return (inline == null)
				? new MainContext(debug, locator, mainpath, useOldWF, noLiveness, minEfsm, fair,
							noLocalChoiceSubjectCheck, noAcceptCorrelationCheck, noValidation, noModuleNameCheck)
				: new MainContext(debug, locator, inline, useOldWF, noLiveness, minEfsm, fair,
							noLocalChoiceSubjectCheck, noAcceptCorrelationCheck, noValidation, noModuleNameCheck);
	}
}
