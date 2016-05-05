/**
 * 
 */
package ast.global;

import ast.local.LocalType;
import ast.local.LocalTypeSanitizer;

import ast.name.MessageLab;
import ast.name.RecVar;

import java.util.Collection;
import java.util.Map;

import org.scribble.main.ScribbleException;

/** Perform sanity checks on a global type AST
 * @author ascalas
 *
 */
public class GlobalTypeSanitizer extends GlobalTypeVisitor<GlobalType>
{
	private Collection<RecVar> bound = new java.util.HashSet<RecVar>();
	private Collection<String> errors = new java.util.LinkedList<String>();
	static private GlobalType gtype;
	
	/** Sanitize the given global type
	 * 
	 * @param g Global type to be sanitized
	 * @return A sanitized version of the given global type
	 * @throws ScribbleException
	 */
	public static GlobalType apply(GlobalType g) throws ScribbleException
	{
		GlobalTypeSanitizer s = new GlobalTypeSanitizer(g);
		return s.process();
	}
	
	private GlobalTypeSanitizer(GlobalType g)
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
					pay = LocalTypeSanitizer.apply((LocalType)c.pay);
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
	protected GlobalRec visit(GlobalRec node)
	{
		// FIXME: here we are assuming that all recursion vars are distinct
		this.bound.add(node.recvar);
		GlobalRec r = new GlobalRec(node.recvar, visit(node.body));
		this.bound.remove(node.recvar);
		return r;
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