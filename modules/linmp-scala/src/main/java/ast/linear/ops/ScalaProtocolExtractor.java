package ast.linear.ops;

import org.scribble.main.ScribbleException;

import ast.linear.AbstractVariant;
import ast.linear.Case;
import ast.linear.End;
import ast.linear.In;
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
import ast.name.Role;

import java.util.Collection;
import java.util.Map;

/** Build the Scala case classes definitions corresponding to a linear I/O type.
 * 
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public class ScalaProtocolExtractor extends Visitor<String>
{
	private Collection<String> errors = new java.util.LinkedList<String>();
	private final Type visiting;
	private Map<AbstractVariant, String> nameEnv;
	
	public static String apply(Type t) throws ScribbleException
	{
		return apply(t, DefaultNameEnvBuilder.apply(t));
	}
	
	public static String apply(Type t, Map<AbstractVariant, String> nameEnv) throws ScribbleException
	{
		ScalaProtocolExtractor te = new ScalaProtocolExtractor(t, nameEnv);
		
		return te.process();
	}
	
	private ScalaProtocolExtractor(Type t, Map<AbstractVariant, String> nameEnv)
	{
		this.visiting = t;
		this.nameEnv = nameEnv;
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
			res = "case class " + l + "(p: " + payload + ")(val cont: " + cont + ")";
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
