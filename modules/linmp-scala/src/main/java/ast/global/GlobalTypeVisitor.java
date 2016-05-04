package ast.global;

/** Visitor pattern for global type ASTs. 
 *  
 *  @author Alceste Scalas <alceste.scalas@imperial.ac.uk>
 */
public interface GlobalTypeVisitor {
	/**
	 * @param node Node being visited
	 */
	void visit(GlobalEnd node);
	
	/**
	 * @param node Node being visited
	 */
	void visit(GlobalSend node);
	
	/**
	 * @param node Node being visited
	 */
	void visit(GlobalSendCase node);
	
	/**
	 * @param node Node being visited
	 */
	void visit(GlobalRec node);
	
	/**
	 * @param node Node being visited
	 */
	void visit(ast.name.RecVar node);
}
