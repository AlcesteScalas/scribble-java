package ast.name;

import ast.global.GlobalType;
import ast.global.GlobalTypeVisitor;
import ast.local.LocalType;

public class RecVar extends NameNode implements GlobalType, LocalType
{
	public RecVar(String name)
	{
		super(name);
	}

	@Override
	public boolean canEqual(Object o)
	{
		return (o instanceof RecVar);
	}
	
	@Override
	public void accept(GlobalTypeVisitor v) {
		v.visit(this);
	}
}
