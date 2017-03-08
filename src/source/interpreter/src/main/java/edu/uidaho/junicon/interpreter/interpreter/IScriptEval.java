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

import java.util.Properties;
import java.util.Map;
import java.util.Collection;

import javax.script.ScriptException;
import java.util.concurrent.Future;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Evaluates scripts and sets bindings for the environment.
 * Scripts are typically evaluated using a given ScriptEngine
 * specified by its script extension, e.g., "js".
 *
 * @author Peter Mills
 */
public interface IScriptEval {

  //======================================================================
  // Eval for substrate.
  //======================================================================

  /**
   * Evaluate a parseable complete statement
   * using the script engine for the given substrate script type.
   * @param scriptText	script to evaluate
   * @param context	script context
   * @return result of script evaluation.
   *		Returns null on null input.
   * @throws ScriptException if script error occurs.
   */
  public Object eval (String scriptText, ILineContext context)
	throws ScriptException;

  /**
   * Evaluates a complete statement using the given script engine.
   * @param scriptText	script to evaluate
   * @param context	script context
   * @param vars	environment variables to set in engine
   * @param params  	parameters
   * @return result of script evaluation.
   * @throws ScriptException if script error occurs.
   */
  public Object eval (String scriptText, ILineContext context,
		Map<String, Object> vars,
		String... params) throws ScriptException;

  /**
   * Spawns a future to evaluate a command using the given script engine.
   * @return Future holding result of eval.
   */
  public Future<Object> spawnEval (String command, ILineContext context);

  /**
   * Spawns a future to evaluate a command using the given script engine.
   * @return Future holding result of eval.
   */
  public Future<Object> spawnEval (String command, ILineContext context,
		Map<String, Object> vars, String... params);

  /**
   * Invokes object method in the given script engine.
   * See: javax.script.Invocable.
   * @param objectName	object name
   * @param methodName	method name
   * @param args  parameters
   * @return result of method call, or null if not String
   * @throws ScriptException if script error occurs,
   *	if object or method not found,
   *	or if ScriptEngine is not Invocable.
   */
  String invokeMethod (String objectName, String methodName, String[] args)
	throws ScriptException;

  //======================================================================
  // Environment bindings for substrate.
  //======================================================================

  /**
   * Get the value of an environment variable.
   */
  public Object getEnv (String name); 

  /**
   * Set the value of an environment variable.
   */
  public void setEnv (String name, Object value);

  /**
   * Gets the set of environment variable names.
   */
  public Collection<String> getEnvNames ();

  /**
   * Gets the set of environment variable names and values.
   */
  public Map<String, Object> getEnvBindings ();

}
 
//==== END OF FILE
