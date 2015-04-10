package org.scribble2.model.visit.env;



// Immutable
public abstract class Env
{
	//private final JobContext jcontext;  // FIXME: redundant, can be obtained from ModuleContext
	//private final ModuleDelegate mcontext;
	
	// FIXME: duplicated with Visitor fields?  Visitor jcontext is from Job and mcontext is from SubprotocolVisitor
	//protected Env(JobContext jcontext, ModuleDelegate mcontext)//, Module mod)
	protected Env()
	{
		//this.jcontext = jcontext;
		//this.mcontext = mcontext;
	}
	
	// FIXME: deep clone (but shallow copy fine for immutable parts)
	protected abstract Env copy();  // Shallow copy (FIXME: factor out shallow copy interface)

	// Default push for entering a compound interaction context (e.g. used in CompoundInteractionDelegate)
	public abstract Env push();
		
	/*//@Override
	public JobContext getJobContext() 
	{
		return this.jcontext;
	}
		
	//@Override
	public ModuleDelegate getModuleDelegate() 
	{
		return this.mcontext;
	}*/
}
