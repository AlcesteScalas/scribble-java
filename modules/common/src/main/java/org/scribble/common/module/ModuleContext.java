/*
 * Copyright 2009-11 www.scribble.org
 *
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
package org.scribble.common.module;

import org.scribble.common.resources.Resource;
import org.scribble.model.ModelObject;
import org.scribble.model.Module;

/**
 * This interface represents the validation context.
 *
 */
public interface ModuleContext {

	/**
	 * This method returns the resource associated with the module.
	 * 
	 * @return The resource
	 */
	public Resource getResource();

	/**
	 * This method imports and returns the named module.
	 * 
	 * @param module The module name
	 * @return The module, or null if not found
	 */
	public Module importModule(String module);

	/**
	 * This method returns member associated with a previously
	 * imported module, defined by the supplied fully qualified
	 * name.
	 * 
	 * @param fqn The fully qualified name, e.g. <module>.<member>
	 * @return The member, or null if not found
	 */
	public ModelObject getMember(String fqn);
	
	/**
	 * This method returns member associated with a previously
	 * imported module, defined by the supplied module and local
	 * names.
	 * 
	 * @param module The module name
	 * @param localName The local name
	 * @return The member, or null if not found
	 */
	public ModelObject getMember(String module, String localName);
	
	/**
	 * This method returns the imported member associated with the name.
	 * 
	 * @param name The name
	 * @return The imported member, or null if not found
	 */
	public ModelObject getImportedMember(String name);
	
	/**
	 * This method registers the imported member, associated with the module,
	 * against the optional alias or member name if alias is not specified.
	 * 
	 * @param module The module name
	 * @param member The member name
	 * @param alias The alias
	 * @return The member, or null if not found
	 */
	public ModelObject registerImportedMember(String module, String member, String alias);
	
}
