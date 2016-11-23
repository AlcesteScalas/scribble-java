package org.scribble.del.global;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.ast.AstFactoryImpl;
import org.scribble.ast.Module;
import org.scribble.ast.NonRoleParamDeclList;
import org.scribble.ast.RoleDeclList;
import org.scribble.ast.ScribNode;
import org.scribble.ast.context.DependencyMap;
import org.scribble.ast.context.global.GProtocolDeclContext;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.ast.local.LProtocolDecl;
import org.scribble.ast.local.LProtocolDef;
import org.scribble.ast.local.LProtocolHeader;
import org.scribble.ast.name.qualified.LProtocolNameNode;
import org.scribble.del.ModuleDel;
import org.scribble.del.ProtocolDeclDel;
import org.scribble.main.Job;
import org.scribble.main.JobContext;
import org.scribble.main.ScribbleException;
import org.scribble.model.endpoint.EFSM;
import org.scribble.model.endpoint.EGraph;
import org.scribble.model.endpoint.EStateKind;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.model.endpoint.actions.ESend;
import org.scribble.model.global.GMBuffers;
import org.scribble.model.global.GMConfig;
import org.scribble.model.global.GMGraph;
import org.scribble.model.global.GMState;
import org.scribble.model.global.GMStateErrors;
import org.scribble.model.global.actions.GMAction;
import org.scribble.sesstype.kind.Global;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.sesstype.name.ProtocolName;
import org.scribble.sesstype.name.Role;
import org.scribble.visit.context.Projector;
import org.scribble.visit.context.ProtocolDeclContextBuilder;
import org.scribble.visit.context.env.ProjectionEnv;
import org.scribble.visit.util.RoleCollector;
import org.scribble.visit.validation.GMChecker;

public class GProtocolDeclDel extends ProtocolDeclDel<Global>
{
	public GProtocolDeclDel()
	{

	}
	
	@Override
	public GProtocolDeclContext getProtocolDeclContext()
	{
		return (GProtocolDeclContext) super.getProtocolDeclContext();
	}

	@Override
	protected GProtocolDeclDel copy()
	{
		return new GProtocolDeclDel();
	}

	@Override
	protected void addSelfDependency(ProtocolDeclContextBuilder builder, ProtocolName<?> proto, Role role)
	{
		builder.addGlobalProtocolDependency(role, (GProtocolName) proto, role);
	}
	
	@Override
	public GProtocolDecl
			leaveProtocolDeclContextBuilding(ScribNode parent, ScribNode child, ProtocolDeclContextBuilder builder, ScribNode visited) throws ScribbleException
	{
		GProtocolDecl gpd = (GProtocolDecl) visited;
		GProtocolDeclContext gcontext = new GProtocolDeclContext(builder.getGlobalProtocolDependencyMap());
		GProtocolDeclDel del = (GProtocolDeclDel) setProtocolDeclContext(gcontext);
		return (GProtocolDecl) gpd.del(del);
	}

	@Override
	public ScribNode leaveRoleCollection(ScribNode parent, ScribNode child, RoleCollector coll, ScribNode visited) throws ScribbleException
	{
		GProtocolDecl gpd = (GProtocolDecl) visited;

		// Need to do here (e.g. RoleDeclList too early, def not visited yet)
		// Currently only done for global, local does roledecl fixing after role collection -- should separate this check to a later pass after context building
		// Maybe relax to check only occs.size() > 1
		List<Role> decls = gpd.header.roledecls.getRoles();
		Set<Role> occs = coll.getNames();
		if (occs.size() != decls.size()) 
		{
			decls.removeAll(occs);
			throw new ScribbleException("Unused role decl(s) in " + gpd.header.name + ": " + decls);
		}

		return super.leaveRoleCollection(parent, child, coll, gpd);
	}

	@Override
	public GProtocolDecl
			leaveProjection(ScribNode parent, ScribNode child, Projector proj, ScribNode visited) throws ScribbleException
	{
		JobContext jc = proj.job.getContext();
		Module root = jc.getModule(proj.getModuleContext().root);
		GProtocolDecl gpd = (GProtocolDecl) visited;
		Role self = proj.peekSelf();
		LProtocolDecl lpd = project(proj, gpd);
		Map<GProtocolName, Set<Role>> deps = ((GProtocolDeclDel) gpd.del()).getGlobalProtocolDependencies(self);
		Module projected = ((ModuleDel) root.del()).createModuleForProjection(proj, root, lpd, deps);
		
		/*if (lpd.getHeader().name.toString().endsWith("_C"))
		{
			System.out.println("ZZZ:\n" + projected);
		}*/
		
		proj.addProjection(gpd.getFullMemberName(root), self, projected);
		return gpd;
	}

	private Map<GProtocolName, Set<Role>> getGlobalProtocolDependencies(Role self)
	{
		DependencyMap<GProtocolName> deps = getProtocolDeclContext().getDependencyMap();
		return deps.getDependencies().get(self);
	}
	
	// FIXME: project modifiers?
	private LProtocolDecl project(Projector proj, GProtocolDecl gpd) throws ScribbleException
	{
		Role self = proj.peekSelf();
		LProtocolDef def = (LProtocolDef) ((ProjectionEnv) gpd.def.del().env()).getProjection();
		LProtocolNameNode pn = Projector.makeProjectedSimpleNameNode(gpd.getHeader().getDeclName(), self);
		
		// Move to delegates? -- maybe fully integrate into projection pass
		RoleDeclList roledecls = gpd.header.roledecls.project(self);
		NonRoleParamDeclList paramdecls = gpd.header.paramdecls.project(self);
		LProtocolHeader lph = AstFactoryImpl.FACTORY.LProtocolHeader(pn, roledecls, paramdecls);
		GProtocolName gpn = gpd.getFullMemberName(proj.job.getContext().getModule(proj.getModuleContext().root));
		LProtocolDecl projected = AstFactoryImpl.FACTORY.LProjectionDecl(gpd.modifiers, gpn, proj.peekSelf(), lph, def);
		return projected;
	}
	
	@Override
	public void enterCompatCheck(ScribNode parent, ScribNode child, GMChecker checker) throws ScribbleException
	{
		GProtocolDecl gpd = (GProtocolDecl) child;
		if (gpd.isAuxModifier())
		{
			return;
		}

		GProtocolName fullname = gpd.getFullMemberName((Module) parent);
		checker.job.debugPrintln("(" + fullname + ") Build and check \"fair\" output choices: ");
		buildAndCheck(checker.job, gpd, fullname, true);
		if (!checker.job.fair)
		{
			checker.job.debugPrintln("(" + fullname + ") Build and check \"unfair\" output choices: ");
			buildAndCheck(checker.job, gpd, fullname, false);
		}
	}

	private GProtocolDecl buildAndCheck(Job job, GProtocolDecl gpd, GProtocolName fullname, boolean fair) throws ScribbleException
	{
		JobContext jc = job.getContext();

		GMGraph graph;
		if (fair)
		{
			graph = jc.getGlobalModel(fullname);
			if (graph == null)  // FIXME: factor into JobContext
			{
				Map<Role, EFSM> egraphs = getEndpointFSMs(job, fullname, gpd, fair);
				graph = buildGlobalModel(job, fullname, gpd, egraphs);
				jc.addGlobalModel(fullname, graph);
			}
		}
		else
		{
			graph = jc.getUnfairGlobalModel(fullname);
			if (graph == null)
			{
				Map<Role, EFSM> egraphs = getEndpointFSMs(job, fullname, gpd, fair);
				graph = buildGlobalModel(job, fullname, gpd, egraphs);
				jc.addUnfairGlobalModel(fullname, graph);
			}
		}

		checkGlobalModel(job, fullname, graph);
		
		return gpd;
	}

	private void checkGlobalModel(Job job, GProtocolName fullname, GMGraph graph) throws ScribbleException
	{
		GMState init = graph.init;
		Map<Integer, GMState> all = graph.states;

		String errorMsg = "";

		int count = 0;
		for (GMState s : all.values())
		{
			if (job.debug)
			{
				count++;
				if (count % 50 == 0)
				{
					job.debugPrintln("(" + fullname + ") Checking global states: " + count);
				}
			}
			GMStateErrors errors = s.getErrors();
			if (!errors.isEmpty())
			{
				// FIXME: getTrace can get stuck when local choice subjects are disabled
				List<GMAction> trace = graph.getTrace(init, s);  // FIXME: getTrace broken on non-det self loops?
				//errorMsg += "\nSafety violation(s) at " + s.toString() + ":\n    Trace=" + trace;
				errorMsg += "\nSafety violation(s) at " + s.id + ":\n    Trace=" + trace;
			}
			if (!errors.stuck.isEmpty())
			{
				errorMsg += "\n    Stuck messages: " + errors.stuck;  // Deadlock from reception error
			}
			if (!errors.waitFor.isEmpty())
			{
				errorMsg += "\n    Wait-for errors: " + errors.waitFor;  // Deadlock from input-blocked cycles, terminated dependencies, etc
			}
			if (!errors.orphans.isEmpty())
			{
				errorMsg += "\n    Orphan messages: " + errors.orphans;  // FIXME: add sender of orphan to error message 
			}
			if (!errors.unfinished.isEmpty())
			{
				errorMsg += "\n    Unfinished roles: " + errors.unfinished;
			}
		}
		job.debugPrintln("(" + fullname + ") Checked all states: " + count);
		//*/
		
		if (!job.noLiveness)
		{
			Set<Set<Integer>> termsets = graph.getTerminalSets();
			//findTerminalSets(all, reach, termsets);

			//System.out.println("Terminal sets: " + termsets.stream().map((s) -> s.toString()).collect(Collectors.joining("\n")));

			//for (Set<WFState> termset : termsets)
			for (Set<Integer> termset : termsets)
			{
				Set<Role> safety = new HashSet<>();
				Set<Role> roleLiveness = new HashSet<>();
				//checkTerminalSet(init, termset, safety, roleLiveness);
				checkTerminalSet(all, init, termset, safety, roleLiveness);
				if (!safety.isEmpty())
				{
					// Redundant
					errorMsg += "\nSafety violation for " + safety + " in terminal set:\n    " + termSetToString(job, termset, all);
				}
				if (!roleLiveness.isEmpty())
				{
					errorMsg += "\nRole progress violation for " + roleLiveness + " in terminal set:\n    " + termSetToString(job, termset, all);
				}
				Map<Role, Set<ESend>> msgLiveness = checkMessageLiveness(all, init, termset);
				if (!msgLiveness.isEmpty())
				{
					errorMsg += "\nMessage liveness violation for " + msgLiveness + " in terminal set:\n    " + termSetToString(job, termset, all);
				}
			}
		}
		
		if (!errorMsg.equals(""))
		{
			//throw new ScribbleException("\n" + init.toDot() + errorMsg);
			throw new ScribbleException(errorMsg);
		}
	}
	
	private String termSetToString(Job job, Set<Integer> termset, Map<Integer, GMState> all)
	{
		return job.debug
				? termset.stream().map((i) -> all.get(i).toString()).collect(Collectors.joining(","))
				: termset.stream().map((i) -> new Integer(all.get(i).id).toString()).collect(Collectors.joining(","));
	}

	private Map<Role, EFSM> getEndpointFSMs(Job job, GProtocolName fullname, GProtocolDecl gpd, boolean fair) throws ScribbleException
	{
		JobContext jc = job.getContext();
		Map<Role, EFSM> egraphs = new HashMap<>();
		
		for (Role self : gpd.header.roledecls.getRoles())
		{
			EGraph graph = fair ? jc.getEndpointGraph(fullname, self) : jc.getUnfairEndpointGraph(fullname, self);
			job.debugPrintln("(" + fullname + ") EFSM (fair=" + fair + ") for " + self + ":\n" + graph.init.toDot());
			
			egraphs.put(self, graph.toFsm());
		}
		return egraphs;
	}


	// FIXME: this is now just "role liveness"
	// ** Could subsume terminal state check, if terminal sets included size 1 with reflexive reachability (but probably not good)
	//private static void checkTerminalSet(WFState init, Set<WFState> termset, Set<Role> safety, Set<Role> liveness) throws ScribbleException
	private static void checkTerminalSet(Map<Integer, GMState> all, GMState init, Set<Integer> termset, Set<Role> safety, Set<Role> liveness) throws ScribbleException
	{
		Iterator<Integer> i = termset.iterator();
		GMState s = all.get(i.next());
		Map<Role, GMState> ss = new HashMap<>();
		s.config.states.keySet().forEach((r) -> ss.put(r, s));
		while (i.hasNext())
		{
			GMState next = all.get(i.next());
			Map<Role, EFSM> tmp = next.config.states;
			for (Role r : tmp.keySet())
			{
				if (ss.get(r) != null)
				{
					/*if (!ss.get(r).equals(tmp.get(r)))
					{	
						ss.put(r, null);
					}
					else*/
					{
						for (GMAction a : next.getAllActions())
						{
							if (a.containsRole(r))
							{
								ss.put(r, null);
								break;
							}
						}
					}
				}
			}
		}
		for (Role r : ss.keySet())
		{
			GMState foo = ss.get(r);
			if (foo != null)
			{
				EFSM tmp = foo.config.states.get(r);
				if (tmp != null)
				{
					if (!foo.config.canSafelyTerminate(r))
					{
						if (s.config.buffs.get(r).values().stream().allMatch((v) -> v == null))
						{
							liveness.add(r);
						}
						/*
						// Should be redundant given explicit reception error etc checking
						else
						{
							safety.add(r);
						}*/
					}
				}
			}
		}
	}

	// "message liveness"
	private static Map<Role, Set<ESend>> checkMessageLiveness(Map<Integer, GMState> all, GMState init, Set<Integer> termset) throws ScribbleException
	{
		Set<Role> roles = all.get(termset.iterator().next()).config.states.keySet();

		Iterator<Integer> i = termset.iterator();
		Map<Role, Map<Role, ESend>> b0 = all.get(i.next()).config.buffs.getBuffers();
		while (i.hasNext())
		{
			GMState s = all.get(i.next());
			GMBuffers b = s.config.buffs;
			for (Role r1 : roles)
			{
				for (Role r2 : roles)
				{
					ESend s0 = b0.get(r1).get(r2);
					if (s0 != null)
					{
						ESend tmp = b.get(r1).get(r2);
						if (tmp == null)
						{
							b0.get(r1).put(r2, null);
						}
					}
				}
			}
		}
	
		Map<Role, Set<ESend>> res = new HashMap<>();
		for (Role r1 : roles)
		{
			for (Role r2 : roles)
			{
				ESend m = b0.get(r1).get(r2);
				if (m != null)
				{
					Set<ESend> tmp = res.get(r2);
					if (tmp == null)
					{
						tmp = new HashSet<>();
						res.put(r2, tmp);
					}
					tmp.add(m);
				}
			}
		}

		return res;
	}
	
	
	
	// FIXME: factor out
	private GMGraph buildGlobalModel(Job job, GProtocolName fullname, GProtocolDecl gpd, Map<Role, EFSM> egraphs) throws ScribbleException
	{
		GMBuffers b0 = new GMBuffers(egraphs.keySet(), !gpd.modifiers.contains(GProtocolDecl.Modifiers.EXPLICIT));
		GMConfig c0 = new GMConfig(egraphs, b0);
		GMState init = new GMState(c0);

		Map<Integer, GMState> seen = new HashMap<>();
		LinkedHashSet<GMState> todo = new LinkedHashSet<>();
		todo.add(init);

		// FIXME: factor out model building and integrate with getAllNodes (seen == all)
		int count = 0;
		while (!todo.isEmpty())
		{
			Iterator<GMState> i = todo.iterator();
			GMState curr = i.next();
			i.remove();
			seen.put(curr.id, curr);

			if (job.debug)
			{
				count++;
				if (count % 50 == 0)
				{
					job.debugPrintln("(" + fullname + ") Building global states: " + count);
				}
			}
			
			Map<Role, List<EAction>> takeable = curr.getTakeable();

			//job.debugPrintln("Acceptable at (" + curr.id + "): " + acceptable);

			for (Role r : takeable.keySet())
			{
				List<EAction> acceptable_r = takeable.get(r);
				
				// Hacky?  // FIXME: factor out and make more robust (e.g. for new state kinds) -- e.g. "hasPayload" in IOAction
				//EndpointState currstate = curr.config.states.get(r);
				EFSM currfsm = curr.config.states.get(r);
				EStateKind k = currfsm.getStateKind();
				if (k == EStateKind.OUTPUT)
				{
					for (EAction a : acceptable_r)  // Connect implicitly has no payload (also accept, so skip)
					{
						if (acceptable_r.stream().anyMatch((x) ->
								!a.equals(x) && a.peer.equals(x.peer) && a.mid.equals(x.mid) && !a.payload.equals(x.payload)))
						{
							throw new ScribbleException("Bad non-deterministic action payloads: " + acceptable_r);
						}
					}
				}
				else if (k == EStateKind.UNARY_INPUT || k == EStateKind.POLY_INPUT || k == EStateKind.ACCEPT)
				{
					for (EAction a : acceptable_r)
					{
						if (currfsm.getAllTakeable().stream().anyMatch((x) ->
								!a.equals(x) && a.peer.equals(x.peer) && a.mid.equals(x.mid) && !a.payload.equals(x.payload)))
						{
							throw new ScribbleException("Bad non-deterministic action payloads: " + currfsm.getAllTakeable());
						}
					}
				}
			}  // Need to do all action payload checking before next building step, because doing sync actions will also remove peer's actions from takeable set

			for (Role r : takeable.keySet())
			{
				List<EAction> acceptable_r = takeable.get(r);
				
				for (EAction a : acceptable_r)
				{
					if (a.isSend() || a.isReceive() || a.isDisconnect())
					{
						getNextStates(todo, seen, curr, a.toGlobal(r), curr.take(r, a));
					}
					else if (a.isAccept() || a.isConnect())
					{	
						List<EAction> as = takeable.get(a.peer);
						EAction d = a.toDual(r);
						if (as != null && as.contains(d))
						{
							as.remove(d);  // Removes one occurrence
							//getNextStates(seen, todo, curr.sync(r, a, a.peer, d));
							GMAction g = (a.isConnect()) ? a.toGlobal(r) : d.toGlobal(a.peer);
							getNextStates(todo, seen, curr, g, curr.sync(r, a, a.peer, d));
						}
					}
					else if (a.isWrapClient() || a.isWrapServer())
					{
						List<EAction> as = takeable.get(a.peer);
						EAction w = a.toDual(r);
						if (as != null && as.contains(w))
						{
							as.remove(w);  // Removes one occurrence
							GMAction g = (a.isConnect()) ? a.toGlobal(r) : w.toGlobal(a.peer);
							getNextStates(todo, seen, curr, g, curr.sync(r, a, a.peer, w));
						}
					}
					else
					{
						throw new RuntimeException("Shouldn't get in here: " + a);
					}
				}
			}
		}

		job.debugPrintln("(" + fullname + ") Building global model..\n" + init.toDot() + "\n(" + fullname + ") Built global model (" + count + " states)");

		return new GMGraph(seen, init);
	}

	private void getNextStates(LinkedHashSet<GMState> todo, Map<Integer, GMState> seen, GMState curr, GMAction a, List<GMConfig> nexts)
	{
		for (GMConfig next : nexts)
		{
			GMState news = new GMState(next);
			GMState succ = null; 
			//if (seen.contains(succ))  // FIXME: make a WFModel builder
			/*if (seen.containsValue(succ))
			{
				for (WFState tmp : seen)
				{
					if (tmp.equals(succ))
					{
						succ = tmp;
					}
				}
			}*/
			for (GMState tmp : seen.values())  // Key point: checking "semantically" if model state already created
			{
				if (tmp.equals(news))
				{
					succ = tmp;
				}
			}
			if (succ == null)
			{
				for (GMState tmp : todo)  // If state created but not "seen" yet, then it will be "todo"
				{
					if (tmp.equals(news))
					{
						succ = tmp;
					}
				}
			}
			if (succ == null)
			{
				succ = news;
				todo.add(succ);
			}
			//curr.addEdge(a.toGlobal(r), succ);
			curr.addEdge(a, succ);  // FIXME: make a Builder util, cf. EGraphBuilderUtil
			//if (!seen.contains(succ) && !todo.contains(succ))
			/*if (!seen.containsKey(succ.id) && !todo.contains(succ))
			{
				todo.add(succ);
			}*/
		}
	}
}

