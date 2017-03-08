//========================================================================
// Copyright (c) 2012 Orielle, LLC.  
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

import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Document;		// For DOM Processing

/**
 * Location in source file
 * for text undergoing parsing and evaluation.
 * The text can originate from multiple sources,
 * so the context is broken up into segments that span the lines of text.
 * Files can also be included from other files, so provision is made
 * to record the include history in chains of contexts.
 * For simplicity, we assume each line of text to be from a single source,
 * so only line numbers and not column numbers are provided.
 */
public class LineContext implements ILineContext {

  private String filename = null;
  private int line = 0;
  private ILineContext includedFrom = null;
  private String text = "";
  // private int length = 0;
  private int numberLines = 0;
  List<ILineContext> segments = null;
  private boolean isSegment = false;
  private ILineContext segmentParent = null;
  private int segmentCharOffset = 0;
  private int segmentLineOffset = 0;

  //======================================================================
  // Constructors.
  //======================================================================
  /**
   * Constructor.
   */
  public LineContext () {
  }

  /**
   * Constructor.
   */
  public LineContext (String filename, int line,
		ILineContext fileIncludedFrom, String text) {
	setFilename(filename);
	setLine(line);
	setFileIncludedFrom(fileIncludedFrom);
	setText(text);
  }

  /**
   * Constructor.
   */
  public LineContext (String filename, ILineContext fileIncludedFrom) {
	setFilename(filename);
	setFileIncludedFrom(fileIncludedFrom);
  }

  /**
   * Copy constructor.
   * Copies fields from given context, except sets segments to null.
   */
  public LineContext (ILineContext context) {
	if (context == null) { return; };
	this.filename = context.getFilename();
	this.line = context.getLine();
	this.includedFrom = context.getFileIncludedFrom();
	this.text = context.getText();
	this.numberLines = context.getNumberOfLines();
	this.segments = null;
	this.isSegment = context.isSegment();
	this.segmentParent = context.getSegmentParent();
	this.segmentCharOffset = context.getCharOffset();
	this.segmentLineOffset = context.getLineOffset();
  }

  /**
   * Constructor that derives line number from given context and offset.
   * Ignores offset if < 0 or context is null or its line < 0.
   */
  public LineContext (ILineContext context, String text, int offset) {
	this(context);
	setText(text);
	setLineFromContext(context, offset);
  }

  /**
   * Copy constructor that clones given context with new text.
   */
  public LineContext (ILineContext context, String text) {
	this(context);
	setText(text);
  }

  /**
   * Copy constructor that clones given context with new text
   * and segments.
   */
  public LineContext (ILineContext context, String text,
  		int numberLines, List<ILineContext> segments) {
	this(context);
	setText(text);
	setNumberOfLines(numberLines);
	setSegments(segments);
  }

  //======================================================================
  // Setters for dependency injection.
  //======================================================================
  public void setFilename (String filename) {
	this.filename = filename;
  }

  public String getFilename () {
	return filename;
  }

  public void setLine (int lineNumber) {
	if (lineNumber < 0) { lineNumber = 0; };
	this.line = lineNumber;
  }

  public int getLine () {
	return line;
  }

  public void setFileIncludedFrom (ILineContext includedFrom) {
	this.includedFrom = includedFrom;
  }

  public ILineContext getFileIncludedFrom () {
	return includedFrom;
  }

  public void setText (String text) {
	if (text == null) { text = ""; }
	this.text = text;
  }

  public String getText () {
	return text;
  }

  //====
  // public void setLength (int length) {
  //	if (length < 0) { length = 0; };
  //	this.length = length;
  // }
  // public int getLength () {
  //	return length;
  // }
  //====

  public void setNumberOfLines (int numberLines) {
	if (numberLines < 0) { numberLines = 0; };
	this.numberLines = numberLines;
  }

  public int getNumberOfLines () {
	return numberLines;
  }

  public void setSegments (List<ILineContext> segments) {
	this.segments = segments;
  }

  public List<ILineContext> getSegments () {
	return segments;
  }

  public void setIsSegment (boolean isSegment) {
	this.isSegment = isSegment;
  }

  public boolean isSegment () {
	return isSegment;
  }

  public void setSegmentParent (ILineContext parent) {
	this.segmentParent = parent;
  }

  public ILineContext getSegmentParent () {
	return segmentParent;
  }

  public void setCharOffset (int offset) {
	if (offset < 0) { offset = 0; };
	this.segmentCharOffset = offset;
  }

  public int getCharOffset () {
	return segmentCharOffset;
  }

  public void setLineOffset (int offset) {
	if (offset < 0) { offset = 0; };
	this.segmentLineOffset = offset;
  }

  public int getLineOffset () {
	return segmentLineOffset;
  }

  public void setLineFromContext (ILineContext context, int lineOffset) {
	if (lineOffset < 0) { lineOffset = 0; };
	if (context != null) { line = context.getLine(); };
	line += lineOffset;
  }

  public ILineContext addSegment (ILineContext from,
		String text, int lineOffset) {
	if ((text == null) || text.isEmpty()) { return null; };
	if (segments == null) { 
		segments = new ArrayList<ILineContext>();
	}
	ILineContext segment = new LineContext(from);
	segment.setText(text);
	segment.setIsSegment(true);
	segment.setSegmentParent(this);
	segment.setLineOffset(lineOffset);
	segments.add(segment);
	return segment;
  }

  public void addSegmentsFrom (ILineContext from, int lineOffset) {
	if (from == null) { return; };
	if (lineOffset < 0) { lineOffset = 0; };
	List<ILineContext> fromSegments = from.getSegments();
	if (fromSegments != null) {
		if (segments == null) {
			segments = fromSegments; 
		} else {
			segments.addAll(fromSegments);
		};
	}
	if (fromSegments != null) {
		for (ILineContext segment : fromSegments) {
			segment.setLineOffset(segment.getLineOffset() +
				lineOffset);
		}
	}
  }

  public boolean follows (ILineContext context) {
	if ((context == null) || (context == this) ||
		(getLine() == context.getLine()) ||
		(getLine() == (context.getLine() + 1))) {
		return true;
	};
	return false;
  }

}

//==== END OF FILE
