package ast.linear.ops;

import org.scribble.main.ScribbleException;

import ast.linear.ops.ScalaProtocolExtractor.Option;
import ast.linear.NameEnv;
import ast.linear.Type;

import java.util.Collection;

public class ScalaMessageExtractor
{
	public static String inputs(Type t, Collection<String> classNames) throws ScribbleException
	{
		return apply(t, DefaultNameEnvBuilder.apply(t), new Option().new MPInputMessages(classNames));
	}
	
	public static String outputs(Type t, Collection<String> classNames) throws ScribbleException
	{
		return apply(t, DefaultNameEnvBuilder.apply(t), new Option().new MPOutputMessages(classNames));
	}
	
	public static String apply(Type t, NameEnv nameEnv, Option option) throws ScribbleException
	{
		ScalaProtocolExtractor te = new ScalaProtocolExtractor(t, nameEnv, option);
		
		return te.process();
	}
}
