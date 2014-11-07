package org.scribble2.model.name.qualified;

import org.scribble2.model.MessageNode;
import org.scribble2.sesstype.name.MessageSignatureName;
import org.scribble2.sesstype.name.ModuleName;

public class MessageSignatureNameNode extends MemberNameNode implements MessageNode
{
	/*// FIXME: not syntax
	public final String schema;
	public final String extName;
	public final String source;*/
	
	//public PayloadTypeNameNodes(PrimitiveNameNode... ns)
	//public MessageSignatureNameNodes(CommonTree ct, String... ns, String schema, String extName, String source)
	public MessageSignatureNameNode(String... ns)
	{
		super(ns);
	}

	@Override
	protected MessageSignatureNameNode copy()
	{
		return new MessageSignatureNameNode(this.elems);
	}

	/*// Basically a copy without the AST
	@Override
	public MessageSignatureNameNodes leaveProjection(Projector proj) //throws ScribbleException
	{
		//MessageSignatureNameNodes projection = new MessageSignatureNameNodes(null, getElements(), this.schema, this.extName, this.source);
		MessageSignatureNameNodes projection = new MessageSignatureNameNodes(null, getElements());
		this.setEnv(new ProjectionEnv(proj.getJobContext(), proj.getModuleContext(), projection));
		return this;
	}*/
	
	@Override
	public MessageSignatureName toName()
	{
		String membname = getLastElement();
		if (!isPrefixed())
		{
			return new MessageSignatureName(membname);
		}
		//ModuleName modname = ModuleNameNodes.toModuleName(getModulePrefix());
		ModuleName modname = getModulePrefix().toName();
		return new MessageSignatureName(modname, membname);
	}

	/*@Override
	public MessageSignatureName toArgument()
	{
		return toName();
	}

	@Override
	public Message toMessage()
	{
		return toArgument();
	}

	public boolean isMessageSignatureNode()
	{
		return false;
	}

	@Override
	public boolean isPayloadTypeNode()
	{
		return false;
	}

	@Override
	public boolean isParameterNode()
	{
		return false;
	}

	@Override
	public boolean isAmbiguousNode()
	{
		return false;
	}*/
}
