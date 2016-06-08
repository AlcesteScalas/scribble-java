package ast.linear.ops;

import org.scribble.main.ScribbleException;

import ast.linear.NameEnv;
import ast.linear.Type;

public class ScalaMessageExtractor
{
	public static String apply(Type t) throws ScribbleException
	{
		return apply(t, DefaultNameEnvBuilder.apply(t));
	}
	
	public static String apply(Type t, NameEnv nameEnv) throws ScribbleException
	{
		ScalaProtocolExtractor te = new ScalaProtocolExtractor(t, nameEnv, false);
		
		return te.process();
	}
}
