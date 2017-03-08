//========================================================================
// Copyright (c) 2015 Orielle, LLC.  
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
import edu.uidaho.junicon.support.transforms.Catalog;
import edu.uidaho.junicon.support.transforms.TransformSupport;
import edu.uidaho.junicon.runtime.junicon.iterators.IconNumber;
import edu.uidaho.junicon.runtime.util.FileCopy;
import edu.uidaho.junicon.runtime.util.ILogger;
import edu.uidaho.junicon.runtime.util.ILogger.Trace;
import edu.uidaho.junicon.runtime.util.LoggerFactory;
import edu.uidaho.junicon.runtime.util.JarRunner;
import edu.uidaho.junicon.runtime.util.BatRunner;

import java.util.*;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URI;
import org.w3c.dom.Document;		// For DOM building

import javax.script.ScriptException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * CommandShell is an outer interpretive shell 
 * implemented using a transformational meta-interpreter.
 * The command shell detects complete statements and their target language,
 * and then dispatches them to the appropriate
 * sub-interpreter to parse, transform, and execute
 * on a script engine substrate.
 *
 * The command shell will either interpret, translate, compile, or extract.
 * These options are mutually exclusive.
 * The compile and extract options are ignored if useCmdFile() is off.
 * Otherwise, to compile or extract, the command shell will invoke a
 * futher cmd file to perform the work.
 * If compiling and the file does not end in Java, it will first be translated.
 * If explicitly asked to translate, any compile or extract options are ignored.
 *
 * @author Peter Mills
 */
public class CommandShell extends AbstractShell {

  //==========================================================================
  // Command line arguments
  //==========================================================================
  // Command line arguments
  private String lastScriptName = null;	// Last nonOption script name
  private String[] mainArgs = new String[0];

  // Startup scripts
  private List<String> inputScripts = new ArrayList<String>();
  private List<String> inputScriptNames = new ArrayList<String>();
  private List<String> startupScripts = new ArrayList<String>();
  private List<String> startupScriptNames = new ArrayList<String>();

  // For redirected output
  private String outFilename = null;

  // For error messages before shell is initialized
  private static ILogger defaultLogger = LoggerFactory.getLogger();
  	// private static PrintStream err = System.err;
  private static boolean cmdLinesDebug = false;

  // Usage message
  private String usage = "";

  //==========================================================================
  // Spring configuration
  //==========================================================================
  // System Properties
  private String prompt = ">>> ";
  private String partialPrompt = "... ";

  // Header and trailer
  private String header = "";	// header = buildNumber + "\n" + help;
  private String trailer = "";
  private String help = "";	// help = help.replaceAll("\\\\n","\n");
  private String license = "";
  private String credits = "";

  // Java preface for translate to Java
  private String javaPreface = "";

  // Script files to run for compiling.
  private String appLinux = "";
  private String appWindows = "";

  //==========================================================================
  // Command line arguments, that override Spring configuration
  //==========================================================================
  // Command line arguments
  // private boolean isInterpretive = false; // Interpret vs translate|compile
  // private boolean isInteractive = false;  // Interactive, displayPrompt
  private String manifestFilename = "Manifest.mf";  // When translating to Java

  // Command line arguments
  boolean ignoreSystemStartup = false;
  boolean echoStartup = false;
  boolean echoSystemStartup = false;
  boolean exitOnScriptError = false;  // Exit interpreter on script error
  String commandPrefix = "";
  boolean isAggregateScripts = true;

  // Compile, extract, translate arguments
  boolean performCompile = false;
  boolean performExtract = false;
  boolean performTranslate = false;	// if isToJava, else to Groovy
  // boolean performInterpret = false;	// getIsInterpretive()
  boolean useCmdFile = false;

  // Transform parameters
  private boolean isToJava = false;	// compile to Java
  private boolean useLambdaExpressions = true;	// compile to Java
  private boolean methodAsClosure = false; //vs method reference to plain method

  //==========================================================================
  // Startup processing
  //==========================================================================
  // Saved interpreter properties
  private Properties savedProperties = new Properties();

  // Temporary working directory
  String workingDirectory = "";

  //====================================================================
  // Constructors
  //====================================================================

  /**
   * Creates a command shell.
   * Delegates properties to System.getProperties().
   */
  public CommandShell() throws InterpreterException {
	super( new ParserFromPreprocessor(new StatementDetector()),
		null, System.getProperties() );
	init();
  }

  /**
   * Constructor with input and output stream redirection.
   * Delegates properties to System.getProperties().
   */
  public CommandShell (InputStream in, OutputStream out, OutputStream err)
			throws InterpreterException {
	super( new ParserFromPreprocessor(new StatementDetector()),
		null, System.getProperties() );
	init();
	getLogger().redirect(in, out, err);
  }

  /**
   * Constructor with delegated environment and properties,
   * and input and output stream redirection.
   */
  public CommandShell (IEnvironment delegateEnvironment,
		Properties defaultProperties,
		InputStream in, OutputStream out, OutputStream err)
			throws InterpreterException {
	super( new ParserFromPreprocessor(new StatementDetector()),
		delegateEnvironment, defaultProperties);
	init();
	getLogger().redirect(in, out, err);
  }

  //====================================================================
  // Initialization.
  //====================================================================

  /**
   * Constructor initialization.
   * This occurs before setters in dependency injection.
   * Initializes the meta-interpreter
   * with a default logger.
   */
  void init () throws InterpreterException {
    try {
	// Set logger
  	setLogger(LoggerFactory.getLogger(System.getProperties()));

	// Set prompt
	setPrompt(prompt);
	setPartialPrompt(partialPrompt);

    } catch (Exception e) {
	throw filterException(e);
    }
  }

  /**
   * Spring initialization.
   * Initialize interpreter after setters in dependency injection.
   * Intended to be last setter in Spring dependency injection. 
   * <P>
   * The recommended usage scenario is as follows.
   * <PRE>
   *	main() { shell = createSpringBean(); // Spring calls setInit last
   *		 shell.runInterpreter(); }
   * </PRE>
   * @param dummy Ignored dummy parameter for Spring invocation.
   */
  public void setInit (String dummy) throws InterpreterException {
  }

  //====================================================================
  // Interpretive loop steps: normalize, filter, transform, execute.
  //====================================================================

  public Object execute (String transformedSource) {
	return null;
  }

  //====================================================================
  // Shortcuts for Transform parse cache accessors.
  //====================================================================
  /**
   * Shortcut to current interpreter getParser().getParserCache().getParseTree.
   */
  public static Document getParseTree(String statement) {
  	return getCurrentInterpreter().getParser().getParserCache().getParseTree(statement, null);
  }

  /**
   * Shortcut to current interpreter getParser().getParserCache().getLiteral.
   */
  public static String getLiteral(String key) {
  	return getCurrentInterpreter().getParser().getParserCache().getLiteral(key);
  }

  //====================================================================
  // Setters for dependency injection.
  //====================================================================

  /**
   * Sets the license text.
   */
  public void setLicense (String license) {
	if (license == null) { return; };
	this.license = license;
  }

  /**
   * Gets the license text.
   */
  public String getLicense () {
	return license.trim();
  }

  /**
   * Sets the credits text.
   */
  public void setCredits (String credits) {
	if (credits == null) { return; };
	this.credits = credits;
  }

  /**
   * Gets the credits text.
   */
  public String getCredits () {
	return credits.trim();
  }

  /**
   * Sets the header displayed at startup.
   */
  public void setHeader (String header) {
	if (header == null) { return; };
	this.header = header;
  }

  /**
   * Gets the header displayed at startup.
   */
  public String getHeader () {
	return header.trim();
  }

  /**
   * Sets the trailer displayed at shutdown.
   */
  public void setTrailer (String trailer) {
	if (trailer == null) { return; };
	this.trailer = trailer;
  }

  /**
   * Gets the trailer displayed at shutdown.
   */
  public String getTrailer () {
	return trailer.trim();
  }

  /**
   * Sets the help text.
   */
  public void setHelp (String help) {
	if (help == null) { return; };
	this.help = help;
  }

  /**
   * Gets the help text.
   */
  public String getHelp () {
	return help.trim();
  }

  /**
   * Sets the usage message.
   */
  public void setUsage (String usage) {
	if (usage == null) { return; };
	this.usage = usage;
  }

  /**
   * Gets the usage message.
   */
  public String getUsage () {
	return usage;
  }

  /**
   * Sets the Java preface for translating to Java.
   */
  public void setJavaPreface (String preface) {
	if (preface == null) { return; };
	this.javaPreface = preface;
  }

  /**
   * Gets the Java preface.
   */
  public String getJavaPreface () {
	return javaPreface;
  }

  /**
   * Sets the Linux script to run when compiling.
   * Intended to be invoked last, as Java tail recursion.
   */
  public void setAppLinux (String commandText) {
	if (commandText == null) { return; };
	this.appLinux = commandText;
  }

  /**
   * Gets the Linux script to run when compiling.
   */
  public String getAppLinux () {
	return appLinux;
  }

  /**
   * Sets the Windows script to run when compiling.
   * Intended to be invoked last, as Java tail recursion.
   */
  public void setAppWindows (String commandText) {
	if (commandText == null) { return; };
	this.appWindows = commandText;
  }

  /**
   * Gets the Windows script to run when compiling.
   */
  public String getAppWindows () {
	return appWindows;
  }

  /**
   * Sets if should use cmd files to compile and extract.
   * Default is off.
   */
  public void setUseCmdFile (boolean onoff) {
	this.useCmdFile = onoff;
  }

  /**
   * Gets if should use cmd files to compile and extract.
   */
  public boolean getUseCmdFile () {
	return useCmdFile;
  }

  //====================================================================
  // System startup scripts.
  //====================================================================

  /**
   * Gets system startup scripts, as text. 
   */
  public List<String> getStartupScripts () {
	return startupScripts;
  }

  /**
   * Sets system startup scripts, as text. 
   */
  public void setStartupScripts (List<String> scripts) {
	if (scripts == null) { return; };
	startupScripts = scripts;
  }

  /**
   * Gets system startup script names.
   */
  public List<String> getStartupScriptNames () {
	return startupScriptNames;
  }

  /**
   * Sets system startup script names.
   */
  public void setStartupScriptNames (List<String> scriptnames) {
	if (scriptnames == null) { return; }
	startupScriptNames = scriptnames;
  }

  /**
   * Adds a system startup script, as text.
   * A null entry indicates input from stdin.
   */
  public void setAddStartupScript (String script) {
	startupScripts.add(script);
  }

  /**
   * Sets the filename for the last system startup script added, for context.
   */
  public void setAddStartupScriptName (String scriptname) {
	startupScriptNames.add(scriptname);
  }

  //====================================================================
  // Input scripts, from command line.
  //====================================================================

  /**
   * Gets input scripts, as text. 
   */
  public List<String> getInputScripts () {
	return inputScripts;
  }

  /**
   * Sets input scripts, as text. 
   */
  public void setInputScripts (List<String> scripts) {
	if (scripts == null) { return; };
	inputScripts = scripts;
  }

  /**
   * Gets input script names.
   */
  public List<String> getInputScriptNames () {
	return inputScriptNames;
  }

  /**
   * Sets input script names.
   */
  public void setInputScriptNames (List<String> scriptnames) {
	if (scriptnames == null) { return; }
	inputScriptNames = scriptnames;
  }

  /**
   * Adds an input script, as text.
   * A null entry indicates input from stdin.
   */
  public void setAddInputScript (String script) {
	inputScripts.add(script);
  }

  /**
   * Sets the filename for the last input script added, for context.
   */
  public void setAddInputScriptName (String scriptname) {
	inputScriptNames.add(scriptname);
  }

  //====================================================================
  // Command line options : code generation options
  //====================================================================

  public void setIsToJava (boolean isToJava) {
	this.isToJava = isToJava;
  }

  public boolean getIsToJava () {
	return isToJava;
  }

  public void setUseLambdaExpressions (boolean useLambdas) {
	this.useLambdaExpressions = useLambdas;
  }

  public boolean getUseLambdaExpressions () {
	return useLambdaExpressions;
  }

  /**
   * Define method as closure, Groovy only.
   * Instead of defining plain method, and adding a method reference to it
   * of the same name.
   */
  public void setMethodAsClosure (boolean asClosure) {
	this.methodAsClosure = asClosure;
  }

  public boolean getMethodAsClosure () {
	return methodAsClosure;
  }

  //====================================================================
  // Command line options : interactive interpreter options
  //====================================================================

  /**
   * Sets if ignore system startup script.
   */
  public void setIgnoreSystemStartup (boolean onoff) {
	this.ignoreSystemStartup = onoff;
  }

  /**
   * Gets if ignore system startup script.
   */
  public boolean getIgnoreSystemStartup () {
	return ignoreSystemStartup;
  }

  /**
   * Sets if apply verbose, trace, and echo settings to command-line scripts.
   */
  public void setEchoStartup (boolean onoff) {
	this.echoStartup = onoff;
  }

  /**
   * Gets if apply verbose, trace, and echo settings to command-line scripts.
   */
  public boolean getEchoStartup () {
	return echoStartup;
  }

  /**
   * Sets if apply verbose, trace, and echo settings to the system startup scripts.
   */
  public void setEchoSystemStartup (boolean onoff) {
	this.echoSystemStartup = onoff;
  }

  /**
   * Gets if apply verbose, trace, and echo settings to the system startup scripts.
   */
  public boolean getEchoSystemStartup () {
	return echoSystemStartup;
  }

  /**
   * Sets if exit script on error.
   */
  public void setExitOnScriptError (boolean onoff) {
	this.exitOnScriptError = onoff;
  }

  /**
   * Gets if exit script on error.
   */
  public boolean getExitOnScriptError () {
	return exitOnScriptError;
  }

  /**
   * Sets if evaluate aggregated input scripts instead of line by line.
   * System startup scripts are never aggregated.
   */
  public void setIsAggregateScripts (boolean onoff) {
	this.isAggregateScripts = onoff;
  }

  /**
   * Gets if evaluate aggregated input scripts instead of line by line.
   */
  public boolean getIsAggregateScripts () {
	return isAggregateScripts;
  }

  /**
   * Sets the temporary working directory.
   */
  public void setWorkingDirectory (String path) {
	if (path == null) { return; };
	this.workingDirectory = path;
  }

  /**
   * Gets the temporary working directory.
   */
  public String getWorkingDirectory () {
	return workingDirectory;
  }

  //====================================================================
  // Shell prefix, to prepend to commands.
  //====================================================================

  /**
   * Sets the command prefix,
   * which is prepended to "/command" to yield "prefix.command()".
   * Commands are of the form:
   * <PRE>
   *	/id.id...		=> prefix.id...()
   *	/id.id... expr ...	=> prefix.id...(expr,...)
   * </PRE>
   * which are translated into method calls, typically prefixed
   * with a shell class that is statically imported.
   * Commands are also of the form:
   * <PRE>
   *	id.id... expr ...	=> id...(expr,...)
   * </PRE>
   * that allows a style of method invocation that omits parenthesis.
   */
  public void setCommandPrefix (String commandPrefix) {
	if (commandPrefix == null) { commandPrefix = ""; }
	this.commandPrefix = commandPrefix;
  }

  /**
   * Gets the command prefix,
   * which is prepended to "/command" to yield "prefix.command()".
   */
  public String getCommandPrefix () {
	return commandPrefix;
  }

  /**
   * Use Java syntax.
   * Use = for assign, == for compare, var for variable declarations,
   *	and def for method definitions.
   */
  public void setIsJavaSyntax () {
	String setup = "$defineFixed = :=\n$defineFixed == =\n$define var local\n$define def method";
	startupScripts.add(setup);
	startupScriptNames.add("setJavaSyntax");
  }

  //====================================================================
  // Main.
  //====================================================================

  /**
   * Main method to run the interpreter.
   * Spring first creates the shell and its dependencies,
   * then processes command line arguments
   * to override Spring configuration file settings.
   * The Spring configuration filename is taken from the
   * "junicon.spring.config" system property if it is set,
   * in which case a file system application context is created.
   * Otherwise Spring configuration is from the classpath
   * using "config/startup/spring_config.xml".
   * @param args	command line arguments, will override Spring settings.
   */
  public static void main (String[] args)
  {
      try {

	//====
	// Preprocess command-line arguments
	//	to see if Spring uses classPathContext or fileSystemContext.
	//====
	String configFile = preprocessMainArgs(args);

	//====
	// Create shell and process command line arguments.
	//====
	CommandShell shell = createSpringBean(configFile); // new CommandShell()
	if (shell == null) { return; }
	
	//====
	// Run shell
	//====
	boolean invalidArgs = shell.processCommandLineArgs(args);
	if (! invalidArgs) {
	    if (shell.shouldRunInterpreter()) {
		shell.initInterpreter();
		shell.runInterpreter();
	    }
	    if (shell.shouldRunCmdFile()) {	// Java tail recursion
		shell.runCmdFile(args);
	    }
	}

	//====
        // Exit the shell process
	//====
        System.exit(0);

    } catch (IllegalArgumentException e) {
    } catch (Throwable e) {
	AbstractShell.printException(e, defaultLogger,
		cmdLinesDebug ||
		defaultLogger.getIsDebug().contains(Trace.ON));
    }
  }

  //====================================================================
  // Create shell from Spring configuration.
  //====================================================================
  /**
   * Create shell from Spring configuraton.
   * Spring will create a CommandShell,
   * but will not run the shell or processCommandLineArgs.
   * Spring configuration is from the classpath,
   * or from the file system if configFile is non-null.
   * If configFile is null or empty, it defaults to
   * "config/startup/spring_config.xml" and uses classPathContext.
   */
  private static CommandShell createSpringBean (String configFile) {
	CommandShell shell = null;
	boolean isClassPathContext = false;

	// Invoke Spring to create shell, but do not runInterpreter yet.
	try {
	  if ((configFile == null) || configFile.isEmpty()) {
		isClassPathContext = true;
		configFile = "config/startup/spring_config.xml";
	  }
	  String[] configLocations = { configFile };
	  BeanFactory context;
	  if (isClassPathContext) {
        	context = new ClassPathXmlApplicationContext(configLocations);
	  } else {
		context = new FileSystemXmlApplicationContext(configLocations);
	  }
	  // Get bean from Spring.
	  shell = (CommandShell) context.getBean("CommandShell");
	} catch (Exception e) {
		defaultLogger.error("Error in starting Junicon.");
		defaultLogger.error("Error in loading Spring configuration file.");
		// e.printStackTrace();
		defaultLogger.error(e.toString());
		return null;
	}
	return shell;
  }
        
  //==========================================================================
  // Run bat or sh command file, if needed for compile or extract.
  //==========================================================================

  /**
   * Only run interpreter if not compile or extract, or needs to translate.
   */
  private boolean shouldRunInterpreter () {
	return ((! (performCompile || performExtract)) || performTranslate);
  }

  /**
   * Only run cmd file if compiling or extracting,
   * requested to use cmd files, and not already in cmd file.
   */
  private boolean shouldRunCmdFile () {
	if (! useCmdFile) { return false; }
	String isInCmdFile = System.getenv("JARRUNNER_IN_CMD_FILE");
	if ((isInCmdFile != null) && (! isInCmdFile.isEmpty())){
		return false;
	}
	return (performCompile || performExtract);
  }

  /**
   * Run bat command file, if compiling under windows (Java tail recursion).
   * Run sh command file, if compiling under Linux.
   * Sees if isWindows from environment variable JARRRUNER_ISWINDOWS.
   * Does not add this executable jar filename (JARRUNNER_ARG0) as args[0].
   */
  private int runCmdFile (String[] args) {
	boolean isWindows = false;
	String jarrunner_isWindows = System.getenv("JARRUNNER_ISWINDOWS");
	if ((jarrunner_isWindows != null) && (! jarrunner_isWindows.isEmpty())){
		isWindows = true;
	}
	String jarrunner_arg0 = System.getenv("JARRUNNER_ARG0");
	int retcode;
	if (isWindows) {
	    retcode = BatRunner.runCommand(getAppWindows(),
			"junicon", ".bat", null, args);
	} else {
	    retcode = BatRunner.runCommand(getAppLinux(),
			"junicon", "", null, args);
	}
	return retcode;
  }

  //====================================================================
  // Process command line arguments
  //====================================================================

  /**
   * Preprocess command-line arguments
   * to see if Spring uses classPathContext or fileSystemContext.
   * Returns the configDirectory if have "-Ic configDirectory",
   * indicating Spring should choose fileSystemContext.
   */
  private static String preprocessMainArgs (String[] args) {
	// Set option defaults
	String configFile = null;

	// Process "-" prefixed command line arguments
	if (args == null) { args = new String[0]; }
	boolean invalidArgs = false;
	int i = 0;
	String arg = "";
	while ((! invalidArgs) && (i < args.length) && (args[i] != null) &&
			args[i].startsWith("-")) {
	    arg = args[i++];
	    switch (arg) {
		case "-Ic": if ((i < args.length) && (args[i] != null)) {
				configFile = args[i++];
			   } else { invalidArgs = true; };
			   break;
		case "-m": // Other two-word options
		case "-D":
		case "-c": 
		case "-f":
		case "-o": if ((i < args.length) && (args[i] != null)) {
				  arg = args[i++];
			   } else { invalidArgs = true; };
			   break;
		default: break;
	    }
	}
	return configFile;
  }

  /**
   * Process command line arguments.
   */
  private boolean processCommandLineArgs (String[] args)
		throws InterpreterException {

	// Set option defaults
	setIsInterpretive(true);	// Default
	setIsInteractive(false);	// Default
	resetLanguage();	// Point getLogger().trace to defaultLanguage

	// Process "-" prefixed command line arguments
	if (args == null) { args = new String[0]; }
	boolean hasStdin = false;
	boolean printHelp = false;
	boolean invalidArgs = false;
	int i = 0;
	String arg = "";
	while ((! invalidArgs) && (i < args.length) && (args[i] != null) &&
			args[i].startsWith("-")) {
	    arg = args[i++];
	    switch (arg) {
		//====
		// Interpret options
		//====
		case "-h": printHelp = true;
				break;
		case "-i": setIsInteractive(true);
				break;
		case "-ni": setIsInterpretive(false);
				break;

		case "-n": setDoNotExecute(true);
				break;
		case "-np": setDoNotPreprocess(true);
				break;
		case "-nd": setDoNotDetect(true);
				break;
		case "-nt": setDoNotTransform(true);
				break;
		case "-N": setJustNormalize(true);
				break;
		case "-na": setIsAggregateScripts(false);
				break;
		case "--groovy": setIsToJava(false);
				break;
		case "--java": setIsToJava(true);
				break;
		case "--manifest": if ((i < args.length) && (args[i] != null)) {
				manifestFilename = args[i++];
			   } else { invalidArgs = true; };
			   break;
		case "-J":
		case "--java-syntax": setIsJavaSyntax();
				break;
		case "--no-precision":
				IconNumber.setIsIntegerPrecision(false);
				IconNumber.setIsRealPrecision(false);
				break;
		case "--java-origin": IconNumber.setIndexOrigin(0);
				break;
		case "-v": getInheritVerbose().add(Trace.ON);
				break;
		case "-vd": getIsVerbose().add(Trace.PREPROCESS);
				getInheritVerbose().add(Trace.DETAIL);
				break;
		case "-dt": getLogger().getIsTrace().add(Trace.ON);
				break;
		case "-dtp": getLogger().getIsTrace().add(Trace.PARSE);
				break;
		case "-dtn": getLogger().getIsTrace().add(Trace.NORMALIZE);
				break;
		case "-dtt": getLogger().getIsTrace().add(Trace.TRANSFORM);
				break;
		case "-dtd": getLogger().getIsTrace().add(Trace.DETAIL);
				break;
		case "-x": setEcho(true);
				break;
		case "-vf": setEchoStartup(true);
				break;
		case "-vs": setEchoSystemStartup(true);
				break;

		case "-d": getLogger().getIsDebug().add(Trace.ON);
				cmdLinesDebug = true;
				break;
		case "-dd": getLogger().getIsDebug().add(Trace.DETAIL);
				cmdLinesDebug = true;
				break;

		case "-F": setIgnoreSystemStartup(true);
				break;
		case "-k": setStopScriptOnError(true);
				break;
		case "-K": setExitOnScriptError(true);
				break;

		case "-D": if ((i < args.length) && (args[i] != null)) {
				String directive = asDefineDirective(args[i++]);
				if (directive != null) {
					addInputCommand(directive, "define");
				}
			   } else { invalidArgs = true; };
			   break;

		case "-o": if ((i < args.length) && (args[i] != null)) {
				outFilename = args[i++];
			   } else { invalidArgs = true; };
			   break;
		case "-c": if ((i < args.length) && (args[i] != null)) {
				  addInputScript(args[i++], "command");
			   } else { invalidArgs = true; };
			   break;
		case "-f": if ((i < args.length) && (args[i] != null)) {
				  arg = args[i++];
				  try {
					addInputScript(fileToString(arg), arg);
				  } catch (IOException e) {
					throw new InterpreterException(e);
				  }
			    } else { invalidArgs = true; };
			    break;
		case "-": hasStdin = true;
				// addInputScript(null, "stdin");
				break;

		//====
		// Translate options
		//====
		case "-T": performTranslate = true;
				setIsToJava(true);
				break;
		case "-G": performTranslate = true;
				setIsToJava(false);
				break;
		case "-E": // Preprocess only
				setDoNotTransform(true);
				setDoNotExecute(true);
				getInheritVerbose().add(Trace.ON);
				setEchoStartup(true);
				setIsInterpretive(false);
				break;

		//====
		// PreprocessMain options
		//====
		case "-Ic": if ((i < args.length) && (args[i] != null)) {
				arg = args[i++];
			   } else { invalidArgs = true; };
			   break;

		//====
		// Compile options
		//====
		case "-e":
		case "-el":
		case "-ew":
		case "-C":
		case "-R": performCompile = true;
				break;

		//====
		// Extract options
		//====
		case "-Xc":
		case "-Xr": performExtract = true;
				break;

		default: invalidArgs = true;
				break;
	    }
	}

	//====
	// Process remaining arguments, first nonOption is scriptName
	//====
	if ((! invalidArgs) && (i < args.length) && (args[i] != null)) {
		// Use first nonOption arg as script name
		lastScriptName = args[i++];
		try {
			addInputScript(fileToString(lastScriptName), lastScriptName);
		} catch (IOException e) {
			throw new InterpreterException(e);
		}

		// Pass remaining arguments to script
		if (i < args.length) {
			mainArgs = Arrays.copyOfRange(args, i, args.length);
		}
	}

	//====
	// Mutually exclusive options
	//====
	if (performTranslate) {		// Mutually exclusive options
		performCompile = false;
		performExtract = false;
	}
	if (performCompile) {
		performExtract = false;
	}
	if (performTranslate || performCompile || performExtract) {
		setIsInterpretive(false);	// performInterpret
		setIsInteractive(false);
		setDoNotExecute(true);
	}

	//====
	// Invalid options ?
	//====
	if (invalidArgs) {
		getLogger().error("Invalid option " + (arg==null?"":arg));
	}
	if ((performTranslate || performCompile) && (inputScripts.isEmpty() ||
			inputScriptNames.isEmpty())) {
		invalidArgs = true;
	}
	if (invalidArgs || printHelp) {
		getLogger().error(usage);
		invalidArgs = true;
		return invalidArgs;
	}

	//====
	// Handle performCompile and performExtract
	//====
	String className = "";
	String extension = "";
	if (performTranslate || performCompile) {
		String tail = JarRunner.getFilenameTail(
			inputScriptNames.get(inputScriptNames.size() - 1));
		className = JarRunner.getFilenameWithoutExtension(tail);
		extension = JarRunner.getFilenameExtension(tail);
	}
	// if performCompile && not .java, must translate
	if (performCompile) {
		if (! extension.equals(".java")) {
			performTranslate = true;
			setIsToJava(true);
		}
	}

	//====
	// Handle translate to Java or Groovy.
	//====
	if (performTranslate) {
		// Java translate: junicon    -j -ni -n -v -vc -o "$code" $*:q
		// Groovy translate: junicon -vs -ni -n -v -vc -o "$code" $*:q
		String packageName = "";
		if (! getIsToJava()) {		// Groovy
			setEchoSystemStartup(true);
		} else {
			//====
			// Prepend Java preface to first script, after package
			//====
			// Java preface.  Inserted after package statement
			//	when translating to Java.
			//====
			String toTranslate = inputScripts.get(0);
			Matcher matcher = Pattern.compile(
			   "(package\\s*([^;\\s]*)\\s*;)").matcher(toTranslate);
			if (matcher.find()) {
			    packageName = matcher.group(2);
			    toTranslate = matcher.replaceFirst(
				"$1" + "\n" + getJavaPreface() + "\n");
			} else {
			    toTranslate = getJavaPreface() + "\n" + toTranslate;
			}
			inputScripts.set(0,toTranslate);

			// Create manifest addition with main class entrypoint
			// Main-Class: packageName.className <CR>
			String manifestAddition = "Main-Class: ";
			if (! packageName.isEmpty()) {
				manifestAddition += packageName + ".";
			}
			manifestAddition += className + "\n\n";
			try {
			  FileCopy.stringToFile(manifestAddition,
				manifestFilename);
			} catch (IOException e) {
				throw new InterpreterException(e);
			}
		}
		// setIsInterpretive(false);
		// setIsInteractive(false);
		// setDoNotExecute(true);
		getInheritVerbose().add(Trace.ON);
		setEchoStartup(true);

		// Derive output filename from last script name
		if ((outFilename == null) || outFilename.isEmpty()) {
		    outFilename = className;
		    if (getIsToJava()) {
			outFilename += ".java";
		    } else {
			outFilename += ".groovy";
		    }
		}
	}

	//====
	// After nonoption script, if interpretive, add command to invoke main
	//====
	if (getIsInterpretive() && (lastScriptName != null) &&
			(! lastScriptName.isEmpty())) {
		// Add command: lastScriptName.main("arg",...);
		String tail = JarRunner.getFilenameTail(lastScriptName);
		String classname = JarRunner.getFilenameWithoutExtension(tail);
		String command = classname + ".main(";
		boolean isFirst = true;
		for (String scriptArg : mainArgs) {
			if (scriptArg == null) continue;
			if (! isFirst) { command += ","; };
			command += "\"" + scriptArg + "\"";
			isFirst = false;
		}
		command += ");";
		addInputScript(command, "main");
	}

	//====
	// Add stdin as default input
	//====
	if (getIsInterpretive()) {
	    if (inputScripts.isEmpty() && (! hasStdin)) {
		setIsInteractive(true);
	    }
	    if (getIsInteractive()) {
		hasStdin = true;
	    }
	    if (hasStdin) {
		addInputScript(null, "stdin");
	    }
	}

	return invalidArgs;
  }

  //====================================================================
  // Run shell.
  //====================================================================

  /**
   * Initialize interpreter after setters in dependency injection.
   * Creates default dispatched interpreter if one does not exist.
   * Sets default properties to System.Properties,
   * no delegate environment, and adds built in commands.
   */
  public void initInterpreter () throws InterpreterException {
	// Create default interpreter if one does not exist
	if (getDispatchChildren().isEmpty()) {
		IInterpreter interp = new TransformInterpreter();
		interp.setName("junicon");
		setAddDispatchChild(interp);
		setDefaultDispatchInterpreter(interp);
	}
  }

  /**
   * Run interpreter, if outer interactive shell.
   * Intended to be called after Spring bean creation and setInit.
   * <P>
   * Executes system startup scripts, user startup scripts,
   * and input files and/or stdin.
   */
  public void runInterpreter () throws InterpreterException {
      try {
	//====
	// Open output and error files
	//====
	if ((outFilename != null) && (! outFilename.isEmpty())) {
	    try {
		getLogger().setOut( new PrintStream( new BufferedOutputStream(
			new FileOutputStream(outFilename))));
	    } catch (IOException ioe) {
		throw new ExecuteException(
			"File not found: " + outFilename, ioe);
	    }
	}

	//====
	// Set properties used by transforms: isInterpretive, isToJava, useLambdas
	//====
	if (getIsToJava()) {
		setProperty("isToJava", "true");
	}
	if (useLambdaExpressions) {
		setProperty("useLambdaExpressions", "true");
	}
	if (methodAsClosure) {
		setProperty("methodAsClosure", "true");
	}

	//====
	// Display header if interactive and have stdin
	//====
	setProperty("echo", (new Boolean(getEcho())).toString());
	if (getIsInteractive()) {
		displayHeader();
	}

	//====
	// Process system startup scripts
	//====
	if (! ignoreSystemStartup) {
		executeSystemStartup();
	}

	//====
	// Process input from filenames or stdin (default or -)
	//====
	for (int pos=0; pos < inputScripts.size(); pos++) {
		String script = inputScripts.get(pos);
		String scriptName = "default";
		if (pos < inputScriptNames.size()) {
			scriptName = inputScriptNames.get(pos);
		}
		// Don't aggregate stdin
		runScript(script, scriptName, echoStartup,
			(script != null) && isAggregateScripts);
	}

      } catch (InterpreterException e) {
	throw e;
      } finally {
	getLogger().getOut().close();	// in case output file
	getLogger().getErr().close();	// in case output file
	if (getIsInteractive()) {
		displayTrailer();
	}
      }
  }

  //====================================================================
  // Run scripts.
  //====================================================================

  /**
   * Execute system startup file.
   */
  public void executeSystemStartup() throws InterpreterException {
	// Load startup command files
	for (int pos=0; pos < startupScripts.size(); pos++) {
		String script = startupScripts.get(pos);
		String scriptName = startupScriptNames.get(pos);
		if (script != null) {
			runScript(script, scriptName, echoSystemStartup, false);
		}
	};
  }

  /**
   * Run script.
   */
  private void runScript (String script, String scriptName,
			boolean echoStartup, boolean isAggregate)
			throws InterpreterException {
		resetLanguage();
		pushLanguage();

		// Input from stdin
		InputStream input = getLogger().getIn();
		boolean doPrompt = getIsInteractive();
		boolean doStopOnError = 
			getIsInteractive() ? false : getStopScriptOnError();

		// Input from file
		if (script != null) {
		    doPrompt = false;
		    doStopOnError = getStopScriptOnError();
		    input = new ByteArrayInputStream(script.getBytes());
		};

		// Set filename property for transforms
		if ((script != null) && (scriptName != null)
				&& (! scriptName.isEmpty())) {
			setProperty("filename", scriptName);
		}

		// Execute script
		try {
			if ((script != null) && (! echoStartup)) {
				resetVerboseSettings();
			};
			ILineContext context = new LineContext(
				scriptName, null);
			execFile(input, context, doPrompt, doStopOnError,
				isAggregate);
		} catch (InterpreterException e) {
			if (exitOnScriptError) { throw e; };
		} finally {
			resetLanguage();
			setProperty("filename", "");
		}
  }

  //====================================================================
  // Command line utility methods.
  //====================================================================

  /**
   * Add synthetic input command that does not count as a script.
   */
  private void addInputScript (String script, String name) {
	inputScripts.add(script);
	inputScriptNames.add(name);
  }

  /**
   * Add synthetic input command that does not count as a script.
   */
  private void addInputCommand (String script, String name) {
	inputScripts.add(script);
	inputScriptNames.add(name);
  }

  /** 
   * Read file into string.
   */
  private String fileToString (String filename) throws IOException {
	return FileCopy.fileToString(filename);
  }

  /**
   * Translate "property=value" into preprocessor directive:
   * $define property value.
   * The value may be omitted.
   */
  private String asDefineDirective (String directive) {
	if ((directive == null) || directive.isEmpty()) { return null; }
	String[] split = directive.split("=",2);
	if ((split[0]).trim().isEmpty()) { return null; }
	if (split.length < 2) { return "$define " + split[0]; }
	return "$define " + split[0] + " " + split[1];
  }

  //====================================================================
  // Read user property file, set property definitions from -D option.
  //====================================================================

  /**
   * Set property definitions from command-line -D options.
   */
  private void setPropertiesFromDOption (List<String> propertyDefinitions) {
	if (propertyDefinitions == null) { return; };
	for (String val : propertyDefinitions) {
		if (val == null) { continue; };
		String prop[] = val.split("=",2);
		if ((prop.length < 2) || (prop[0] == null) ||
				(prop[1] == null)) {
			continue;
		};
		System.setProperty(prop[0], prop[1]);	
	};
  }

  //====================================================================
  // Handle transform directives.
  //====================================================================

  /**
   * Handle a transform directive given by a scoped annotation.
   * If a line, outside of any statement, starts with
   *	{@literal @}&lt;script lang="..."&gt;
   * any contents up to an ending
   *	{@literal @}&lt;/script&gt;
   * tag are directly passed to the substrate interpreter
   * without preprocessing or transformation.
   * If inside another statement, such a script annotation 
   * will be turned into an annotated block quote
   *	{@literal @}&lt;script&gt;{&lt; code &gt;}
   * and then later transformed from to
   *	IconInvokeIterator(()->{code}).
   * <P>
   * Otherwise, if a line in a file starts with
   *	{@literal @}&lt;tag attr="..."&gt;
   * (or without the quotes),
   * the given interpreter property name.attr is set from that value,
   * and an ending
   *	{@literal @}&lt;/name.attr&gt;
   * tag resets the property to its previous value.
   * <P>
   * Commented scoped annotations of the form
   *	#{@literal @}&lt;tag attr="..."&gt;
   * or
   *	#{@literal @}&lt;/tag&gt;
   * are treated in the same way as scoped annotations.
   * <P>
   * We currently do not allow nested transform directives.
   * @return non-null text to continue processing,
   *	or null if directive was consumed.
   */
  public String handleDirective (String line) throws InterpreterException {
	if (line == null) { return null; }
	line = line.trim();

	// Start script, inside or outside statement
	if (line.startsWith("@<script>") || line.startsWith("@<script lang=")
			|| line.startsWith("#@<script>")
			|| line.startsWith("#@<script lang=")) {
	    boolean isIcon = line.contains("icon");
	    if (currentLanguage.isInsideAtScript) {
	      if (isIcon == currentLanguage.isIcon) {    // Same, do nothing
		currentLanguage.sameAsParent ++;
		return null;
	      } else {
		currentLanguage.isNested = true;
	      }
	    }
	    currentLanguage.atBeginOfScript = true;
	    return null;	// throw away line, do not process further

	// End script
	} else if (line.startsWith("@</script>")
			|| line.startsWith("#@</script>")) {
	    if (currentLanguage.sameAsParent > 0) {	// Ignore
		currentLanguage.sameAsParent --;
		return null;
	    }
	    currentLanguage.atEndOfScript = true;
	    return null;

	// Reset property
	} else if (line.startsWith("@</")
			|| line.startsWith("#@</")) {
		TransformSupport catalog =
			(TransformSupport) getTransformSupport();
		Catalog.resetAnnotation(line, getProperties(), savedProperties,
			(catalog == null ? null : catalog.getCatalog()), null);
		return null;

	// Set property
	} else if (line.startsWith("@<")
			|| line.startsWith("#@<")) {
		// Parse: @<name attr="value" OR @<name attr=value
		TransformSupport catalog =
			(TransformSupport) getTransformSupport();
		Catalog.addAnnotation(line, getProperties(), savedProperties,
			(catalog == null ? null : catalog.getCatalog()), null);
		return null;
	}

	return null;	// throw away line, do not process further
  }

  public String handleBeginOfScript (String annotation) {
	    boolean isIcon = annotation.contains("icon");
	    currentLanguage.isIcon = isIcon;

	    if (currentLanguage.isIcon) {
		setDoNotPreprocess(false);	// inherited into substrate
		setDoNotTransform(false);
		if (currentLanguage.isEmbedded) {
			setDoNotExecute(true);
			setIsInterpretive(true);
		}
	    } else {
		setDoNotPreprocess(true);	// inherited into substrate
		setDoNotTransform(true);
		if (currentLanguage.isEmbedded) {
			setDoNotExecute(true);
			setIsInterpretive(false);
		}
	    }
	    return null;
  }

  public String handleEndOfScript (String text) {
    if (currentLanguage.isEmbedded) {
	if (currentLanguage.isIcon) {
		currentLanguage.evalAtEndOfScript = true;
		return text;
	} else {	// Emit @<script> {< code >}
		return "@<script> {< " + text + ">}";
	}
    } else {
	return text;
    }
  }

  //====================================================================
  // Built-in commands.
  //====================================================================

  /**
   * Display the Meta-Interpreter Header information.
   */
  private void displayHeader() {
	getLogger().println(getHeader());
	getLogger().println();
	getLogger().flush();
  } 
	
  /**
   * Display the Meta-Interpreter Trailer information.
   */
  private void displayTrailer() {
	getLogger().println(getTrailer());
	getLogger().flush();
  }

  /**
   * Display the Meta-Interpreter Help information.
   */
  private void displayHelp() {
	getLogger().println(getHelp());
	getLogger().flush();
  }

  /**
   * Display the Meta-Interpreter Credits.
   */
  private void displayCredits() {
	getLogger().println(credits);
	getLogger().flush();
  }

  /**
   * Display the Meta-Interpreter License information.
   */
  private void displayLicense() {
	getLogger().println(getLicense());
	getLogger().flush();
  }

  public String echo (String str) {
  	return str;
  }

}

//==== END OF FILE
