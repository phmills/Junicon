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

import edu.uidaho.junicon.grammars.common.ILineContext;

import org.w3c.dom.Document;		// For DOM Processing

/**
 * Provides literal and parse-tree caches for parsed statements.
 * <br>
 * Provides a map from string keys to parsed literals for later use
 * by the interpreter.
 * <br>
 * Provides a map from statements to parse trees for use in
 * the interpreter transform to hold parsed function templates.
 */
public interface IParserCache {

  /**
   * Adds the parsed literal to the literal cache.
   * @return <code>generated key</code> for the parsed literal.
   * The same key is returned for equal literals.
   */
  public String putLiteral (String value);

  /**
   * Gets the parsed literal for the specified key in the literal cache.
   * @return <code>literal</code> or null if no key found.
   */
  public String getLiteral (String key);

  /**
   * Resets the literal cache to empty, and initializes the generated key to 0.
   */
  public void clearLiterals ();

  /**
   * Gets the parse tree for the specified statement from the parseTree cache.
   * @return <code>parse tree</code> for statement.
   */
  public Document getParseTree (String statement, ILineContext context);

  /**
   * Resets the parseTree cache to empty.
   */
  public void clearParseTrees ();

}

//==== END OF FILE
