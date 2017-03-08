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
package edu.uidaho.junicon.interpreter.interpreter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * ICorrelateSource correlates a span in transformed code, given by beginning
 * and ending line and column number,
 * to an approximate span in the original source code.
 * <br>
 * CorrelateSource is intended to be used with interpreter modules that
 * decorate a parsed AST with
 * source location attributes before transformation, 
 * carry this information through the
 * transformation chains, then use these approximation techniques after
 * transformation to back-correlate error locations for debugging.
 *
 * @author Peter Mills
 */
public interface ICorrelateSource {

	/**
	 * Decorates parseTree adding line and column attributes 
	 * to the concrete syntax nodes. 
	 * Concrete syntax node types are defined in 
	 * docs/schemas/SourceAST.xsd.
	 * Line index origin is 0, and Column index origin is 0.
	 */
	public Document decoratePosition (Document parseTree);

	/**
	 * Checks if node is concrete syntax node.
	 * Concrete syntax is carried by Element nodes
	 * with "@ID" attribute or text child node.
	 */
	public boolean isConcreteSyntax (Node node);

	/**
	 * Gets text value for concrete syntax node.
	 * @return value of DELIMITER ID attribute, or
	 *	first text node child, or null otherwise.
	 */
	public String getTextValue (Node node);

	/**
	 * Gets line number for given decorated node.
	 * @return line, or -1 if not numeric or "line" attribute not found.
	 */
	public int getDecoratedLine (Node node);

	/**
	 * Gets beginning column number for given decorated node.
	 * @return column, or -1 if not numeric or "column" attribute not found.
	 */
	public int getDecoratedColumn (Node node);

	/**
	 * Finds closest bounding decorated concrete syntax nodes
	 * in AST for given span in source. 
	 * Source can be subset of ParseTree, e.g., for Groovy we
	 * preserve ";" in Deconstructed.xsl but it is removed from source.
	 * So ignores a parse token if it is not found as next token in source,
	 * i.e., is found but separated by non-whitespace.
	 * Line index origin is 0, and Column index origin is 0.
	 * fromLine < 0 is first line; fromColumn < 0 is first column.
	 * toLine < 0 is last line; toColumn < 0 is end of toLine.
	 * @return [fromLine, fromColumn, toLine, toColumn]
	 */
	public int[] findDecoratedSpan (Document parseTree,
			int fromLine, int fromColumn,
			int toLine, int toColumn);

	/**
	 * Extracts lines from source for the span defined by the line
	 * and column numbers in the closest bounding decorated nodes.
	 * Decorates extracted lines with "^" symbols for span start and end.
	 * Line index origin is 0, and Column index origin is 0.
	 * fromLine < 0 is first line; fromColumn < 0 is first column.
	 * toLine < 0 is last line; toColumn < 0 is end of toLine.
	 */
	public String extractDecoratedLines (int fromLine, int fromColumn,
			int toLine, int toColumn);

	/**
	 * Extracts line with corresponding line number from source.
	 * Line index origin is 0.
	 * @return Last line if line > number of lines in source,
	 *	or entire source if line < 0,
	 *	or empty string if source was null.
	 */
	public String getSourceLine (int line);

	/**
	 * Gets number of lines in source.
	 */
	public int getNumLines ();

  //====================================================================
  // Setters and defaults.
  //    The setters are non-static for Spring dependency injection.
  //====================================================================

	/**
	 * Gets default source line separator.
	 */
	public String getDefaultSourceLineSeparator ();

	/**
	 * Sets default source line separator.
	 */
	public void setDefaultSourceLineSeparator (String linesep);

	/**
	 * Gets default line separator for decorated extracts.
	 */
	public String getDefaultExtractLineSeparator ();

	/**
	 * Sets default line separator for decorated extracts.
	 */
	public void setDefaultExtractLineSeparator (String linesep);

	/**
	 * Gets source line separator.
	 */
	public String getSourceLineSeparator ();

	/**
	 * Sets source line separator.
	 */
	public void setSourceLineSeparator (String linesep);

	/**
	 * Gets line separator for decorated extracts 
	 * using extractDecoratedLines.
	 */
	public String getExtractLineSeparator ();

	/**
	 * Sets line separator for decorated extracts
	 * using extractDecoratedLines.
	 */
	public void setExtractLineSeparator (String linesep);

	/**
	 * Gets XML concrete syntax node names used in correlating source.
	 */
	public String[] getConcreteSyntaxNodes ();

	/**
	 * Sets XML concrete syntax node names used in correlating source.
	 */
	public void setConcreteSyntaxNodes (String[] nodes);

	/**
	 * Gets source to correlate.
	 */
	public String getSource ();

	/**
	 * Sets source to correlate.
	 */
	public void setSource (String source);

}

//==== END OF FILE
