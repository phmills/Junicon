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
package edu.uidaho.junicon.grammars.common;

import edu.uidaho.junicon.runtime.util.ILogger;

import java.util.List;
import java.text.ParseException;

/**
 * Interface that preprocessors and statement detectors must satisfy.
 * A preprocessor or statement detector is a stateful metaparser
 * that extracts parseable units from a buffered history of input lines,
 * handling statements that may span multiple lines.
 * The parser then operates on the complete statements.
 * A preprocessor is thus essentially a stream.
 * <P>
 * A statement detector pre-parses input according to a meta-grammar.
 * This meta-grammar is the grammar common to sub-interpreter languages,
 * and captures notions of structural grouping necessary to
 * decide what forms a parseable complete statement for the sub-interpreters.
 * <br>
 * The meta-grammar
 * consists of tokens with hierarchical grouping delimited by punctuation
 * including {e;e} [e,e] (e,e) e:e e.e  
 * as well as quotes and comments.
 * <br>
 * Literals include single and double quotes, single and multi-line comments,
 * and big literals (big quotes) {< >}.
 * Quotes but not comments are respected inside literals.
 * Quotes and comments are preserved inside big literals,
 * to allow arbitrary text inside them.
 * All literal delimiters can be escaped using \.
 * Newlines are preserved, except within quotes and big
 * literals where they are treated as spaces unless escaped, and thus
 * multicharacter grouping delimiters cannot be split across lines.
 *
 * @author Peter Mills
 */
public interface IPreprocessor {

  //======================================================================
  // Setters for dependency injection.
  //======================================================================

  /** 
   * Sets the invoker of this parser, if this is a line parser.
   */
  public void setParentParser (IPreprocessor invoker);

  /**
   * Gets the invoker of this parser, if this is a line parser.
   */
  public IPreprocessor getParentParser ();

  /**
   * Sets line separator in source code returned by getParseUnit().
   * The line separator is used when joining
   * input spanning multiple lines in the statement detector.
   */
  public void setLineSeparator (String lineSeparator);

  /**
   * Gets line separator in source code returned by getParseUnit().
   */
  public String getLineSeparator ();

  //========================================================================
  // Parser stack.
  //========================================================================
  /**
   * Clones preprocessor state.
   * Returns placeholder with saved state.
   */
  public IPreprocessor saveState ();

  /**
   * Restores preprocessor state from given placeholder,
   * but keeps configuration settings.
   */
  public void restoreState (IPreprocessor state);

  //======================================================================
  // Parse methods.
  //======================================================================

  /**
   * Adds text to the buffered history of input lines for parsing.
   * @param	inputLines	source input, possibly with multiple lines,
   *				to add to the input line history.
   * @param	context		starting line context
   *				-- each line within the input 
   *				has its line number offset from this.
   */
  public void addTextToParse (String inputLines, ILineContext context);

  /**
   * Adds a line to the buffered history of input lines for parsing.
   * @param	inputLine	line to add to input history.
   * @param	context		line context.
   */
  public void addLineToParse (String inputLine, ILineContext context);

  /**
   * Gets next line to parse from input history.
   * Removes line from input history.
   */
  public String getNextLineToParse ();

  /**
   * Gets context of line returned by last call to getNextLineToParse,
   * or null if no line returned on last call.
   */
  public ILineContext getNextLineContext ();

  /**
   * Sees if have a next line in the input history.
   */
  public boolean hasNextLineToParse ();

  /** 
   * Gets the next complete parse unit from 
   * a single line of preprocessed input.
   * The parse units are derived from the history of partial or incomplete
   * statements observed so far, appended with the input line.
   * The default behavior is to return the original input.
   * <P>
   * The preprocessor may add lines back into the input history,
   * for example from include directives.
   * @return found statement, or null if no found statements.
   */
  public ILineContext getParseUnitFromLine (String inputLine,
		ILineContext context) throws ParseException;

  /**
   * Returns true if the input so far to getParseUnitFromLine
   * does not form a complete parseable statement.
   * Complete statements have matching group delimiters "{}" "()" "[]"
   * and are terminated by a semicolon ";".
   */
  public boolean isPartialStatement ();

  /**
   * Returns true if the input so far to getParseUnitFromLine
   * has matching group delimiters, but need not be
   * terminted by a semicolon ";".
   */
  public boolean isClosedStatement ();

  /**
   * Returns partial statement so far.
   */
  public String getPartialStatement ();

  /**
   * Resets the parser state.
   * If a preprocessor has been set, this is also reset.
   */
  public void resetParser ();

  /**
   * Returns parent logger, or new logger if null parent.
   */
  public ILogger getLogger ();

  /**
   * Wraps exception as a java.text.ParseException.
   */
  public ParseException asParseException (String message);

  /**
   * Wraps exception as a java.text.ParseException.
   */
  public ParseException asParseException (Throwable cause);

  /**
   * Wraps exception as a java.text.ParseException.
   */
  public ParseException asParseException (String message, Throwable cause);

}

//==== END OF FILE
