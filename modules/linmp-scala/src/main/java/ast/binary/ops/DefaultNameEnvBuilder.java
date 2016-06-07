package ast.binary.ops;

import java.util.Collection;

import org.scribble.main.ScribbleException;

import ast.binary.Branch;
import ast.binary.End;
import ast.binary.NameEnv;
import ast.binary.Rec;
import ast.binary.Select;
import ast.binary.Type;
import ast.binary.Visitor;
import ast.name.RecVar;

public class DefaultNameEnvBuilder extends Visitor<NameEnv>
{
	private Collection<String> errors = new java.util.LinkedList<String>();
	private final Type visiting; 
	
	public static NameEnv apply(Type t) throws ScribbleException
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
		NameEnv res = visit(visiting);
		if (errors.isEmpty())
		{
			return res;
		}
		throw new ScribbleException("Error(s) assigning names to " + visiting + ": "
				                    + String.join(";", errors));
	}

	@Override
	protected NameEnv visit(End node)
	{
		return new NameEnv();
	}

	@Override
	protected NameEnv visit(Branch node)
	{
		NameEnv res = new NameEnv();
		res.put(node, ast.linear.ops.DefaultNameEnvBuilder.nameChoiceFromLabels(node.cases.keySet()));
		return res;
	}

	@Override
	protected NameEnv visit(Select node)
	{
		NameEnv res = new NameEnv();
		res.put(node, ast.linear.ops.DefaultNameEnvBuilder.nameChoiceFromLabels(node.cases.keySet()));
		return res;
	}

	@Override
	protected NameEnv visit(Rec node)
	{
		Type body = node.body;
		NameEnv res = visit(body);
		// Ensure that the recursive type, its variable and its body
		// are mapped to the same name
		res.put(node, res.get(body));
		res.put(node.recvar, res.get(body));
		return res;
	}

	@Override
	protected NameEnv visit(RecVar node)
	{
		// The RecVar is associated to a name in the Rec case above
		return new NameEnv();
	}
	
	
}
