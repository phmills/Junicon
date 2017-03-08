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
import edu.uidaho.junicon.support.transforms.IThreadResolver;
import edu.uidaho.junicon.support.transforms.IThreadResource;
import edu.uidaho.junicon.runtime.util.ILogger;
import edu.uidaho.junicon.runtime.util.ILogger.Trace;

import java.util.EnumSet;
import java.util.Map;

import org.w3c.dom.Document;		// For DOM building

/**
 * Setters for dependency injection of properties 
 * into a transformational interpreter.
 *
 * @author Peter Mills
 */
public interface IInterpreterSetters {

  //====================================================================
  // Setters for static defaults.
  //	The setters are non-static for Spring dependency injection.
  //====================================================================

  /**
   * Set if should compile transforms.
   */
  public void setDefaultCompileTransforms (boolean doCompile);

  /**
   * Get if should compile transforms.
   */
  public boolean getDefaultCompileTransforms ();

  //======================================================================
  // Setters for dependency injection.
  //======================================================================

  /**
   * Sets if is in interactive mode.
   * This attribute is inherited into delegated sub-interpreters.
   */
  public void setIsInteractive (boolean on);

  /**
   * Gets if is in interactive mode.
   * Interactive mode means line-by-line user input with 
   * a prompt and the capability to erase lines, while
   * the term interpretive means whether to interpret instead
   * of translate or compile.
   */
  public boolean getIsInteractive ();

  /**
   * Sets if is in interpretive mode.
   * This attribute is inherited into delegated sub-interpreters.
   */
  public void setIsInterpretive (boolean on);

  /**
   * Gets if is in interpretive mode.
   * The term interpretive means whether to interpret instead
   * of translate or compile.
   * Interpretation need not be interactive, for example
   * for aggregate script files.
   */
  public boolean getIsInterpretive ();

  /**
   * Set prompt.
   */
  public void setPrompt (String prompt);

  /**
   * Get prompt.
   */
  public String getPrompt ();

  /**
   * Set prompt when awaiting input to complete a multi-line statement.
   */
  public void setPartialPrompt (String prompt);

  /**
   * Get prompt for awaiting input to complete a multi-line statement.
   */
  public String getPartialPrompt();

  /**
   * Set transform XSLTC bytecode compile flag.
   */
  public void setCompileTransforms (boolean doCompile);

  /**
   * Get transform XSLTC bytecode compile flag.
   */
  public boolean getCompileTransforms ();

  /**
   * Set if should output raw substrate error messages.
   */
  public void setShowRawSubstrateErrors (boolean showRawErrors);

  /**
   * Get if should output raw substrate error messages.
   */
  public boolean getShowRawSubstrateErrors ();	// substrateCodeOnError

  /**
   * Sets if should not preprocess statements.
   * Will still detect complete statements and parse unless overridden.
   * Will still handle directives.
   * This attribute is inherited into delegated sub-interpreters.
   */
  public void setDoNotPreprocess (boolean on);

  /**
   * Gets if should not preprocess statements.
   * Will still detect complete statements and parse unless overridden.
   * Will still handle directives.
   */
  public boolean getDoNotPreprocess ();

  /**
   * Sets if should not detect complete statements (getParseUnit).
   * Implies do not preprocess.
   * This attribute is inherited into delegated sub-interpreters.
   */
  public void setDoNotDetect (boolean on);

  /**
   * Gets if should not detect complete statements (getParseUnit).
   * Implies do not preprocess.
   */
  public boolean getDoNotDetect ();

  /**
   * Sets if should not transform or parse statements.
   * Can still detect complete statements.
   * This attribute is inherited into delegated sub-interpreters.
   */
  public void setDoNotTransform (boolean on);

  /**
   * Gets if should not transform or parse statements.
   * Can still detect complete statements.
   * Will still delegate to subinterpreter if needed.
   */
  public boolean getDoNotTransform ();

  /**
   * Sets if should normalize but not further transform statements.
   * Thus only performs a subset of transformations.
   */
  public void setJustNormalize (boolean on);

  /**
   * Gets if should normalize but not further transform statements.
   * Thus only performs a subset of transformations.
   */
  public boolean getJustNormalize ();

  /**
   * Sets if should not execute statements on the scripting substrate.
   * Will still delegate to subinterpreter if needed.
   * This attribute is inherited into delegated sub-interpreters.
   */
  public void setDoNotExecute (boolean on);

  /**
   * Gets if should not execute statements on the scripting substrate.
   * Will still delegate to subinterpreter if needed.
   */
  public boolean getDoNotExecute ();

  /**
   * Sets if verbose, i.e., print commands after transform.
   */
  public void setIsVerbose (EnumSet<Trace> isVerbose);

  /**
   * Gets if verbose, i.e., print commands after transform.
   * If verbose is detail, prints commands
   *    after normalization, and after transform.
   * If preprocessor, print commands after preprocessing.
   */
  public EnumSet<Trace> getIsVerbose ();

  /**
   * If true, turns on verbose in sub-interpreters.
   * If on, this attribute is inherited into delegated sub-interpreters.
   */
  public void setInheritVerbose (EnumSet<Trace> isVerbose);

  /**
   * Gets if verbose is turned on and inherited in sub-interpreters.
   */
  public EnumSet<Trace> getInheritVerbose ();

  /**
   * Sets if echo commands before preprocessing and transform.
   */
  public void setEcho (boolean on);

  /**
   * Gets if echo commands before preprocessing and transform.
   */
  public boolean getEcho ();

  /**
   * Sets if stop script on error in execFile and execScript.
   */
  public void setStopScriptOnError (boolean on);

  /**
   * Gets if stop script on error in execFile and execScript.
   */
  public boolean getStopScriptOnError ();

  /**
   * Sets if reset parser on parse error.
   */
  public void setResetParserOnError (boolean on);

  /**
   * Gets if reset parser on parse error.
   */
  public boolean getResetParserOnError ();

  /**
   * Set the kill-line control character.
   */
  public void setKillLineChar (char killChar);

  /**
   * Get the kill-line control character.
   */
  public char getKillLineChar ();

  //======================================================================
  // Parser delegation -- Setters for dependency injection.
  //======================================================================

  /**
   * Gets line separator in source code returned by getParseUnit().
   */
  public String getLineSeparator ();

  //==========================================================================
  // Current thread's transform support.
  //==========================================================================

  /**
   * Set cut-through holding methods used by XSLT transforms.
   * Static methods in the cut-through, used by XSLT,
   * delegate to this current thread's instance.
   * Delegating uses an inheritable thread local, or a resolver if non-null.
   */
  public void setTransformSupport (IThreadResource support);

  /**
   * Get cut-through holding methods used by XSLT transforms.
   */
  public IThreadResource getTransformSupport ();

}
  
//==== END OF FILE
