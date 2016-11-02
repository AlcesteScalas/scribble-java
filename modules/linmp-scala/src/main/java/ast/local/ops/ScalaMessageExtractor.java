/**
 * 
 */
package ast.local.ops;

import org.scribble.main.ScribbleException;

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

import java.util.Collection;
import java.util.Map;

/**
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 *
 */
public class ScalaMessageExtractor extends LocalTypeVisitor<String>
{
	public static String MESSAGE_CLASSES_PFX = "Msg";
	
	// What kind of message classes are we extracting?
	private enum Option {
		INPUTS, OUTPUTS
	}
	
	private Collection<String> errors = new java.util.LinkedList<String>();
	private final LocalType visiting;
	private final LocalNameEnv nameEnv;
	private final Option option;
	
	public static String inputs(LocalType t, LocalNameEnv nameEnv) throws ScribbleException
	{
		ScalaMessageExtractor te = new ScalaMessageExtractor(t, nameEnv, Option.INPUTS);
		
		return te.process();
	}
	
	public static String outputs(LocalType t, LocalNameEnv nameEnv) throws ScribbleException
	{
		ScalaMessageExtractor te = new ScalaMessageExtractor(t, nameEnv, Option.OUTPUTS);
		
		return te.process();
	}
	
	protected ScalaMessageExtractor(LocalType t, LocalNameEnv env, Option opt)
	{
		visiting = t;
		nameEnv = env;
		option = opt;
	}
	
	@Override
	protected String process() throws ScribbleException
	{
		String res = visit(visiting);
		if (errors.isEmpty())
		{
			return res;
		}
		
		String what = (option == Option.INPUTS ? "input" : "output");
		throw new ScribbleException("Error(s) extracting " + what + " messages of " + visiting + ": "
				                    + String.join("; ", errors));
	}
	
	@Override
	protected String visit(LocalEnd node)
	{
		return "";
	}
	
	@Override
	protected String visit(LocalBranch node)
	{
		String res = "";
		String xtnds = "";
		
		if (option == Option.INPUTS)
		{
			if (node.cases.keySet().size() > 1)
			{
				xtnds = MESSAGE_CLASSES_PFX + nameEnv.get(node);
				res += "sealed abstract class " + xtnds + "\n";
			}
			
			for (Map.Entry<Label, LocalCase> e: node.cases.entrySet())
			{
				res += "case class " + e.getKey().name + "(";
				LocalCase c = e.getValue();

				if (c.pay instanceof BaseType)
				{
					res += "p: " + c.pay; // Directly represent payload type
				}
				else if (c.pay instanceof LocalType)
				{
					res += "p: " + nameEnv.get((LocalType)c.pay);
				}
				else
				{
					throw new RuntimeException("BUG: unsupported payload type: " + c.pay);
				}

				if (c.body instanceof LocalEnd)
				{
					// Nothing to do (continuation omitted)
				}
				else
				{
					res += ", cont: " + nameEnv.get(c.body);
				}

				res += ")";
				res += (xtnds.isEmpty() ? "" : " extends " + xtnds);
				res += "\n";
			}
			res += "\n";
		}
		
		// Finally, visit the continuations
		for (LocalCase c: node.cases.values())
		{
			res += visit(c.body);
		}
		return res;
	}
	
	@Override
	protected String visit(LocalSelect node)
	{
		String res = "";
		String xtnds = "";
		
		if (option == Option.OUTPUTS)
		{
			if (node.cases.keySet().size() > 1)
			{
				// Here we could generate sealed abstract class, but not needed
				// xtnds = MESSAGE_CLASSES_PFX + nameEnv.get(node);
				// res += "sealed abstract class " + xtnds + "\n";
			}
			
			for (Map.Entry<Label, LocalCase> e: node.cases.entrySet())
			{
				res += "case class " + e.getKey().name + "(";
				LocalCase c = e.getValue();

				if (c.pay instanceof BaseType)
				{
					res += "p: " + c.pay; // Directly represent payload type
				}
				else if (c.pay instanceof LocalType)
				{
					res += "p: " + nameEnv.get((LocalType)c.pay);
				}
				else
				{
					throw new RuntimeException("BUG: unsupported payload type: " + c.pay);
				}
				
				res += ")";
				res += (xtnds.isEmpty() ? "" : " extends " + xtnds);
				res += "\n";
			}
			res += "\n";
		}
		
		// Finally, visit the continuations
		for (LocalCase c: node.cases.values())
		{
			res += visit(c.body);
		}
		return res;
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
