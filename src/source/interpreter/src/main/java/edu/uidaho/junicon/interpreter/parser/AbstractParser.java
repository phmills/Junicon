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
import edu.uidaho.junicon.interpreter.interpreter.ExecuteException;
import edu.uidaho.junicon.interpreter.interpreter.InterpreterException;
import edu.uidaho.junicon.runtime.util.ILogger;
import edu.uidaho.junicon.runtime.util.ILogger.Trace;
import edu.uidaho.junicon.runtime.util.LoggerFactory;

// import java.util.EnumSet;
import java.util.*;
import java.io.InputStream;

import org.w3c.dom.Document;		// For DOM Processing

/**
 * AbstractParser encapsulates common features of parsers including
 * buffering input lines to parse and
 * caching literals.
 *
 * @author Peter Mills
 */
public class AbstractParser extends AbstractPreprocessor
		implements IParser, Cloneable {

  private String name = "";
  private String type = "";
  private IInterpreter parent = null;		// Parent interpreter

  private IParserCache cache = new ParserCache();
  private String lastParseUnitInput = null;
  private String lastParseInput = null;
  private ILineContext lastParseUnitContext = null;
  IParser preprocessor = null;
  private boolean doNotPreprocess = false;
  private AbstractParser lastState = null;
  private boolean isEnabledUngetParse = false;

  //===================================================================
  // Constructors.
  //===================================================================
  /**
   * Constructs an AbstractParser
   * with a new literal and parseTree cache.
   */
  public AbstractParser () {
	super();
	resetParserState();
  }

  /**
   * Constructs an AbstractParser
   * with a given literal and parseTree cache.
   */
  public AbstractParser (IParserCache parserCache) {
	super();
	resetParserState();
	setParserCache(parserCache);
  }

  //===================================================================
  // Setters for dependency injection.
  //===================================================================

  public String getName () {
	return name;
  }

  public void setName (String name) {
	this.name = name;
  }

  public String getType () {
	return type;
  }

  public void setType (String type) {
	this.type = type;
  }

  public void setParent (IInterpreter parent) {
	this.parent = parent;
  }

  public IInterpreter getParent () {
	return parent;
  }

  public void setPreprocessor (IParser preprocessor) {
	this.preprocessor = preprocessor;
  }

  public IParser getPreprocessor () {
	return preprocessor;
  }

  public void setDoNotPreprocess (boolean on) {
	this.doNotPreprocess = on;
  }

  public boolean getDoNotPreprocess () {
	return doNotPreprocess;
  }

  public void setParserCache (IParserCache cache) {
	if (cache == null) { return; }
	this.cache = cache;
  }

  public IParserCache getParserCache () {
	return cache;
  }

  public void setEnableUngetParse (boolean isEnabled) {
	this.isEnabledUngetParse = isEnabled;
  }

  public boolean getEnableUngetParse () {
	return isEnabledUngetParse;
  }

  //========================================================================
  // Parser stack.
  //========================================================================
  protected void copyState (AbstractParser from,
		AbstractParser to, boolean move) {
	if ((from == null) || (to == null)) { return; }
	to.lastParseUnitInput = from.lastParseUnitInput;
	to.lastParseInput = from.lastParseInput;
	to.lastParseUnitContext = from.lastParseUnitContext;
	to.lastState = from.lastState;
	to.cache = from.cache;

	if (move) {
		from.cache = new ParserCache();
	}
  }

  public IParser saveState () {
	AbstractParser saved = new AbstractParser();
	copyState(this, saved, true);
	if (preprocessor != null) {
		saved.preprocessor = preprocessor.saveState();
	}
	return saved;
  }

  public void restoreState (IParser state) {
	if (state == null) { return; }
	copyState((AbstractParser) state, this, false);
	if (preprocessor != null) {
		preprocessor.restoreState(state.getPreprocessor());
	}
  }

  //===================================================================
  // Parse methods.
  //===================================================================

  public final String getParseUnit (String inputLine, ILineContext context)
		throws ParseException {
	lastParseUnitInput = inputLine;
	lastParseUnitContext = null;
	ILineContext found = null;

	if (isEnabledUngetParse) {
	  try {
		lastState = (AbstractParser) this.clone();
	  } catch (CloneNotSupportedException e) {
		throw filterParseException(e, inputLine);
	  }
	}

	getLogger().traceDetail("Entering parser " 
		+ getName() + ": " + ((inputLine == null)?"":inputLine));

	// Invoke preprocessor
	if ((preprocessor != null) && (! doNotPreprocess)) {
	    try {
		inputLine = preprocessor.getParseUnit(inputLine, context);
		context = preprocessor.getParseUnitContext();
	    } catch (Throwable e) {
		throw filterParseException(e, inputLine);
	    };
	}

	// Add input line to input history
	if (inputLine != null) { addLineToParse(inputLine, context); };

	// Find statements in buffered input history
	while ((found == null) && hasNextLineToParse()) {
		String nextline = getNextLineToParse();
		try {
		    found = getParseUnitFromLine(nextline,
			getNextLineContext());
		} catch (Throwable e) {
		    throw filterParseException(e, nextline);
		}
	}

	if (found == null) {
		getLogger().traceDetail("Exiting parser " + getName() + ": null");
		return null;
	}

	lastParseUnitContext = found;
	String result = found.getText();
	getLogger().traceDetail("Exiting parser " + getName()
		+ ": " + ((result == null)?"":result));
	return result;
  }

  public ILineContext getParseUnitContext () {
	return lastParseUnitContext;
  }

  /**
   * Parse the source input.
   * For AbstractShell this method only
   * sets lastParseInput and returns the original input.
   */
  public Document parse (String sourceInput, ILineContext context)
		throws ParseException {
	lastParseInput = sourceInput;
	return null;
  }

  public String getLastParseUnitInput () {
	return lastParseUnitInput;
  }

  public String getLastParseInput () {
	return lastParseInput;
  }

  public void setLastParseInput (String input) {
	this.lastParseInput = input;
  }

  public void resetParser () {
	super.resetParser();
	if (preprocessor != null) {
		preprocessor.resetParser();
	}
	resetParserState();
  }

  /**
   * Resets the parser state, and clears its caches.
   * <br>
   * Does not affect getParseUnit() as it is idempotent.
   */
  private void resetParserState () {
	lastParseUnitInput = null;
	lastParseInput = null;
	lastParseUnitContext = null;
	lastState = null;
	cache.clearLiterals();
	cache.clearParseTrees();
  }

  public IParser ungetLastParseUnit () {
	return lastState;
  }

  public ParseException filterParseException (Throwable cause, String source) {
	if (cause == null) { return null; };
	if (cause instanceof ParseException) {
		ParseException ie = (ParseException) cause;
		if (ie.getHaveFilteredParse()) { return ie; };
	};

	int line = -1;
	int col = -1;
	String message = cause.getMessage();
	String lineKey = "at line ";
	String colKey = "column ";
	if (message != null) { 
		int lineStart = message.indexOf(lineKey) + lineKey.length();
		int lineEnd = message.indexOf(",", lineStart);
		if (lineEnd >= lineStart) {
			try {
				line = Integer.parseInt(
					message.substring(lineStart, lineEnd));
			} catch (NumberFormatException e) {
				line = -1;
			};
			int colStart = message.indexOf(colKey) + colKey.length();
			int colEnd = message.indexOf(".", colStart);
			if (colEnd >= colStart) {
				try {
					col = Integer.parseInt(
						message.substring(colStart, colEnd));
				} catch (NumberFormatException e) {
					col = -1;
				}
			};
		};
	};

	if (cause instanceof InterpreterException) {
		return (ParseException) new ParseException(message, cause.getCause()).setLine(line).setColumn(col).setSource(source).setHaveFilteredParse(true);
	}
	return (ParseException) new ParseException(cause).setLine(line).setColumn(col).setSource(source).setHaveFilteredParse(true);
  }

  /**
   *	Clone the AbstractParser object.
   */
  protected Object clone () throws CloneNotSupportedException {

	AbstractParser qp = null;
	try {
		qp = (AbstractParser) super.clone();
		if (bufferedSource != null) {		// deep copy
		    qp.bufferedSource = new LinkedList<ILineContext>(bufferedSource);
		};
	} catch (CloneNotSupportedException e) {
		//====
		// throw new ExecuteException(e);
		//====
	}
	return qp;
  }

  /**
   * Returns parent logger, or new logger if null parent.
   */ 
  public ILogger getLogger () {
	if (parent == null) {
		return super.getLogger();
	}
	return parent.getLogger();
  }

}

//==== END OF FILE
