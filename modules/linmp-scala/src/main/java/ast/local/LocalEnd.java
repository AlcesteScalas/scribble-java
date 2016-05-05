package ast.local;

public class LocalEnd implements LocalType
{
	//public final Role self;
	
	//public LocalEnd(Role self)
	public LocalEnd()
	{
		//this.self = self;
	}
	
	@Override 
	public String toString()
	{
		return "end";
	}
	
	@Override
	public int hashCode()
	{
		return 977;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof LocalEnd)
		{
			return true;
		}
		return false;
	}
}
