package ast.local;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import ast.name.MessageLab;
import ast.name.Role;

public class LocalBranch implements LocalType
{
	//public final Role self;
	
	public final Role src;
	public final Map<MessageLab, LocalCase> cases;
	
	//public LocalBranch(Role self, Role src, Map<MessageLab, LocalCase> cases)
	public LocalBranch(Role src, Map<MessageLab, LocalCase> cases)
	{
		//this.self = self;
		this.src = src;
		this.cases = Collections.unmodifiableMap(cases);
	}
	
	// A ? { l1 : S1, l2 : S2, ... }
	@Override
	public String toString()
	{
		return this.src + "?{" +
				this.cases.entrySet().stream()
					.map((e) -> e.getKey().toString() + e.getValue().toString())
					.collect(Collectors.joining(", ")) + "}";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cases == null) ? 0 : cases.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
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
		if (!(obj instanceof LocalBranch))
		{
			return false;
		}
		LocalBranch other = (LocalBranch) obj;
		if (cases == null)
		{
			if (other.cases != null)
			{
				return false;
			}
		} else if (!cases.equals(other.cases))
		{
			return false;
		}
		if (src == null)
		{
			if (other.src != null)
			{
				return false;
			}
		} else if (!src.equals(other.src))
		{
			return false;
		}
		return true;
	}
}
