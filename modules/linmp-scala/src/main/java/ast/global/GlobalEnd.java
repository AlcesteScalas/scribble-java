package ast.global;


public class GlobalEnd implements GlobalType
{
	public GlobalEnd()
	{

	}
	
	@Override 
	public String toString()
	{
		return "end";
	}
	
	@Override
	public int hashCode()
	{
		return 997;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof GlobalEnd)
		{
			return true;
		}
		return false;
	}
}
