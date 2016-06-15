package ast.global;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.scribble.ast.MessageSigNode;
import org.scribble.ast.context.ModuleContext;
import org.scribble.ast.global.GChoice;
import org.scribble.ast.global.GContinue;
import org.scribble.ast.global.GInteractionNode;
import org.scribble.ast.global.GMessageTransfer;
import org.scribble.ast.global.GProtocolBlock;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.ast.global.GProtocolDef;
import org.scribble.ast.global.GRecursion;
import org.scribble.del.global.GProtocolDefDel;
import org.scribble.main.ScribbleException;
import org.scribble.sesstype.SessionTypeFactory;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.visit.JobContext;

import ast.AstFactory;
import ast.PayloadType;
import ast.local.ops.Merge;
import ast.local.ops.Sanitizer;
import ast.name.Label;
import ast.name.RecVar;
import ast.name.Role;

public class GlobalTypeTranslator
{
	private final AstFactory factory = new AstFactory();
	//private final LocalTypeParser ltp = new LocalTypeParser();

	public GlobalTypeTranslator()
	{

	}

	// merge is for projection of "delegation payload types"
	public GlobalType translate(JobContext jobc, ModuleContext mainc, Merge.Operator merge, GProtocolDecl gpd) throws ScribbleException
	{
		GProtocolDef inlined = ((GProtocolDefDel) gpd.def.del()).getInlinedProtocolDef();
		return translate(jobc, mainc, merge, inlined);
	}
	
	public GlobalType translate(JobContext jobc, ModuleContext mainc, Merge.Operator merge, GProtocolDef gpd) throws ScribbleException
	{
		return parseSeq(jobc, mainc, merge, gpd.getBlock().getInteractionSeq().getInteractions());
	}
	
	private GlobalType parseSeq(JobContext jobc, ModuleContext mainc, Merge.Operator merge, List<GInteractionNode> is) throws ScribbleException
	{
		//List<GInteractionNode> is = block.getInteractionSeq().getInteractions();
		if (is.isEmpty())
		{
			return this.factory.GlobalEnd();
		}
		else
		{
			GInteractionNode first = is.get(0);
			if (first instanceof GMessageTransfer)
			{
				GMessageTransfer gmt = (GMessageTransfer) first;
				Role src = this.factory.Role(gmt.src.toString());
				if (gmt.getDestinations().size() > 1)
				{
					throw new RuntimeException("[TODO]: " + gmt);
				}
				Role dest = this.factory.Role(gmt.getDestinations().get(0).toString());
				if (!gmt.msg.isMessageSigNode())
				{
					throw new RuntimeException("[TODO]: " + gmt);
				}
				MessageSigNode msn = ((MessageSigNode) gmt.msg);
				Label lab = this.factory.MessageLab(msn.op.toString());
				PayloadType pay = null;
				if (msn.payloads.getElements().size() > 1)
				{
					throw new RuntimeException("[TODO]: " + gmt);
				}
				else if (!msn.payloads.getElements().isEmpty())
				{
					String tmp = msn.payloads.getElements().get(0).toString().trim();
					/*if (tmp.length() > 1 && tmp.startsWith("\"") && tmp.endsWith("\""))  // Obsoleted by DELEGATION payloadelement
					{
						tmp = tmp.substring(1, tmp.length() - 1);
						pay = this.ltp.parse(tmp);
						if (pay == null)
						{
							throw new RuntimeException("Shouldn't get in here: " + tmp);
						}
					}*/
					int i = tmp.indexOf('@');
					if (i != -1)
					{
						GProtocolName proto = SessionTypeFactory.parseGlobalProtocolName(tmp.substring(0, i));  // Should already be full name (DelegationElem disamb)
						Role role = new Role(tmp.substring(i+1, tmp.length()));

						GProtocolName fullname = (GProtocolName) mainc.getVisibleProtocolDeclFullName(proto);
						GProtocolDecl gpd = (GProtocolDecl) jobc.getModule(fullname.getPrefix()).getProtocolDecl(fullname.getSimpleName());  // FIXME: cast
						//GlobalType gt = new GlobalTypeTranslator().translate(jobc, mainc, merge, gpd);
						GlobalType gt = translate(jobc, mainc, merge, gpd);
						pay = Sanitizer.apply(ast.global.ops.Projector.apply(gt, role, merge));
					}
					else
					{
						pay = this.factory.BaseType(tmp);
					}
				}
				GlobalType cont = parseSeq(jobc, mainc, merge, is.subList(1, is.size()));
				Map<Label, GlobalSendCase> cases = new HashMap<>();
				cases.put(lab, this.factory.GlobalSendCase(pay, cont));
				return this.factory.GlobalSend(src, dest, cases);
			}
			else if (first instanceof GChoice)
			{
				if (is.size() > 1)
				{
					throw new RuntimeException("[TODO]: " + is);
				}
				GChoice gc = (GChoice) first; 
				/*List<GlobalType> parsed = gc.getBlocks().stream()
						.map((b) -> parseSeq(b.getInteractionSeq().getInteractions()))
						.collect(Collectors.toList());*/
				List<GlobalType> parsed = new LinkedList<>();
				for (GProtocolBlock b : gc.getBlocks())
				{
					parsed.add(parseSeq(jobc, mainc, merge, b.getInteractionSeq().getInteractions()));
				}
				Role src = null;
				Role dest = null;
				Map<Label, GlobalSendCase> cases = new HashMap<>();
				for (GlobalType p : parsed)
				{
					if (!(p instanceof GlobalSend))
					{
						throw new RuntimeException("Shouldn't get in here: " + p);
					}
					GlobalSend tmp = (GlobalSend) p;
					if (src == null)
					{
						src = tmp.src;
						dest = tmp.dest;
					}
					tmp.cases.entrySet().forEach((e) -> cases.put(e.getKey(), e.getValue()));
				}
				return this.factory.GlobalSend(src, dest, cases);
			}
			else if (first instanceof GRecursion)
			{
				if (is.size() > 1)
				{
					throw new RuntimeException("[TODO]: " + is);
				}
				GRecursion gr = (GRecursion) first;
				RecVar recvar = this.factory.RecVar(gr.recvar.toString());
				GlobalType body = parseSeq(jobc, mainc, merge, gr.getBlock().getInteractionSeq().getInteractions());
				return new GlobalRec(recvar, body);
			}
			else if (first instanceof GContinue)
			{
				if (is.size() > 1)
				{
					throw new RuntimeException("Shouldn't get in here: " + is);
				}
				return this.factory.RecVar(((GContinue) first).recvar.toString());
			}
			else
			{
				throw new RuntimeException("Shouldn't get in here: " + first);
			}
		}
	}
}
