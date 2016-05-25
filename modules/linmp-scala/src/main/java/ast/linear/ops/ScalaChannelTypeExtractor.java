package ast.linear.ops;

import java.util.Collection;
import java.util.Map;

import org.scribble.main.ScribbleException;

import ast.linear.AbstractVariant;
import ast.linear.End;
import ast.linear.In;
import ast.linear.Out;
import ast.linear.Type;
import ast.linear.Visitor;

/** Build the Scala channel type corresponding to a linear I/O type.
 * 
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public class ScalaChannelTypeExtractor extends Visitor<String>
{
	private Collection<String> errors = new java.util.LinkedList<String>();
	private final Type visiting;
	private Map<AbstractVariant, String> nameEnv;
	
	public static String apply(Type t, Map<AbstractVariant, String> nameEnv) throws ScribbleException
	{
		ScalaChannelTypeExtractor te = new ScalaChannelTypeExtractor(t, nameEnv);
		
		return te.process();
	}
	
	private ScalaChannelTypeExtractor(Type t, Map<AbstractVariant, String> nameEnv)
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
		throw new ScribbleException("Error(s) extracting channel type of " + visiting + ": "
				                    + String.join(";", errors));
	}

	@Override
	protected String visit(End node)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String visit(In node)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String visit(Out node)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
