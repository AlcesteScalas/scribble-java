package org.scribble.del.global;

import org.scribble.ast.AstFactoryImpl;
import org.scribble.ast.ScribNode;
import org.scribble.ast.global.GContinue;
import org.scribble.ast.local.LContinue;
import org.scribble.ast.name.simple.RecVarNode;
import org.scribble.del.ContinueDel;
import org.scribble.main.ScribbleException;
import org.scribble.sesstype.kind.RecVarKind;
import org.scribble.visit.Projector;

public class GContinueDel extends ContinueDel implements GSimpleInteractionNodeDel
{
	@Override
	public GContinue leaveProjection(ScribNode parent, ScribNode child, Projector proj, ScribNode visited) throws ScribbleException
	{
		GContinue gc = (GContinue) visited;
		RecVarNode recvar = (RecVarNode) AstFactoryImpl.FACTORY.SimpleNameNode(RecVarKind.KIND, gc.recvar.toName().toString());
		LContinue projection = AstFactoryImpl.FACTORY.LContinue(recvar);
		proj.pushEnv(proj.popEnv().setProjection(projection));
		return (GContinue) GSimpleInteractionNodeDel.super.leaveProjection(parent, child, proj, gc);
	}
}