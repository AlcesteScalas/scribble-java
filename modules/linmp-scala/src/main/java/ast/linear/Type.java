package ast.linear;

import java.util.Set;

import ast.name.RecVar;

/** Base for all linear types.
 * 
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public interface Type
{
	/**
	 * @return the free variables in the type.
	 */
	abstract public Set<RecVar> freeVariables();
}
