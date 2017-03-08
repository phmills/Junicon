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
package edu.uidaho.junicon.interpreter.parser;

import edu.uidaho.junicon.grammars.common.*;

import org.w3c.dom.Document;		// For DOM Processing

import java.text.ParseException;
import java.util.*;
import java.io.*;
import java.util.regex.Pattern;

/** 
 * StatementDetector identifies parsible units in the source input
 * to pass to specific target sub-interpreters for parsing and execution.
 * Parsible units are strings ended by ";" that contain only
 * matching group delimiters {} () [] and no open quotes.
 * In addition to single and double quotes,
 * several types of big quotes are allowed,
 * depending on flag settings, delimited by:
 * {{@literal <} {@literal >}} (block quote), 
 * {@literal <}| |{@literal >}; (pattern literal),
 * [| |] (ontology literal), or {| |} (command literal).
 * The StatementDetector takes an input line,
 * adds it to the incomplete statement so far
 * built up from the input history,
 * and then extracts any complete statements from the front.
 * The StatementDetector carries the quote/grouping state as it traverses 
 * the input lines to form the complete statements.
 *
 * @author Peter Mills
 */
public class StatementDetector extends AbstractPreprocessor
		implements IPreprocessor {

	//====
	// Setters for dependency injection
	//====
	private boolean isQuoteOnlyMode = false;
	private boolean allowPoundComment = true;	// #
	private boolean allowSlashComment = false;	// //
	private boolean allowMultilineComment = false;	// /* */
	private boolean allowBlockQuote = true;		// {< >}
	private boolean allowCommandLiteral = false;	// {| |}
	private boolean allowPatternLiteral = false;	// <| |>
	private boolean allowOntologyLiteral = false;	// [| |]
	private boolean allowEscapedStartQuote = false;
	private boolean allowEscapedEndQuote = true;
	private boolean allowEscapedBlockQuote = false;	// big literals
	private boolean allowEscapedComment = false;
	private boolean allowEscapedNewline = true;
	private boolean allowEscapedNewlineInQuote = true;
	private boolean allowEscapedNewlineInBlockQuote = true;
	private boolean splitMultipleStatementsOnLine = true;
	private boolean preserveEmptyLines = true;

	//====
	// getParseUnitFromLine() fields
	//====
	private boolean inQuote = false;
	private boolean inComment = false;	// Multi-line comment
	private boolean inBlockQuote = false;	// big quote / big literal
	private boolean inCommandLiteral = false;
	private boolean inPatternLiteral = false;
	private boolean inOntologyLiteral = false;
	private int parenCount = 0;
	private int braceCount = 0; // nestingLevel (stmt, list, bracket, paren)
	private int bracketCount = 0;
	private char quoteChar = 0;		// (double, single, back, xml)

	private String complete = ""; // Statements found so far
	private String partial = "";  // incomplete statement found so far
				      // [complete[], partial, remainder]
	private int partialLineOffset = 0;
	private int completeLineOffset = 0;
	private ILineContext partialContext = null;
	private ILineContext lastPartialContext = null;
	private ILineContext completeContext = null;
	private boolean foundPartialContent = false;
		// line has non-whitespace content outside comments
		// implies foundNonWhitespace
	private boolean foundNonWhitespace = false;
		// line has non-whitespace content, possibly inside comments
	//====
	// getParseUnit() fields
	//====
	private String quoteEscapedNewline = "\\n"; // literal line continuation
	private String quoteNewline = "\n";	// literal line newline
	private String blockQuoteEscapedNewline = "\\\\\\n"; // line continuation
	private String blockQuoteNewline = "\\n";      // blockQuote newline
	private String lineEscapedNewline = " ";	// line continuation
	private String lineNewline = "\n";	// line preserved newline
	// private char clearChar = 27;		// Esc (decimal)

	private LinkedList<ILineContext> foundStatementsInLine =
		new LinkedList<ILineContext>();

	private Pattern trimLeft = Pattern.compile("^\\s+");
	private Pattern trimRight = Pattern.compile("\\s+$");

//========================================================================
// Constructors
//========================================================================
  /**
   * Constructs a StatementDetector instance.
   */
  public StatementDetector () {
	super();
	resetParserState();
  }

//========================================================================
// Setters for dependency injection
//========================================================================

  /**
   * Split multiple statements on one line,
   * or spanning across the middle of lines.
   * If not allowed, statements will not be processed until
   * they end on a line by themselves.
   */
  public void setSplitMultipleStatementsOnLine (boolean isAllowed) {
	this.splitMultipleStatementsOnLine = isAllowed;
  }

  /**
   * Set if preserve empty lines.
   */
  public void setPreserveEmptyLines (boolean allow) {
	this.preserveEmptyLines = allow;
  }

  /**
   * Get if preserve empty lines.
   */
  public boolean getPreserveEmptyLines () {
	return preserveEmptyLines;
  }

  /**
   * Sets if getParseUnit only fuses multi-line quotes and returns
   * lines with non-open-quotes.
   * Otherwise getParseUnit partitions input into statements ending
   * with ";" and containing no open group delimiters {} () [].
   */
  public void setQuoteOnlyMode (boolean isQuoteOnlyMode) {
	this.isQuoteOnlyMode = isQuoteOnlyMode;
  }

  /**
   * Gets if getParseUnit only fuses multi-line quotes
   * instead of partitioning into statements ending with ";".
   */
  public boolean getQuoteOnlyMode () {
	return isQuoteOnlyMode;
  }

  /**
   * Set if allow # single line comments.
   */
  public void setAllowPoundComment (boolean allow) {
	this.allowPoundComment = allow;
  }

  public boolean getAllowPoundComment () {
	return allowPoundComment;
  }

  /**
   * Set if allow Java multi line comments.
   */
  public void setAllowMultilineComment (boolean allow) {
	this.allowMultilineComment = allow;
  }

  public boolean getAllowMultilineComment () {
	return allowMultilineComment;
  }

  /**
   * Set if allow Java double slash line comments.
   */
  public void setAllowSlashComment (boolean allow) {
	this.allowSlashComment = allow;
  }

  public boolean getAllowSlashComment () {
	return allowSlashComment;
  }

  /**
   * Set if allow "{< >}" big literals (block quotes).
   */
  public void setAllowBlockQuote (boolean allow) {
	this.allowBlockQuote = allow;
  }

  public boolean getAllowBlockQuote () {
	return allowBlockQuote;
  }

  /**
   * Set if allow "<| |>" big literals.
   */
  public void setAllowPatternLiteral (boolean allow) {
	this.allowPatternLiteral = allow;
  }

  public boolean getAllowPatternLiteral () {
	return allowPatternLiteral;
  }

  /**
   * Set if allow "[| |]" big literals.
   */
  public void setAllowOntologyLiteral (boolean allow) {
	this.allowOntologyLiteral = allow;
  }

  public boolean getAllowOntologyLiteral () {
	return allowOntologyLiteral;
  }

  /**
   * Set if allow "{| |}" big literals.
   */
  public void setAllowCommandLiteral (boolean allow) {
	this.allowCommandLiteral = allow;
  }

  public boolean getAllowCommandLiteral () {
 	return allowCommandLiteral;
  }

  /**
   * Set if allow start of quotes to be escaped.
   */
  public void setAllowEscapedStartQuote (boolean allow) {
	this.allowEscapedStartQuote = allow;
  }

  public boolean getAllowEscapedStartQuote () {
	return allowEscapedStartQuote;
  }

  /**
   * Set if allow end of quotes to be escaped.
   * I.e., if inside quotes the quote symbol can be escaped.
   * Default is true.
   */
  public void setAllowEscapedEndQuote (boolean allow) {
	this.allowEscapedEndQuote = allow;
  }

  public boolean getAllowEscapedEndQuote () {
	return allowEscapedEndQuote;
  }

  /**
   * Set if allow block quotes, i.e. big literals, to be escaped.
   */
  public void setAllowEscapedBlockQuote (boolean allow) {
	this.allowEscapedBlockQuote = allow;
  }

  public boolean getAllowEscapedBlockQuote () {
	return allowEscapedBlockQuote;
  }

  /**
   * Set if allow comments to be escaped.
   */
  public void setAllowEscapedComment (boolean allow) {
	this.allowEscapedComment = allow;
  }

  public boolean getAllowEscapedComment () {
	return allowEscapedComment;
  }

  /**
   * Set if allow escaped newlines to be converted to newlines.
   */
  public void setAllowEscapedNewline (boolean allow) {
	this.allowEscapedNewline = allow;
  }

  public boolean getAllowEscapedNewline () {
	return allowEscapedNewline;
  }

  /**
   * Set if allow escaped newlines in multiline quotes
   * to be converted to newlines.
   */
  public void setAllowEscapedNewlineInQuote (boolean allow) {
	this.allowEscapedNewlineInQuote = allow;
  }

  public boolean getAllowEscapedNewlineInQuote () {
	return allowEscapedNewlineInQuote;
  }

  /**
   * Set if allow escaped newlines in multiline block quotes
   * to be converted to newlines.
   */
  public void setAllowEscapedNewlineInBlockQuote (boolean allow) {
	this.allowEscapedNewlineInBlockQuote = allow;
  }

  public boolean getAllowEscapedNewlineInBlockQuote () {
	return allowEscapedNewlineInBlockQuote;
  }

  //====
  // * Set the clear-line control character.
  //====
  // public void setClearLineChar (char clearLine) {
  //	this.clearChar = clearLine;
  // }
  //====

  public String getLineSeparator () {
	return lineNewline;
  }

  public void setLineSeparator (String lineSeparator) {
	setLineNewline(lineSeparator);
  }

  /**
   * Set string to insert at end-of-line when
   * outside of quotes and pattern and ontology literals
   * (where they are treated as spaces unless escaped),
   * and outside of block quote or command literals
   * (where they are always preserved).
   * @param newline Null or empty not to preserve newlines.
   * Default = "\n".
   */
  public void setLineNewline (String newline) {
	if (newline == null) { newline = ""; };
	this.lineNewline = newline;
  }

  /**
   * Set string to insert at end-of-line when
   * escaping newlines outside of quotes, pattern and ontology literals,
   * and command literals.
   * Default = " ".
   */
  public void setLineEscapedNewline (String newline) {
	if (newline == null) { newline = ""; };
	this.lineEscapedNewline = newline;
  }

  /**
   * Set string to insert at end-of-line when
   * escaping newlines inside quotes and pattern and ontology literals.
   * Default = "\\n".
   */
  public void setQuoteEscapedNewline (String newline) {
	if (newline == null) { newline = ""; };
	this.quoteEscapedNewline = newline;
  }

  /**
   * Set string to insert at end-of-line when
   * inside quotes and pattern and ontology literals.
   * Default = " ".
   */
  public void setQuoteNewline (String newline) {
	if (newline == null) { newline = ""; };
	this.quoteNewline = newline;
  }

  /**
   * Set string to insert at end-of-line when
   * escaping newlines inside block quotes and command literals.
   * Default = "\\\\\\n".
   */
  public void setBlockQuoteEscapedNewline (String newline) {
	if (newline == null) { newline = ""; };
	this.blockQuoteEscapedNewline = newline;
  }

  /**
   * Set string to insert at end-of-line when
   * inside block quotes and command literals.
   * Default = "\\n".
   */
  public void setBlockQuoteNewline (String newline) {
	if (newline == null) { newline = ""; };
	this.blockQuoteNewline = newline;
  }

//========================================================================
// Parser stack.
//========================================================================
  protected void copyState (StatementDetector from,
		StatementDetector to, boolean move) {
	if ((from == null) || (to == null)) { return; }
	super.copyState(from, to, move);

	to.inQuote = from.inQuote;
	to.inComment = from.inComment;
	to.inBlockQuote = from.inBlockQuote;
	to.inCommandLiteral = from.inCommandLiteral;
	to.inPatternLiteral = from.inPatternLiteral;
	to.inOntologyLiteral = from.inOntologyLiteral;
	to.parenCount = from.parenCount;
	to.braceCount = from.braceCount;
	to.bracketCount = from.bracketCount;
	to.quoteChar = from.quoteChar;
	to.foundStatementsInLine = from.foundStatementsInLine;

	to.complete = from.complete;
	to.completeContext = from.completeContext;
	to.completeLineOffset = from.completeLineOffset;

	to.partial = from.partial;
	to.foundPartialContent = from.foundPartialContent;
	to.foundNonWhitespace = from.foundNonWhitespace;
	to.partialContext = from.partialContext;
	to.partialLineOffset = from.partialLineOffset;

	if (move) {
		from.foundStatementsInLine = new LinkedList<ILineContext>();
	}
  }

  public IPreprocessor saveState () {
	StatementDetector saved = new StatementDetector();
	copyState(this, saved, true);
	return saved;
  }

  public void restoreState (IPreprocessor state) {
	if (state == null) { return; }
	copyState((StatementDetector) state, this, false);
  }

//========================================================================
// Parse methods
//========================================================================

  /**
   * Reset parser state.
   */
  private void resetParserState () {
	inQuote = false;
	inComment = false;
	inBlockQuote = false;
	inCommandLiteral = false;
	inPatternLiteral = false;
	inOntologyLiteral = false;
	parenCount = 0;
	braceCount = 0;
	bracketCount = 0;
	quoteChar = 0;
	foundStatementsInLine.clear();
	resetCompleteStatement();
	resetPartialStatement();
  }

  /** 
   * Gets the next complete parse unit from the line of input.
   * The parse units are derived from the history of partial or incomplete
   * statements observed so far, appended with the input line.
   * Partitions the source history into [complete[], partial],
   * sets foundPartialContent and foundNonwhitespace,
   * and records complete[] statements,
   * where "complete" is a complete parse unit and "partial" is a partial
   * parse unit that has been scanned.
   * "foundPartialContent" indicates if partial line 
   * has non-whitespace outside of comments.
   * @return next complete parse unit, or null if there is none.
   */
  public ILineContext getParseUnitFromLine (String inputLine,
		ILineContext context) throws ParseException {
	int startPartialStatement = 0;	// Start of partial statement
					// within this line.
				// Prior "partial" is is carried in state
	int lastStatementEnd = -1;	// end of statement
	char prevChar = 0; // last char, 0 if was escaped or line is empty
	boolean prevCharWasEscaped = false;
	boolean inEscape = false;	// this character was escaped
	boolean inLineComment = false;

	boolean isQuoteOnlyMode = getQuoteOnlyMode();
	boolean startedInQuote = inQuote; // started in quote continuation

	if (inputLine == null) { return null; };
	if (inputLine.isEmpty() && (! preserveEmptyLines)) { return null; }

	int lenStr = inputLine.length();	

	for (int i = 0; i < lenStr; i++) {

		char curChar = inputLine.charAt(i);	// inputLine.toCharArray
		boolean isEscapeChar = false;
		boolean isWhitespaceChar = false;	// is whitespace
		boolean isCommentChar = false;		// in comment
		boolean isEndOfStatement = false;

		//====
		// Check for "clear line" control character
		//====
		// if (curChar == clearChar) {
		//	resetParser();
		//	return null;
		// };
		//====

		//========================================================
		// If inLineComment, check for end of line, for multi-line input
		//========================================================
		if ((inLineComment) && (curChar == '\n')) { // && (! inEscape) 
			inLineComment = false;
		}

		//========================================================
		// Check for escape
		//========================================================

		if (curChar == '\\') {
			if (! inEscape) { isEscapeChar = true; };
			if (inComment || inLineComment) {
				isCommentChar = true;
			};
		} else

		//========================================================
		// Check if still inside quote, big literal, or comment
		//========================================================

		// Check if in single-line comment
		if (inLineComment) {
			isCommentChar = true;
		} else

		// Check for end of multi-line comment
		if (inComment) {
			if ((prevChar == '*') && (curChar == '/') &&
			   (! (prevCharWasEscaped && allowEscapedComment))) {
				inComment = false;
			};
			isCommentChar = true;
		} else

		// Check for end of quote -- always allow escapes here 
		if (inQuote) {
			if ((curChar == quoteChar) && 
				 (! (inEscape && allowEscapedEndQuote))) {
				inQuote = false;
			};
		} else

		//====
		// Check for end of big literal (block quote).
		//	This is before quote or comment processing to
		//	allow arbitrary text inside command literals.
		//====
		if (inBlockQuote) {		// { preserve balance
			if ((prevChar == '>') && (curChar == '}') &&
			   (! (prevCharWasEscaped && allowEscapedBlockQuote))) {
				inBlockQuote = false;
				if (braceCount > 0) { braceCount--; };
			};
		} else

		//====
		// Check for end of command literal.
		//	This is before quote or comment processing to
		//	allow arbitrary text inside command literals.
		//====
		if (inCommandLiteral) {		// {  preserve balance
			if ((prevChar == '|') && (curChar == '}') &&
			   (! (prevCharWasEscaped && allowEscapedBlockQuote))) {
				inCommandLiteral = false;
				if (braceCount > 0) { braceCount--; };
			};
		} else

		//========================================================
		// Check for quotes
		//========================================================

		if ((curChar == '"') || (curChar == '\'')) {
			if (! (inEscape && allowEscapedStartQuote)) {
				inQuote = true;
				quoteChar = curChar;
			};
		} else

		//========================================================
		// Check for big literals that allow quotes inside them
		//========================================================

		//====
		// Check for end of pattern literal.
		//	This is after quote processing to allow
		//	quotes inside pattern and ontology literals.
		//====
		if (inPatternLiteral) {
			if ((prevChar == '>') && (curChar == '|') &&
			   (! (prevCharWasEscaped && allowEscapedBlockQuote))) {
				inPatternLiteral = false;
			};
		} else

		// Check for end of ontology literal.
		if (inOntologyLiteral) {
			if ((prevChar == '|') && (curChar == ']') &&
			   (! (prevCharWasEscaped && allowEscapedBlockQuote))) {
				inOntologyLiteral = false;
				if (bracketCount > 0) { bracketCount--; };
			};

		} else

		//========================================================
		// NOT in quote, big literal, or comment
		//========================================================

		// Check for whitespace
		if (Character.isWhitespace(curChar)) {
			isWhitespaceChar = true;

		} else switch (curChar) {

		case '/':	// Check for // single line comments
			if ((prevChar == '/') && allowSlashComment &&
			   (! (prevCharWasEscaped && allowEscapedComment))) {
				inLineComment = true;
				isCommentChar = true;
			} else {

			// Lookahead to see if this is commentChar
			if ((! (inEscape && allowEscapedComment))
				&& ((i+1) < lenStr)) {
			  char nextChar = inputLine.charAt(i+1);
			  if (((nextChar == '*') && allowMultilineComment)
			    || ((nextChar == '/') && allowSlashComment)) {
				isCommentChar = true;
			  };
			};
			};
			break;

		case '#':	// Check for # single line comments
			if (allowPoundComment &&
				  (! (inEscape && allowEscapedComment))) {
				inLineComment = true;
				isCommentChar = true;
			};
			break;

		case '*':	// Check for /* */ multi-line comments
			if ((prevChar == '/') && allowMultilineComment &&
			   (! (prevCharWasEscaped && allowEscapedComment))) {
				inComment = true;
				isCommentChar = true;
			};
			break;

		case '<':     // Check for block quote and pattern literal
			if ((prevChar == '|') && allowPatternLiteral &&
			   (! (prevCharWasEscaped && allowEscapedBlockQuote))) {
				inPatternLiteral = true;
			} else
			if ((prevChar == '{') && allowBlockQuote &&
			   (! (prevCharWasEscaped && allowEscapedBlockQuote))) {
				inBlockQuote = true;  // } preserve balance
			};
			break;

		case '|':	// Check for command and ontology literal
			if ((prevChar == '[') && allowOntologyLiteral &&
			   (! (prevCharWasEscaped && allowEscapedBlockQuote))) {
				inOntologyLiteral = true;
			} else
			if ((prevChar == '{') && allowCommandLiteral &&
			   (! (prevCharWasEscaped && allowEscapedBlockQuote))) {
				inCommandLiteral = true;  // } preserve balance
			};
			break;

		case '(':	// Check for grouping aggregates
			parenCount++;
			break;

		case ')':
			if (parenCount > 0) { parenCount--; };
			break;

		case '{':		// } preserve balance
			braceCount++;
			break;		// { preserve balance

		case '}':
			if (braceCount > 0) { braceCount--; };
			break;

		case '[':		// ] preserve balance
			bracketCount++;
			break;		// [ preserve balance

		case ']':
			if (bracketCount > 0) { bracketCount--; };
			break;

		case ';':	// Check for end-of-statement
			if (isClosedStatement()) {
				isEndOfStatement = true;
			};
			break;
		default: break;
		}

		//====
		// Add interior statement to list of parsible 
		//====
		if (isEndOfStatement) {
		    if (! isQuoteOnlyMode) {
			// Add text to partial, forming complete statement
			lastStatementEnd = i;
			String text = inputLine.substring(
				startPartialStatement,
				lastStatementEnd + 1);
			startPartialStatement = lastStatementEnd + 1;
			partial += text;
			addTextToPartialContext(context, text);

			// Flush complete, if split multiple statements
			if (splitMultipleStatementsOnLine) { 
			    if (! complete.isEmpty()) {  // Flush complete
			      addCompleteStatement(foundStatementsInLine,
					context);
			    }
			}
			complete += partial;
			addPartialToCompleteContext();
			resetPartialStatement();
		    };
		    isEndOfStatement = false;
		} else {

		//====
		// Not end of statement, see if found nonempty content
		//====
			if (! isWhitespaceChar) {
				foundNonWhitespace = true;
				if (! isCommentChar) {
					foundPartialContent = true;
				};
			};
		};

		//====
		// if (inEscape) { prevChar = 0;
		// } else { prevChar = curChar; };
		//====
		prevChar = curChar;
		prevCharWasEscaped = inEscape;
		inEscape = isEscapeChar;
	};

	//================================================================
	// End-of-line processing
	//================================================================

	boolean isEscapedNewline = false;
	String insertedNewline = lineNewline;
	String remainder = "";	// Remainder string, to add to partial
	int endStr = lenStr-1;

	if (! inLineComment) {	  // Check for escaped line continuation
		if (inEscape) {
			isEscapedNewline = true;
		};
	};

	//====
	// Check for line continuation
	//====
	if (inBlockQuote || inCommandLiteral) {
		if (isEscapedNewline && allowEscapedNewlineInBlockQuote) {
			endStr--;  // don't include trailing backslash
			insertedNewline = blockQuoteEscapedNewline;
		} else { insertedNewline = blockQuoteNewline; };
	} else if (inQuote || inPatternLiteral || inOntologyLiteral) {
		    if (isEscapedNewline && allowEscapedNewlineInQuote) {
			endStr--;  // don't include trailing backslash
			insertedNewline = quoteEscapedNewline;
		    } else { insertedNewline = quoteNewline; };
	} else {
		if (isEscapedNewline && allowEscapedNewline) {
			endStr--;  // don't include trailing backslash
			insertedNewline = lineEscapedNewline;
		} else { insertedNewline = lineNewline; };
        }

	//====
	// Add remainder after any complete statement to
	//	partial observed so far.
	// Add newline if \ last character, or preserveNewlines.
	// RESULT: Partial = previous partial + remainder so far + newline
	//====
	if (startPartialStatement <= endStr) {
		remainder = inputLine.substring(startPartialStatement,
					endStr+1);
	};
	if ((insertedNewline != null) && (! insertedNewline.isEmpty())) {
		remainder += insertedNewline;
	};
	if (! remainder.isEmpty()) {
		partial += remainder;
		addTextToPartialContext(context, remainder);
	};

	//====
	// Add partial to complete statement if not in quote or comment
	//	and isQuoteOnlyMode or partial is empty.
	// In the latter case,
	//	add any comments or newline to complete statement.
	//====
	if ((! (inQuote || inComment)) && (isQuoteOnlyMode ||
		((! foundPartialContent) &&
			(! (inBlockQuote || inCommandLiteral 
			|| inPatternLiteral || inOntologyLiteral))))) {
		complete += partial;
		addPartialToCompleteContext();
		resetPartialStatement();
	}

	//====
	// If complete is non-empty, add to output statements,
	//	unless there is partial content remaining on the line and
	//	we don't split multiple statements spanning a single line.
	//====
	if ((! complete.isEmpty()) && (! (foundPartialContent && 
			(!  splitMultipleStatementsOnLine)))) {
		addCompleteStatement(foundStatementsInLine, context);
	}

	if (! partial.isEmpty()) { partialLineOffset++; };
	if (! complete.isEmpty()) { completeLineOffset++; };

	if (foundStatementsInLine.isEmpty()) { return null; }
	return foundStatementsInLine.removeFirst();
  }

  /**
   * Adds text segment to partial context.
   * Creates new partialContext if does not exist.
   * Will create segment if on multiple lines from different contexts.
   */
  private void addTextToPartialContext (ILineContext context,
		String text) {
	if (partialContext == null) {
		partialContext = new LineContext(context, text);
	};
	if ((context != null) &&
		(! context.follows(lastPartialContext))) {
		partialContext.addSegment(context,
			text, partialLineOffset);
	}
	lastPartialContext = context;
  }

  /**
   * Adds partial to complete context.
   * Offsets partial segments by completeLineOffset.
   */
  private void addPartialToCompleteContext () {
	if (completeContext == null) {
		completeContext = partialContext; 
	} else {
		completeContext.addSegmentsFrom(partialContext,
			completeLineOffset);
	}
  }

  private void addCompleteStatement (List<ILineContext> statements,
		ILineContext context) {
	if (completeContext == null) {
		completeContext = new LineContext(context);
	}
	completeContext.setText(complete);
	completeContext.setNumberOfLines(completeLineOffset + 1);
	foundStatementsInLine.add(completeContext);
	resetCompleteStatement();
  }

  private void resetCompleteStatement () {
	complete = "";
	completeContext = null;
	completeLineOffset = 0;
  }

  private void resetPartialStatement () {
	partial = "";
	foundPartialContent = false;
	foundNonWhitespace = false;
	partialContext = null;
	partialLineOffset = 0;
  }

  public boolean isPartialStatement () {
	if ((! partial.isEmpty()) && (foundPartialContent || inComment)) {
		return true;
	};
	return false;
  }

  public boolean isClosedStatement () {
	if ((braceCount == 0) && (parenCount == 0)
			&& (bracketCount == 0)) {
		return true;
	}
	return false;
  }

  public String getPartialStatement () {
	return partial;
  }

  public void resetParser () {
	super.resetParser();
	resetParserState();
  }

  /**
   * Trims leading white space.
   */
  private String trimLeft (String str) {
	return trimLeft.matcher(str).replaceFirst("");
  }

}

//==== END OF FILE
