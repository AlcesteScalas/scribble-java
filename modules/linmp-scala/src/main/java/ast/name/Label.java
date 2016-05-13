package ast.name;

public class Label extends NameNode
{
	public Label(String name)
	{
		super(name);
	}

	@Override
	public boolean canEqual(Object o)
	{
		return (o instanceof Label);
	}
}
