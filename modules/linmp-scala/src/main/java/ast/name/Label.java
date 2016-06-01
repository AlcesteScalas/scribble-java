package ast.name;

public class Label extends NameNode implements Comparable<Label>
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

	@Override
	public int compareTo(Label o)
	{
		return this.name.compareTo(o.name);
	}
}
