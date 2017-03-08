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
package edu.uidaho.junicon.interpreter.parser;

import edu.uidaho.junicon.grammars.common.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Outer parser wrapper for a line parser
 * such as a preprocessor or statement detector.
 *
 * @author Peter Mills
 */
public class ParserFromPreprocessor extends AbstractParser implements IParser {

  private IPreprocessor parserWrapper = null;

  //===================================================
  // Constructors.
  //===================================================

  /**
   * No-arg constructor.
   */
  public ParserFromPreprocessor () {
	super();
  }

  /**
   * Construct parser from given preprocessor.
   */
  public ParserFromPreprocessor (IPreprocessor preprocessor) {
	super();
	setParserWrapper(preprocessor);
  }

  //===================================================
  // Setters for dependency injection.
  //===================================================

  public void setParserWrapper (IPreprocessor parserWrapper) {
	if (parserWrapper == null) { return; };
	this.parserWrapper = parserWrapper;
	parserWrapper.setParentParser(this);
  }

  public IPreprocessor getParserWrapper () {
	return parserWrapper;
  }

  //========================================================================
  // Parser stack.
  //========================================================================
  protected void copyState (ParserFromPreprocessor from,
		ParserFromPreprocessor to, boolean move) {
	if ((from == null) || (to == null)) { return; }
	super.copyState(from, to, move);
  }

  public IParser saveState () {
	ParserFromPreprocessor saved = new ParserFromPreprocessor();
	copyState(this, saved, true);
	if (preprocessor != null) {
		saved.preprocessor = preprocessor.saveState();
	}
	if (parserWrapper != null) {
		saved.parserWrapper = parserWrapper.saveState();
	}
	return saved;
  }

  public void restoreState (IParser state) {
	if (state == null) { return; }
	copyState((ParserFromPreprocessor) state, this, false);
	if (preprocessor != null) {
		preprocessor.restoreState(state.getPreprocessor());
	}
	if (parserWrapper != null) {
		parserWrapper.restoreState(((ParserFromPreprocessor) state).parserWrapper);
	}
  }

  //===================================================
  // Delegate parse methods.
  //===================================================

  public ILineContext getParseUnitFromLine (String inputLine,
		ILineContext context) throws java.text.ParseException {
	if (parserWrapper == null) {
		return new LineContext(context, inputLine);
	}
	return parserWrapper.getParseUnitFromLine(inputLine, context);
  }

  public boolean isPartialStatement () {
	if (parserWrapper == null) { return false; }
	return parserWrapper.isPartialStatement();
  }

  public boolean isClosedStatement () {
	if (parserWrapper == null) { return true; }
	return parserWrapper.isClosedStatement();
  }

  public String getPartialStatement () {
	if (parserWrapper == null) { return ""; }
	return parserWrapper.getPartialStatement();
  }

  public void setLineSeparator (String lineSeparator) {
	if (parserWrapper == null) { return; }
	parserWrapper.setLineSeparator(lineSeparator);
  }

  public String getLineSeparator () {
	if (parserWrapper == null) { return "\n"; }
	return parserWrapper.getLineSeparator();
  }

  public void resetParser () {
	super.resetParser();
	if (parserWrapper != null) {
		parserWrapper.resetParser();
	}
  }

  //======================================================================
  // Main
  //======================================================================
  /**
   * Parse and print from file in args[0], with default System.in.
   */
  public static void main (String args[]) throws Exception {
	ParserFromPreprocessor quickparse = new ParserFromPreprocessor(
			new StatementDetector());
	try {
		BufferedReader br = 
			new BufferedReader (new InputStreamReader(System.in));
		String line = null;
		while (null != (line = br.readLine())) {
			String parsed = quickparse.getParseUnit(line, null);
			if (parsed != null) {
				System.out.println(parsed);
			};
		};
        } catch (ParseException e) {
           e.printStackTrace(System.err);
        } catch (IOException e) {
           e.printStackTrace(System.err);
        };
  }

}

//==== END OF FILE
