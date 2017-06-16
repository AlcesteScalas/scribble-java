package ast;

import java.util.LinkedList;
import java.util.List;

import org.scribble.ast.Module;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.del.ModuleDel;
import org.scribble.main.Job;
import org.scribble.main.MainContext;
import org.scribble.main.ScribbleException;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.util.ScribParserException;

import ast.global.GlobalType;
import ast.global.GlobalTypeTranslator;
import ast.local.ops.Merge;

public class ScribProtocolTranslator
{
	public ScribProtocolTranslator()
	{

	}

	public List<GlobalType> parseAndCheckAll(MainContext maincon, Merge.Operator merge) throws ScribbleException, ScribParserException
	{
		List<GlobalType> res = new LinkedList<>();
		
		Job job = maincon.newJob();
		job.checkLinearMPScalaWellFormedness();
		Module main = job.getContext().getMainModule();
		for (GProtocolDecl gpd : main.getGlobalProtocolDecls())
		{
			//if (!gpd.modifiers.contains(Modifiers.AUX))  // Should *not* skip here: linmp currently skips job.checkLinearMPScalaWellFormedness() -- so it just uses the result of (naive) inlining, for which default Scribble WF has not been checked
			                                               // So linmp needs to check aux, to detect linmp syntax errors
			                                               // CHECKME: is checking linmp syntax of all subprotos separately sufficient?  (Or should it be checked on the 
			                                               // FIXME: handle subprotos in linmp properly
			{
				res.add(parseAndCheck(maincon, gpd.getHeader().getDeclName(), merge));
			}
		}

		return res;
	}
	

	// merge is for projection of "delegation payload types"
	//public GlobalType parseAndCheck(Path mainmod, String simplename) throws ScribbleException, ScribParserException
	public GlobalType parseAndCheck(MainContext maincon, GProtocolName simplename, Merge.Operator merge) throws ScribbleException, ScribParserException
	{
		/*Module main = parseMainScribModule(mainmod);
		Job job = new Job(false, parsed, main.getFullModuleName(), false, false, false, false);*/

		//MainContext maincon = newMainContext(mainmod);
		//Job job = new Job(maincon.debug, maincon.getParsedModules(), maincon.main, maincon.useOldWF, maincon.noLiveness, maincon.minEfsm, maincon.fair);
		Job job = maincon.newJob();
		job.checkLinearMPScalaWellFormedness();
		Module main = job.getContext().getMainModule();

		if (!main.hasProtocolDecl(simplename))
		{
			throw new ScribbleException("Global protocol not found: " + simplename);
		}
		GProtocolDecl gpd = (GProtocolDecl) main.getProtocolDecl(simplename);  // FIXME: cast
		/*if (gpd.modifiers.contains(Modifiers.AUX))  // Skipping: cf. parseAndCheckAll
		{
			throw new RuntimeException("[linmp] Target protocol should not be aux: " + simplename);
		}*/
		return new GlobalTypeTranslator().translate(job.getContext(), ((ModuleDel) main.del()).getModuleContext(), merge, gpd);
	}
	
	// TODO: doesn't support Scribble module imports yet (no import path given to resource locator)
	/*private Module parseMainScribModule(Path mainmod) throws ScribbleException, ScribParserException
	{
		AntlrParser antlrParser = new AntlrParser();
		ScribParser scribParser = new ScribParser();
		//ResourceLocator locator = new DirectoryResourceLocator(Collections.emptyList()); 
		//this.loader = new ScribModuleLoader(this.locator, this.antlrParser, this.scribParser);
		Resource res = DirectoryResourceLocator.getResourceByFullPath(mainmod);
		Module main = (Module) scribParser.parse(antlrParser.parseAntlrTree(res));
		Map<ModuleName, Module> parsed = new HashMap<>();
		parsed.put(main.getFullModuleName(), main);
		//Job job = new Job(false, parsed, main.getFullModuleName(), false, false, false, false);
		MainContext mc = newMainContext(mainmod);
		Job job = new Job(mc.debug, mc.getParsedModules(), mc.main, mc.useOldWF, mc.noLiveness, mc.minEfsm, mc.fair);
		job.checkLinearMPScalaWellFormedness();  // FIXME TODO
		return job.getContext().getMainModule();
	}*/
}
