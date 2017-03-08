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
public interface ILineContext {

  //======================================================================
  // Setters for dependency injection.
  //======================================================================
  /**
   * Sets the source for this text.
   */
  public void setFilename (String filename);

  /**
   * Gets the source for this text.
   */
  public String getFilename ();

  /**
   * Sets the line number of this text in the source file.
   * @param lineNumber	if < 0, is set to 0.
   */
  public void setLine (int lineNumber);

  /**
   * Gets the line number of this text in the source file.
   * Origin is 0.
   */
  public int getLine ();

  /**
   * Sets where this file is included from.
   */
  public void setFileIncludedFrom (ILineContext includedFrom);

  /**
   * Gets where this file is included from.
   */
  public ILineContext getFileIncludedFrom ();

  /**
   * Sets the text.
   * @param text	if null, set to empty string.
   */
  public void setText (String text);

  /**
   * Gets the text.   This is an optional field.
   */
  public String getText ();

  /**
   * Sets the number of lines of this text.
   * @param lineNumber	if < 0, is set to 0.
   */
  public void setNumberOfLines (int lineNumber);

  /**
   * Gets number of lines of this text.
   */
  public int getNumberOfLines ();

  /**
   * Partitions the text into segments from different sources.
   */
  public void setSegments (List<ILineContext> segments);

  /**
   * Gets the segments that partition the text into sections
   * from different sources.
   * Non-null only if is segmented.
   */
  public List<ILineContext> getSegments ();

  /**
   * Sets if this is a segment.
   */
  public void setIsSegment (boolean isSegment);

  /**
   * Gets if this is a segment.
   */
  public boolean isSegment ();

  /**
   * Sets the parent context if this is a segment.
   */
  public void setSegmentParent (ILineContext parent);

  /**
   * Gets the parent context if this is a segment.
   */
  public ILineContext getSegmentParent ();

  /**
   * Sets the character offset in the parent text for this segment.
   * @param offset	if < 0, is set to 0.
   */
  public void setCharOffset (int offset);

  /**
   * Gets the character offset in the parent text for this segment.
   */
  public int getCharOffset ();

  /**
   * Sets the line offset of this segment in the parent text.
   * @param linenumber	if < 0, is set to 0.
   */
  public void setLineOffset (int linenumber);

  /**
   * Gets the line offset of this segment in the parent text.
   */
  public int getLineOffset ();

  /**
   * Sets line number by adding offset to given context's line number.
   * If context is null uses offset,
   * otherwise adds offset to its beginning line number.
   * @param lineOffset	offset of text in context,
   *			if < 0 is set to 0.
   */
  public void setLineFromContext (ILineContext context, int lineOffset);

  /**
   * Adds a segment created by copying from the given context.
   * Does not add segment if text is null or empty.
   * @param from	segment context
   * @param text	segment text
   * @param lineOffset	offset of the segment in the parent text,
   *			if < 0 is set to 0.
   * @return added segment, or null if text is null or empty.
   */
  public ILineContext addSegment (ILineContext from,
	String text, int lineOffset);

  /**
   * Adds segments from the given context.
   * Performs a shallow copy of any segment references.
   * Added segments have their lineOffsets adjusted by given lineOffset,
   * which is thus destructive on the original context.
   */
  public void addSegmentsFrom (ILineContext from, int lineOffset);

  /**
   * Returns if this context follows the given context.
   * This context follows the given context if it is null,
   * it is the the same as this,
   * its linenumber is equal to this, 
   * or its linenumber is 1 less than this.
   */
  public boolean follows (ILineContext context);

}

//==== END OF FILE
