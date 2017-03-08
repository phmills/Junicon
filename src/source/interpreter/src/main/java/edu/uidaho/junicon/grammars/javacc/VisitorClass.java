//============================================================================
// Copyright (c) 2011 Orielle, LLC.  
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//  1. Redistributions of source code must retain the above copyright notice,
//     this list of conditions and the following disclaimer. 
//  2. Redistributions in binary form must reproduce the above copyright notice,
//     this list of conditions and the following disclaimer in the documentation
//     and/or other materials provided with the distribution. 
//
// This software is provided by the copyright holders and contributors "as is"
// and any express or implied warranties, including, but not limited to, the
// implied warranties of merchantability and fitness for a particular purpose
// are disclaimed. In no event shall the copyright holder or contributors be
// liable for any direct, indirect, incidental, special, exemplary, or
// consequential damages (including, but not limited to, procurement of
// substitute goods or services; loss of use, data, or profits; or business
// interruption) however caused and on any theory of liability, whether in
// contract, strict liability, or tort (including negligence or otherwise)
// arising in any way out of the use of this software, even if advised of the
// possibility of such damage.
//============================================================================
package edu.uidaho.junicon.grammars.javacc;

import edu.uidaho.junicon.grammars.common.IParserCache;
import edu.uidaho.junicon.grammars.document.DocumentHandler;

/**
 * Base for parser visitor class.
 */
public class VisitorClass {
  String tag = "DEFAULT";
  String attr = null;
  String value = null;

  public VisitorClass () { 
  }

  public VisitorClass (String tag) {
	if (tag != null) { this.tag = tag; };
  }
  
  public VisitorClass (String tag, String attr, String value) {
	if (tag != null) { this.tag = tag; };
	if ((attr != null) && (value != null)) {
		this.attr = attr;
		this.value = value;
	};
  }

  public String getTag() { return tag; };

  public String getAttr() { return attr; };

  public String getValue() { return value; };
  
  public void visit_start(IMyNode n, DocumentHandler dochandler,
	IParserCache cache, int numsiblings, int numchildren) {
  }

  public void visit_end(IMyNode n, DocumentHandler dochandler,
	IParserCache cache, int numsiblings, int numchildren) {
  }
 
}

//==== END OF FILE
