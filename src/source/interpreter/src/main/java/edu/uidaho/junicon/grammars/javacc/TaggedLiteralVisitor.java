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
 * Generates XML syntax node for big literal of the form {< literal >}.
 * Output is given tag with literal as text child stripped of delimiters,
 * as well as opening and closing "DELIMITER ID=..." tags.
 * Adds attribute to tag if specified, otherwise if cache is non-null
 * adds "literalKey" attribute with value derived from the literal cache.
 */
public class TaggedLiteralVisitor extends VisitorClass {
  private String openDelim = "{<";
  private String closeDelim = ">}";

  public TaggedLiteralVisitor (String tag, String openDelim,
		String closeDelim) {
	super(tag);
	if (openDelim != null) { this.openDelim = openDelim; };
	if (closeDelim != null) { this.closeDelim = closeDelim; };
  }
  
  public TaggedLiteralVisitor (String tag, String attr, String value,
  		String openDelim, String closeDelim) {
	super(tag, attr, value);
	if (openDelim != null) { this.openDelim = openDelim; };
	if (closeDelim != null) { this.closeDelim = closeDelim; };
  }
  
  public void visit_start(IMyNode n, DocumentHandler dochandler,
			IParserCache cache, int numsiblings, int numchildren) {
      	dochandler.add_text_node ("DELIMITER", "ID", openDelim, null);
	int pat_length = n.getName().length()-2;
	String theLiteral = "";
	if (pat_length >= 2) {
		theLiteral = n.getName().substring(2,pat_length);
	};
	if ((attr == null) && (cache != null)) {
	        attr = "literalKey";
	        value = cache.putLiteral(theLiteral);
	};
	dochandler.add_text_node(tag, attr, value, theLiteral);
  }

  public void visit_end(IMyNode n, DocumentHandler dochandler,
		IParserCache cache, int numsiblings, int numchildren) {
	dochandler.add_text_node ("DELIMITER", "ID", closeDelim, null);
  }

}

//==== END OF FILE
