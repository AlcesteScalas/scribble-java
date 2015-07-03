package org.scribble.ast.name.qualified;

import org.scribble.ast.AstFactoryImpl;
import org.scribble.sesstype.kind.Global;
import org.scribble.sesstype.name.GProtocolName;



public class GProtocolNameNode extends ProtocolNameNode<Global>
{
	public GProtocolNameNode(String... ns)
	{
		super(ns);
	}

	@Override
	protected GProtocolNameNode copy()
	{
		return new GProtocolNameNode(this.elems);
	}
	
	@Override
	public GProtocolNameNode clone()
	{
		return (GProtocolNameNode) AstFactoryImpl.FACTORY.QualifiedNameNode(Global.KIND, this.elems);
	}
	
	@Override
	public GProtocolName toName()
	{
		GProtocolName membname = new GProtocolName(getLastElement());
		return isPrefixed()
				? new GProtocolName(getModuleNamePrefix(), membname)
				: membname;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof GProtocolNameNode))
		{
			return false;
		}
		return ((GProtocolNameNode) o).canEqual(this) && super.equals(o);
	}
	
	public boolean canEqual(Object o)
	{
		return o instanceof GProtocolNameNode;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 419;
		hash = 31 * hash + this.elems.hashCode();
		return hash;
	}

}
