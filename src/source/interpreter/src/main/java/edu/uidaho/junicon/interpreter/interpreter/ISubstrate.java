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
import edu.uidaho.junicon.runtime.util.ILogger;

import java.util.Map;
import java.util.Collection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.File;

import javax.script.ScriptException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Interface for the underlying substrate that executes scripts.
 * The substrate either uses a Java ScriptEngine, or can delegate to
 * another IScriptEval for evaluating commands.
 * Methods are also provided for compiling to the substrate
 * and from there to Java.
 * This interface houses all the substrate specific information,
 * such as the underlying script engine manager and the script extension type,
 * and methods for script evaluation, environment setting, IO redirection,
 * compiling to the substrate and from there to Java,
 * and loading jar files so as to be recognized 
 * by the substrate classloader.
 *
 * @author Peter Mills
 */
public interface ISubstrate extends IScriptEval
{

  //======================================================================
  // Setters for dependency injection.
  //======================================================================

  /** 
   * Sets the parent of this substrate.
   */
  public void setParent (IInterpreter parent);

  /**
   * Gets the parent of this substrate.
   */
  public IInterpreter getParent ();

  /**
   * Sets if quiets script exceptions.
   */
  public void setQuietScriptExceptions (boolean quietScriptExceptions);

  /**
   * Gets if quiets script exceptions.
   */
  public boolean isQuietScriptExceptions ();

  /**
   * Sets the logger used for logging.
   */
  public void setLogger (ILogger logger);

  /**
   * Gets the logger used for logging.
   * Default is to use the parent logger, unless overridden by setLogger().
   */
  public ILogger getLogger ();
  
  //======================================================================
  // Setters for delegate substrate.
  //======================================================================

  /**
   * Sets the delegate substrate to be used for evaluating scripts
   * instead of a ScriptEngine.
   * Overrides any use of a script engine.
   */
  public void setDelegateForEval (IScriptEval delegate);

  /**
   * Gets the delegate substrate to be used for evaluating scripts,
   * or null if no delegate is used.
   */
  public IScriptEval getDelegateForEval ();

  //======================================================================
  // Setters for the script engine.
  //======================================================================
  /**
   * Setter for the script engine handler.
   * If not set, defaults to javax.script.ScriptEngineManager.
   */
  public void setScriptEngineManager (ScriptEngineManager manager);

  /**
   * Gets the script engine handler.
   */
  public ScriptEngineManager getScriptEngineManager ();

  /**
   * Sets the default script extension.
   */
  public void setDefaultScriptExtension (String scriptExtension);

  /**
   * Gets the default script extension.
   */
  public String getDefaultScriptExtension ();

  /**
   * Sets the default script engine.
   */
  public void setDefaultScriptEngine (ScriptEngine engine);

  /**
   * Gets the default script engine.
   */
  public ScriptEngine getDefaultScriptEngine ();

  /**
   * Sets the current script engine for script evaluation.
   */
  public void setScriptEngine (ScriptEngine engine);

  /**
   * Gets the current engine to be used for script evaluation.
   */
  public ScriptEngine getScriptEngine ();

  /**
   * Gets the current engine used for script evaluation.
   * If not set or null, uses the default script engine,
   * otherwise the engine for the default extension.
   */
  public ScriptEngine getCurrentEngine ();

  /**
   * Gets a new scriptEngine for the given extension.
   * @param extension script engine extension
   *	If extension is null or empty, uses default script extension.
   * @return script engine, or null if not found.
   */
  public ScriptEngine getEngineByExtension (String extension);
  
  /**
   * Gets a scriptEngine for the given extension.
   * Only creates a new ScriptEngine if not found in cache.
   * @param extension script engine extension
   *	If extension is null or empty, uses default script extension.
   * @return script engine, or null if not found.
   */
  public ScriptEngine getCachedEngineByExtension (String extension);

  /**
   * Reset script engines.
   */
  public void resetScriptEngines ();

  //======================================================================
  // Line separator.
  //======================================================================

  /**
   * Gets default line separator.
   */
  public String getDefaultLineSeparator ();

  /**
   * Sets default line separator.
   */
  public void setDefaultLineSeparator (String linesep);

  /**
   * Sets line separator.
   */
  public void setLineSeparator (String linesep);

  /**
   * Gets line separator.
   */
  public String getLineSeparator ();

  //======================================================================
  // Input and output redirection.
  //======================================================================

  /**
   * Reassigns the "standard" input stream for this substrate.
   */
  public void setIn (InputStream in);

  /**
   * Reassigns the "standard" output stream for this substrate.
   * Wraps the stream as a printstream.
   */
  public void setOut (OutputStream out);

  /**
   * Reassigns the "standard" error stream for this substrate.
   * Wraps the stream as a printstream.
   */
  public void setErr (OutputStream err);

  /**
   * Returns the standard input stream for this substrate.
   */
  public Reader getIn ();

  /**
   * Returns the standard output stream for this substrate.
   */
  public Writer getOut ();

  /**
   * Returns the standard error stream for this substrate.
   */
  public Writer getErr ();

  //======================================================================
  // Compile substrate to Java
  //======================================================================

  /**
   * Compiles source language to a substrate language.
   */
  public File[] SourceToSubstrate ( Map<String, Object> source,
					String[] transformedSource) 
	throws ExecuteException;

  /**
   * Compiles the substrate language into Java class files.
   */
  public File[] SubstrateToJava ( String[] transformedSource,
					File[] inputs) 
	throws ExecuteException;

  /**
   * Enables the contents of the given .jar file to be accessible to the 
   * user in the given substrate.
   */
  public void load (String jar);

  /**
   * Unloads the jar file for the substrate.
   */
  public void unload (String jar);

  /**
   * Reloads the jar file for the substrate.
   */
  public void reload (String jar);

}

//==== END OF FILE
