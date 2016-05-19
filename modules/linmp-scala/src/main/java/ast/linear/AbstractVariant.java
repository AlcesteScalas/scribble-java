package ast.linear;

import java.util.Set;

import ast.name.RecVar;

/** Base class for (possibly recursive) variant types.
 * 
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public interface AbstractVariant
{
	/**
	 * @return the free variables in the type.
	 */
	abstract public Set<RecVar> freeVariables();
}
