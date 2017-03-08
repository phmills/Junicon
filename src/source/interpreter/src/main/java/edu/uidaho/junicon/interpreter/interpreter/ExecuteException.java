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

import java.lang.*;

/**
 * ExecuteException contains errors or warnings from processing 
 * source statements in the interpreter or in the
 * extension libraries.
 * If the application needs to pass through other types of exceptions, it must
 * wrap those exceptions in the constructor.
 *
 * @author Peter Mills
 */
public class ExecuteException extends InterpreterException {

  /**
   * Constructor with message.
   */
  public ExecuteException(String message) {
	super(message);	
  }

  /**
   * Constructor wrapping an existing exception and overriding the message.
   */
  public ExecuteException(String message, Throwable cause) {
	super(message, cause);	
  }

  /**
   * Constructor wrapping an existing exception. 
   */
  public ExecuteException(Throwable cause) {
	super(cause);	
  }

}

//==== END OF FILE
