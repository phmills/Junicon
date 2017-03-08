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
import edu.uidaho.junicon.grammars.document.DocumentHandler;

import edu.uidaho.junicon.runtime.util.ILogger;
import edu.uidaho.junicon.runtime.util.LoggerFactory;

import java.io.InputStream;
import java.io.FileInputStream;
import org.w3c.dom.Document;	// For DOM building

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Outer parser wrapper for generated parser.
 *
 * @author Peter Mills
 */
public class ParserFromGrammar extends AbstractParser implements IParser {

  private IGrammarParser parserWrapper = null;
  private String emptyStr = "";

  //===================================================
  // Constructors.
  //===================================================

  /**
   * Constructs a Parser
   * with a new literal and parseTree cache.
   */
  public ParserFromGrammar () {
	super();
  }

  //===================================================
  // Setters for dependency injection.
  //===================================================

  public void setParserWrapper (IGrammarParser parserWrapper) {
	if (parserWrapper == null) { return; };
	this.parserWrapper = parserWrapper;
	parserWrapper.setParserCache(getParserCache());
  }

  public IGrammarParser getParserWrapper () {
	return parserWrapper;
  }

  public void setParserCache (IParserCache parserCache) {
	super.setParserCache(parserCache);
	if (parserWrapper == null) { return; };
	parserWrapper.setParserCache(parserCache);
  }

  public void setInputStream (InputStream stream) {
	if (parserWrapper == null) { return; };
	parserWrapper.redirectInput(stream);
  }

  public void setContext (ILineContext context) {
	if (parserWrapper == null) { return; };
	parserWrapper.setContext(context);
  }

  //========================================================================
  // Parser stack.
  //========================================================================
  protected void copyState (ParserFromGrammar from,
		ParserFromGrammar to, boolean move) {
	if ((from == null) || (to == null)) { return; }
	super.copyState(from, to, move);
  }

  public IParser saveState () {
	ParserFromGrammar saved = new ParserFromGrammar();
	copyState(this, saved, true);
	if (preprocessor != null) {
		saved.preprocessor = preprocessor.saveState();
	}
	return saved;
  }

  public void restoreState (IParser state) {
	if (state == null) { return; }
	copyState((ParserFromGrammar) state, this, false);
	if (preprocessor != null) {
		preprocessor.restoreState(state.getPreprocessor());
	}
  }

  //===================================================
  // Parse methods.
  //===================================================

  /**
   * Parse a string.
   */
  public Document parse (String sourceInput, ILineContext context)
		throws ParseException {
	super.parse(sourceInput, context);	// setLastParseInput
	if (parserWrapper == null) { return null; };
	Document document = null;
	try {
		parserWrapper.setContext(context);
		parserWrapper.redirectInput(sourceInput);
		document = parserWrapper.parseInput();
	} catch (Throwable e) {
		throw filterParseException(e, sourceInput);
	};
	return document;
  }

  /**
   * Parse an input stream.
   * Must first set input stream and context.
   */
  public Document parse () throws ParseException {
	super.parse(null, null);	// setLastParseInput
	if (parserWrapper == null) { return null; };
	Document document = null;
	try {
		document = parserWrapper.parseInput();
	} catch (Throwable e) {
		throw filterParseException(e, null);
	};
	return document;
  }

  /**
   * Parse and print from file in args[0], with default System.in.
   */
  public static void main (String args[]) throws Exception {
	ParserFromGrammar parser;
	InputStream input;
	ILogger logger = LoggerFactory.getLogger();

	try {
	// Get input file.
		input = System.in;
		if ((args != null) && (args.length > 0)) {
			input = new FileInputStream(args[0]);
		};

		//====
		// WILL NOT WORK ANYMORE,
		// needs dependency injection of GeneratedParser.
		//====
		// Parser parser = new Parser();
		//====

	// Create parser using Spring.
	try {
	  String configFile = System.getProperty("junicon.spring.config","");
	  String[] serviceResources = { configFile };
	  BeanFactory context = new
	    	FileSystemXmlApplicationContext(serviceResources);
        	// ClassPathXmlApplicationContext(serviceResources);

	  // Get bean from Spring.
	  parser = (ParserFromGrammar) context.getBean("Parser");
	} catch (Exception e) {
		logger.error("Error in starting Junicon.");
		logger.error("Error in loading Spring configuration file.");
		// e.printStackTrace();
		logger.error(e.toString());
		return;
	}

	// Run parser.
	parser.setInputStream(input);
	Document document = parser.parse();
	DocumentHandler.print_document(document);

	} catch (Exception e) {
		// e.printStackTrace(System.err);
		logger.error(e);
	}
  }

}

//==== END OF FILE
