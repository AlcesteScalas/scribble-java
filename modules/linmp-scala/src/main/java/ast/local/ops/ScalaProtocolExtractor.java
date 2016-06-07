package ast.local.ops;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.scribble.main.ScribbleException;

import ast.linear.AbstractVariant;
import ast.linear.In;
import ast.linear.Out;
import ast.local.ops.DefaultNameEnvBuilder;
import ast.local.LocalBranch;
import ast.local.LocalCase;
import ast.local.LocalEnd;
import ast.local.LocalNameEnv;
import ast.local.LocalRec;
import ast.local.LocalSelect;
import ast.local.LocalType;
import ast.local.LocalTypeVisitor;
import ast.name.Label;
import ast.name.RecVar;
import ast.name.Role;

public class ScalaProtocolExtractor extends LocalTypeVisitor<String>
{
	// Simple pair of a binary type and its naming environment
	private class LinearTypeNameEnv
	{
		public ast.linear.Type t;
		public final ast.linear.NameEnv env;
		
		public LinearTypeNameEnv(ast.linear.Type t) throws ScribbleException
		{
			this.t = t;
			this.env = ast.linear.ops.DefaultNameEnvBuilder.apply(t);
		}
	}
	
	// Maps a role to its (current) binary type, and its naming environment
	private class ChannelTracker extends HashMap<Role, LinearTypeNameEnv>
	{
		private static final long serialVersionUID = 1L;
		
		public ChannelTracker(LocalType t) throws ScribbleException
		{
			super();
			for (Role r: t.roles())
			{
				this.put(r, new LinearTypeNameEnv(t.linear(r)));
			}
		}
	}
	
	private Collection<String> errors = new java.util.LinkedList<String>();
	private final LocalType visiting;
	private LocalNameEnv nameEnv;
	private final ChannelTracker ctracker;
	// Sorted the roles
	private final List<Role> roles;
	
	public static String apply(LocalType t) throws ScribbleException
	{
		return apply(t, DefaultNameEnvBuilder.apply(t));
	}
	
	public static String apply(LocalType t, LocalNameEnv nameEnv) throws ScribbleException
	{
		ScalaProtocolExtractor te = new ScalaProtocolExtractor(t, nameEnv);
		
		return te.process();
	}
	
	private ScalaProtocolExtractor(LocalType t, LocalNameEnv nameEnv) throws ScribbleException
	{
		this.visiting = t;
		this.nameEnv = nameEnv;
		this.ctracker = new ChannelTracker(t);
		this.roles = new java.util.ArrayList<>(new java.util.TreeSet<>(ctracker.keySet()));
	}
	
	@Override
	protected String process() throws ScribbleException
	{
		String res = visit(visiting);
		if (errors.isEmpty())
		{
			return res;
		}
		throw new ScribbleException("Error(s) extracting protocol of " + visiting + ": "
				                    + String.join(";", errors));
	}

	@Override
	protected String visit(LocalEnd node)
	{
		return "";
	}

	@Override
	protected String visit(LocalBranch node)
	{
		String className = nameEnv.get(node);
				
		// The tuple of underlying channels
		List<String> chanspecs = new java.util.LinkedList<>();
		for (Role r: roles)
		{
			String chanspec = "";
			ast.linear.Type t = ctracker.get(r).t;
			ast.linear.NameEnv env = ctracker.get(r).env;
			chanspec += r.name + ": ";
			try
			{
				chanspec += ast.linear.ops.ScalaChannelTypeExtractor.apply(t, env);
			}
			catch (ScribbleException e)
			{
				errors.add("Cannot extract channel type of " + t + ": " + e);
				chanspec += "ERROR";
			}
			chanspecs.add(chanspec);
		}
		
		String res = "case class " + className + "(" + String.join(", ", chanspecs) + ")";
		
		LinearTypeNameEnv lte = ctracker.get(node.src);
		// Ensure labels are sorted
		for (Label l: new java.util.ArrayList<>(new java.util.TreeSet<>(node.cases.keySet())))
		{
			LocalCase c = node.cases.get(l);
			// Update the channel involved in the interaction
			// Note: we use the fact that we know that lte.t is an input type
			AbstractVariant v = ((In)lte.t).variant;
			lte.t = v.continuation(l); 
			
			if (c.pay instanceof LocalType)
			{
				res += "\n" + "TODO: extract payload classes!"; // FIXME
			}
			res += "\n" + visit(c.body);
		}
		
		return res;
	}

	@Override
	protected String visit(LocalSelect node)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String visit(LocalRec node)
	{
		return visit(node.body);
	}

	@Override
	protected String visit(RecVar node)
	{
		return "";
	}

}
