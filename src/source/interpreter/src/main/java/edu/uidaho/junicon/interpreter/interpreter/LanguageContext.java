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
package edu.uidaho.junicon.interpreter.interpreter;

import edu.uidaho.junicon.interpreter.parser.IParser;
import edu.uidaho.junicon.runtime.util.ILogger;
import edu.uidaho.junicon.runtime.util.ILogger.Trace;

import java.util.EnumSet;

/**
 * Provides state for the language declared in a script directive.
 */
public class LanguageContext {
  String language = "icon";	// if "java", compile to Java
  boolean isIcon = true;

  // Script state
  LanguageContext parentContext = null;
  int sameAsParent = 0;		// Nested level of same @<script> type

  boolean isInsideAtScript = false;	// inside @<script>
  StringBuilder atScriptContents = new StringBuilder();
  boolean atBeginOfScript = false;
  boolean atEndOfScript = false;
  boolean evalAtEndOfScript = false;
  boolean isEmbedded = false;		// isInsideStatement
  boolean isNested = false;		// is inside another different script

  // Parser state
  IParser parserState = null;

  // Transform state
  boolean isInteractive = false;
  boolean isInterpretive = false;

  boolean doNotPreprocess = false;
  boolean doNotDetect = false;
  boolean doNotTransform = false;
  boolean justNormalize = false;	// only perform subset of transforms
  boolean doNotExecute = false;

  // Verbose state
  boolean echo = false;
  EnumSet<Trace> verbose = EnumSet.of(Trace.OFF); 
  EnumSet<Trace> inheritVerbose = EnumSet.of(Trace.OFF);
  EnumSet<Trace> trace = EnumSet.of(Trace.OFF);		// from getLogger()

  /**
   * No-arg constructor.
   */
  public LanguageContext () {
  }

  /**
   * Clone given context.
   */
  public LanguageContext (LanguageContext context) {
	if (context == null) { return; }

	this.language = context.language;
	this.isIcon = context.isIcon;
	this.parentContext = context.parentContext;

	this.isInsideAtScript = context.isInsideAtScript;
	this.atScriptContents = context.atScriptContents;
	this.atBeginOfScript = context.atBeginOfScript;
	this.atEndOfScript = context.atEndOfScript;
	this.evalAtEndOfScript = context.evalAtEndOfScript;
	this.isEmbedded = context.isEmbedded;
	this.isNested = context.isNested;
	// this.sameAsParent = context.sameAsParent;
	// this.parserState = context.parserState;

	this.isInteractive = context.isInteractive;
	this.isInterpretive = context.isInterpretive;

	this.doNotPreprocess = context.doNotPreprocess;
	this.doNotDetect = context.doNotDetect;
	this.doNotTransform = context.doNotTransform;
	this.justNormalize = context.justNormalize;
	this.doNotExecute = context.doNotExecute;

	this.echo = context.echo;
	this.verbose = EnumSet.copyOf(context.verbose);
	this.inheritVerbose = EnumSet.copyOf(context.inheritVerbose);
	this.trace = EnumSet.copyOf(context.trace);
  }

  public void resetState () {
	sameAsParent = 0;
	isInsideAtScript = false;
	atScriptContents = new StringBuilder();
	atBeginOfScript = false;
	atEndOfScript = false;
	evalAtEndOfScript = false;
	isEmbedded = false;
	isNested = false;
  }

}

//==== END OF FILE
