package ast.name;

public class Role extends NameNode implements Comparable<Role>
{
	public Role(String name)
	{
		super(name);
	}

	@Override
	public boolean canEqual(Object o)
	{
		return (o instanceof Role);
	}
	
	@Override
	public int compareTo(Role o)
	{
		return this.name.compareTo(o.name);
	}
}
