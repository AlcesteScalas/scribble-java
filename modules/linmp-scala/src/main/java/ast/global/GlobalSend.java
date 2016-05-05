package ast.global;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import ast.name.MessageLab;
import ast.name.Role;

public class GlobalSend implements GlobalType
{
	public final Role src;
	public final Role dest;
	public final Map<MessageLab, GlobalSendCase> cases;
	
	public GlobalSend(Role src, Role dest, Map<MessageLab, GlobalSendCase> cases)
	{
		this.src = src;
		this.dest = dest;
		this.cases = Collections.unmodifiableMap(cases);
	}
	
	@Override
	public String toString()
	{
		return this.src + "->" + this.dest + ":{" +
				this.cases.entrySet().stream()
					.map((e) -> e.getKey().toString() + e.getValue().toString())
					.collect(Collectors.joining(", ")) + "}";
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 29;
		int result = 1;
		result = prime * result + ((cases == null) ? 0 : cases.hashCode());
		result = prime * result + ((dest == null) ? 0 : dest.hashCode());
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
		if (!(obj instanceof GlobalSend))
		{
			return false;
		}
		GlobalSend other = (GlobalSend) obj;
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
		if (dest == null)
		{
			if (other.dest != null)
			{
				return false;
			}
		} else if (!dest.equals(other.dest))
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
