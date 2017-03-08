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
package edu.uidaho.junicon.grammars.common;

import edu.uidaho.junicon.runtime.util.ILogger;
import edu.uidaho.junicon.runtime.util.ILogger.Trace;
import edu.uidaho.junicon.runtime.util.LoggerFactory;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * AbstractPreprocessor forms the basis for a preprocessor that
 * filters input lines.
 *
 * @author Peter Mills
 */
public class AbstractPreprocessor implements IPreprocessor {

  private IPreprocessor parentParser = null;
  private String source_lineSeparator = "\n";

  private Pattern splitPattern = Pattern.compile("\\n|\\r|\\r\\n");
  private ILogger logger = LoggerFactory.getLogger();

  // Parser state
  private ILineContext lastInputContext = null;
  protected LinkedList<ILineContext> bufferedSource =
		new LinkedList<ILineContext>();
		// History of buffered input lines, with context

  //===================================================================
  // Constructors.
  //===================================================================
  /**
   * No-arg constructor.
   */
  public AbstractPreprocessor () {
	resetParserState();
  }

  //===================================================================
  // Setters for dependency injection.
  //===================================================================

  public void setParentParser (IPreprocessor invoker) {
	this.parentParser = invoker;
  }

  public IPreprocessor getParentParser () {
	return parentParser;
  }

  public void setLineSeparator (String lineSeparator) {
	this.source_lineSeparator = lineSeparator;
  }

  public String getLineSeparator () {
	return source_lineSeparator;
  }

  //========================================================================
  // Parser stack.
  //========================================================================
  protected void copyState (AbstractPreprocessor from,
		AbstractPreprocessor to, boolean move) {
	if ((from == null) || (to == null)) { return; }
	to.bufferedSource = from.bufferedSource;
	to.lastInputContext = from.lastInputContext;
	if (move) {
		from.bufferedSource = new LinkedList<ILineContext>();
	}
  }

  public IPreprocessor saveState () {
	AbstractPreprocessor saved = new AbstractPreprocessor();
	copyState(this, saved, true);
	return saved;
  }

  public void restoreState (IPreprocessor state) {
	if (state == null) { return; }
	copyState((AbstractPreprocessor) state, this, false);
  }

  //===================================================================
  // Parse methods.
  //===================================================================
  public void addTextToParse (String inputLines, ILineContext context) {
	if (inputLines == null) { return; };
	String[] split = splitPattern.split(inputLines, -1); // keep empty lines
	if (split == null) { return; };
	int offset = 0;
	for (String line : split) {
	    bufferedSource.add( new LineContext (context, line, offset));
	    offset ++;
	}
  }

  public void addLineToParse (String inputLine, ILineContext context) {
	if (inputLine == null) { return; };
	bufferedSource.add( new LineContext (context, inputLine));
  }

  public String getNextLineToParse () {
	lastInputContext = null;
	if (bufferedSource.isEmpty()) { return null; };
	lastInputContext = bufferedSource.removeFirst();
	return lastInputContext.getText();
  }

  public ILineContext getNextLineContext () {
	return lastInputContext;
  }

  public boolean hasNextLineToParse () {
	return (! bufferedSource.isEmpty());
  }

  public ILineContext getParseUnitFromLine (String inputLine,
		ILineContext context) throws ParseException {
	if (inputLine == null) { return null; };
	return new LineContext(context, inputLine);
  }

  public boolean isPartialStatement () {
	return false;
  }

  public boolean isClosedStatement () {
	return true;
  }

  public String getPartialStatement () {
	return "";
  }

  public void resetParser () {
	resetParserState();
  }

  private void resetParserState () {
	bufferedSource.clear();
	lastInputContext = null;
  }

  public ILogger getLogger () {
	if (parentParser == null) {
		return logger;
	}
	return parentParser.getLogger();
  }

  public ParseException asParseException (String message) {
	if (message == null) { message = ""; }
	return new ParseException(message, 0);
  }

  public ParseException asParseException (Throwable cause) {
	String message = "";
	if (cause != null) { message = cause.getMessage(); }
	ParseException e = new ParseException(message, 0);
	if (cause != null) { e.initCause(cause); }
	return e;
  }

  public ParseException asParseException (String message, Throwable cause) {
	if (message == null) {
		if (cause != null) { message = cause.getMessage();
		} else { message = ""; }
	}
	ParseException e = new ParseException(message, 0);
	if (cause != null) { e.initCause(cause); }
	return e;
  }

}

//==== END OF FILE
