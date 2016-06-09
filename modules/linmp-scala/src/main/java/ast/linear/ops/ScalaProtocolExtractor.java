package ast.linear.ops;

import org.scribble.main.ScribbleException;

import ast.linear.AbstractVariant;
import ast.linear.Case;
import ast.linear.End;
import ast.linear.In;
import ast.linear.NameEnv;
import ast.linear.Out;
import ast.linear.Rec;
import ast.linear.Record;
import ast.linear.Type;
import ast.linear.Variant;
import ast.linear.Visitor;
import ast.local.LocalNameEnv;
import ast.local.LocalType;
import ast.name.BaseType;
import ast.name.Label;
import ast.name.RecVar;

import java.util.Collection;

/** Build the Scala case classes definitions corresponding to a linear I/O type.
 * 
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public class ScalaProtocolExtractor extends Visitor<String>
{
	/** Options for class generation. */
	public static class Option {
		/** Default class generation for CPS linear interaction. */
		static class Default extends Option { };

		/** Class generation for output messages (without continuations). */
		public class MPOutputMessages extends Option
		{
			private final Collection<String> classNames;

			/**
			 * @param classNames output message classes to be generated (descendants will be automatically included).
			 */
			public MPOutputMessages(Collection<String> classNames)
			{
				this.classNames = classNames;
			}
		}

		/** Class generation for output messages (without continuations). */
		public class MPInputMessages extends Option
		{
			private final Collection<String> classNames;
			private final String contPrefix;

			/**
			 * @param classNames output message classes to be generated (descendants will be automatically included)
			 * @param contPrefix prefix for continuation class names (used for namespacing)
			 */
			public MPInputMessages(Collection<String> classNames, String contPrefix)
			{
				this.classNames = classNames;
				this.contPrefix = contPrefix;
			}
		}
	}
	
	private Collection<String> errors = new java.util.LinkedList<String>();
	private final Type visiting;
	private NameEnv nameEnv;
	private Option option;
	
	public static String apply(Type t) throws ScribbleException
	{
		return apply(t, DefaultNameEnvBuilder.apply(t), new Option.Default());
	}
	
	public static String apply(Type t, NameEnv nameEnv, Option opt) throws ScribbleException
	{
		ScalaProtocolExtractor te = new ScalaProtocolExtractor(t, nameEnv, opt);
		
		return te.process();
	}
	
	protected ScalaProtocolExtractor(Type t, NameEnv nameEnv, Option opt)
	{
		this.visiting = t;
		this.nameEnv = nameEnv;
		this.option = opt;
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
				                    + String.join("; ", errors));
	}

	@Override
	protected String visit(End node)
	{
		return "";
	}

	@Override
	protected String visit(In node)
	{
		return fromVariant(node.variant, node);
	}
	
	@Override
	protected String visit(Out node)
	{
		return fromVariant(node.variant, node);
	}
	
	private String fromVariant(AbstractVariant av, Type node)
	{
		if (av instanceof Variant)
		{
			String res;
			Variant v = (Variant)av;
			if (v.cases.size() == 1)
			{
				Label l = v.cases.keySet().iterator().next();
				Case c = v.cases.get(l);
				res = fromVariantCase(node, l, c, null);
				
				if (c.payload instanceof BaseType)
				{
					// Nothing to do
				}
				else if (c.payload instanceof Record)
				{
					// Let's generate the protocols of the record channels
					for (Type lt: ((Record)c.payload).values())
					{
						res += "\n" + visit(lt);
					}
				}
				else
				{
					throw new RuntimeException("BUG: unsupported payload type " + c.payload);
				}
				
				res += "\n" + visit(c.cont);
			}
			else if (v.cases.size() > 1)
			{
				String xtnd = nameEnv.get(v);
				java.util.List<Label> ls = new java.util.ArrayList<>(new java.util.TreeSet<>(v.cases.keySet()));
				res = "sealed abstract class " + xtnd + "\n";
				for (Label l: ls)
				{
					Case c = v.cases.get(l);
					res += fromVariantCase(node, l, c, xtnd);
				}
				for (Label l: ls)
				{
					Case c = v.cases.get(l);
					if (c.payload instanceof BaseType)
					{
						// Nothing to do
					}
					else if (c.payload instanceof Record)
					{
						// The record was originated from a local type:
						// let's find out its name
						LocalType origin = ((Record)c.payload).origin;
						try {
							// FIXME: what about custom name environments?
							LocalNameEnv env = ast.local.ops.DefaultNameEnvBuilder.apply(origin);
							return env.get(origin);
						}
						catch (ScribbleException e)
						{
							errors.add("Cannot determine name of " + c.payload + ": " + e);
							return "";
						}
					}
					else
					{
						throw new RuntimeException("BUG: unsupported payload type " + c.payload);
					}
					res += visit(c.cont);
				}
			}
			else
			{
				throw new RuntimeException("BUG: found 0-branches variant " + v);
			}
			return res;
		}
		else if (av instanceof Rec)
		{
			return fromVariant(((Rec)av).body, node);
		}
		else if (av instanceof RecVar)
		{
			return ""; // No protocol needs to be generated
		}
		else
		{
			throw new RuntimeException("BUG: unsupported variant-like type " + av);
		}
	}
	
	// If not null, xtnds is the class extended by each case class
	private String fromVariantCase(Type node, Label l, Case c, String xtnds)
	{
		String contPfx = ""; // Prefixes for namespacing of continuation classes
		
		if (option instanceof Option.Default)
		{
			// Nothing to do here
		}
		else if (option instanceof Option.MPInputMessages)
		{
			Option.MPInputMessages o = (Option.MPInputMessages)option;
			
			// If the abstract class is not required, skip it
			if ((!xtnds.isEmpty()) && (!o.classNames.contains(xtnds)))
			{
				return "";
			}
		}
		else if (option instanceof Option.MPOutputMessages)
		{
			Option.MPOutputMessages o = (Option.MPOutputMessages)option;
			
			// If the abstract class is not required, skip it
			if ((!xtnds.isEmpty()) && (!o.classNames.contains(xtnds)))
			{
				return "";
			}
		}
		else
		{
			throw new RuntimeException("BUG: unsupported option " + option);
		}
		
		String res;
		
		try {
			String payload;
			
			if (c.payload instanceof ast.name.BaseType)
			{
				payload = c.payload.toString();
			}
			else if (c.payload instanceof Record)
			{
				// The record was originated from a local type:
				// let's find out its name
				LocalType origin = ((Record)c.payload).origin;
				try {
					// FIXME: what about custom name environments?
					LocalNameEnv env = ast.local.ops.DefaultNameEnvBuilder.apply(origin);
					payload = env.get(origin);
				}
				catch (ScribbleException e)
				{
					errors.add("Cannot determine name of " + c.payload + ": " + e);
					payload = "ERROR";
				}
			}
			else
			{
				throw new RuntimeException("BUG: unsupported payload " + c.payload);
			}
			String cont = ScalaChannelTypeExtractor.apply(c.cont, nameEnv);
			res = "case class " + l.name + "(p: " + payload; // Provide ")" below!
			if (option instanceof Option.Default)
			{
				res += ")(val cont: " + cont + ")";
			}
			else if (option instanceof Option.MPInputMessages)
			{
				Option.MPInputMessages o = (Option.MPInputMessages)option;
				// If the class is not required, skip it - FIXME: do it earlier
				if ((xtnds.isEmpty()) && (!o.classNames.contains(l.name)))
				{
					return "";
				}
				
				res += ", cont: " + o.contPrefix + cont + ")";
			}
			else if (option instanceof Option.MPOutputMessages)
			{
				Option.MPOutputMessages o = (Option.MPOutputMessages)option;
				// If the class is not required, skip it - FIXME: do it earlier
				if ((xtnds.isEmpty()) && (!o.classNames.contains(l.name)))
				{
					return "";
				}
				
				res += ")";
			}
			else
			{
				throw new RuntimeException("BUG: unsupported option " + option);
			}
			
			if (xtnds != null)
			{
				res += " extends " + xtnds;
			}
			res += "\n";
		}
		catch (ScribbleException e)
		{
			errors.add("Cannot extract protocol of " + node + ": " + e);
			return "";
		}
		return res;
	}
}
