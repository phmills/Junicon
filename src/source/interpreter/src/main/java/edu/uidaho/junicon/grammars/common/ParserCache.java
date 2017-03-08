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

import java.util.Map;
import java.util.HashMap;
import org.w3c.dom.Document;		// For DOM Processing

/**
 * Implementas a literal and parse-tree cache for parsed statements.
 */
public class ParserCache implements IParserCache {

  private Map<String,String> literalCache = new HashMap<String,String>();
  private Map<String,Document> parseTreeCache = new HashMap<String,Document>();
  private int literalKey = 0;

  /**
   * Constructor for literal cache only.
   */
  public ParserCache () {
  }

  public String putLiteral (String value) {
	if (value == null) { return null; };
	String key = literalCache.get(value);
	if (key == null) {
		key = String.valueOf(literalKey++);
		literalCache.put(key,value);
	};
	return key;
  }

  public String getLiteral (String key) {
	return literalCache.get(key);
  }

  public void clearLiterals () {
	literalCache.clear();
	literalKey = 0;
  }

  public Document getParseTree (String statement, ILineContext context) {
	if (statement == null) { return null; };
	Document parseTree = parseTreeCache.get(statement);
	return parseTree;
  }

  public void clearParseTrees () {
	parseTreeCache.clear();
  }

}

//==== END OF FILE
