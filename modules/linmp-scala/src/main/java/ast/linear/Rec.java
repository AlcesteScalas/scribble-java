package ast.linear;

import java.util.Set;

import ast.name.RecVar;

public class Rec implements AbstractVariant
{
	private final RecVar recvar;
	private final AbstractVariant body;
	
	public Rec(RecVar recvar, AbstractVariant body)
	{
		this.recvar = recvar;
		this.body = body;
	}

	@Override
	public Set<RecVar> freeVariables()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String toString()
	{
		return "mu " + recvar + "." + body;
	}

	@Override
	public int hashCode()
	{
		final int prime = 37;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((recvar == null) ? 0 : recvar.hashCode());
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
		if (!(obj instanceof Rec))
		{
			return false;
		}
		Rec other = (Rec) obj;
		if (body == null)
		{
			if (other.body != null)
			{
				return false;
			}
		} else if (!body.equals(other.body))
		{
			return false;
		}
		if (recvar == null)
		{
			if (other.recvar != null)
			{
				return false;
			}
		} else if (!recvar.equals(other.recvar))
		{
			return false;
		}
		return true;
	}
}
