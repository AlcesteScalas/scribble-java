package ast.local;

import java.util.Set;

import ast.PayloadType;
import ast.name.RecVar;
import ast.name.Role;

public interface LocalType extends PayloadType
{
	/**
	 * @return the free variables in the type.
	 */
	public Set<RecVar> freeVariables();
	
	/**
	 * @return the roles involved in the type.
	 */
	public Set<Role> roles();
}
