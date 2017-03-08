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
 * InterpreterException contains errors or warnings from running the 
 * the interpreter or its interpreter substrates.
 * InterpreterExceptions are further partitioned into:
 * <br>
 * <i>ParseException</i> that may occur when parsing 
 * the source language or target substrate languages,
 * <br>
 * <i>TransformException</i> that may occur when transforming 
 * source statements for target interpreter substrates,
 * <br>
 * <i>ExecuteException</i> that may occur when processing 
 * source statements in the interpreter or in the
 * extension libraries, and
 * <br>
 * <i>SubstrateException</i> that may occur when executing 
 * transformed source statements on target interpreter substrates.
 * <br>
 * <i>PatternException</i> is a further refinement of ParseException
 * that may occur when parsing or processing XML patterns.
 * <br>
 * If the application needs to pass through other types of exceptions, it must
 * wrap those exceptions in the constructor.
 * <br>
 * InterpreterException also has a notion of "have printed" and "have filtered",
 * useful for situations where the exception is propagated but should only be 
 * printed or filtered once.
 *
 * @author Peter Mills
 */
public class InterpreterException extends Exception {

  private int errorColumn = -1;	// column where the error was found, if known
  private int errorLine = -1;	// line where the error was found, if known
  private String errorSource = null;
  private boolean haveNotified = false;
  private boolean haveFiltered = false;
  private boolean haveFilteredParse = false;
		
  /**
   * Constructor with message.
   */
  public InterpreterException (String message) {
	super(message);	
  }

  /**
   * Constructor wrapping an existing exception and overriding the message.
   * InterpreterException state
   * (line, column, source, and haveNotified)
   * is copied from cause if it is an InterpreterException.
   */
  public InterpreterException (String message, Throwable cause) {
	super(message, cause);	
	init(cause);
  }

  /**
   * Constructor wrapping an existing exception. 
   * InterpreterException state
   * (line, column, source, and haveNotified)
   * is copied from cause if it is an InterpreterException.
   */
  public InterpreterException (Throwable cause) {
	super(cause);	
	init(cause);
  }

  /**
   * Copies state (line, column, source, and havePrinted) from cause.
   */
  private void init (Throwable cause) {
	if ((cause != null) && (cause instanceof InterpreterException)) {
		InterpreterException ie = (InterpreterException) cause;
		errorLine = ie.getLine();
		errorColumn = ie.getColumn();
		errorSource = ie.getSource();
		haveNotified = ie.getHaveNotified();
		haveFiltered = ie.getHaveFiltered();
		haveFilteredParse = ie.getHaveFilteredParse();
	};
  }

  /**
   * Returns the column where the error occurred, or -1 if not known.
   */
  public int getColumn () {
	return errorColumn;
  }

  /**
   * Sets the column where the error occurred.
   * @return this
   */
  public InterpreterException setColumn (int column) {
	errorColumn = column;
	return this;
  }

  /**
   * Returns the line where the error occurred, or -1 if not known.
   */
  public int getLine () {
	return errorLine;
  }

  /**
   * Sets the line where the error occurred.
   * @return this
   */
  public InterpreterException setLine (int line) {
	errorLine = line;
	return this;
  }

  /**
   * Returns the source code where the error occurred, or null if not known.
   */
  public String getSource () {
	return errorSource;
  }

  /**
   * Sets the source where the error occurred.
   * @return this
   */
  public InterpreterException setSource (String source) {
	errorSource = source;
	return this;
  }

  /**
   * Returns if have already notified the user of the exception.
   */
  public boolean getHaveNotified ()  {
	return haveNotified;
  }

  /**
   * Set if have notified the user of the exception.
   * @return this
   */
  public InterpreterException setHaveNotified (boolean haveNotified) {
	this.haveNotified = haveNotified;
	return this;
  }

  /**
   * Returns if have filtered the exception.
   */
  public boolean getHaveFiltered ()  {
	return haveFiltered;
  }

  /**
   * Set if have filtered the exception.
   * @return this
   */
  public InterpreterException setHaveFiltered (boolean haveFiltered) {
	this.haveFiltered = haveFiltered;
	return this;
  }

  /**
   * Returns if have filtered the parse exception.
   */
  public boolean getHaveFilteredParse ()  {
	return haveFilteredParse;
  }

  /**
   * Set if have filtered the parse exception.
   * @return this
   */
  public InterpreterException setHaveFilteredParse (boolean haveFiltered) {
	this.haveFilteredParse = haveFiltered;
	return this;
  }

}

//==== END OF FILE
