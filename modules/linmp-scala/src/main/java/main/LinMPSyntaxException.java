package main;

import org.antlr.runtime.tree.CommonTree;

public class LinMPSyntaxException extends LinearMPException
{
	private static final long serialVersionUID = 1L;

	public LinMPSyntaxException()
	{
		// TODO Auto-generated constructor stub
	}

	public LinMPSyntaxException(CommonTree blame, String arg0)
	{
		super(blame, arg0);
		// TODO Auto-generated constructor stub
	}

	public LinMPSyntaxException(String arg0)
	{
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
