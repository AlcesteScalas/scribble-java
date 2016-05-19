package ast.name;

import java.util.Set;

import ast.binary.Type;
import ast.global.GlobalType;
import ast.linear.AbstractVariant;
import ast.local.LocalType;

public class RecVar extends NameNode implements GlobalType, LocalType, Type, AbstractVariant
{
	public RecVar(String name)
	{
		super(name);
	}
	
	@Override
	public Set<RecVar> freeVariables()
	{
		return java.util.Collections.singleton(this);
	}
	
	@Override
	public Set<Role> roles()
	{
		return java.util.Collections.emptySet();
	}
	
	@Override
	public Type.Action action()
	{
		return Type.Action.input();
	}
	
	@Override
	public boolean canEqual(Object o)
	{
		return (o instanceof RecVar);
	}
}
