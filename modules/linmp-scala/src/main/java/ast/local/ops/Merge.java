/**
 * 
 */
package ast.local.ops;

import ast.local.LocalType;

/** Merge two local types iff they are equal.
 * 
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public class Merge
{
	public static LocalType id(LocalType t, LocalType u)
	{
		if (t.equals(u)) {
			return t;
		}
		throw new RuntimeException("Cannot merge non-equal types: "+t+" and "+u);
	}

}
