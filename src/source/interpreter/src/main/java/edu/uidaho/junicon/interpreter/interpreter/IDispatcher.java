//========================================================================
// Copyright (c) 2011 Orielle, LLC.  
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// This software is provided by the copyright holders and contributors
// "as is" and any express or implied warranties, including, but not
// limited to, the implied warranties of merchantability and fitness for
// a particular purpose are disclaimed. In no event shall the copyright
// holder or contributors be liable for any direct, indirect, incidental,
// special, exemplary, or consequential damages (including, but not
// limited to, procurement of substitute goods or services; loss of use,
// data, or profits; or business interruption) however caused and on any
// theory of liability, whether in contract, strict liability, or tort
// (including negligence or otherwise) arising in any way out of the use
// of this software, even if advised of the possibility of such damage.
//========================================================================
package edu.uidaho.junicon.interpreter.interpreter;

import edu.uidaho.junicon.grammars.common.ILineContext;
import edu.uidaho.junicon.runtime.util.*;

import java.util.Map;
import java.util.List;

/**
 * Dispatcher that chooses the delegate, i.e., another interpreter,
 * to which to hand statements for execution.
 * The dispatcher also manages the set of delegate sub-interpreters.
 *
 * @author Peter Mills
 */
public interface IDispatcher {

  //======================================================================
  // Sub-interpreter management.
  //======================================================================

  /**
   * Set the default interpreter to dispatch to, by name.
   */
  public void setDefaultDispatchInterpreterByName (String delegate);

  /**
   * Set the default interpreter to dispatch to.
   */
  public void setDefaultDispatchInterpreter (IInterpreter delegate);

  /**
   * Get the default interpreter to dispatch to.
   */
  public IInterpreter getDefaultDispatchInterpeter ();

  /** 
   * Add children sub-interpreters with the given names.
   * Sets the child's name to match the map, and sets parent to this.
   */
  public void setDispatchChildren (Map<String, IInterpreter> children);

  /** 
   * Add children sub-interpreters.
   * Uses the child's name in the map, and sets parent to this.
   */
  public void setDispatchChildren (List<IInterpreter> children);

  /**
   * Returns the sub-interpreters for this interpreter.
   */
  public Map<String, IInterpreter> getDispatchChildren ();

  /**
   * Adds a child sub-interpreter.
   * Uses the child's name in the map, and sets parent to this.
   */
  public void setAddDispatchChild (IInterpreter child);

}

//==== END OF FILE
