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
import edu.uidaho.junicon.interpreter.parser.IParser;
import edu.uidaho.junicon.interpreter.parser.ParseException;
import edu.uidaho.junicon.support.transforms.IThreadResolver;
import edu.uidaho.junicon.runtime.util.*;

import java.util.Map;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import org.w3c.dom.Document;		// For DOM building

/**
 * Interface for transformational interpreters
 * that parse, transform, and then execute statements
 * on a scripting substrate.
 * The steps involved in transformational interpretation are to
 * handle any directives, preprocess each line of input,
 * accumulate a complete statement and then parse it,
 * decorate the parse tree, normalize it, 
 * test to filter it out for export, transform it, 
 * decide to whom to delegate it, and then either
 * execute the transformed line of input using
 * a scripting engine substrate
 * or dispatch it to another sub-interpreter for further transformation.
 * <br>
 * The interface is common to and satisfied by both the
 * outer interactive command shell and its sub-interpreters that map 
 * from sub-languages such as Unicon onto
 * specific substrates such as Groovy via cross-translation.
 * The command shell, or meta-interpreter, has the added responsibility
 * to identify parsable units and their target language from lines of input,
 * filter them, and then dispatch them to the appropriate sub-interpreter.
 * <br>
 * Each transformational interpreter thus has a notion of
 * sub-interpreters as well as a substrate, onto one of which 
 * it may dispatch the transformed input for execution.
 * Substrates differ from sub-interpreters in that they are implementation
 * targets such as Groovy that need not obey the
 * transformational interpreter interface.
 * <br>
 * The equational specification of the steps involved in
 * transformational interpretation is given below in eval() and
 * evalLine().
 *
 * @author Peter Mills
 */
public interface IInterpreter extends IInterpreterContext,
	IInterpreterSetters, IDispatcher, IEnvironment, IPropertiesExtender,
	IThreadResolver {

  //======================================================================
  // Setters for Initialization.
  //======================================================================

  /**
   * Initialize interpeter, after setters in dependency injection.
   * Intended to be last setter in Spring dependency injection. 
   * The recommended usage scenario is as follows.
   * <PRE>
   *	main() { shell = Spring_bean_creation(XML lastly calls setInit);
   *		 shell.setRunInterpreter(); }
   * </PRE>
   * @param dummy Ignored dummy parameter for Spring invocation.
   */
  public void setInit (String dummy) throws InterpreterException;

  /**
   * Run interpreter, if outer interactive shell.
   * Intended to be called after Spring bean creation and setInit.
   * @param dummy Ignored dummy parameter for Spring invocation.
   */
  public void setRunInterpreter (String dummy) throws InterpreterException;

  //======================================================================
  // Interpretive loop.
  //======================================================================

  /**
   * Test if an input line is a directive
   * given by a scoped annotation of the form:
   * &#64;&lt;tag attr=x attr'=y ... &gt; ... &#64;&lt;/tag&gt;,
   * or just &#64;&lt;tag attr=x ... /&gt;.
   * Directives are scoped annotations on a single line,
   * and bypass all parsers if handled.
   * <P>
   * Scoped annotations are a hybrid of XML and Java annotations.
   * Java style attributes such as
   * &#64;&lt;tag (key=value, ...)&gt; 
   * or
   * &#64;&lt;tag (value)&gt;
   * can also be used, instead of XML style attributes.
   * One can override this method so that scoped annotations
   * might alternatively follow a different syntax such as @command(args).
   * <P>
   * Commented scoped annotations that begin with #@&lt;
   * are also allowed, and are treated the same way as scoped annotations.
   */
   public boolean isDirective (String line);

  /**
   * Handle a directive.
   * By default this method returns the input line unchanged.
   * It can be overriden for other behavior.
   * For example, if a line in a file starts with
   * &#64;&lt;script lang="..."&gt;,
   * any file contents up to an ending
   * &#64;&lt;/script&gt; tag
   * are directly passed to the substrate interpreter,
   * without preprocessing or transformation.
   * @return non-null text to continue processing,
   *		or null if directive was consumed.
   */
   public String handleDirective (String line) throws InterpreterException;

  /**
   * Handle beginning of script.
   * Invoked by handleDirective.
   */
   public String handleBeginOfScript (String annotation);

  /**
   * Handle end of script.
   * Invoked by handleDirective.
   */
   public String handleEndOfScript (String line);

  /**
   * Obtains a parsable unit from the history of lines in the input stream.
   * If a preprocessor has been set, this is used to
   * first process the input line using its getParseUnit.
   * The result is added to a history of preprocessed input lines.
   * The next parsable unit, i.e. a complete statement,
   * is then obtained from the preprocessed input history.
   * @param	inputLine	a line of source input,
   *				without the line separator
   *				(e.g., carriage return),
   *				to add to the input line history.
   * @param	context		input line context, passed to the
   *				preprocessor and carried through
   *				into the input history.
   * @return	<code>parse unit</code>, if the input line history has
   *				a parsable unit;
   * 		<code>null</code> otherwise.
   */
  public String getParseUnit (String inputLine, ILineContext context)
		throws ParseException;

  /**
   * Gets context of parse unit returned by last call to getParseUnit,
   * or null if no parse unit returned on last call.
   */
  ILineContext getParseUnitContext ();

  /**
   * Returns true if input so far to getParseUnit
   * does not form a complete parsable statement.
   * Complete statements have matching group delimiters "{}" "()" "[]"
   * and are terminated by a semicolon ";".
   */
  public boolean isPartialStatement ();

  /**
   * Returns true if the input so far to getParseUnit
   * has matching group delimiters, but need not be
   * terminated by a semicolon ";".
   */
  public boolean isClosedStatement ();

  /**
   * Parses the source input for the target substrate.
   * The input must be a parsable complete statement.
   * @param	sourceInput	parsable unit of source input
   * @param	context		input line context
   * @return	XML Abstract Syntax Tree (XML-AST),
   *		or null if sourceInput is null.
   */
  public Document parse (String sourceInput, ILineContext context)
	throws ParseException;

  /**
   * Decorate the parse tree with additional syntactic or semantic information.
   * @return <code>parseTree</code> if inputSource or parseTree is null,
   *	or if there is no transform.
   */
  public Document decorate (String inputSource, Document parseTree,
	ILineContext context) throws InterpreterException;
  
  /**
   * Reset evaluation state.
   */
  public void resetTransformed ();

  /**
   * Reduce the parsed input to normal form.
   * @return <code>parseTree</code> if parseTree is null,
   *	or if there is no transform.
   */
  public Document normalize (Document parseTree, ILineContext context)
	throws InterpreterException;
  
  /**
   * Reduce the input to normal form.
   * @return <code>inputSource</code> if inputSource is null,
   *	or if there is no transform.
   */
  public String normalize (String inputSource, Document normalized,
	ILineContext context) throws InterpreterException;
  
  /**
   * Test a statement based on its AST and source,
   * and if needed take it out of the transform sequence
   * and separately handle it.
   * Filtering takes items out of the transform sequence, 
   * for example to export artifacts such as
   * classes, interfaces, or webservices.
   * @return <code>inputSource</code> if should continue to process 
   *		the transform stream,
   *		otherwise returns <code>null</code> if filtered it out.
   */
  public String filterOut (String inputSource, Document parseTree,
	ILineContext context) throws InterpreterException;
  
  /**
   * Perform the language-specific transformation from the input source
   * and/or the parse tree.
   * <br>
   * This will typically be a transform chain with several stages:
   * transforming the XML Abstract Syntax Tree (XML-AST) to 
   * another Document, and then
   * deconstructing and formatting it into the specific target language.
   * @return <code>inputSource</code> if inputSource or parseTree is null,
   *	or if there is no transform.
   */
  public String transform (String inputSource, Document parseTree,
	ILineContext context) throws InterpreterException;
  
  /**
   * Decide which sub-interpreter to delegate to.
   *
   * @return <code>null</code> if this interpreter should 
   *			handle the command.
   */
  public IInterpreter chooseDelegate (String transformedSource,
	ILineContext context);

  /**
   * Execute the transformed source code on a sub-interpreter or
   * directly on a substrate.
   * Sets currentInterpreter to this.
   * <br>
   * Equivalent to:
   * <PRE>
   *	if (null or this == chooseDelegate(source)) { executeOnSubstrate(source);
   *	} else { chooseDelegate(source).evalLine(source); };
   * </PRE>
   *
   * @param transformedSource
   *	representing the transformed input source code.
   *
   * @return <code>result</code> of evaluation.
   */
  public Object dispatch (String transformedSource, ILineContext context)
	throws InterpreterException;

  /**
   * Execute the transformed source code on the substrate, if it exists.
   *
   * @param transformedSource
   *	the transformed input source code.
   *
   * @return <code>result</code> of execution.
   */
  public Object executeOnSubstrate (String transformedSource,
	ILineContext context) throws InterpreterException;

  /**
   * Evaluate a parsable complete statement.
   * Synonymous with eval().
   * @return <code>result</code> of evaluation.
   * Returns null on null input.
   */
  public Object exec (String inputSource, ILineContext context)
	throws InterpreterException;

  /**
   * Evaluate a parsable complete statement or set of statements.
   * Evaluation does not use the preprocessor and statement detector.
   * Eval() is stateful, and synonymous with exec().
   * Sets currentInterpreter to this.
   * <br>
   * Equivalent to:
   * <PRE>
   *	let normalized = normalize (decorate (input, parse (input)));
   *	let normalizedText = normalize (input, normalized);
   *	if (null != filterOut (normalizedText, normalized)) {
   *		dispatch (transform (normalizedText, normalized)) };
   * </PRE>
   * where dispatch is equivalent to:
   * <PRE>
   *	if (null or this == chooseDelegate(source)) {
   *		executeOnSubstrate(source);
   *	} else { chooseDelegate(source).evalLine(source); };
   * </PRE>
   * @return <code>result</code> of evaluation.
   * Returns null on null input (i.e., is null-idempotent).
   */
  public Object eval (String inputSource, ILineContext context)
	throws InterpreterException;

  /**
   * Evaluate all parsable complete statements
   * found so far in the input line history.
   * Evaluation uses the preprocessor and statement detector.
   * Statements can thus span multiple lines and begin or end in mid-line.
   * The statement detector is stateful in looking for parsable
   * statements in the input passed to it.
   * <br>
   * Equivalent to:
   * <PRE>
   *	(eval (getParseUnit(isDirective(line)?handleDirective(line):line)))* 
   * </PRE>
   * where "*" means repeat until no more parseUnits returned.
   * @return <code>result</code> of evaluation.
   */
  public Object evalLine (String inputLine, ILineContext context)
	throws InterpreterException;

  /**
   * Line-by-line evaluation of the input stream,
   * extracting statements using the preprocessor and statement detector.
   * Repeatedly parse, normalize, filter, transform and execute
   * commands from the input stream.
   * <br>
   * Equivalent to:
   * <PRE>
   *	(displayPrompt; evalLine (readLine (instream)))*
   *			until exit() or eof().
   * </PRE>
   * A line is considered to be terminated by any one of a
   * line feed ('\n'), a carriage return ('\r'), or a carriage return followed
   * immediately by a linefeed.
   * Thus, works with either Unix or Windows text file formats for both input
   * script files and input from stdin.
   *
   * @param instream	input stream containing source code.
   * @param context	file and line context
   * @param promptFlag	print a prompt before each getting each line of input.
   * @param stopOnScriptError	stop processing input lines if an error occurs.
   * @param aggregateInput  evaluate the whole file at once, not line by line.
   */
  public void execFile (InputStream instream, ILineContext context,
	boolean promptFlag, boolean stopOnScriptError, boolean aggregateInput)
	throws InterpreterException;
  
  /**
   * Line-by-line evaluation of the input stream,
   * extracting statements using the preprocessor and statement detector.
   * <br>
   * Prints a prompt if promptflag, and does not stop on error.
   * Works with either Unix or Windows text file formats for both input
   * script files and input from stdin.
   */
  public void execFile (InputStream instream, ILineContext context,
	boolean promptflag) throws InterpreterException;
  
  /**
   * Filter exceptions in the execFile interpretive loop.
   * Default behavior is to print the error message and,
   * if getIsDebug(), printstackTrace.
   * @return filtered exception.
   */
  public InterpreterException filterException (Throwable cause);
  
  //====================================================================
  // Script execution using execFile.
  //====================================================================

  /**
   * Line-by-line evaluation of the input string,
   * extracting statements using the preprocessor and statement detector.
   * Uses execFile to load and run the shell script from a string.
   */
  public boolean execScript (String script, ILineContext context,
		boolean promptFlag, boolean stopOnScriptError)
		throws InterpreterException;

  /**
   * Line-by-line evaluation of the input file,
   * extracting statements using the preprocessor and statement detector.
   * Uses execFile to load and run the shell script from a file.
   */
  public boolean execScriptFile (String filename,
		boolean promptFlag, boolean stopOnScriptError)
		throws InterpreterException;

  //======================================================================
  // Interactive support.
  //======================================================================

  /**
   * Tells the interpreter to exit.
   */
  public void exit ();
  
  /**
   * Displays the command prompt.
   */
  public void displayPrompt ();

}

//==== END OF FILE
