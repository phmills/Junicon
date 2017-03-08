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

import edu.uidaho.junicon.runtime.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * CorrelateSource correlates a span in transformed code, given by beginning
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
public class CorrelateSource implements ICorrelateSource {
	private static String defaultLinesep = System.getProperty("line.separator", "\n");
	private static String defaultExtractLinesep = System.getProperty("line.separator", "\n");
	private String linesep = defaultLinesep;
	private String extractLinesep = defaultExtractLinesep;
	private int searchIndex = 0;	// position in source traversal
	private int nextSearchIndex = 0;
	private String source = null;		// source string
	private String[] sourceLines = null;	// source split into lines
	private int[] lineStart = null;	// source starting position in string
	private int lastNewlineLength = 0;	// set in getLines()
	private Map<String,String> concreteTagMap = new HashMap<String,String>();

	// Exclusive cases for findDecorated (interval logic, six cases)
	private Node before;
	private Node beforeOverlapping;
	private Node after;
	private Node afterOverlapping;
	private Node around;
	private Node inside;
	private int spanFrom;
	private int spanTo;

	//====
	// Concrete syntax is carried by "@ID" attribute or text node.
	//====
	private String[] concreteSyntaxNodes = { "WORD", "OPERATOR",
		"DELIMITER", "IDENTIFIER", "QUOTE", "XPATTERN", "ONTOLOGY",
		"COMMAND" };

	/**
	 * Constructor.
	 */
	public CorrelateSource (String source) {
		this(source, null, null);
	}

	/**
	 * Constructor with source line separator.
	 * Default is "line.separator" System property.
	 */
	public CorrelateSource (String source, String sourceLineSeparator) {
		this(source, sourceLineSeparator, null);
	}

	/**
	 * Constructor with source and extract line separator.
	 * Default is "line.separator" System property.
	 */
	public CorrelateSource (String source, String sourceLineSeparator,
			String extractLineSeparator) {
		setSourceLineSeparator(sourceLineSeparator);
		setExtractLineSeparator(extractLineSeparator);
		setSource(source);
		setConcreteTagMap();
	}

	public String getSource () {
		return source;
	}

	public void setSource (String source) {
		if (source == null) { source = ""; };
		this.source = source;
		sourceLines = getLines(source);
		lineStart =  getLineStart(sourceLines);
	}

	private void setConcreteTagMap () {
		concreteTagMap.clear();
		for (int i=0; i < concreteSyntaxNodes.length; i++) {
			concreteTagMap.put(concreteSyntaxNodes[i], "true");
		};
	}

	//==================================================================
	// Setters.
	//==================================================================

	public String getDefaultSourceLineSeparator () {
		return defaultLinesep;
	}

	public void setDefaultSourceLineSeparator (String linesep) {
		if ((linesep == null) || (linesep.length() == 0)) { return; };
		this.defaultLinesep = linesep;
	}

	public String getDefaultExtractLineSeparator () {
		return defaultExtractLinesep;
	}

	public void setDefaultExtractLineSeparator (String linesep) {
		if ((linesep == null) || (linesep.length() == 0)) { return; };
		this.defaultExtractLinesep = linesep;
	}


	public String getSourceLineSeparator () {
		return linesep;
	}

	public void setSourceLineSeparator (String linesep) {
		if ((linesep == null) || (linesep.length() == 0)) { return; };
		this.linesep = linesep;
	}

	public String getExtractLineSeparator () {
		return extractLinesep;
	}

	public void setExtractLineSeparator (String linesep) {
		if ((linesep == null) || (linesep.length() == 0)) { return; };
		this.extractLinesep = linesep;
	}

	public String[] getConcreteSyntaxNodes () {
		return concreteSyntaxNodes;
	}

	public void setConcreteSyntaxNodes (String[] nodes) {
		if (nodes == null) { return; };
		for (int i=0; i<nodes.length; i++) {
			if (nodes[i] == null) { nodes[i] = ""; };
		};
		this.concreteSyntaxNodes = nodes;
		setConcreteTagMap();
	}

	//==================================================================
	// Document helper functions.
	//==================================================================

	public boolean isConcreteSyntax(Node node) {
		if (node == null) { return false; };
		String name = node.getNodeName();
		if ((node instanceof Element) &&
				(null != concreteTagMap.get(name))) {
			return true;
		};
		return false;
	}
	
	public String getTextValue (Node node) {
		if ((node == null) || (! (node instanceof Element))) {
			return null;
		};
		Element elt = (Element) node;
		if (elt.getNodeName().equals("DELIMITER")) {
			return elt.getAttribute("ID");
		} else {
			NodeList nodeList = elt.getChildNodes();
			if (nodeList == null) { return null; };
			for (int i=0; i < nodeList.getLength(); i++) {	
				Node n = nodeList.item(i);
				if (n.getNodeType() == Node.TEXT_NODE) {
					return n.getNodeValue();
				}
			}
		}
		return null;
	}

	public int getDecoratedLine (Node node) {
		return getNumericAttribute(node, "line");
	}
	
	public int getDecoratedColumn (Node node) {
		return getNumericAttribute(node, "column");
	}
	
	/**
	 * Gets ending column number for given decorated node.
	 * @return ending column, of beginning column if value is empty
	 * string or null.
	 */
	private int getColumnEnd (int from, String value) {
		if ((from < 0) || (value == null) || (value.length() == 0)) {
			return from;
		};
		return from + (value.length() - 1); 
	}
	
	/**
	 * Gets numeric attribute for given node.
	 * @return value of attribute,
	 * or -1 if not numeric or attribute not found.
	 */
	private int getNumericAttribute(Node node, String attrname) {
		int value = -1;
		if ((node == null) || (! (node instanceof Element))) {
			return value;
		};
		Element elt = (Element) node;
		String val = elt.getAttribute(attrname);
		if ((val == null) || (val.length() == 0)) {
			return value;
		};
		try {
			value = Integer.parseInt(val);
		} catch (NumberFormatException e) { ; };
		return value;
	}
	
	//====================================================================
	// Decorate parseTree.
	//====================================================================

	public Document decoratePosition (Document parseTree) {
		if (parseTree == null) { return null; };
		initTraverseSource();
		decorate(parseTree.getDocumentElement());
		return parseTree;
	}
	
	/**
	 * Recursively decorates position for element and its children.
	 */
	private void decorate (Node node) {
		if ((node == null) || (! (node instanceof Element))) {
			return;
		};
		if (isConcreteSyntax(node)) {
			String value = getTextValue(node);
			int position = traverseSource(value, false);
			int line = getLine(position);
			int column = getColumn(line, position);
			((Element) node).setAttribute("line",
				String.valueOf(line));
			((Element) node).setAttribute("column",
				String.valueOf(column));
		}
		NodeList nodeList = node.getChildNodes();
		if (nodeList == null) { return; };
		for (int i=0; i < nodeList.getLength(); i++) {
			decorate(nodeList.item(i));
		};
	}
	
	//====================================================================
	// Find decorated nodes.
	// Scoots thru source, matching in depth-first traversal of parseTree.
	//====================================================================

	public int[] findDecoratedSpan (Document parseTree,
			int fromLine, int fromColumn,
			int toLine, int toColumn) {
		int decFromLine = -1;
		int decFromColumn = -1;
		int decToLine = -1;
		int decToColumn = -1;

		if (parseTree == null) { return null; };

		// Set bounds
		if (fromLine < 0) { fromLine = 0; };
		if (fromColumn < 0) { fromColumn = 0; };
		if (toLine < 0) { toLine = getNumLines()-1; };
		if (toColumn < 0) {
			toColumn = getLastColumn(toLine);
		};

		spanFrom = getPosition(fromLine, fromColumn);
		spanTo = getPosition(toLine, toColumn);
		before = null;
		beforeOverlapping = null;
		after = null;
		afterOverlapping = null;
		around = null;
		inside = null;
		initTraverseSource();
		traverseDecoratedNodes(parseTree.getDocumentElement());
		if (around != null) {
			decFromLine = getDecoratedLine(around);
			decFromColumn = getDecoratedColumn(around);
			decToLine = decFromLine;
			decToColumn = getColumnEnd(decToLine, getTextValue(around));
		} else {
			if (beforeOverlapping != null) {
				decFromLine = getDecoratedLine(beforeOverlapping);
				decFromColumn =
					getDecoratedColumn(beforeOverlapping);
			} else {
				if (before != null) {
					decFromLine = getDecoratedLine(before);
					decFromColumn = getColumnEnd(
						getDecoratedColumn(before),
						getTextValue(before));
				};
			};
			if (afterOverlapping != null) {
				decToLine = getDecoratedLine(afterOverlapping);
				decToColumn = getColumnEnd(
					getDecoratedColumn(afterOverlapping),
					getTextValue(afterOverlapping));
			} else {
				if (after != null) {
					decToLine = getDecoratedLine(after);
					decToColumn = getDecoratedColumn(after);
				};
			};
		};
		if ((decToLine >= 0) && ((decToLine < decFromLine) ||
				((decToLine == decFromLine) &&
				(decToColumn < decFromColumn)))) {  // Flipped
			int[] result = { decToLine, decToColumn,
				decFromLine, decFromColumn };
			return result;

		};
		int[] result = { decFromLine, decFromColumn,
				decToLine, decToColumn };
		return result;
	}

	/**
	 * Finds decorated node that is after, before, or around the source
	 * span.
	 */
	private void traverseDecoratedNodes (Node node) {
		if ((node == null) || (! (node instanceof Element))) {
			return;
		};
		if ((around != null) || (after != null) ||
				(afterOverlapping != null)) {
			return;		// we are done
		};
		if (isConcreteSyntax(node)) {
			String value = getTextValue(node);
			int from = traverseSource(value, true);
			if ((from >= 0) && (getDecoratedLine(node) >= 0)) {
				int to = getColumnEnd(from, value);
				if ((from <= spanFrom) && (to >= spanTo)) {
					around = node;	// Around or equal
				} else if ((from > spanFrom) && (to < spanTo)) {
					inside = node;	// Inside not touching
				} else if (to <= spanFrom) {
					before = node;	// Before or touching
				} else if (from >= spanTo) {
					after = node;	// May touch end
				} else if ((to > spanFrom) && (to <  spanTo)) {
					beforeOverlapping = node; // Not touch end
				} else if ((from > spanFrom) && (from < spanTo)) {
					afterOverlapping = node; // Not touch beg
				};
			};
		}
		NodeList nodeList = node.getChildNodes();
		if (nodeList == null) { return; };
		for (int i=0; i < nodeList.getLength(); i++) {
			traverseDecoratedNodes(nodeList.item(i));
		};
	}

	//====================================================================
	// Extract decorated source lines
	//====================================================================

	public String extractDecoratedLines (int fromLine, int fromColumn,
			int toLine, int toColumn) {

		// Set bounds
		if (fromLine < 0) { fromLine = 0; };
		if (fromColumn < 0) { fromColumn = 0; };
		if (toLine < 0) { toLine = getNumLines()-1; };
		String lastLine = getSourceLine(toLine);
		if (toColumn < 0) {
			toColumn = getLastColumn(toLine);
		};
		String firstLine = getSourceLine(fromLine);
		
		// create "^"-lines
		String spaces = "                    ";
		while ((spaces.length() < fromColumn) || 
				(spaces.length() < toColumn)) { 
			spaces += spaces; 
		};

		// carot1 generated only if fromColumn > 0, carot2 always generated
		String carot1 = (fromColumn <= 0) ? "" :
					(spaces.substring(0, fromColumn) + "^");
		String carot2 = (toColumn < 0) ? "" :
					(spaces.substring(0, toColumn) + "^");
		if (fromLine >= toLine) {
			if (fromColumn > 0) {
				carot2 = (fromColumn >= toColumn) ? "" :	
					(spaces.substring(fromColumn + 1, toColumn) + "^");
			};
			return addLineSep(firstLine) + carot1 + carot2; 
		};
		String linesBetween = "";
		for (int i=fromLine+1; i < toLine; i++) {
			linesBetween += addLineSep(getSourceLine(i));
		}
		return (addLineSep(firstLine) + carot1 + extractLinesep + 
				linesBetween + addLineSep(lastLine) + carot2);
	}
	
	//====================================================================
	// Source helper functions:  Map position in source to line and column
	//====================================================================

	public int getNumLines() {
		if (sourceLines == null) { return 0; };
		return sourceLines.length;
	}

	/**
	 * Gets length of source string.
	 */
	private int getSourceLength() {
		if (source == null) { return 0; };
		return source.length();
	}

	/**
	 * Add line separator if not there, using extractLinesep
	 */
	private String addLineSep(String line) {
		if (line == null) { return null; };
		if (line.endsWith(linesep)) {
			line = line.substring(0,line.length()-linesep.length());
		};
		return line + extractLinesep;
	}

	/**
	 * Splits the source into lines, preserving newlines.
	 * Maintains if last line has no terminating newline.
	 */
	private String[] getLines (String source) {
		if (source == null) { return null; };
		String[] lines = source.split(linesep, -1);
		int numlines = lines.length;
		if (source.endsWith(linesep)) {
			if (lines[lines.length - 1].length() == 0) {
				numlines--;
				if (numlines <= 0) { numlines = 1; };
			};
		};
		String[] result = new String[numlines];
		int lastline = numlines - 1;
		for (int i=0; i<lastline; i++) {
			result[i] = lines[i] + linesep;
		};
		if (! source.endsWith(linesep)) {
			result[lastline] = lines[lastline];
			lastNewlineLength = 0;
		} else {
			result[lastline] = lines[lastline] + linesep;
			lastNewlineLength = linesep.length();
		};
		return result;
	}

	/**
	 * Gets the starting position for each line in the source (scan).
	 * Line index origin is 0.
	 */
	private int[] getLineStart (String[] lines) {
		if (lines == null) {
			return null;
		};
		int[] starts = new int[lines.length];
		int position = 0;
		for (int i=0; i<lines.length; i++) {
			starts[i] = position;
			if (lines[i] != null) {
				position += lines[i].length();
			};
		};
		return starts;
	}

	/**
	 * Gets line number for position in the source.
	 * Line index origin is 0.
	 * @return last line if position is beyond end of source,
	 *	and 0 if position < 0.
	 */
	private int getLine (int position) {
		if ((lineStart == null) || (position < 0)) {
			return 0;
		};
		for (int i=lineStart.length - 1; i>=0; i--) {
			if (position >= lineStart[i]) {
				return i;
			};
		};
		return 0;
	}

	/**
	 * Gets column number for position in the source.
	 * Column index origin is 0.
	 * @return last column, including newline,
	 *	if position is beyond end of source,
	 *	and 0 if position < 0.
	 */
	private int getColumn (int line, int position) {
		if ((lineStart == null) || (lineStart.length == 0)
				|| (position < 0)) {
			return 0;
		};
		if (line < 0) { line = 0; };
		if (line >= lineStart.length) {
			line = lineStart.length - 1;
		};
		if (position >= getSourceLength()) {
			position = getSourceLength() - 1;
		};
		return position-lineStart[line];
	}

	/**
	 * Returns last column before newline, for given line.
	 * Returns -1 if line is empty.
	 */
	private int getLastColumn (int line) {
		if ((sourceLines == null) || (sourceLines.length == 0)) {
			return 0;
		};
		if (line < 0) { line = 0; };
		if (line >= sourceLines.length) {
			line = sourceLines.length - 1;
			return (sourceLines[line]).length() -
				(1 + lastNewlineLength);
		};
		return (sourceLines[line]).length() - (1 + linesep.length());
	}

	/**
	 * Converts line and column into position for given source.
	 * @return position, using last line if line is beyond source,
	 *	last column including newline
	 *	if given column is beyond end of line,
	 *	column is 0 if column < 0, and line is 0 if line < 0.
	 */
	private int getPosition (int line, int column) {
		if ((lineStart == null) || (lineStart.length == 0)) {
			return 0;
		};
		if (column < 0) { column = 0; };
		if (line < 0) { line = 0; };
		if (line >= lineStart.length) {
			line = lineStart.length - 1;
		};
		String sourceline = sourceLines[line];
		if ((sourceline != null) && (column >= sourceline.length())) {
			column = sourceline.length() - 1;
		};
		int position = column+lineStart[line];
		if (position >= getSourceLength()) {
			position = getSourceLength() - 1;
		};
		return position;
	}

	/**
	 * Initialize source string traversal.
	 */
	private void initTraverseSource(){
		searchIndex = 0;
		nextSearchIndex = 0;
	}

	/**
	 * Finds position of value in source, beginning from last search point.
	 * Source can be subset of ParseTree, e.g., for Groovy we
	 * preserve ";" in Deconstructed.xsl but it is removed from source.
	 * So ignores a parse token if it is not found as next token in source,
	 * i.e., is found but separated by non-whitespace.
	 * @param ignoreExtraTokens if tokens are not found, will
	 *	not advance source searchIndex.
	 *	Default if not found is to set searchIndex to -1.
	 * @return position, or -1 if not found.
	 */
	private int traverseSource (String value, boolean ignoreExtraTokens) {
		if ((value == null) || (value.length() == 0) ||
			 (searchIndex < 0) || (source == null)) { 
			return searchIndex; 
		};
		searchIndex = source.indexOf(value, nextSearchIndex);
		if (ignoreExtraTokens) {
			if ((searchIndex < 0) ||
					((source.substring(nextSearchIndex,
					searchIndex).trim()).length() > 0)) {
				// Do not advance
				searchIndex = nextSearchIndex;
				return -1;
			};
		};
		nextSearchIndex = searchIndex + value.length();
		return searchIndex;
	}
	//====
	// INVARIANT: metaparser does not allow concrete syntax tokens to
	// span lines.
	//====

	public String getSourceLine (int line) {
		if ((sourceLines == null) || (sourceLines.length == 0)) {
			return "";
		};
		if (line < 0) { return source; };
		if (line >= sourceLines.length) {
			line = sourceLines.length - 1;
		};
		return sourceLines[line];
	}
	
	/**
	 * Returns line without line separator.
	 */
	private String getSourceLineTrim (int line) {
		String src = getSourceLine(line);
		if ((src == null) || (src.length() == 0)) {
			return "";
		};
		return(src.substring(0,getLastColumn(line)+1));
	}
	
}

//==== END OF FILE
