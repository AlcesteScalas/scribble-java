package org.scribble.ast;

import org.scribble.main.ScribbleException;
import org.scribble.sesstype.kind.ProtocolKind;
import org.scribble.visit.AstVisitor;


public abstract class ProtocolDef<K extends ProtocolKind> extends ScribNodeBase implements ProtocolKindNode<K>
{
	public final ProtocolBlock<K> block;

	protected ProtocolDef(ProtocolBlock<K> block)
	{
		this.block = block;
	}
	
	protected abstract ProtocolDef<K> reconstruct(ProtocolBlock<K> block);

	public abstract ProtocolBlock<K> getBlock();
	
	@Override
	public ProtocolDef<K> visitChildren(AstVisitor nv) throws ScribbleException
	{
		ProtocolBlock<K> block = visitChildWithStrictClassCheck(this, this.block, nv);
		return reconstruct(block);
	}

	@Override
	public String toString()
	{
		return this.block.toString();
	}
}
