package ast.local.ops;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
	public static String BINARY_CLASSES_NS = "binary";
	
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
		
		public LinearTypeNameEnv(ast.linear.Type t, ast.linear.NameEnv env)
		{
			this.t = t;
			this.env = env;
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
	
	// Classes used for input
	private Set<String> inputClassNames = new java.util.HashSet<>();
	
	// Classes used for inputting
	private Set<String> outputClassNames = new java.util.HashSet<>();
	
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
		List<String> linProtoClasses = new java.util.LinkedList<>(); 
		List<String> msgInProtoClasses = new java.util.LinkedList<>();
		List<String> msgOutProtoClasses = new java.util.LinkedList<>();
		
		// Pick roles in alphabetical order from channel tracker
		for (Role r: new java.util.ArrayList<>(new java.util.TreeSet<>(ctracker.keySet())))
		{
			ast.linear.Type t = ctracker.get(r).t;
			linProtoClasses.add(ast.linear.ops.ScalaProtocolExtractor.apply(t));
		}

		String mpProtoClasses = visit(visiting);
		if (!errors.isEmpty())
		{
			throw new ScribbleException("Error(s) extracting protocol of " + visiting + ": " + String.join(";", errors));
		}
		
		// Pick roles in alphabetical order from channel tracker
		for (Role r: new java.util.ArrayList<>(new java.util.TreeSet<>(ctracker.keySet())))
		{
			ast.linear.Type t = ctracker.get(r).t;
			msgInProtoClasses.add(ast.linear.ops.ScalaMessageExtractor.inputs(
					t, inputClassNames));
			msgOutProtoClasses.add(ast.linear.ops.ScalaMessageExtractor.outputs(
					t, outputClassNames));
		}
		
		return (
				"// Input message types for multiparty sessions\n" +
				String.join("\n", msgInProtoClasses) +
				"\n// Output message types for multiparty sessions\n" +
				String.join("\n", msgOutProtoClasses) +
				"\n// Multiparty session classes\n" +
				mpProtoClasses +
				"// Classes representing messages in binary sessions\n" +
				"\npackage object " + BINARY_CLASSES_NS + " {\n" +
				String.join("\n", linProtoClasses) +
				"}\n"
				);
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
		assert(className != null);
				
		List<String> chanspecs = getChanspecs();
		
		// Save the current tracker status (we'll restore it before returning)
		LinearTypeNameEnv lte = ctracker.get(node.src);
		// Note: we use the fact that we know lte.t is In or Out (not End)
		AbstractVariant v = getCarried(lte.t);
		
		// Remember that the underlying variant is used for input
		assert(lte.env.get(v) != null);
		inputClassNames.add(lte.env.get(v));
		
		String res = "case class " + className + "(" + String.join(", ", chanspecs) + ") {\n";
		res += "  def receive() // TODO\n";
		res += "}\n";
		
		// Ensure that labels are sorted
		for (Label l: new java.util.ArrayList<>(new java.util.TreeSet<>(node.cases.keySet())))
		{
			LocalCase c = node.cases.get(l);
			// Update the channel involved in the interaction
			ctracker.put(node.src, new LinearTypeNameEnv(v.continuation(l), lte.env));
			
			if (c.pay instanceof LocalType)
			{
				res += "\n" + "TODO: extract payload classes!"; // FIXME
			}
			res += "\n" + visit(c.body);
		}
		
		// Restore the channel tracker status before returning
		ctracker.put(node.src, lte);
		
		return res;
	}
	
	@Override
	protected String visit(LocalSelect node)
	{
		String className = nameEnv.get(node);
		assert(className != null);
		
		List<String> chanspecs = getChanspecs();
		
		// Save the current tracker status (we'll restore it before returning)
		LinearTypeNameEnv lte = ctracker.get(node.dest);
		// Note: we use the fact that we know lte.t is In or Out (not End)
		AbstractVariant v = getCarried(lte.t);
		
		// Remember that the underlying variant is used for output
		assert(lte.env.get(v) != null);
		outputClassNames.add(lte.env.get(v));
		
		String res = "case class " + className + "(" + String.join(", ", chanspecs) + ") {\n";
		res += "  def send(v: " + lte.env.get(lte.t) + ") // TODO\n";
		res += "}\n";
		
		// Ensure that labels are sorted
		for (Label l: new java.util.ArrayList<>(new java.util.TreeSet<>(node.cases.keySet())))
		{
			LocalCase c = node.cases.get(l);
			// Update the channel involved in the interaction
			// Note: we dualise the continuation, since it follows an output!
			ctracker.put(node.dest, new LinearTypeNameEnv(v.continuation(l).dual(), lte.env));
			
			if (c.pay instanceof LocalType)
			{
				res += "\n" + "TODO: extract payload classes!"; // FIXME
			}
			res += "\n" + visit(c.body);
		}
		
		// Restore the channel tracker status before returning
		ctracker.put(node.dest, lte);
		
		return res;
	}
	
	// Get the variant carried by a linear type t, throwing a runtime exception
	// if t is End
	private AbstractVariant getCarried(ast.linear.Type t)
	{
		if (t instanceof In)
		{
			return ((In)t).carried();
		}
		else if (t instanceof Out)
		{
			return ((Out)t).carried();
		}
		else
		{
			throw new RuntimeException("BUG: expecting In/Out underlying type, got " + t);
		}
	}
	
	// Determine the channel types underlying a multiparty session object
	private List<String> getChanspecs()
	{
		List<String> chanspecs = new java.util.LinkedList<>();
		for (Role r: roles)
		{
			String chanspec = "";
			ast.linear.Type t = ctracker.get(r).t;
			ast.linear.NameEnv env = ctracker.get(r).env;
			chanspec += r.name + ": ";
			try
			{
				chanspec += ast.linear.ops.ScalaChannelTypeExtractor.apply(
						t, env, BINARY_CLASSES_NS + ".");
			}
			catch (ScribbleException e)
			{
				errors.add("Cannot extract channel type of " + t + ": " + e);
				chanspec += "ERROR";
			}
			chanspecs.add(chanspec);
		}
		return chanspecs;
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
