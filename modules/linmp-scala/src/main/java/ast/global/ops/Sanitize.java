package ast.global.ops;

import ast.global.GlobalEnd;
import ast.global.GlobalRec;
import ast.global.GlobalSend;
import ast.global.GlobalSendCase;
import ast.global.GlobalType;
import ast.global.GlobalTypeVisitor;
import ast.local.LocalType;
import ast.name.MessageLab;
import ast.name.RecVar;

import java.util.Collection;
import java.util.Map;

import org.scribble.main.ScribbleException;

/** Perform sanity checks on a global type AST
 * 
 *  @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public class Sanitize extends GlobalTypeVisitor<GlobalType>
{
	private Collection<RecVar> bound = new java.util.HashSet<RecVar>();
	private Collection<String> errors = new java.util.LinkedList<String>();
	static private GlobalType gtype;
	
	/** Sanitize the given global type.
	 * 
	 * A sanitized type all "vacuous" recursions removed, and all recursion
	 * variables pairwise distinct (a form of Ottmann/Barendregt convention).
	 * 
	 * @param g Global type to be sanitized
	 * @return A sanitized version of the given global type
	 * @throws ScribbleException
	 */
	public static GlobalType apply(GlobalType g) throws ScribbleException
	{
		Sanitize s = new Sanitize(g);
		return s.process();
	}
	
	private Sanitize(GlobalType g)
	{
		gtype = g;
	}
	
	@Override
	public GlobalType process() throws ScribbleException
	{
		GlobalType res = visit(gtype);
		if (errors.isEmpty())
		{
			return res;
		}
		throw new ScribbleException("Error(s) validating " + gtype + ": "
				                    + String.join(";", errors));
	}
	
	@Override
	protected GlobalEnd visit(GlobalEnd node)
	{
		return node;
	}
	
	@Override
	protected GlobalSend visit(GlobalSend node)
	{
		Map<MessageLab, GlobalSendCase> cases2 = new java.util.HashMap<MessageLab, GlobalSendCase>();
		for (Map.Entry<MessageLab, GlobalSendCase> x: node.cases.entrySet())
		{
			GlobalSendCase c = x.getValue();
			ast.PayloadType pay = c.pay;
			
			if (c.pay instanceof LocalType)
			{
				try
				{
					pay = ast.local.ops.Sanitize.apply((LocalType)c.pay);
				}
				catch (ScribbleException e)
				{
					errors.add(e.toString());
				}
			}
			
			cases2.put(x.getKey(), new GlobalSendCase(pay, visit(c.body)));
		}
		
		return new GlobalSend(node.src, node.dest, cases2);
	}

	@Override
	protected GlobalType visit(GlobalRec node)
	{
		RecVar var = node.recvar;
		
		if (!node.body.freeVariables().contains(var))
		{
			// The recursion is vacuous: let's skip it
			return visit(node.body);
		}
		
		if (this.bound.contains(var))
		{
			// The recursion re-binds a variable, let's alpha-convert
			try
			{
				return visit(AlphaConvert.apply(node, var,
															new RecVar(var.name+"'")));
			}
			catch (ScribbleException e)
			{
				errors.add(e.toString());
			}
			return node;
		}
		
		this.bound.add(var);
		GlobalRec res = new GlobalRec(var, visit(node.body));
		this.bound.remove(var);
		return res;
	}

	@Override
	protected RecVar visit(RecVar node) {
		if (!this.bound.contains(node))
		{
			errors.add("Unbound variable: " + node);
		}
		
		return node;
	}
}
