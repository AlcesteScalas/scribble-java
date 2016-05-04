package ast.global;


public class GlobalEnd implements GlobalType
{
	public GlobalEnd()
	{

	}
	
	@Override 
	public String toString()
	{
		return "end";
	}
	
	@Override
	public void accept(ast.global.GlobalTypeVisitor v) {
		v.visit(this);
	}
}
