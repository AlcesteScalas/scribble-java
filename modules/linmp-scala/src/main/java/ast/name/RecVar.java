package ast.name;

import java.util.Set;

import ast.global.GlobalType;
import ast.local.LocalType;

public class RecVar extends NameNode implements GlobalType, LocalType
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
	public boolean canEqual(Object o)
	{
		return (o instanceof RecVar);
	}
}
