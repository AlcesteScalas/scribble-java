package ast.global;

import ast.name.MessageLab;
import ast.name.RecVar;

import java.util.Map;

import org.scribble.main.ScribbleException;

/** Perform alpha-conversion on a global type AST.
 *
 *  @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public class GlobalTypeAlphaConverter extends GlobalTypeVisitor<GlobalType>
{
	private RecVar old; // Original variable
	private RecVar conv; // Alpha-converted variable
	private boolean open = false; // Becomes true when "old" is traversed
	static private GlobalType gtype;
	
	/** Sanitize the given global type
	 * 
	 * @param g Global type to be alpha-converted
	 * @param old Old variable name
	 * @param conv New variable name
	 * @return An alpha-converted version of the given global type
	 */
	public static GlobalType apply(GlobalType g, RecVar old, RecVar conv) throws ScribbleException
	{
		GlobalTypeAlphaConverter s = new GlobalTypeAlphaConverter(g, old, conv);
		return s.process();
	}
	
	private GlobalTypeAlphaConverter(GlobalType g, RecVar old, RecVar conv)
	{
		gtype = g;
		this.old = old;
		this.conv = conv;
	}
	
	@Override
	public GlobalType process() throws ScribbleException
	{
		return visit(gtype);
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
			cases2.put(x.getKey(), new GlobalSendCase(c.pay, visit(c.body)));
		}
		
		return new GlobalSend(node.src, node.dest, cases2);
	}

	@Override
	protected GlobalRec visit(GlobalRec node)
	{
		if (node.recvar == old)
		{
			if (open)
			{
			    // We have found a distinct occurrence of "old": don't convert
			    return node;
			}
			// We have found a top-level occurrence of "old": let's convert
			open = true; // Rememer that "old" is open in the recursion body
			GlobalRec res = new GlobalRec(conv, visit(node.body));
			open = false; // We are leaving the recursion body
			return res;
		}
		return new GlobalRec(conv, visit(node.body));
	}

	@Override
	protected RecVar visit(RecVar node) {
		if ((node == old) && open) {
			return conv;
		}
		// Here we might also spot unbound occurrences of "old",
		// but we take care of them in other processing steps 
		return node;
	}
}
