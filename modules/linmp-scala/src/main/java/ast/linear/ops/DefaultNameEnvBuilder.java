package ast.linear.ops;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import ast.name.Label;
import ast.name.RecVar;

public class DefaultNameEnvBuilder extends Visitor<NameEnv>
{
	private final Type visiting;
	
	public static Map<AbstractVariant, String> apply(Type t) throws ScribbleException
	{
		DefaultNameEnvBuilder b = new DefaultNameEnvBuilder(t);
		
		return b.process();
	}
	
	private DefaultNameEnvBuilder(Type t)
	{
		visiting = t;
	}
	
	@Override
	protected NameEnv process() throws ScribbleException
	{
		return visit(visiting);
	}
	
	@Override
	protected NameEnv visit(End node)
	{
		return new NameEnv();
	}

	@Override
	protected NameEnv visit(In node)
	{
		return visit(node.variant);
	}

	@Override
	protected NameEnv visit(Out node)
	{
		return visit(node.variant);
	}
	
	private NameEnv visit(AbstractVariant v)
	{
		// FIXME: what about adding an AbstractVariant visitor?
		if (v instanceof Variant)
		{
			Variant vrnt = (Variant)v;
			NameEnv res = new NameEnv();
			res.put(v, fromLabels(vrnt.cases.keySet()));
			
			for (Case c: vrnt.cases.values())
			{
				if (c.payload instanceof Record)
				{
					throw new RuntimeException("TODO"); // FIXME!
				}
				else if (c.payload instanceof ast.name.BaseType)
				{
					// Nothing to do
				}
				else
				{
					throw new RuntimeException("BUG: unsupported payload type " + c.payload);
				}
				res.putAll(visit(c.cont));
			}
			return res;
		}
		else if (v instanceof Rec)
		{
			AbstractVariant body = ((Rec)v).body;
			NameEnv res = visit(body);
			// Ensure that the recursive variant, its variable and its body
			// are mapped to the same name 
			res.put(v, res.get(body));
			res.put(((Rec)v).recvar, res.get(body));
			return res;
		}
		else if (v instanceof RecVar)
		{
			// The RecVar is associated to a name in the Rec case above
			return new NameEnv();
		}
		else
		{
			throw new RuntimeException("BUG: unsupported variant type " + v);
		}
	}
	
	private String fromLabels(Set<Label> labels)
	{
		java.util.List<String> ls = new java.util.ArrayList<>(new java.util.TreeSet<String>(labels.stream().map(l -> l.name).collect(Collectors.toSet())));
		String base = ls.remove(0);
		return ls.stream().reduce(base, (l1, l2) -> l1 + "Or" + l2);
	}
}
