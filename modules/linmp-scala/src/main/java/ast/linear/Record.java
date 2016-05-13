package ast.linear;

import java.util.Map;
import java.util.Set;

import ast.name.Role;

public class Record implements Payload
{
	public final Map<Role, Type> types;
	
	Record(Map<Role, Type> types)
	{
		this.types = types;
	}
	
	public Set<Role> roles()
	{
		return types.keySet();
	}
	
	@Override 
	public String toString()
	{
		return "{" + types + "}";
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((types == null) ? 0 : types.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof Record))
		{
			return false;
		}
		Record other = (Record) obj;
		if (types == null)
		{
			if (other.types != null)
			{
				return false;
			}
		} else if (!types.equals(other.types))
		{
			return false;
		}
		return true;
	}
}
