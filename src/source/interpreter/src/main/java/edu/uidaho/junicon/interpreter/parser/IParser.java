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
package edu.uidaho.junicon.interpreter.parser;

import edu.uidaho.junicon.grammars.common.*;
import edu.uidaho.junicon.interpreter.interpreter.IInterpreter;
import edu.uidaho.junicon.runtime.util.ILogger;

import java.util.List;
import org.w3c.dom.Document;		// For DOM Processing

/**
 * Interface that parsers, preprocessors, and statement detectors
 * must satisfy.
 * A statement detector, or stateful metaparser, first
 * extracts parseable units from a buffered history of input lines,
 * handling statements that may span multiple lines.
 * The parser then operates on the complete statements.
 * <br>
 * The parser can also take another parser as a preprocessor,
 * thus forming a chain of parsers.
 * This is a simpler alternative to forming a chain of transformational
 * interpreters that only do parsing.
 *
 * @author Peter Mills
 */
public interface IParser extends IPreprocessor {

  //======================================================================
  // Setters for dependency injection.
  //======================================================================

  /** 
   * Sets the name of this parser.
   */
  public void setName (String name);

  /**
   * Returns the name of this parser.
   */
  public String getName ();

  /** 
   * Sets the type of this parser.
   */
  public void setType (String type);

  /**
   * Returns the type of this parser.
   */
  public String getType ();

  /** 
   * Sets the parent interpreter of this parser.
   */
  public void setParent (IInterpreter parent);

  /**
   * Gets the parent interpreter.
   */
  public IInterpreter getParent ();

  /**
   * Sets the preprocessor for this parser.
   * The preprocessor is recursively invoked by getParseUnit()
   * and addTextToParse().
   * The preprocessor should preserve lines, i.e., it can fuse lines
   * together but not break apart lines into smaller statements.
   */
  public void setPreprocessor (IParser preprocessor);

  /**
   * Gets the preprocessor for this parser.
   */
  public IParser getPreprocessor ();

  /**
   * Sets if should not preprocess statements.
   */
  public void setDoNotPreprocess (boolean on);

  /**
   * Gets if should not preprocess statements.
   */
  public boolean getDoNotPreprocess ();

  /**
   * Sets the parser cache.
   */
  public void setParserCache (IParserCache cache);

  /**
   * Returns the parser cache.
   */
  public IParserCache getParserCache ();

  /**
   * Enable the ungetParse operation.
   */
  public void setEnableUngetParse (boolean isEnabled);

  /**
   * Gets if ungetParse is enabled.
   */
  public boolean getEnableUngetParse ();

  //========================================================================
  // Parser stack.
  //========================================================================
  /**
   * Clones parser state.
   * Returns placeholder with saved state.
   */
  public IParser saveState ();

  /**
   * Restores parser state from given placeholder,
   * but keeps configuration settings.
   */
  public void restoreState (IParser state);

  //======================================================================
  // Parse methods.
  //======================================================================
  /**
   * Obtains a parsible unit from the history of lines in the input stream.
   * If a preprocessor has been set, this is used to
   * first process the input line using its getParseUnit.
   * The result is added to a history of preprocessed input lines.
   * The next parsible unit, i.e. a complete statement,
   * is then obtained from the preprocessed input history.
   * @param	inputLine	a line of source input,
   *				without the line separator
   *				(e.g., carriage return),
   *				to add to the input line history.
   * @param	context		input line context, passed to the
   *				preprocessor and carried through
   *				into the input history.
   * @return	<code>parse unit</code>, if the input line history has
   *				a parsible unit;
   * 		<code>null</code> otherwise.
   */
  public String getParseUnit (String inputLine, ILineContext context)
		throws ParseException;

  /**
   * Gets context of parse unit returned by last call to getParseUnit,
   * or null if no parse unit returned on last call.
   */
  public ILineContext getParseUnitContext ();

  /**
   * Parses the source input 
   * for the target sub-interpreter language grammar.
   * The input must be a parseable complete statement.
   * @param	sourceInput	parsible unit of source input
   * @param	context		input line context
   * @return	XML Abstract Syntax Tree (XML-AST),
   *		or null if sourceInput is null.
   */
  public Document parse (String sourceInput, ILineContext context)
	throws ParseException;

  /**
   * Returns last input to getParseUnit().
   */
  public String getLastParseUnitInput ();

  /**
   * Returns last input to parse().
   */
  public String getLastParseInput ();

  /**
   * Sets last input to parse().
   */
  public void setLastParseInput (String input);

  /**
   * Filter parser exception (e.g., Javacc)
   * into ParseException with extracted line, column, and source code.
   */
  public ParseException filterParseException (Throwable cause, String source);

  /**
   * Put back the last parse unit, 
   * i.e., return the last parser state.
   */
  public IParser ungetLastParseUnit ();

}

//==== END OF FILE
