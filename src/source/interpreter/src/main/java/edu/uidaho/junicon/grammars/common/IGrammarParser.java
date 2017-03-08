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
import edu.uidaho.junicon.grammars.common.IParserCache;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import org.w3c.dom.Document;		// For DOM building

/**
 * Utility wrapper around generated parsers.
 */
public interface IGrammarParser {

  //============================================================
  // Setters for dependency injection.
  //============================================================
  public void setParserCache (IParserCache parserCache);

  public IParserCache getParserCache ();

  public void setRootTag (String rootTag);

  public String getRootTag ();

  public void setContext (ILineContext context);

  public ILineContext getContext ();

  //============================================================
  // Parse methods.
  //============================================================
  public boolean redirectInput (Reader stream);

  public boolean redirectInput (InputStream stream);

  public boolean redirectInput (String str);

  public Document parseInput () throws Exception;

}

//==== END OF FILE
