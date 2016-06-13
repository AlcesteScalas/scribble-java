package ast.local.ops;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.scribble.main.ScribbleException;

import ast.linear.AbstractVariant;
import ast.linear.In;
import ast.linear.End;
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
import ast.name.BaseType;
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
	private final List<Role> roles; // Sorted list of roles in "visiting"
	private final String root; // Root of the class hierarchy
	
	// Classes used for input
	private Set<String> inputClassNames = new java.util.HashSet<>();
	
	// Classes used for inputting
	private Set<String> outputClassNames = new java.util.HashSet<>();
	
	/** Generate the Scala protocol classes representing a local type.
	 * 
	 * @param t Local type to extract classes for
	 * @param root The root of the class hierarchy to be generated
	 * @return The Scala class definitions representing {@code t}
	 * @throws ScribbleException in case of error
	 */
	public static String apply(LocalType t, String root) throws ScribbleException
	{
		return apply(t, root, DefaultNameEnvBuilder.apply(t));
	}
	
	/** Generate the Scala protocol classes representing a local type, using
	 * a given naming environment.
	 *  
	 * @param t Local type to extract classes for
	 * @param root The root of the class hierarchy to be generated
	 * @param nameEnv Naming environment (supposed to be suitable for {@code t})
	 * @return The Scala class definitions representing {@code t}
	 * @throws ScribbleException in case of error
	 */
	public static String apply(LocalType t, String root, LocalNameEnv nameEnv) throws ScribbleException
	{
		ScalaProtocolExtractor te = new ScalaProtocolExtractor(t, root, nameEnv);
		
		return te.process();
	}
	
	private ScalaProtocolExtractor(LocalType t, String root, LocalNameEnv nameEnv) throws ScribbleException
	{
		this.visiting = t;
		this.nameEnv = nameEnv;
		this.ctracker = new ChannelTracker(t);
		this.roles = new java.util.ArrayList<>(new java.util.TreeSet<>(ctracker.keySet()));
		this.root = root;
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
			String linp = ast.linear.ops.ScalaProtocolExtractor.apply(t).trim();
			if (!linp.isEmpty())
			{
				linProtoClasses.add(linp);
			}
		}

		String mpProtoClasses = visit(visiting).trim();
		if (!errors.isEmpty())
		{
			throw new ScribbleException("Error(s) extracting protocol of " + visiting + ": " + String.join(";", errors));
		}
		
		assert(!inputClassNames.isEmpty() || !outputClassNames.isEmpty());
		
		// Pick roles in alphabetical order from channel tracker
		for (Role r: new java.util.ArrayList<>(new java.util.TreeSet<>(ctracker.keySet())))
		{
			ast.linear.Type t = ctracker.get(r).t;
			String inmsgs = ast.linear.ops.ScalaMessageExtractor.inputs(
					t, inputClassNames).trim();
			if (!inmsgs.isEmpty())
			{
				msgInProtoClasses.add(inmsgs);
			}
			
			String outmsgs = ast.linear.ops.ScalaMessageExtractor.outputs(
					t, outputClassNames).trim();
			if (!outmsgs.isEmpty())
			{
				msgOutProtoClasses.add(outmsgs);
			}
		}
		
		assert(msgInProtoClasses.size() == inputClassNames.size());
		assert(msgOutProtoClasses.size() == outputClassNames.size());
		
		return ("package " + root + "\n\n" +
				"import lchannels._\n\n" +
				"// Input message types for multiparty sessions (" + String.join(", ", inputClassNames) + ")\n" +
				String.join("\n", msgInProtoClasses) +
				"\n\n// Output message types for multiparty sessions (" + String.join(", ", outputClassNames) + ")\n" +
				String.join("\n", msgOutProtoClasses) +
				"\n\n// Multiparty session classes\n" +
				mpProtoClasses +
				"\n\n// Classes representing messages (with continuations) in binary sessions\n" +
				"package object " + BINARY_CLASSES_NS + " {\n" +
				String.join("\n", linProtoClasses) +
				"\n}\n"
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
		String vname = lte.env.get(v);
		assert(vname != null);
		
		// Remember that the underlying variant is used for output
		inputClassNames.add(vname);
		
		
		String res = "case class " + className + "(" + String.join(", ", chanspecs) + ") {\n";
		res += ("  def receive() = {\n" +
				"    " + node.src.name + ".receive() match {\n");
		// FIXME: here we could simplify if the variant has just one label
		for (Label l: v.labels())
		{
			res += "      case m @ " + BINARY_CLASSES_NS + "." + l + "(p) => {\n";
			
			ast.linear.Payload payload = v.payload(l);
			String payloadRepr;
			if (payload instanceof BaseType)
			{
				payloadRepr = "p";
			}
			else if (payload instanceof LocalType)
			{
				payloadRepr = "TODO";
			}
			else
			{
				throw new RuntimeException("BUG: unsupported payload type " + payload);
			}
			
			LocalType contb = node.cases.get(l).body;
			String ret;
			if (contb instanceof LocalEnd)
			{
				ret = l.name + "(" + payloadRepr + ")"; // We are done
			}
			else
			{
				String contClassName = nameEnv.get(contb);
				List<String> contSpecs = new java.util.LinkedList<>();
				for (Role rcont: this.roles)
				{
					if (!rcont.equals(node.src))
					{
						contSpecs.add(rcont.name); // Reuse the channel
					}
					else
					{
						contSpecs.add("m.cont"); // Use the continuation
					}
				}
				ret = l.name + "(" + payload + ", " + contClassName + "(" + String.join(", ", contSpecs) + "))";
			}
			
			res += ("        " + ret + "\n" +
					"      }\n");
			
		}
		res += ("    }\n" +
				"  }\n" +
				"}");
		
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
		String vname = lte.env.get(v);
		assert(vname != null);
		
		// Remember that the underlying variant is used for output
		outputClassNames.add(vname);
		
		String res = "case class " + className + "(" + String.join(", ", chanspecs) + ") {\n";
		res += ("  def send(v: " + vname + ") = {\n" +
				"    v match {\n");
		// FIXME: here we could simplify if the variant has just one label
		for (Label l: v.labels())
		{
			res += "      case " + l + "(p) => {\n";
			
			ast.linear.Payload payload = v.payload(l);
			String payloadRepr;
			if (payload instanceof BaseType)
			{
				payloadRepr = "p";
			}
			else if (payload instanceof LocalType)
			{
				payloadRepr = "TODO";
			}
			else
			{
				throw new RuntimeException("BUG: unsupported payload type " + payload);
			}
			
			ast.linear.Type cont = v.continuation(l);
			String sendRepr;
			if ((cont instanceof In) || (cont instanceof Out))
			{
				// Let lchannels take care of the continuation
				sendRepr = " !! " + BINARY_CLASSES_NS + "." + l + "(" + payloadRepr + ")_";
			}
			else if (cont instanceof End)
			{
				sendRepr = " ! " + BINARY_CLASSES_NS + "." + l + "(" + payloadRepr + ")";
			}
			else
			{
				throw new RuntimeException("BUG: unsupported continuation type " + cont);
			}
			
			LocalType contb = node.cases.get(l).body;
			String ret;
			if (contb instanceof LocalEnd)
			{
				ret = "()"; // We are done
			}
			else
			{
				String contClassName = nameEnv.get(contb);
				List<String> contSpecs = new java.util.LinkedList<>();
				for (Role rcont: this.roles)
				{
					if (!rcont.equals(node.dest))
					{
						contSpecs.add(rcont.name); // Reuse the channel
					}
					else
					{
						contSpecs.add("cnt"); // Use the continuation
					}
				}
				ret = contClassName + "(" + String.join(", ", contSpecs) + ")";
			}
			
			res += ("        val cnt = " + node.dest.name + sendRepr + "\n" +
					"        " + ret + "\n" +
					"      }\n");
			
		}
		res += ("    }\n" +
				"  }\n" +
				"}");
		
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
