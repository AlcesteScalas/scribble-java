package org.scribble.ast.global;

import java.util.List;

import org.scribble.ast.AstFactoryImpl;
import org.scribble.ast.Constants;
import org.scribble.ast.MessageNode;
import org.scribble.ast.MessageTransfer;
import org.scribble.ast.name.simple.RoleNode;
import org.scribble.del.ScribDel;
import org.scribble.sesstype.kind.Global;
import org.scribble.util.ScribUtil;

public class GMessageTransfer extends MessageTransfer<Global> implements GSimpleInteractionNode
{
	public GMessageTransfer(RoleNode src, MessageNode msg, List<RoleNode> dests)
	{
		super(src, msg, dests);
	}

	@Override
	protected GMessageTransfer copy()
	{
		return new GMessageTransfer(this.src, this.msg, getDestinations());
	}
	
	@Override
	public GMessageTransfer clone()
	{
		RoleNode src = this.src.clone();
		MessageNode msg = this.msg.clone();
		List<RoleNode> dests = ScribUtil.cloneList(getDestinations());
		return AstFactoryImpl.FACTORY.GMessageTransfer(src, msg, dests);
	}

	@Override
	public GMessageTransfer reconstruct(RoleNode src, MessageNode msg, List<RoleNode> dests)
	{
		ScribDel del = del();
		GMessageTransfer gmt = new GMessageTransfer(src, msg, dests);
		gmt = (GMessageTransfer) gmt.del(del);
		return gmt;
	}

	// FIXME: shouldn't be needed, but here due to Eclipse bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=436350
	@Override
	public Global getKind()
	{
		return GSimpleInteractionNode.super.getKind();
	}

	@Override
	public String toString()
	{
		List<RoleNode> dests = getDestinations();
		StringBuilder sb = new StringBuilder(this.msg + " " + Constants.FROM_KW + " " + this.src + " " + Constants.TO_KW + " ");
		sb.append(dests.get(0));
		dests.subList(1, dests.size()).stream().forEach((dest) ->
				{
					sb.append(", " + dest);
				});
		sb.append(";");
		return sb.toString();
	}
}