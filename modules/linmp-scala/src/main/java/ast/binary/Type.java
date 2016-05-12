package ast.binary;

import java.util.Set;

import ast.name.RecVar;

public interface Type
{
	/**
	 * @return the free variables in the type.
	 */
	public Set<RecVar> freeVariables();
}
