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

import edu.uidaho.junicon.interpreter.parser.IParser;
import edu.uidaho.junicon.interpreter.parser.ParseException;
import edu.uidaho.junicon.runtime.util.ILogger;

import java.util.Map;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import org.w3c.dom.Document;		// For DOM building

/**
 * Context for a transformational interpreter,
 * such as parent, dispatch children,
 * and other subordinate classes set by dependency injection.
 *
 * @author Peter Mills
 */
public interface IInterpreterContext {

  //======================================================================
  // Interpreter identity.
  //======================================================================

  /** 
   * Sets the name of this interpreter.
   */
  public void setName (String name);

  /**
   * Returns the name of this interpreter.
   */
  public String getName ();

  /** 
   * Sets the type of this interpreter.
   */
  public void setType (String type);

  /**
   * Returns the type of this interpreter.
   */
  public String getType ();

  //======================================================================
  // Dispatcher and substrate management.
  //======================================================================

  /** 
   * Sets the parent of this interpreter, that dispatches to this.
   * Redirects this interpreter's IO to match that of the parent.
   */
  public void setParent (IInterpreter parent);

  /**
   * Returns the parent interpreter, that dispatches to this.
   */
  public IInterpreter getParent ();

  /**
   * Sets the interpreter substrate.
   * Redirects the substrate's IO to match this interpreter.
   */
  public void setSubstrate (ISubstrate substrate);
  
  /**
   * Returns the substrate for this interpreter.
   */
  public ISubstrate getSubstrate ();
  
  /**
   * Sets the logger.
   */
  public void setLogger (ILogger logger);

  /**
   * Returns the logger.
   * Default is to use the parent logger, unless overridden by setLogger().
   */
  public ILogger getLogger ();

  /**
   * Sets the parser for this interpreter.
   */
  public void setParser (IParser parser);

  /**
   * Returns the parser for this interpreter.
   */
  public IParser getParser ();

}

//==== END OF FILE
