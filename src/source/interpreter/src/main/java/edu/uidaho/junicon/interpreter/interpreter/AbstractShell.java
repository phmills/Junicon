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

import edu.uidaho.junicon.grammars.common.*;
import edu.uidaho.junicon.interpreter.parser.*;
import edu.uidaho.junicon.interpreter.transformer.*;
import edu.uidaho.junicon.support.transforms.IThreadResolver;
import edu.uidaho.junicon.support.transforms.IThreadResource;
import edu.uidaho.junicon.runtime.util.FileCopy;
import edu.uidaho.junicon.runtime.util.ILogger;
import edu.uidaho.junicon.runtime.util.ILogger.Trace;
import edu.uidaho.junicon.runtime.util.LoggerFactory;

// import java.util.EnumSet;
import java.io.*;
import java.util.*;
import org.w3c.dom.Document;		// For DOM building

/**
 * Base implementation of a transformational multi-interpreter
 * used by both the command shell, or meta-interpreter,
 * and sub-interpreters for specific languages.
 * The action is to parse and transform statements, 
 * decide to whom to delegate them to, and then
 * either execute them using a scripting substrate
 * or dispatch them to another sub-interpreter for further transformation.
 * AbstractShell has a per-thread notion of current interpreter.
 * Child threads inherit the parent thread's notion of interpreter.
 *
 * @author Peter Mills
 */
public class AbstractShell extends Environment
		implements IInterpreter {

  //====
  // Configuration
  //====
  private String name = "";	// Is non-null
  private String type = "";	// Is non-null
  private IInterpreter parent = null;		// Parent interpreter
  private IParser parser = null;
  private ISubstrate substrate = null;
  private ILogger logger = LoggerFactory.getLogger();	// Never null
  private boolean useParentLogger = true;

  // Dispatcher for delegate selection
  private Map<String, IInterpreter> children =
	new HashMap<String, IInterpreter>();
  private IInterpreter defaultInterpreter = null;  // Default child interpreter

  //====
  // State
  //====
  private boolean emptyLastVerbose = false;  // last printVerbose line was empty

  //====
  // Current interpreter thread local.
  //====
  private static InheritableThreadLocal<IInterpreter> currentInterpreter =
		new InheritableThreadLocal<IInterpreter> ();

  // Static methods used by XSLT transforms.
  private IThreadResource transformSupport = null;

  //====
  // Setters
  //====
  private boolean quitFlag = false;
  private String prompt = "";
  private String partialPrompt = "";

  private boolean normalizeArtifact = false;
  private boolean filterArtifact = false;
  private boolean transformArtifact = false;
  private IParser xmlPatternParser = null;	// new RexPathParser();

  private static boolean defaultCompileTransforms = false;
  private boolean compileTransforms = defaultCompileTransforms;
  private boolean showRawSubstrateErrors = false;

  boolean stopScriptOnError = false;  // Stop in middle of script on error
  boolean resetParserOnError = false;

  //====
  // Language context
  //====
  LanguageContext defaultLanguage = new LanguageContext();
  LanguageContext currentLanguage = defaultLanguage;
  Deque<LanguageContext> languageStack = new ArrayDeque<LanguageContext>();

  //====
  // Constants
  //====
  private final static int line_feed = '\n';
  private final static int carriage_return = '\r';
  private char escapeChar = '\u001B';		// Esc
  private char killChar = '\u0015';		// Kill
  private String killLineString = String.valueOf(killChar);

  //======================================================================
  // Constructors
  //======================================================================

  /**
   * Constructor.
   * Sets currentInterpreter to this.
   * Should separately setParser, setDefaultProperties,
   * and setDelegateEnvironment.
   */
  public AbstractShell () {
	init();
  }

  /**
   * Constructor with parser.
   * Sets currentInterpreter to this.
   */
  public AbstractShell (IParser parser) {
	init(parser);
  }

  /**
   * Constructor with parser and delegated environment.
   * Sets currentInterpreter to this.
   */
  public AbstractShell (IParser parser, IEnvironment delegateEnvironment) {
	super(delegateEnvironment);
	init(parser);
  }

  /**
   * Constructor with parser, delegated environment, and default properties.
   */
  public AbstractShell (IParser parser, IEnvironment delegateEnvironment,
		Properties defaultProperties) {
	super(delegateEnvironment, defaultProperties);
	init(parser);
  }

  //======================================================================
  // Initialization
  //======================================================================

  /**
   * Initialize interpreter shell state.
   * This occurs before setters in dependency injection.
   */
  private void init (IParser parser) {
	this.parser = parser;
	setCurrentInterpreter(this);
  }

  /**
   * Initialize interpreter shell state.
   * This occurs before setters in dependency injection.
   */
  private void init () {
	setCurrentInterpreter(this);
  }

  public void setInit (String dummy) throws InterpreterException {
  }

  public void setRunInterpreter (String dummy) throws InterpreterException {
  }

  //======================================================================
  // Destroy
  //======================================================================

  public void destroy () {
	return;
  }

  //======================================================================
  // Current thread's interpreter
  //======================================================================

  /**
   * Set the current thread's interpreter.   
   */
  public static void setCurrentInterpreter (IInterpreter theCurrentInterpreter) {
	currentInterpreter.set(theCurrentInterpreter);
  }

  /**
   * Get the current thread's interpreter.
   */
  public static IInterpreter getCurrentInterpreter () {
	return currentInterpreter.get();
  }

  //==========================================================================
  // Current thread's transform cut-through support.
  // Get a unique transform cut-through for each thread.
  // Delegates to this resolver.
  //==========================================================================
  public void setTransformSupport (IThreadResource support) {
	this.transformSupport = support;
	if (transformSupport != null) {
		transformSupport.setThreadResolver(this);
	}
  }

  public IThreadResource getTransformSupport () {
	return transformSupport;
  }

  public IThreadResource getThreadResource () {
	return getCurrentInterpreter().getTransformSupport();
  }

  //======================================================================
  // Language stack.
  //======================================================================
  public void setCurrentLanguage (LanguageContext context) {
	if (context == null) { return; }
	currentLanguage = context;
	setIsInterpretive(currentLanguage.isInterpretive);
	setIsEmbedded(currentLanguage.isEmbedded);
	getLogger().setIsTrace(currentLanguage.trace);
  }

  public LanguageContext getCurrentLanguage () {
	return currentLanguage;
  }

  public LanguageContext saveLanguage () {
	LanguageContext saved = new LanguageContext(currentLanguage);
	saved.trace = EnumSet.copyOf(getLogger().getIsTrace());
	return saved;
  }

  public void restoreLanguage (LanguageContext saved) {
	if (saved == null) { return; }
	setCurrentLanguage(saved);
	if (parser != null) {
		parser.restoreState(saved.parserState);
	}
  }

  public LanguageContext pushLanguage (LanguageContext context) {
	if (context == null) { return context; }
	LanguageContext previous = currentLanguage;
	// Save parser state in previous, moves collections & resets actual
	if (parser != null) {
		previous.parserState = parser.saveState();
	}
	languageStack.push(previous);
	restoreLanguage(context);
	return previous;
  }

  public LanguageContext pushLanguage () {
	LanguageContext previous = currentLanguage;
	LanguageContext context = saveLanguage();
	// Save parser state in previous, moves collection & resets actual
	if (parser != null) {
		previous.parserState = parser.saveState();
	}
	languageStack.push(previous);
	context.resetState();
	context.parentContext = previous;
	context.isEmbedded = isPartialStatement();
	setCurrentLanguage(context);
	resetParser();
	return previous;
  }

  public LanguageContext popLanguage () {
	if (languageStack.isEmpty()) { return currentLanguage; }
	LanguageContext saved = languageStack.pop();
	restoreLanguage(saved);
	return saved;
  }

  public void resetLanguage () {
	languageStack.clear();
	restoreLanguage(defaultLanguage);
	currentLanguage.resetState();
	resetParser();
  }

  public void resetVerboseSettings () {
	setEcho(false);
	setIsVerbose(EnumSet.of(Trace.OFF));
	setInheritVerbose(EnumSet.of(Trace.OFF));
	getLogger().setIsTrace(EnumSet.of(Trace.OFF));
  }

  public void setIsEmbedded (boolean on) {
	currentLanguage.isEmbedded = on;
	setProperty("isEmbedded", 
		Boolean.toString(currentLanguage.isEmbedded));
  }

  public boolean getIsEmbedded () {
	return currentLanguage.isEmbedded;
  }

  //======================================================================
  // Identity and parent.
  //======================================================================

  public String getName () {
	return name;
  }

  public void setName (String name) {
	if (name == null) { name = ""; };
	this.name = name;
  }

  public String getType () {
	return type;
  }

  public void setType (String type) {
	if (type == null) { type = ""; };
	this.type = type;
  }

  //======================================================================
  // Dispatcher and substrate setters.
  //======================================================================

  public final void setParent (IInterpreter parent) {
	this.parent = parent;
  }

  public final IInterpreter getParent() {
	return parent;
  }

  public final void setSubstrate (ISubstrate substrate) {
	this.substrate = substrate;
  }
  
  public final ISubstrate getSubstrate () {
	return substrate;
  }

  //======================================================================
  // Parser and logger setters.
  //======================================================================

  public final void setLogger (ILogger logger) {
	if (logger == null) { return; }
	this.logger = logger;
	useParentLogger = false;
  }

  public final ILogger getLogger () {
	if ((parent == null) || (! useParentLogger)) {
		return logger;
	}
	return parent.getLogger();
  }

  public final void setParser (IParser parser) {
	this.parser = parser;
  }

  public final IParser getParser () {
	return parser;
  }

  //======================================================================
  // Setters for dependency injection : language context
  //======================================================================

  public void setIsInteractive (boolean on) {
	currentLanguage.isInteractive = on;
  }

  public boolean getIsInteractive () {
	return currentLanguage.isInteractive;
  }

  public void setIsInterpretive (boolean on) {
	currentLanguage.isInterpretive = on;
	setProperty("isInterpretive", 
		Boolean.toString(currentLanguage.isInterpretive));
  }

  public boolean getIsInterpretive () {
	return currentLanguage.isInterpretive;
  }

  public void setDoNotPreprocess (boolean on) {
	currentLanguage.doNotPreprocess = on;
  }

  public boolean getDoNotPreprocess () {
	return currentLanguage.doNotPreprocess;
  }

  public void setDoNotDetect (boolean on) {
	currentLanguage.doNotDetect = on;
  }

  public boolean getDoNotDetect () {
	return currentLanguage.doNotDetect;
  }

  public void setDoNotTransform (boolean on) {
	currentLanguage.doNotTransform = on;
  }

  public boolean getDoNotTransform () {
	return currentLanguage.doNotTransform;
  }

  public void setJustNormalize (boolean on) {
	currentLanguage.justNormalize = on;
  }

  public boolean getJustNormalize () {
	return currentLanguage.justNormalize;
  }

  public void setDoNotExecute (boolean on) {
	currentLanguage.doNotExecute = on;
  }

  public boolean getDoNotExecute () {
	return currentLanguage.doNotExecute;
  }

  public void setIsVerbose (EnumSet<Trace> isVerbose) {
	if (isVerbose == null) { return; }
	currentLanguage.verbose = isVerbose;
  }

  public EnumSet<Trace> getIsVerbose () {
	return currentLanguage.verbose;
  }

  public void setInheritVerbose (EnumSet<Trace> isVerbose) {
	if (isVerbose == null) { return; }
	currentLanguage.inheritVerbose = isVerbose;
  }

  public EnumSet<Trace> getInheritVerbose () {
	return currentLanguage.inheritVerbose;
  }

  public void setEcho (boolean on) {
	currentLanguage.echo = on;
  }

  public boolean getEcho () {
	return currentLanguage.echo;
  }

  //======================================================================
  // Setters for dependency injection.
  //======================================================================

  public void setStopScriptOnError (boolean on) {
	this.stopScriptOnError = on;
  }

  public boolean getStopScriptOnError () {
	return stopScriptOnError;
  }

  public void setCompileTransforms (boolean doCompile) {
	this.compileTransforms = doCompile;
  }

  public boolean getCompileTransforms () {
	return compileTransforms;
  }

  public void setShowRawSubstrateErrors (boolean showRawErrors) {
	this.showRawSubstrateErrors = showRawErrors;
  }

  public boolean getShowRawSubstrateErrors () {
	return showRawSubstrateErrors;
  }

  public void setResetParserOnError (boolean on) {
	this.resetParserOnError = on;
  }

  public boolean getResetParserOnError () {
	return resetParserOnError;
  }

  public void setKillLineChar (char killChar) {
	this.killChar = killChar;
  }

  public char getKillLineChar () {
	return killChar;
  }

  //====================================================================
  // Setters for static defaults.
  //	The setters are non-static for Spring dependency injection.
  //====================================================================

  public void setDefaultCompileTransforms (boolean doCompile) {
	this.defaultCompileTransforms = doCompile;
  }

  public boolean getDefaultCompileTransforms () {
	return defaultCompileTransforms;
  }

  //======================================================================
  // Parser delegation - Setters for dependency injection.
  //======================================================================

  public final String getLineSeparator () {
	if (parser == null) { return "\n"; };
	return parser.getLineSeparator();
  }
	
  //======================================================================
  // Parser delegation.
  //======================================================================

  public final String getPartialStatement () {
	if (parser == null) { return null; };
	return parser.getPartialStatement();
  }
	
  public void resetParser () {
	if (parser == null) { return; };
	parser.resetParser();
  }

  public final String getLastParseInput () {
	if (parser == null) { return null; };
	return parser.getLastParseInput();
  }
	
  /**
   * Sets last parse input.
   * Used to set input if do not transform or parse,
   * since filter exceptions uses this field.
   */
  public final void setLastParseInput (String input) {
	if (parser == null) { return; };
	parser.setLastParseInput(input);
  }
	
  //======================================================================
  // Verbose getters.
  //======================================================================

  public boolean isVerbose () {
	return currentLanguage.verbose.contains(Trace.ON);
  }

  public boolean isVerboseDetail () {
	return currentLanguage.verbose.contains(Trace.DETAIL);
  }

  public boolean isVerbosePreprocessor () {
	return currentLanguage.verbose.contains(Trace.PREPROCESS);
  }

  //======================================================================
  // Dispatcher for delegate selection: sub-interpreter management.
  //======================================================================

  public void setDefaultDispatchInterpreterByName (String child) {
	if (child == null) { return; };
	defaultInterpreter = children.get(child);
  }

  public void setDefaultDispatchInterpreter (IInterpreter child) {
	defaultInterpreter = child;
  }

  public IInterpreter getDefaultDispatchInterpeter () {
	return defaultInterpreter;
  }

  public final void setDispatchChildren (Map<String, IInterpreter> children) {
	if (children == null) { return; };
	this.children = children; 
	for (String name : children.keySet()) {  // children.values()
		IInterpreter child = children.get(name);
		if (child != null) {
			child.setName(name);
			setAddDispatchChild(child);
		};
	}
  }

  public final void setDispatchChildren (List<IInterpreter> childlist) {
	if (childlist == null) { return; };
	children.clear();
	for (IInterpreter child : childlist) {
		setAddDispatchChild(child);
	};
  }

  public final Map<String, IInterpreter> getDispatchChildren () {
	return children;
  }

  public final void setAddDispatchChild (IInterpreter child) {
	if (child == null) { return; };
	String name = child.getName();
	if (name == null) { return; };
	children.put(name, child);
	child.setParent(this);
  }

  //======================================================================
  // Interpretive loop steps: Handle directive.
  //======================================================================

   public boolean isDirective (String line) {
	if (line == null) { return false; }
	line = line.trim();
	if (line.startsWith("@<") || line.startsWith("#@<")) {
		return true;
	}
	return false;
   }

   public String handleDirective (String line) throws InterpreterException {
	return line;
   }

   public String handleBeginOfScript (String annotation) {
	return annotation;
   }

   public String handleEndOfScript (String line) {
	return line;
   }

  //======================================================================
  // Interpretive loop steps: detect statement.
  //======================================================================

  public final String getParseUnit (String inputLine,
		ILineContext context) throws ParseException {
	if (parser == null) { return inputLine; };
	parser.setDoNotPreprocess(getDoNotPreprocess());
	return parser.getParseUnit(inputLine, context);
  }

  public final ILineContext getParseUnitContext () {
	if (parser == null) { return null; };
	return parser.getParseUnitContext();
  }

  public final boolean isPartialStatement () {
	if (parser == null) { return false; };
	return parser.isPartialStatement();
  }
	
  public final boolean isClosedStatement () {
	if (parser == null) { return true; };
	return parser.isClosedStatement();
  }
	
  //======================================================================
  // Interpretive loop: parse, decorate, normalize, filter, transform, execute.
  //======================================================================

  public final Document parse (String sourceInput, ILineContext context)
		throws ParseException {
	if (parser == null) { return null; };
	return parser.parse(sourceInput, context);
  }
	
  public Document decorate (String inputSource, Document parseTree,
		ILineContext context) throws InterpreterException {
	return parseTree;
  }

  public void resetTransformed () {
  }

  public Document normalize (Document parseTree, ILineContext context)
		throws InterpreterException {
	return parseTree;
  }

  public String normalize (String inputSource, Document normalized,
		ILineContext context) throws InterpreterException {
	return inputSource;
  }

  public String filterOut (String inputSource, Document parseTree,
		ILineContext context) throws InterpreterException {
	return inputSource;
  }

  public String transform (String inputSource, Document parseTree,
		ILineContext context) throws InterpreterException {
	return inputSource;
  }

  public IInterpreter chooseDelegate (String transformedSource,
		ILineContext context) {
	return defaultInterpreter;
  }

  public final Object dispatch (String transformedSource,
		ILineContext context) throws InterpreterException {
  	IInterpreter delegated = chooseDelegate(transformedSource,
						context);
	if ((delegated == null) || (delegated == this)) {
		setCurrentInterpreter(this);
		return executeOnSubstrate(transformedSource, context);
	};

	// Inherit attributes into sub-interpreters
	delegated.setIsInteractive(getIsInteractive());
	delegated.setIsInterpretive(getIsInterpretive());
	delegated.setIsVerbose(getInheritVerbose());
	delegated.setInheritVerbose(getInheritVerbose());
	delegated.setDoNotPreprocess(getDoNotPreprocess());
	delegated.setDoNotDetect(getDoNotDetect());
	delegated.setDoNotTransform(getDoNotTransform());
	delegated.setJustNormalize(getJustNormalize());
	delegated.setDoNotExecute(getDoNotExecute());

	return delegated.evalLine(transformedSource, context);
  }

  public final Object executeOnSubstrate (String transformedSource,
		ILineContext context) throws InterpreterException {
	if ((transformedSource == null) || (substrate == null)) {
		return null; };
	if (getDoNotExecute()) { return transformedSource; };
	Object result = null;
	try {
		result = getSubstrate().eval(transformedSource, context);
	} catch (Throwable e) {
		throw new SubstrateException(e);
	};
	return result;
  }

  public final Object eval (String inputSource, ILineContext context)
		throws InterpreterException {
	setCurrentInterpreter (this);
	setLastParseInput(inputSource);	// for filterException if error
	resetTransformed();		// override for any specific eval steps
	if (inputSource == null) { return null; };
	if (getDoNotTransform()) {
		if (isVerboseDetail()) {
		    printVerbose(getName() + " Transformed:   " + inputSource);
		} else if (isVerbose()) {
		    printVerbose(inputSource);
		}
		return dispatch(inputSource, context);
	}
	Document parsed = parse(inputSource, context);
	Document decorated = decorate(inputSource, parsed, context);
	Document normalized = normalize(decorated, context);
	String normalizedText = normalize(inputSource, normalized, context);
	if (isVerboseDetail() && (normalizedText != null)) {
		printVerbose(getName() + " Normalized:    " + normalizedText);
	}
	if (null == filterOut (normalizedText, normalized, context)) {
		if (isVerboseDetail()) {
		    printVerbose(getName() + " Filter: filtered out");
		};
		return null;
	};
	if (getJustNormalize()) {
		if (normalizedText != null) {
			if (isVerbose()) { printVerbose(normalizedText); }
		}
		return dispatch(normalizedText, context);
	}
	String transformed = transform(normalizedText, normalized, context);
	if (transformed != null) {
		if (isVerboseDetail()) {
		    printVerbose(getName() + " Transformed:   " + transformed);
		} else if (isVerbose()) {
		    printVerbose(transformed);
		}
	}
	return dispatch(transformed, context);
  }
  
  /**
   * Evaluates a line of input.
   * Extracts any complete parseunits from ongoing history.
   * Then does eval on the units,
   * or if aggregate is non-null just appends them to aggregate.
   */
  private Object evalOrAggregateLine (String line, ILineContext context,
		StringBuilder aggregate, boolean preprocessOnly)
		throws InterpreterException {
	Object retcode = null;
	try {
	  if (! preprocessOnly) {
		if (getEcho() && (line != null)) { printVerbose(line); };
	  }
	  if (getIsInteractive() && line.endsWith(killLineString)) {
		resetLanguage();	// resetParser();
		return null;
	  }

	  // Handle directive
	  if (! preprocessOnly) {
	    if (isDirective(line)) {
		String annotation = line;
		line = handleDirective(line);
		//====
		// if (currentLanguage.atStartOfScript) { pushLanguage() }
		//====
		// Embedded: goes thru metaparser and add to atScriptContents
		//	Adds to: atScriptContents, getPartialStatement
		// Not embedded: groovy shoot thru, icon normal eval
		//	Adds to: aggregate, getPartialStatement
		//====
		if (currentLanguage.atBeginOfScript) {
		    currentLanguage.atBeginOfScript = false;
		    // Clear out any previous stuff before script
		    if ((! currentLanguage.isEmbedded) &&
				(aggregate != null)) {
			if (aggregate.length() > 0) {
			  try {
			    retcode = eval(aggregate.toString(), context);
			  } catch (InterpreterException e) {
			    if (stopScriptOnError) { throw e;
			    } else { printException(e); }
			  }
			}
			aggregate.setLength(0);
		    }
		    pushLanguage();
		    currentLanguage.isInsideAtScript = true;
		    handleBeginOfScript(annotation);
		    return null;
		}
		if (currentLanguage.atEndOfScript) {
		  if (currentLanguage.isEmbedded) {
			if (isPartialStatement()) {
			      // Try to complete last parse unit without ";"
			      tryToCompleteStatement(
					currentLanguage.atScriptContents);
			}
			line = handleEndOfScript(
				currentLanguage.atScriptContents.toString());
			// Handle @</script> with eval() of contents
			if (currentLanguage.evalAtEndOfScript) {
			  resetVerboseSettings();
			  Object ret = eval(line, context);
			  if (ret instanceof String) {
				line = (String) ret;
			  } else { line = null; }
			}
		  } else {
			if (aggregate == null) {
				aggregate = new StringBuilder();
			}
			if (isPartialStatement()) {
			      // Try to complete last parse unit with ";"
			      tryToCompleteStatement(aggregate);
			}
			if (aggregate.length() > 0) {
			  try {
			    retcode = eval(aggregate.toString(), context);
			  } catch (InterpreterException e) {
			    if (stopScriptOnError) { throw e;
			    } else { printException(e); }
			  }
			}
			aggregate.setLength(0);
			popLanguage();
			return retcode;
		  }
		  popLanguage();
		  if (line == null) { return null; };
		}
	    }
		 
		  // Line in @<script> inside statement, accumulate it
		  if (currentLanguage.isInsideAtScript &&
				currentLanguage.isEmbedded &&
				(line != null)) {
			//====
			// WARNING: cannot preprocess Icon inside Groovy,
			//  unless have separate metaparser for each language.
	  		//====
			// currentLanguage.atScriptContents += (line + "\n");
			//====
			evalOrAggregateLine(line, context,
				currentLanguage.atScriptContents, true);
			return null;
		  }
	  }

		// If skip metaparser, just append or eval
		if (getDoNotDetect()) {
			if (aggregate != null) {
				aggregate.append(line);
			} else if (preprocessOnly) {
				return line;
			} else {
				retcode = eval(line, context);
			}
			return retcode;
		}

		// Get parseunit, and append or eval
		String parseUnit = getParseUnit(line, context);
		ILineContext unitContext = getParseUnitContext();
		while (parseUnit != null) {
			if (isVerbosePreprocessor() && (parseUnit != null)) {
			  printVerbose(getName() + " Preprocessor:  " + parseUnit);
			}
			if (aggregate != null) {
				aggregate.append(parseUnit);
				aggregate.append("\n");
			} else {
				retcode = eval(parseUnit, unitContext);
			}
			parseUnit = getParseUnit(null, context);
			unitContext = getParseUnitContext();
		}
	} catch (ParseException e) {
		InterpreterException ie = filterException(e);
		if (resetParserOnError) {
			resetLanguage();	// resetParser();
		};
		throw ie;
	} catch (Exception e) {
		throw filterException(e);
	} catch (Error e) {
		throw filterException(e);
	};
	return retcode;
  }

  public final Object evalLine (String line, ILineContext context)
		throws InterpreterException {		// stateful eval
	return evalOrAggregateLine(line, context, null, false);
  }

  public final Object exec (String inputSource, ILineContext context)
		throws InterpreterException {
	return eval(inputSource, context);
  }

  public final void execFile (InputStream instream,
			ILineContext fileContext,
			boolean promptFlag,
			boolean stopScriptOnError,
			boolean aggregateInput) 
		throws InterpreterException {
	if (instream == null) { return; };
	if (fileContext == null) {
		fileContext = new LineContext("", null);
	}
	StringBuilder aggregate = new StringBuilder();
	try {
	    String line = null;
	    int linenumber = 0;
	    if (promptFlag) { displayPrompt(); };
	    UnBufferedReader rdr = new UnBufferedReader(instream);
	    if (aggregateInput) {
		while (null != (line = rdr.readLine())) {
		    ILineContext context = new LineContext(
			fileContext, null, linenumber);
		    evalOrAggregateLine(line, context, aggregate, false);
		    linenumber++;
		}
	    } else {
		while (null != (line = rdr.readLine())) {
			try {
				ILineContext context = new LineContext(
					fileContext, null, linenumber);
				Object retcode = evalLine(line, context);
			} catch (InterpreterException e) {
				if (stopScriptOnError) { throw e;
				} else { printException(e); }
			}
			if (quitFlag) { break; };
			if (promptFlag) { displayPrompt(); };
			linenumber++;
		}
	    }

	    ILineContext context = new LineContext(
		fileContext, null, linenumber);

	    // Try to complete last parse unit without ";"
	    if (isPartialStatement()) {
		tryToCompleteStatement(aggregate);
	    }

	    // Finally, at end, eval using currentLanguage, if nonempty
	    if (aggregate.length() > 0) {
		  try {
			Object retcode = eval(aggregate.toString(), context);
		  } catch (InterpreterException e) {
			if (stopScriptOnError) { throw e;
			} else { printException(e); }
		  }
	    }
        } catch (IOException e) {
		throw filterException(e);
        } finally {
		resetLanguage();	// resetParser();
		// out.close();
		// err.close();
	}
  }

  /**
   * Try to complete last parse unit without ";",
   * if isPartialStatement() and isClosedStatement().
   */
  private void tryToCompleteStatement (StringBuilder aggregate)
		throws InterpreterException {
	// if (currentLanguage.doNotPreprocess) { return; }
	if (isPartialStatement()) {
		if (isClosedStatement()) {
		  if (aggregate != null) {
			aggregate.append(getPartialStatement());
			resetParser();
		  }
		}
	}
	if (isPartialStatement()) {
		throw filterException(
			new ParseException(
		"Parse error: script ends with incomplete statement: "
		+ ((getPartialStatement()==null)?"":getPartialStatement())
				) );
	}
  }
	
  public final void execFile (InputStream instream, ILineContext context,
		boolean promptflag) throws InterpreterException {
	execFile(instream, context, promptflag, false, false);
  }

  public InterpreterException filterException (Throwable cause) {
	if (cause == null) { return null; };
	InterpreterException ie = null;
	if (cause instanceof InterpreterException) {
		ie = (InterpreterException) cause;
	} else {
		ie = new ExecuteException(cause);
	};
	printException(ie);
	return ie;
  }

  /**
   * Print out an exception.
   */
  protected void printException (Throwable cause) {
	printException(cause, logger, logger.isDebugDetail());
  }

  /**
   * Print to stdout.
   * If non-empty line ends in carriage return or line feed,
   * do print instead of println.
   * Lines evaluated in CommandShell will not end with CR,
   * while those below it in sub-interpreters will,
   * since StatementDetector adds back in the CR stripped by readLine.
   * StatementDetector also deletes empty lines
   * when forming a complete statement.
   * Since verbose is typically inherited into sub-interpreters
   * and not active in the CommandShell, verbose lines will end in CR.
   */
  void printVerbose (String line) {
	if (line == null) { return; }
	if (line.trim().isEmpty()) {	// Skip multiple empty lines
		if (emptyLastVerbose) { return; }
		emptyLastVerbose = true;
	} else {
		emptyLastVerbose = false;
	}
	if (line.endsWith("\n") || line.endsWith("\r")) {
	    logger.print(line);
	} else {
	    logger.println(line);
	}
  }

  /**
   * Trim string on right;
   */
  String trimRight (String str) {
	if (str == null) { return null; }
	return str.replaceFirst("\\s+$", "");
  }

  //======================================================================
  // Readline.
  //======================================================================

/**
 * UnBufferedReader supports one method, readline, on
 * an unbuffered inputstream.
 */
private class UnBufferedReader {

  private int prevChr = -1;	// lookbehind for carriage_return
  private boolean isEof = false;
  private InputStream rdr = null;

  /**
   * Constructor.
   */
  public UnBufferedReader (InputStream rdr) {
	this.rdr = rdr;
  }

  /**
   * Get a line of text input, without buffering.
   * A line is considered to be terminated by any 
   * one of a line feed ('\n'), a carriage return ('\r'), or a 
   * carriage return followed immediately by a linefeed. 
   * (Similar to BufferedReader.readline()).
   * @return line without trailing separator,
   * or <code>null</code> upon end-of-file.
   */
  public String readLine () throws IOException {
	if (rdr == null) { return null; };
	if (isEof) { return null; };
	StringBuffer inputBuf = new StringBuffer();
	boolean isContent = false;
	int chr = -1;
	while ((chr = rdr.read()) != -1) {
		if (chr == carriage_return) {
			isContent = true;
			prevChr = chr;
			break;
		} else if (chr == line_feed) {		// ignore if previous CR
			if (prevChr != carriage_return) {
				isContent = true;
				prevChr = chr;
				break;
			};
		} else {
			isContent = true;
			inputBuf.append((char) chr);
		};
		prevChr = chr;
	};
	if (chr == -1) {	// Degenerate cases: \r\n eof
		isEof = true;
		if (! isContent) { return null; };
	}
	return inputBuf.toString();
  }
}

  //======================================================================
  // Prompt and exit.
  //======================================================================
  public final void exit () { 
	quitFlag = true;
  }

  public void displayPrompt () { 
	String thePrompt = prompt;
	if (isPartialStatement()) { thePrompt = partialPrompt; };
	if (thePrompt == null) { return; };
	logger.print(thePrompt);
	logger.flush();
  }

  /**
   * Set prompt.
   */
  public void setPrompt (String prompt) { 
	this.prompt = prompt;
  }

  /**
   * Set prompt when awaiting input to complete a multi-line statement.
   */
  public void setPartialPrompt (String prompt) { 
	this.partialPrompt = prompt;
  }

  /**
   * Get prompt.
   */
  public String getPrompt () { 
	return prompt;
  }

  /**
   * Get prompt for awaiting input to complete a multi-line statement.
   */
  public String getPartialPrompt() {
	return partialPrompt;
  }

  //====================================================================
  // Load and evaluate script.
  //====================================================================

  public boolean execScript (String script, ILineContext context,
		boolean promptFlag, boolean stopScriptOnError)
		throws InterpreterException {
	if (script == null) { return false; };
	execFile( new ByteArrayInputStream(script.getBytes()), context,
		promptFlag, stopScriptOnError, true);
	return true;
  }

  public boolean execScriptFile (String filename,
		boolean promptFlag, boolean stopScriptOnError)
		throws InterpreterException {
	String startupFile = null;
	try {
	    startupFile = FileCopy.fileToString(filename);
	} catch (IOException e) {
	    throw new InterpreterException(e);
	}
	if (startupFile == null) { return false; };
	ILineContext context = new LineContext(filename, null);
	execFile( new ByteArrayInputStream(startupFile.getBytes()),
		context, promptFlag, stopScriptOnError, true);
	return true;
  }

  //======================================================================
  // Shell identifiers.
  //======================================================================
  /**
   * Returns the shell name and instance.
   */
  public String toString(){
	String classname = getClass().getName();
	int hashCode = System.identityHashCode(this);

	return classname + '@' + Integer.toHexString(hashCode);
  }

  //======================================================================
  // Static error message filter.
  //======================================================================

  /**
   * Print out an exception.
   */
  public static void printException (Throwable cause, ILogger logger,
			boolean printStack) {
	if ((logger == null) || (cause == null)) { return; };
	if (cause instanceof InterpreterException) {
		InterpreterException ie = (InterpreterException) cause;
		if (ie.getHaveNotified()) { return; };
		ie.setHaveNotified(true);
	}
	String message = cause.getMessage();
	if (message == null || message.equals("null")) {
	    message = cause.toString();
	}
	logger.error(message);
	if (printStack) {
		logger.error(cause);	// cause.printStackTrace(System.err);
	}
  }

  //======================================================================
  // Static cut-throughs for environment.
  //======================================================================

  /**
   * Get value from the current interpreter thread's environment.
   */
  public static Object getLocalEnv (String name) {
	IEnvironment context = (IEnvironment) getCurrentInterpreter();
	if (context == null) { return null; };
	return context.getEnv(name);
  }

  /**
   * Set value in the current interpreter thread's environment.
   */
  public static void setLocalEnv (String name, Object value) {
	IEnvironment context = (IEnvironment) getCurrentInterpreter();
	if (context == null) { return; };
	context.setEnv(name, value);
  } 

}

//==== END OF FILE
