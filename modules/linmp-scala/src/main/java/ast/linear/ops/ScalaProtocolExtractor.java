package ast.linear.ops;

import org.scribble.main.ScribbleException;

import ast.linear.AbstractVariant;
import ast.linear.Case;
import ast.linear.End;
import ast.linear.In;
import ast.linear.Out;
import ast.linear.Rec;
import ast.linear.Type;
import ast.linear.Variant;
import ast.linear.Visitor;
import ast.name.Label;
import ast.name.RecVar;

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
		throw new RuntimeException("BUG: cannot extract protocol of " + node);
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
			Variant v = (Variant)av;
			if (v.cases.size() == 1)
			{
				Label l = v.cases.keySet().iterator().next();
				Case c = v.cases.get(l);
				String res;
				try {
					String payload;
					
					if (c.payload instanceof ast.name.BaseType)
					{
						payload = c.payload.toString();
					}
					else if (c.payload instanceof Type)
					{
						Type p = (Type)c.payload;
						try
						{
							payload = ScalaChannelTypeExtractor.apply(p, nameEnv);
						}
						catch (ScribbleException e)
						{
							errors.add("Cannot extract protocol of " + node + ": " + e);
							return "";
						}
					}
					else
					{
						throw new RuntimeException("BUG: unsupported payload " + c.payload);
					}
					String cont = ScalaChannelTypeExtractor.apply(c.cont, nameEnv);
					res = "case class " + l + "(p: " + payload + ")(val cont: " + cont + ")";
				}
				catch (ScribbleException e)
				{
					errors.add("Cannot extract protocol of " + node + ": " + e);
					return "";
				}
				return res;
			}
			else if (v.cases.size() > 1)
			{
				// FIXME: TODO
			}
			else
			{
				throw new RuntimeException("BUG: found 0-branches variant " + v);
			}
		}
		else if (av instanceof Rec)
		{
			return fromVariant(((Rec)av).body, node);
		}
		else if (av instanceof RecVar)
		{
			String res = nameEnv.get((RecVar)av);
			if (res == null)
			{
				errors.add("Cannot find " + av + "in name environment: " + nameEnv);
				return "";
			}
			return res;
		}
		else
		{
			throw new RuntimeException("BUG: unsupported variant-like type " + av);
		}
	}
}
