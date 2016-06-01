package ast.local;

import java.util.Set;

import ast.PayloadType;
import ast.name.RecVar;
import ast.name.Role;

import org.scribble.main.ScribbleException;

public interface LocalType extends PayloadType
{
	/**
	 * @return the free variables in the type.
	 */
	Set<RecVar> freeVariables();
	
	/**
	 * @return the roles involved in the type.
	 */
	Set<Role> roles();
	
	/** Project the local type onto the given role,
	 * returning the binary ("partial") type.
	 * 
	 * @param r Role to project
	 * @return the binary type representing the projection
	 * @throws ScribbleException in case of error
	 */
	default ast.binary.Type partial(Role r) throws ScribbleException
	{
		return ast.local.ops.Projector.apply(this, r, ast.binary.ops.Merge::full);
	}
	
	/** Project the local type onto the given role,
	 * returning the linear encoding of the resulting binary type type.
	 * 
	 * @param r Role to project
	 * @return the linear type representing the projection
	 * @throws ScribbleException in case of error
	 */
	default ast.linear.Type linear(Role r) throws ScribbleException
	{
		return ast.binary.ops.LinearEncoder.apply(partial(r));
	}
}
