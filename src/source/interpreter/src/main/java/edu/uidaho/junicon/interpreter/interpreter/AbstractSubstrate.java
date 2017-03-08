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
import edu.uidaho.junicon.runtime.util.ILogger.Trace;
import edu.uidaho.junicon.runtime.util.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.io.File;

import javax.script.ScriptException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.Invocable;
import javax.script.Bindings;
import javax.script.ScriptContext;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;

/**
 * Base class for the underlying substrate that executes scripts.
 * The substrate either uses a Java ScriptEngine, or can delegate to
 * another IScriptEval for evaluating commands.
 * Methods are also provided for compiling to the substrate
 * and from there to Java.
 *
 * @author Peter Mills
 */
public class AbstractSubstrate implements ISubstrate {

  private IInterpreter parent = null;	// Parent interpreter
  private ILogger logger = LoggerFactory.getLogger();	// Never null
  private boolean useParentLogger = true;

  private boolean quietScriptExceptions = false;
  private static String defaultLinesep = System.getProperty("line.separator", "\n");
  private String linesep = defaultLinesep;

  private IScriptEval delegateEval = null;

  private ScriptEngineManager scriptEngineManager = new ScriptEngineManager(); 
  private String defaultScriptExtension = "js";	// Default script engine
  private ScriptEngine defaultEngine = null;
  private ScriptEngine defaultEngineFromExtension = null;
  private ScriptEngine currentEngine = null;
  private Map<String, ScriptEngine> scriptEngineCache =
		 new HashMap<String, ScriptEngine>();

  private Object lock = new Object();	// for synchronized evalToObject
  private ExecutorService executor = Executors.newCachedThreadPool();

  //======================================================================
  // Constructors
  //======================================================================

  /**
   * Constructor.
   */
  public AbstractSubstrate () {
  }

  //======================================================================
  // Setters for dependency injection.
  //======================================================================

  public final void setParent(IInterpreter parent) {
	this.parent = parent;
  }

  public final IInterpreter getParent() {
	return parent;
  }

  public void setLogger (ILogger logger) {
	if (logger == null) { return; }
	this.logger = logger;
	useParentLogger = false;
  }

  public ILogger getLogger () {
	if ((parent == null) || (! useParentLogger)) {
		return logger;
	}
	return parent.getLogger();
  }

  public boolean isQuietScriptExceptions () {
	return quietScriptExceptions;
  }

  public void setQuietScriptExceptions (boolean quietScriptExceptions) {
	this.quietScriptExceptions = quietScriptExceptions;
  }

  //======================================================================
  // Setters for delegate substrate.
  //======================================================================

  public void setDelegateForEval (IScriptEval delegate) {
	this.delegateEval = delegate;
  }

  public IScriptEval getDelegateForEval () {
	return delegateEval;
  }

  //======================================================================
  // Script engine.
  //======================================================================

  public void setScriptEngineManager (ScriptEngineManager manager) {
	if (manager == null) { return; };
	this.scriptEngineManager = manager; 
	resetScriptEngines();
  } 

  public ScriptEngineManager getScriptEngineManager () {
	return scriptEngineManager;
  }

  public void setDefaultScriptExtension (String extension) {
	if (extension == null) { return; };
	this.defaultScriptExtension = extension;
  }

  public String getDefaultScriptExtension () {
	return defaultScriptExtension;
  }

  public void setDefaultScriptEngine (ScriptEngine engine) {
	this.defaultEngine = engine;
  }

  public ScriptEngine getDefaultScriptEngine () {
	return defaultEngine;
  }

  public void setScriptEngine (ScriptEngine engine) {
	currentEngine = engine;
  }

  public ScriptEngine getScriptEngine () {
	return currentEngine;
  }

  public ScriptEngine getCurrentEngine () {
	ScriptEngine engine = currentEngine;
	// Get default engine if no current engine
	if (engine == null) {
		engine = getDefaultScriptEngine();
	}
	// If no default engine, get engine for default extension
	if (engine == null) {
		engine = getCachedEngineByExtension(null);
	}
	return engine;
  }

  public ScriptEngine getEngineByExtension (String extension) {
	if ((extension == null) || extension.isEmpty()) {
		extension = getDefaultScriptExtension();
	};
	return scriptEngineManager.getEngineByExtension(extension);
  }

  public ScriptEngine getCachedEngineByExtension (String extension) {
	if ((extension == null) || extension.isEmpty()) {
		extension = getDefaultScriptExtension();
	};
	ScriptEngine engine = scriptEngineCache.get(extension);
	if (engine != null) { return engine; };
	engine = scriptEngineManager.getEngineByExtension(extension);
	if (engine == null) { return null; };
	scriptEngineCache.put(extension, engine);
	return engine;
  }

  public void resetScriptEngines () {
  	// scriptEngineManager = new ScriptEngineManager(); 
	clearScriptEngineCache();
	defaultEngineFromExtension = scriptEngineManager.getEngineByExtension(defaultScriptExtension);
	scriptEngineCache.put(defaultScriptExtension, defaultEngineFromExtension);
  }

  private void clearScriptEngineCache () {
	scriptEngineCache.clear();
  }

  //======================================================================
  // Eval.
  //======================================================================

  public Object eval (String command, ILineContext context)
		throws ScriptException {
	return eval(command, context, null, null);
  }

  public Object eval (String command, ILineContext context,
		Map<String, Object> vars,
		String... params) throws ScriptException {
	if (delegateEval != null) {
		return delegateEval.eval(command, context, vars, params);
	}
	if (command == null) { return null; };
	command = command.trim();
	if (command.isEmpty()) { return null; };
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) {
		throw new ScriptException (
			"Script error: No script engine found" );
	};
	
	Object result = null;

	if (vars != null) {
		for (String name : vars.keySet()) {
			if (name == null) { continue; };
			Object value = vars.get(name);
			if (value != null) {
				engine.put(name, value);
			}
		};
	}

	// synchronized (lock) { }
	try {
		result = engine.eval(command);
	} catch (ScriptException e) {

	  if (logger.isDebug()) {
	   logger.error("****************************************");
	   logger.error("**** ScriptException");
	   logger.error("****************************************");
			// e.printStackTrace();
			logger.error(e.toString());
	   logger.error("****************************************");
	  }
	  if (quietScriptExceptions) {
		return null;
	  }
	  throw e;
	} catch (Exception e) {		// (ScriptException e)
	  if (quietScriptExceptions) {
		return null;
	  }
	  throw new ScriptException(e);
	} 
	if (logger.isTraceDetail()) {
	  logger.traceDetail("	Result: " + result);
	  logger.traceDetail("****************************************");
	}
	return result;
  }

  public Future<Object> spawnEval (String command, ILineContext context) {
	return spawnEval(command, context, null, null);
  }

  public Future<Object> spawnEval (String command, ILineContext context,
		Map<String, Object> vars, String... params) {
	final String myCommand = command;
	final ILineContext myContext = context;
	final Map<String, Object> myVars = vars;
	final String[] myParams = params;
	Future<Object> future = executor.submit(new Callable<Object>() {
         	public Object call() throws Exception {
             		return eval(myCommand, myContext, myVars, myParams);
         	}});
	return future;
  }

  public String invokeMethod (String objectName, String methodName,
		String[] args)
		throws ScriptException {
	if (delegateEval != null) {
		return delegateEval.invokeMethod(objectName, methodName, args);
	}
	Object result = null;
	try {
		ScriptEngine engine = getCurrentEngine();
		if (engine == null) {
			throw new ScriptException (
			"Script error: No script engine found" );
		}
		Invocable invocableEngine = (Invocable) engine;
		Object obj = engine.get(objectName);
		if (obj == null) {
			throw new ScriptException(
				"InvokeMethod script object not found.");
		};
		result = invocableEngine.invokeMethod(obj, methodName,
					(Object[]) args);
	} catch (ClassCastException ce) {
		throw new ScriptException(ce);
	} catch (NoSuchMethodException e) {
		throw new ScriptException(e);
	}
	return resultToString(result);
  }

  /**
   * Safe Java invoke.
   */
  private String invoke (Object object, String method, String... params) {
	if ((object == null) || (method == null) || (params == null)) {
		return null;
	};
	Method[] methods = object.getClass().getMethods();
	if (methods == null) { return null; };
	Object result = null;
	for (Method i: methods) {	// uses first matching method name
		if (i.getName().equals(method)) {
			try {
				result = i.invoke(object, (Object[]) params);
			} catch (IllegalAccessException e) {
			} catch (IllegalArgumentException e) {
			} catch (InvocationTargetException e) {
			}
		}
	};
	return resultToString(result);
  }

  private String resultToString (Object result) {
	if (result == null) { return null; };
	if (! (result instanceof String)) {
		return result.toString();
	};
	return (String) result;
  }

  //======================================================================
  // Environment.
  //======================================================================

  public Object getEnv (String name) {
	if (delegateEval != null) { return delegateEval.getEnv(name); }
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) { return null; };
	return engine.get(name);
  }

  public void setEnv (String name, Object value) {
	if (delegateEval != null) {
		delegateEval.setEnv(name, value);
		return;
	}
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) { return; };
	engine.put(name, value);
  }

  public Collection<String> getEnvNames () {
	Map<String, Object> bindings = getEnvBindings();
	if (bindings == null) { return null; };
	return bindings.keySet();
  }

  public Map<String, Object> getEnvBindings () {
	if (delegateEval != null) { return delegateEval.getEnvBindings(); }
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) { return null; };
	return engine.getBindings(ScriptContext.ENGINE_SCOPE); // GLOBAL_SCOPE
  }

  //======================================================================
  // Line separator.
  //======================================================================

  public void setDefaultLineSeparator (String linesep) {
	if ((linesep == null) || (linesep.length() == 0)) { return; };
	this.defaultLinesep = linesep;
  }

  public String getDefaultLineSeparator () {
	return defaultLinesep;
  }

  public void setLineSeparator (String linesep) {
	if ((linesep == null) || (linesep.length() == 0)) { return; };
	this.linesep = linesep;
  }

  public String getLineSeparator () {
	return linesep;
  }

  //======================================================================
  // Input and output redirection.
  //======================================================================

  public void setIn (InputStream in) {
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) { return; }
	ScriptContext context = engine.getContext();
	if (context == null) { return; }
	context.setReader( new InputStreamReader(in) );	// BufferedReader
  }

  public void setOut (OutputStream out) {
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) { return; }
	ScriptContext context = engine.getContext();
	if (context == null) { return; }
	context.setWriter( new OutputStreamWriter(out) ); // BufferedWriter
  }

  public void setErr (OutputStream err) {
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) { return; }
	ScriptContext context = engine.getContext();
	if (context == null) { return; }
	context.setErrorWriter( new OutputStreamWriter(err) ); // BufferedWriter
  }

  public Reader getIn () {
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) { return null; }
	ScriptContext context = engine.getContext();
	if (context == null) { return null; }
	return context.getReader();
  }

  public Writer getOut () {
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) { return null; }
	ScriptContext context = engine.getContext();
	if (context == null) { return null; }
	return context.getWriter();
  }

  public Writer getErr () {
	ScriptEngine engine = getCurrentEngine();
	if (engine == null) { return null; }
	ScriptContext context = engine.getContext();
	if (context == null) { return null; }
	return context.getErrorWriter();
  }

  //======================================================================
  // Compile substrate to Java
  //======================================================================

  public File[] SourceToSubstrate ( Map<String, Object> source,
					String[] transformedSource) 
		throws ExecuteException {
	return null;
  }

  public File[] SubstrateToJava ( String[] transformedSource,
					File [] inputs) 
		throws ExecuteException {
	return null;
  }

  public void load (String jar) {
	try {
		URL url = new File(jar).toURI().toURL();
		addURL(url);
	} catch (Exception e) { ; }
  }

  /**
   * Add url to System classpath.
   */
  private void addURL (URL url) throws Exception {
	URLClassLoader classLoader = (URLClassLoader) 
		ClassLoader.getSystemClassLoader();
	Class clazz= URLClassLoader.class;
	Method method= clazz.getDeclaredMethod("addURL", 
		new Class[] { URL.class });
	method.setAccessible(true);
	method.invoke(classLoader, new Object[] { url });
  }  

  public void unload (String jar) {
  }

  public void reload (String jar) {
	load(jar);
  }

}

//==== END OF FILE
