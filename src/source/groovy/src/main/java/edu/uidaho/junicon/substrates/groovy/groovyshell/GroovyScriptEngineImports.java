//========================================================================
// Copyright (c) 2012 Orielle, LLC.  
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
package edu.uidaho.junicon.substrates.groovy.groovyshell;

import java.util.List;
import java.util.ArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptContext;
import javax.script.Invocable;
import javax.script.ScriptException;

import groovy.lang.Binding;
import org.codehaus.groovy.tools.shell.IO;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

/**
 * Wraps a Groovy script engine with corrected import processing
 * that mimics Groovysh.
 * This class incorporates two mechanisms into the default
 * GroovyScriptEngineImpl that are correctly handled in Groovysh.
 * First, a dummy statement "true" must be inserted before classes
 * such as enum's to allow them to run as scripts.
 * Second, all encountered imports must be re-added to each evaluated script.
 * Lastly, the evaluation mechanism of GroovyScriptEngine differs from
 * Groovysh in whether it runs a script if it has a main or run method.
 *
 * @author Peter Mills
 */
public class GroovyScriptEngineImports extends GroovyScriptEngineImpl
	implements ScriptEngine, Invocable {

  private List<String> imports = new ArrayList<String>();

  //==========================================================================
  // Constructors
  //==========================================================================

  /**
   * Creates a ScriptEngineHandler.
   */
  public GroovyScriptEngineImports () {
	super();
  }

  //==========================================================================
  // Setters for dependency injection
  //==========================================================================

  //==========================================================================
  // Eval
  //==========================================================================

  /**
   * Wrap eval to accumulate and inject import statements like Groovysh.
   */
  public Object eval (String script, ScriptContext ctx)
		throws ScriptException {
	if ((script == null) || (script.isEmpty())) { return null; }
	Object result = null;

	// Evaluate script with added imports.
	// MUST ADD "true" before "enum { }" or will eval as script not class
	String joined = join(imports,"\n") + "true\n" + script;
	result = callEval(joined, ctx);

	// filter out imports, add to list of imports
	String lines[] = script.split("[\\r\\n]+");
	for (String line : lines) {
		line = line.trim();
		if (! line.isEmpty()) {
		  String[] tokens =  line.split("\\s+");
		  if ((tokens.length > 0) && (tokens[0].equals("import"))) {
			imports.add(line);
		  }
		}
	}
	return result;
  }

  /**
   * Evaluate the text in the default Groovy ScriptEngine.
   */
  public Object callEval (String scriptText, ScriptContext context)
		throws ScriptException {
	return super.eval(scriptText, context);
  }

  /**
   * Join a list of strings with the given delimiter.
   */
  public static String join (List<String> coll, String delim) {
	if (coll == null) { return null; }
	if (delim == null) { delim = ""; }
        StringBuilder str = new StringBuilder();
        for (String item : coll) { 
	    if (item != null) { str.append(delim).append(item); };
        }
	String result = str.toString();
	if (result.isEmpty()) { return result; }
        return result.substring(delim.length());
  }

}

//==== END OF FILE
