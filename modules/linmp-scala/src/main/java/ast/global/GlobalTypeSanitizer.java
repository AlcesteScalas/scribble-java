/**
 * 
 */
package ast.global;

import ast.name.RecVar;
import java.util.Collection;

/** Perform sanity checks on a global type AST
 * @author ascalas
 *
 */
public class GlobalTypeSanitizer implements GlobalTypeVisitor {
	private Collection<RecVar> bound = new java.util.TreeSet<RecVar>();
	private Collection<String> errors = new java.util.LinkedList<String>();
	
	public boolean isValid(GlobalType g) {
		g.accept(this);
		return errors.isEmpty();
	}
	
	public Collection<String> getErrors() {
		return errors;
	}
	
	@Override
	public void visit(GlobalEnd node) {
		return;
	}
	
	@Override
	public void visit(GlobalSend node) {
		node.cases.values().forEach((x) -> visit(x));
	}

	@Override
	public void visit(GlobalSendCase node) {
		node.body.accept(this);
	}

	@Override
	public void visit(GlobalRec node) {
		// FIXME: here we are assuming that all recursion vars are distinct
		this.bound.add(node.recvar);
		node.body.accept(this);
		this.bound.remove(node.recvar);
	}

	@Override
	public void visit(RecVar node) {
		if (!this.bound.contains(node)) {
			errors.add("Unbound variable: " + node);
		}
	}

}
