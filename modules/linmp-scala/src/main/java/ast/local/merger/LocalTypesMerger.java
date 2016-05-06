package ast.local.merger;

import ast.local.LocalType;

import java.util.function.BiFunction;

/** Base class for all local types merging operators. 
 * 
 * @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public abstract class LocalTypesMerger implements BiFunction<LocalType, LocalType, LocalType>
{
}
