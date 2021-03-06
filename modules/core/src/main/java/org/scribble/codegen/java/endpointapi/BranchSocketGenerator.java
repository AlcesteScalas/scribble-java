package org.scribble.codegen.java.endpointapi;

import java.util.stream.Collectors;

import org.scribble.ast.DataTypeDecl;
import org.scribble.ast.MessageSigNameDecl;
import org.scribble.ast.Module;
import org.scribble.codegen.java.endpointapi.ioifaces.BranchInterfaceGenerator;
import org.scribble.codegen.java.endpointapi.ioifaces.HandleInterfaceGenerator;
import org.scribble.codegen.java.endpointapi.ioifaces.IOStateInterfaceGenerator;
import org.scribble.codegen.java.endpointapi.ioifaces.SuccessorInterfaceGenerator;
import org.scribble.codegen.java.util.ClassBuilder;
import org.scribble.codegen.java.util.EnumBuilder;
import org.scribble.codegen.java.util.JavaBuilder;
import org.scribble.codegen.java.util.MethodBuilder;
import org.scribble.main.ScribbleException;
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.sesstype.name.DataType;
import org.scribble.sesstype.name.MessageSigName;
import org.scribble.sesstype.name.PayloadType;
import org.scribble.sesstype.name.Role;

public class BranchSocketGenerator extends ScribSocketGenerator
{
	public BranchSocketGenerator(StateChannelApiGenerator apigen, EState curr)
	{
		super(apigen, curr);
	}

	@Override
	protected String getSuperClassType()
	{
		return BRANCHSOCKET_CLASS + "<" + getSessionClassName() + ", " + getSelfClassName() + ">";
	}

	@Override
	protected void addImports()
	{
		this.cb.addImports("java.io.IOException");
		super.addImports();
	}

	//private void addBranchMethod(ClassBuilder cb, EndpointState curr)
	@Override
	protected void addMethods() throws ScribbleException
	{
		final String ROLE_PARAM = "role";
		final String MESSAGE_VAR = "m";
		final String OPENUM_VAR = "openum";
		final String OP = MESSAGE_VAR + "." + StateChannelApiGenerator.SCRIBMESSAGE_OP_FIELD;

		Module main = this.apigen.getMainModule();

		//String next = constructCaseClass(curr, main);
		ClassBuilder caseclass = new CaseSocketGenerator(this.apigen, this.curr).generateType();
		String next = caseclass.getName();
		String enumClass = getBranchEnumClassName(this.apigen, this.curr);

		//cb.addImports("java.util.concurrent.ExecutionException");
		
		//boolean first;
		Role peer = this.curr.getActions().iterator().next().obj;

		// Branch method
		addBranchMethod(ROLE_PARAM, MESSAGE_VAR, OPENUM_VAR, OP, peer, next, enumClass);
		
		//if (IOInterfacesGenerator.skipIOInterfacesGeneration(apigen.getInitialState()))
		if (this.apigen.skipIOInterfacesGeneration)
		{	
			EnumBuilder eb = cb.newMemberEnum(enumClass);
			eb.addModifiers(JavaBuilder.PUBLIC);
			eb.addInterfaces(OPENUM_INTERFACE);
			this.curr.getActions().stream().forEach((a) -> eb.addValues(SessionApiGenerator.getOpClassName(a.mid)));
		}
		else
		{
			// Callback methods
			String handleif = addBranchCallbackMethod(ROLE_PARAM, peer);
			addHandleInterfaceCallbackMethod(ROLE_PARAM, MESSAGE_VAR, OP, main, peer, handleif);
			addHandleMethod(ROLE_PARAM, MESSAGE_VAR, OP, main, peer);
		}

		this.apigen.addTypeDecl(new HandlerInterfaceGenerator(this.apigen, this.cb, this.curr).generateType());
	}

	private void addBranchMethod(final String ROLE_PARAM, final String MESSAGE_VAR, final String OPENUM_VAR, final String OP,
			Role peer, String next, String enumClass)
	{
		MethodBuilder mb = cb.newMethod("branch");
		mb.setReturn(next);
		mb.addParameters(SessionApiGenerator.getRoleClassName(curr.getActions().iterator().next().obj) + " " + ROLE_PARAM);
		mb.addModifiers(JavaBuilder.PUBLIC);
		mb.addExceptions(StateChannelApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS, "IOException", "ClassNotFoundException");//, "ExecutionException", "InterruptedException");
		if (!this.apigen.skipIOInterfacesGeneration)
		{
			mb.addAnnotations("@Override");
		}
		
		mb.addBodyLine(StateChannelApiGenerator.SCRIBMESSAGE_CLASS + " " + MESSAGE_VAR + " = "
				+ JavaBuilder.SUPER + ".readScribMessage(" + getSessionApiRoleConstant(peer) + ");");
		mb.addBodyLine(enumClass + " " + OPENUM_VAR + ";");
		boolean first = true;
		for (EAction a : this.curr.getActions())
		{
			mb.addBodyLine(((first) ? "" : "else ") + "if (" + OP + ".equals(" + getSessionApiOpConstant(a.mid) + ")) {");
			mb.addBodyLine(1, OPENUM_VAR + " = "
					+ enumClass + "." + SessionApiGenerator.getOpClassName(a.mid) + ";");
			mb.addBodyLine("}");
			first = false;
		}
		mb.addBodyLine("else {");
		mb.addBodyLine(1, "throw " + JavaBuilder.NEW + " RuntimeException(\"Won't get here: \" + " + OP + ");");
		mb.addBodyLine("}");
		mb.addBodyLine(JavaBuilder.RETURN + " "
				+ JavaBuilder.NEW + " " + next + "(" + SCRIBSOCKET_SE_FIELD + ", true, " + OPENUM_VAR + ", " + MESSAGE_VAR + ");");  // FIXME: dummy boolean not needed
	}

	private String addBranchCallbackMethod(final String ROLE_PARAM, Role peer)
	{
		boolean first;
		String handlerif = HandlerInterfaceGenerator.getHandlerInterfaceName(this.cb.getName());
		String handleif = HandleInterfaceGenerator.getHandleInterfaceName(this.apigen.getSelf(), this.curr);
		MethodBuilder mb2 = this.cb.newMethod("branch");
		mb2.addParameters(SessionApiGenerator.getRoleClassName(peer) + " " + ROLE_PARAM);
		//mb2.addParameters("java.util.concurrent.Callable<" + ifname + "> branch");
		mb2.addParameters(handlerif + " handler");
		mb2.setReturn(JavaBuilder.VOID);
		mb2.addModifiers(JavaBuilder.PUBLIC);
		mb2.addExceptions(StateChannelApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS, "IOException", "ClassNotFoundException");//, "ExecutionException", "InterruptedException");
		first = true;
		handleif += "<";
		for (EAction a : this.curr.getActions().stream().sorted(IOStateInterfaceGenerator.IOACTION_COMPARATOR).collect(Collectors.toList()))
		{
			if (first)
			{
				first = false;
			}
			else
			{
				handleif += ", ";
			}
			EState succ = this.curr.getSuccessor(a);
			if (succ.isTerminal())
			{
				handleif += ScribSocketGenerator.GENERATED_ENDSOCKET_NAME;
			}
			else
			{
				handleif += this.apigen.getSocketClassName(succ);
			}
		}
		handleif += ">";
		mb2.addBodyLine("branch(role, (" + handleif + ") handler);");
		return handleif;
	}

	private void addHandleInterfaceCallbackMethod(final String ROLE_PARAM,
			final String MESSAGE_VAR, final String OP, Module main, Role peer,
			String handleif)
	{
		boolean first;
		MethodBuilder mb3 = this.cb.newMethod("branch");
		mb3.addParameters(SessionApiGenerator.getRoleClassName(peer) + " " + ROLE_PARAM);
		//mb2.addParameters("java.util.concurrent.Callable<" + ifname + "> branch");
		mb3.addParameters(handleif + " handler");
		mb3.setReturn(JavaBuilder.VOID);
		mb3.addModifiers(JavaBuilder.PUBLIC);
		mb3.addAnnotations("@Override");
		mb3.addExceptions(StateChannelApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS, "IOException", "ClassNotFoundException");//, "ExecutionException", "InterruptedException");
		mb3.addBodyLine(StateChannelApiGenerator.SCRIBMESSAGE_CLASS + " " + MESSAGE_VAR + " = "
				+ JavaBuilder.SUPER + ".readScribMessage(" + getSessionApiRoleConstant(peer) + ");");
		first = true;
		for (EAction a : this.curr.getActions())
		{
			EState succ = this.curr.getSuccessor(a);
			if (first)
			{
				first = false;
			}
			else
			{
				mb3.addBodyLine("else");
			}
			mb3.addBodyLine("if (" + MESSAGE_VAR + "." + StateChannelApiGenerator.SCRIBMESSAGE_OP_FIELD + ".equals(" + getSessionApiOpConstant(a.mid) + ")) {");
			if (succ.isTerminal())
			{
				mb3.addBodyLine(1, SCRIBSOCKET_SE_FIELD + ".setCompleted();");
			}
			String ln = "handler.receive(";
			//if (!succ.isTerminal())
			{
				//FIXME: factor out with addReturn?
				 ln += JavaBuilder.NEW + " " + (succ.isTerminal() ? ScribSocketGenerator.GENERATED_ENDSOCKET_NAME : this.apigen.getSocketClassName(succ)) + "(" + SCRIBSOCKET_SE_FIELD + ", true), ";
			}
			ln += getSessionApiOpConstant(a.mid);
					
			// Based on receive parameters
			if (a.mid.isOp())
			{
				if (!a.payload.isEmpty())
				{
					String buffSuper = JavaBuilder.NEW + " " + BUF_CLASS + "<>(";
					int i = 0;
					for (PayloadType<?> pt : a.payload.elems)
					{
						DataTypeDecl dtd = main.getDataTypeDecl((DataType) pt);  // TODO: if not DataType
						ln += ", " + buffSuper + "(" + dtd.extName + ") " + RECEIVE_MESSAGE_PARAM + "." + SCRIBMESSAGE_PAYLOAD_FIELD + "[" + i++ + "])";
					}
				}
			}
			else
			{
				MessageSigNameDecl msd = main.getMessageSigDecl(((MessageSigName) a.mid).getSimpleName());  // FIXME: might not belong to main module
				ln += ", " + JavaBuilder.NEW + " " + BUF_CLASS + "<>((" + msd.extName + ") " +  RECEIVE_MESSAGE_PARAM + "." + SCRIBMESSAGE_PAYLOAD_FIELD + "[0])";
			}
				
			ln += ");";
			mb3.addBodyLine(1, ln);
			mb3.addBodyLine("}");
		}
		mb3.addBodyLine("else {");
		mb3.addBodyLine(1, "throw " + JavaBuilder.NEW + " RuntimeException(\"Won't get here: \" + " + OP + ");");
		mb3.addBodyLine("}");
	}

	// FIXME: factor out with others
	private void addHandleMethod(final String ROLE_PARAM, final String MESSAGE_VAR, final String OP, Module main, Role peer)
	{
		boolean first;
		MethodBuilder mb4 = this.cb.newMethod("handle");
		mb4.addParameters(SessionApiGenerator.getRoleClassName(peer) + " " + ROLE_PARAM);
		String tmp = HandleInterfaceGenerator.getHandleInterfaceName(this.apigen.getSelf(), this.curr) + "<";
		tmp += this.curr.getActions().stream().sorted(IOStateInterfaceGenerator.IOACTION_COMPARATOR)
				.map((a) -> SuccessorInterfaceGenerator.getSuccessorInterfaceName(a)).collect(Collectors.joining(", ")) + ">";
		mb4.addParameters(tmp + " handler");
		mb4.setReturn(JavaBuilder.VOID);
		mb4.addModifiers(JavaBuilder.PUBLIC);
		mb4.addExceptions(StateChannelApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS, "IOException", "ClassNotFoundException");//, "ExecutionException", "InterruptedException");
		mb4.addAnnotations("@Override");
		mb4.addBodyLine(StateChannelApiGenerator.SCRIBMESSAGE_CLASS + " " + MESSAGE_VAR + " = "
				+ JavaBuilder.SUPER + ".readScribMessage(" + getSessionApiRoleConstant(peer) + ");");
		first = true;
		for (EAction a : this.curr.getActions())
		{
			EState succ = this.curr.getSuccessor(a);
			if (first)
			{
				first = false;
			}
			else
			{
				mb4.addBodyLine("else");
			}
			mb4.addBodyLine("if (" + MESSAGE_VAR + "." + StateChannelApiGenerator.SCRIBMESSAGE_OP_FIELD + ".equals(" + getSessionApiOpConstant(a.mid) + ")) {");
			if (succ.isTerminal())
			{
				mb4.addBodyLine(1, SCRIBSOCKET_SE_FIELD + ".setCompleted();");
			}
			String ln = "handler.receive(";
			//if (!succ.isTerminal())
			{
				//FIXME: factor out with addReturn?
				 ln += JavaBuilder.NEW + " " + (succ.isTerminal() ? ScribSocketGenerator.GENERATED_ENDSOCKET_NAME : this.apigen.getSocketClassName(succ)) + "(" + SCRIBSOCKET_SE_FIELD + ", true), ";
			}
			ln += getSessionApiOpConstant(a.mid);
					
			// Based on receive parameters
			if (a.mid.isOp())
			{
				if (!a.payload.isEmpty())
				{
					String buffSuper = JavaBuilder.NEW + " " + BUF_CLASS + "<>(";
					int i = 0;
					for (PayloadType<?> pt : a.payload.elems)
					{
						DataTypeDecl dtd = main.getDataTypeDecl((DataType) pt);  // TODO: if not DataType
						ln += ", " + buffSuper + "(" + dtd.extName + ") " + RECEIVE_MESSAGE_PARAM + "." + SCRIBMESSAGE_PAYLOAD_FIELD + "[" + i++ + "])";
					}
				}
			}
			else
			{
				MessageSigNameDecl msd = main.getMessageSigDecl(((MessageSigName) a.mid).getSimpleName());  // FIXME: might not belong to main module
				ln += ", " + JavaBuilder.NEW + " " + BUF_CLASS + "<>((" + msd.extName + ") " +  RECEIVE_MESSAGE_PARAM + "." + SCRIBMESSAGE_PAYLOAD_FIELD + "[0])";
			}
				
			ln += ");";
			mb4.addBodyLine(1, ln);
			mb4.addBodyLine("}");
		}
		mb4.addBodyLine("else {");
		mb4.addBodyLine(1, "throw " + JavaBuilder.NEW + " RuntimeException(\"Won't get here: \" + " + OP + ");");
		mb4.addBodyLine("}");
	}

	protected static String getBranchEnumClassName(StateChannelApiGenerator apigen, EState curr)
	{
		//return BranchInterfaceGenerator.getBranchInterfaceEnumName(apigen.getSelf(), curr);
		//return (IOInterfacesGenerator.skipIOInterfacesGeneration(apigen.getInitialState()))
		return apigen.skipIOInterfacesGeneration
				? apigen.getSocketClassName(curr) + "_Enum"
				: BranchInterfaceGenerator.getBranchInterfaceEnumName(apigen.getSelf(), curr);
	}
}
