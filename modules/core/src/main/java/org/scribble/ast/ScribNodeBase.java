/*
 * Copyright 2009 www.scribble.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.scribble.ast;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.scribble.del.ScribDel;
import org.scribble.main.RuntimeScribbleException;
import org.scribble.main.ScribbleException;
import org.scribble.sesstype.kind.Global;
import org.scribble.sesstype.kind.Local;
import org.scribble.sesstype.kind.ProtocolKind;
import org.scribble.util.ScribUtil;
import org.scribble.visit.AstVisitor;
import org.scribble.visit.Substitutor;

/**
 * This is the generic object from which all Scribble model objects
 * are derived.
 */
public abstract class ScribNodeBase implements ScribNode
{
	protected ScribDel del;

	// Internal shallow copy for (immutable) ModelNodes
	//@Override
	protected abstract ScribNodeBase copy();
	
	@Override
	public abstract ScribNodeBase clone();
	
	@Override
	public ScribNode accept(AstVisitor nv) throws ScribbleException
	{
		return nv.visit(null, this);
	}

	@Override
	public ScribNode visitChildren(AstVisitor nv) throws ScribbleException
	{
		return this;
	}
	
	protected ScribNode visitChild(ScribNode child, AstVisitor nv) throws ScribbleException
	{
		return nv.visit(this, child);
	}
	
	@Override
	public final ScribDel del()
	{
		return this.del;
	}
	
	@Override
	public final ScribNodeBase del(ScribDel del)
	{
		ScribNodeBase copy = copy();
		copy.del = del;
		return copy;
	}

	/*@Override
	public final <T extends ScribNode> T del(T n, ScribDel del)
	{
		T copy = n.copy();
		copy.del = del;
		return copy;
	}*/

	@Override
	public ScribNode substituteNames(Substitutor subs)
	{
		return this;
	}
		
	// FIXME: remove parent parameter, to make uniform with visitChild
	// Used when a generic cast would otherwise be needed (non-generic children casts don't need this) -- doesn't check any generic parameters, relies on concrete values being instances of non-parameterised types
	// Subtype constraint on visited could still be too restrictive, e.g. AmbigNameNodeDel (although it doesn't matter there), e.g. unfolding continue's into recursion's
	protected final static <T extends ScribNode>
			T visitChildWithStrictClassCheck(ScribNode parent, T child, AstVisitor nv) throws ScribbleException
	{
		ScribNode visited = ((ScribNodeBase) parent).visitChild(child, nv);
		if (!child.getClass().isAssignableFrom(visited.getClass()))  // Same subtyping flexibility as standard cast
		{
			throw new RuntimeException(nv.getClass() + " generic visit error: " + child.getClass() + ", " + visited.getClass());
		}
		@SuppressWarnings("unchecked")
		T t = (T) visited;
		return t;
	}

	protected final static <T extends ScribNode>
			List<T> visitChildListWithStrictClassCheck(ScribNode parent, List<T> children, AstVisitor nv) throws ScribbleException
	{
		return visitChildListWith(parent, children, nv,
				(T t) -> ScribUtil.handleLambdaScribbleException(() -> ScribNodeBase.visitChildWithStrictClassCheck(parent, t, nv)));  // -> T
	}
	
	// R is expected to be N<K>  i.e. the generic (ProtocolKindNode) class N parameterised by K
	protected final static <N extends ProtocolKindNode<?>, K extends ProtocolKind, R extends ProtocolKindNode<K>>
			R visitChildWithCastCheck(ScribNode parent, ScribNode child, AstVisitor nv, Class<N> clazz, K kind, Function<ScribNode, R> cast) throws ScribbleException
	{
		ScribNode visited = ((ScribNodeBase) parent).visitChild(child, nv);
		if (!clazz.isAssignableFrom(visited.getClass()))
		{
			throw new RuntimeException(nv.getClass() + " generic visit error: " + clazz + ", " + visited.getClass());
		}
		/*if ((GNode.class.isAssignableFrom(c) && !(child instanceof GNode)) || (LNode.class.isAssignableFrom(c) && !(child instanceof LNode)))
		{
			throw new RuntimeException(nv.getClass() + " generic visit error: " + c + ", " + visited.getClass());
		}*/
		ProtocolKindNode<?> n = (ProtocolKindNode<?>) visited;
		if ((n.isGlobal() && !kind.equals(Global.KIND)) || (n.isLocal() && !kind.equals(Local.KIND)))
		{
			throw new RuntimeException(nv.getClass() + " generic visit error: " + n.getClass() + ", " + kind);
		}
		return cast.apply(n);
	}

	protected final static <T extends ScribNode, N extends ProtocolKindNode<?>, K extends ProtocolKind, R extends ProtocolKindNode<K>>
			List<R> visitChildListWithCastCheck(ScribNode parent, List<T> children, AstVisitor nv, Class<N> c, K k, Function<ScribNode, R> f) throws ScribbleException
	{
		return visitChildListWith(parent, children, nv,
				(T t) -> ScribUtil.handleLambdaScribbleException(() -> ScribNodeBase.visitChildWithCastCheck(parent, t, nv, c, k, f)));  // -> R
	}

	// Just a list-map with handling for promoted exceptions -- could move to Util (where the exception promoting routine handleLambdaScribbleException is)
	private final static <T extends ScribNode, R extends ScribNode>
			List<R> visitChildListWith(ScribNode parent, List<T> children, AstVisitor nv, Function<T, R> c) throws ScribbleException
	{
		/*List<T> visited = new LinkedList<>();
		for (T n : children)
		{
			visited.add(c.call());
		}
		return visited;*/
		// Maybe this exception hack is not worth it?  Better to throw directly as ScribbleException
		try
		{
			return children.stream().map((n) -> c.apply(n)).collect(Collectors.toList());
		}
		catch (RuntimeScribbleException rse)
		{
			Throwable cause = rse.getCause();
			if (cause instanceof ScribbleException)
			{
				throw (ScribbleException) cause;
			}
			throw (RuntimeException) cause;
		}
	}
}
