package ast.name;

import ast.PayloadType;

public class BaseType extends NameNode implements PayloadType, ast.linear.Payload
{
	public BaseType(String name)
	{
		super(name);
	}
	
	@Override
	public boolean canEqual(Object o)
	{
		return (o instanceof BaseType);
	}
}
