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
package edu.uidaho.junicon.grammars.junicon;

import edu.uidaho.junicon.grammars.common.*;
import edu.uidaho.junicon.grammars.junicon.*;	// Token
import edu.uidaho.junicon.runtime.util.FileCopy;
import edu.uidaho.junicon.runtime.util.ILogger;
import static edu.uidaho.junicon.grammars.junicon.ParserBaseConstants.*;
	// imports static members from the class, e.g., constants

import java.util.*;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Pattern;

import java.text.ParseException;

/** 
 * A line-by-line preprocessor for the Junicon parser.
 * The preprocessor changes the program in the following ways.
 * <UL>
 * <LI>
 * Handles directives such as $include, $define, and $undef.
 * Note that directives can end with ";", and in interactive mode must do so.
 * Provides $defineFixed directive that prevents recursive substitution.
 * <LI>
 * Performs conditional compilation for $ifdef $else $endif, and $ifndef.
 * <LI>
 * Substitutes definitions in identifier tokens.
 * <LI>
 * Inserts semicolons between lines beginning new statements,
 * if a line ends with an ender token and the next line
 * begins with a beginner token.
 * <LI>
 * Inserts braces in procedure, class, method, and initially
 * constructs if not already present.
 * <LI>
 * Fixes quotes by translating backslash escape sequences 
 * not found in Java, including hex, octal, and control escapes.
 * <LI>
 * Fixes integer numbers by truncating leading 0's
 * so as to avoid misinterpretation of them by Java as octals.
 * </UL>
 * A number of other changes to maintain compatibility with Java
 * are performed in the javacc parser and the transformer
 * instead of this preprocessor.
 * These include:
 * <UL>
 * <LI>
 * Quote continuations of the form "... _[newline] [wspace] ..."
 * are already handled in the javacc parser.
 * <LI>
 * Ebcdic translation of sequences $( $) $< $> to { } { }
 * are also handled in the javacc parser.
 * <LI>
 * Character sets (cset) for single quote literals
 * are handled by the transforms.
 * <LI>
 * Radix literals are handled by the transforms.
 * <LI>
 * Arbitrary precision for arithmetic operations
 * are handled by transforming into java.math.BigInteger and BigDecimal
 * operations.
 * </UL>
 *
 * @author Peter Mills
 */
public class JuniconPreprocessor extends AbstractPreprocessor
		implements IPreprocessor {

	// Setters for dependency injection : configuration
	private boolean isRelaxedSyntax = true;
	private boolean doSemicolonInsertion = true;
	private boolean substituteOperators = true;

	// Definitions mapping name to value. Value can be null or empty.
	private class Substitution {
		String name = "";
		String value = "";
		LinkedList<Token> tokens = null;	// null if just boolean
		boolean isFixed = false;		// no recursion
	}

	// Original definitions: must have insertion ordering via LinkedHashMap
	private Map<String, Substitution> orig_defns = new LinkedHashMap();
	// Recursively substituted definitions: insertion ordering
	private Map<String, Substitution> defns = new LinkedHashMap();

	// Parser state
	  // Directive handling state
	  private boolean ifConditionHolds = false;
	  private boolean inIf = false;
	  private boolean inElse = false;
	  private boolean prevLineEnder = false;

	  // FixSemicolon state
	  private boolean inProcedure = false;
	  private boolean inAbstract = false;
	  private boolean inMethod = false;
	  private boolean inClass = false;
	  private boolean inInitially = false;
	  private boolean afterParenthesis = false;
	  private boolean hasParenthesisInClass = false;
	  private boolean hasParenthesisInInitially = false;
	  private boolean hasBraceInInitially = false;	// only for initially
	  private boolean hasInsertedBraceInInitially = false;
	  private Token lastTokenPreviousLine = null;

	// For tokenize
	private ParserBaseTokenManager tokenmanager =
		new ParserBaseTokenManager(null);

	// Pattern to split a multiline string into individual lines.
	private Pattern splitPattern = Pattern.compile("\\n|\\r|\\r\\n");

	// Pattern to remove leading 0's in number.
	private Pattern stripLeadingZeros = Pattern.compile("^0*");

	private static final char[] hexChar = {
        	'0','1','2','3','4','5','6','7',
		'8','9','A','B','C','D','E','F' };

//========================================================================
// Statics
//========================================================================
  /**
   * List literal former.
   */
  public static <T> List<T> ListLiteral (T...elems){
	return Arrays.asList( elems );
  }

  /**
   * Set literal former.
   */
  static <T> Set<T> SetLiteral (T...elems) {
	return new HashSet<T>( ListLiteral( elems ) );
  }
  //====
  // Example: Set<String> set = Set( "a", "b", "c" );
  //====

  // Tokens that can begin statements on a new line.
  private static Set<Integer> beginners = SetLiteral(
	BREAK, CASE, CREATE, CRITICAL, DEFAULT_token, END,
	EVERY, FAIL, IF, INITIAL, LOCAL, NEXT, NOT,
	REPEAT, RETURN, STATIC, SUSPEND, UNTIL, WHILE,
	THREAD, METHOD,		// Method needed for local

	IDENTIFIER, INTEGER_LITERAL, REAL_LITERAL, 
	STRING_LITERAL, SINGLE_QUOTE_LITERAL,

	LBRACKET, LBRACE, LPAREN,

	SND, SNDBK, RCV, RCVBK,

	NEQUIV, SNE, NMNE, TILDE, LCONCAT, CONCAT, BAR,
	CARET, BACKSLASH, AT, QMARK, EQUIV,
	EQ,      // SEQ
	EQUALS,  // NMEQ
	COLONCOLON, SLASH, DOT,
	DECR,    // DIFF
	MINUS, INCR, // UNION
	PLUS, INTER, STAR, AND, BANG
  );

  // Tokens that can end statements at the end of a line.
  private static Set<Integer> enders = SetLiteral(
	BREAK, FAIL, INITIALLY, NEXT,
	RETURN, SUSPEND, 

	IDENTIFIER, INTEGER_LITERAL, REAL_LITERAL,
	STRING_LITERAL, SINGLE_QUOTE_LITERAL,

	RBRACKET, RBRACE, RPAREN,

	SND, SNDBK, RCV, RCVBK,
	BACKQUOTE	// PUNEVAL
  );

//========================================================================
// Constructors
//========================================================================
  /**
   * Constructs a class instance.
   */
  public JuniconPreprocessor() {
	super();
	resetParserState();
  }

//========================================================================
// Setters for dependency injection
//========================================================================

  /**
   * Set if keep strict unicon syntax or allow new relaxed syntax
   * with braces instead of semicolons in procedure, method, class,
   * and initially statements.
   * Strict syntax should only be used if using the preprocessor
   * as a standalone feed into legacy Unicon compilers.
   * Strict syntax is incompatible with the Junicon interpreter,
   * since the statement detector will incorrectly recognize
   * inserted semicolons as end of statement.
   * Default is relaxed.
   */
   public void setRelaxedUniconSyntax (boolean isRelaxed) {
	this.isRelaxedSyntax = isRelaxed;
   }

   public boolean getRelaxedUniconSyntax () {
	return isRelaxedSyntax;
   }

  /**
   * Set if do semicolon insertion, under both relaxed and strict syntax.
   * Default is on.
   */
   public void setDoSemicolonInsertion (boolean doSemicolonInsertion) {
	this.doSemicolonInsertion = doSemicolonInsertion;
   }

   public boolean getDoSemicolonInsertion () {
	return doSemicolonInsertion;
   }

  /**
   * Set if $define will substitute for any token, not just identifiers.
   * Default is on.
   */
   public void setSubstituteOperators (boolean onoff) {
	this.substituteOperators = onoff;
   }

   public boolean getSubstituteOperators () {
	return substituteOperators;
   }

//========================================================================
// Parser state
//========================================================================

  /**
   * Resets the parser state.
   */
  private void resetParserState() {
	ifConditionHolds = false;
	inIf = false;
	inElse = false;
	prevLineEnder = false;
	lastTokenPreviousLine = null;

	resetAfterParenthesis();
	resetClass();
	resetInitially();
  }
  //====
  // orig_defns.clear();
  // defns.clear();
  //====

  /**
   * Reset fixSemicolon state after parenthesis.
   */
  private void resetAfterParenthesis () {
	afterParenthesis = false;
	inProcedure = false;
	inAbstract = false;
	inMethod = false;
	inClass = false;
  }

  /**
   * Reset fixSemicolon state after end.
   */
  private void resetInitially () {
	inInitially = false;
	hasParenthesisInInitially = false;
	hasBraceInInitially = false;
	hasInsertedBraceInInitially = false;
  }

  /**
   * Reset fixSemicolon state after end.
   */
  private void resetClass () {
	hasParenthesisInClass = false;
  }

  public void resetParser () {
	super.resetParser();
	resetParserState();
  }

//========================================================================
// Parser stack.
//========================================================================
  protected void copyState (JuniconPreprocessor from,
		JuniconPreprocessor to, boolean move) {
	if ((from == null) || (to == null)) { return; }
	super.copyState(from, to, move);

	// Directive handling state
	to.ifConditionHolds = from.ifConditionHolds;
	to.inIf = from.inIf;
	to.inElse = from.inElse;
	to.prevLineEnder = from.prevLineEnder;

	// FixSemicolon state
	to.inProcedure = from.inProcedure;
	to.inAbstract = from.inAbstract;
	to.inMethod = from.inMethod;
	to.inClass = from.inClass;
	to.inInitially = from.inInitially;
	to.afterParenthesis = from.afterParenthesis;
	to.hasParenthesisInClass = from.hasParenthesisInClass;
	to.hasParenthesisInInitially = from.hasParenthesisInInitially;
	to.hasBraceInInitially = from.hasBraceInInitially;
	to.hasInsertedBraceInInitially = from.hasInsertedBraceInInitially;
	to.lastTokenPreviousLine = from.lastTokenPreviousLine;
  }

  public IPreprocessor saveState () {
	JuniconPreprocessor saved = new JuniconPreprocessor();
	copyState(this, saved, true);
	return saved;
  }

  public void restoreState (IPreprocessor state) {
	if (state == null) { return; }
	copyState((JuniconPreprocessor) state, this, false);
  }

//============================================================================
// Token processing
//============================================================================
  /** 
   * Preprocesses a single line of input.
   */
  public ILineContext getParseUnitFromLine (String line,
		ILineContext context) throws ParseException {
	if (line == null) { return null; };
	LinkedList<Token> tokens = null;

	getLogger().traceDetail("Preprocessor start: " + line);

	// Tokenize input line
	tokens = tokenize(line);
	if ((tokens == null) || tokens.isEmpty()) {
		// No preprocessing on empty line
		// No preprocessing on parse error -- will reincur error later
		return new LineContext(context, line);
	}
	if (getLogger().isTraceDetail()) {
	    for (Token t : tokens) {
		if (t == null) { continue; };
		String im = t.image;
		if (im == null) im = "";
		getLogger().traceDetail("Input token: " + t.image + " " +
			t.beginColumn + " " + t.endColumn );
	    }
	}

	// Handle directive
	tokens = handleDirective(tokens, context);
	if (tokens == null ) { return null; }

	// Conditional compilation
	if ((inIf && !ifConditionHolds) || (inElse && ifConditionHolds)) {
		return null;
	};

	// Apply definitions
	tokens = substituteFromDefinitions(tokens, defns);

	// Preprocess line
	tokens = fixSemicolons(tokens,	// braces for proc/class/method
			shouldAppendSemicolon(tokens));
	tokens = fixQuotes(tokens);	 // _continuation[cr ws] \escapes
	tokens = fixNumbers(tokens);	 // Translate 0n* => n*

	if (getLogger().isTraceDetail() && (tokens != null)) {
	    for (Token t : tokens) {
		if (t == null) { continue; };
		String im = t.image;
		if (im == null) im = "";
		getLogger().traceDetail("Output token: " + t.image + " " +
			t.beginColumn + " " + t.endColumn );
	    }
	}
	getLogger().traceDetail("Preprocessor end: " + tokensToString(tokens));

	String result = tokensToString(tokens);
	return new LineContext(context, result);
  }

  /**
   * Convert line to tokens using javacc-generated lexical analyzer.
   * Strip any trailing EOF token.
   * Alter begin/endcolumn slots to
   * record space before and after tokens,
   * used later to preserve spacing in token insertion.
   * @return tokens, or null on parse error.
   */
  private LinkedList<Token> tokenize (String line)
		throws ParseException {  // tokenizer generated by javacc
	if (line == null) { return null; };
	LinkedList<Token> tokens = new LinkedList<Token>();
	try {
	  InputStream is = new ByteArrayInputStream(line.getBytes());
	  // BufferedReader br = new BufferedReader(new InputStreamReader(is));
	  tokenmanager.ReInit( new JavaCharStream(is) );

	  Token token = tokenmanager.getNextToken();
	  while ((token != null) && (token.kind != EOF)) {
		tokens.add(token);
		token = tokenmanager.getNextToken();
	  }
	} catch (Throwable e) { 	// (ParseError e)
		throw asParseException(e);
	}

	// Set begin/endcolumn to space before and after token
	Token lastToken = null;
	for (Token token : tokens) {  
		token.beginColumn -= 1;
		if (lastToken != null) {
			token.beginColumn -= lastToken.endColumn;
			lastToken.endColumn = token.beginColumn;
		}
		lastToken = token;
	}
	if (lastToken != null) {    // set endColumn for last token
		lastToken.endColumn = 0;
	}
	return tokens;
  }

  /**
   * Convert tokens to string.
   * Insert whitespace between tokens, do not append newline.
   * Preserve original column positions as much as possible.
   * Token begin/endcolumn must have been set to spacing before and after.
   * Takes maximum of previous token's after and this token's before.
   */
  private String tokensToString (List<Token> tokens) {
	if ((tokens == null) || tokens.isEmpty()) { return ""; }
	StringBuilder value = new StringBuilder();
	Token lastToken = null;
	for (Token token : tokens) {
		if (token == null) { continue; }; // skip though can't happen

		// Insert space before token, max of previous end and this begin
		int pad = 0;
		if (lastToken != null) { pad = lastToken.endColumn; };
		if (token.beginColumn > pad) { pad = token.beginColumn; };
		while (pad > 0) { value.append(' '); pad--; };

		// Append token text
		if (token.image != null) { value.append(token.image); }
		lastToken = token;
	}
	if (lastToken != null) {	// insert padding
		int pad = lastToken.endColumn;
		while (pad > 0) { value.append(' '); pad--; };
	}
	return value.toString();
  }

  /**
   * Copy a token.
   */
  private Token copyToken (Token token) {
	if (token == null) { return null; };
	Token result = new Token();
	result.kind = token.kind;
	result.image = token.image;
	result.next = token.next;
	result.specialToken = token.specialToken;
	result.beginColumn = token.beginColumn;
	result.endColumn = token.endColumn;
	result.beginLine = token.beginLine;
	result.endLine = token.endLine;
	return result;
  }

  /**
   * Create a simple token.
   */
  private Token createToken (int kind, String image,
		int beforeSpace, int afterSpace) {
	if (image == null) { image = ""; };
	Token token = new Token(kind, image);
	token.beginColumn = beforeSpace;
	token.endColumn = afterSpace;
	return token;
  }

//============================================================================
// Directive processing
//============================================================================

  /**
   * Handle directive.
   * If is directive, process and return null.
   * If not directive then just return tokens (identity function).
   * Note that directives must end with ";" in interactive mode.
   */
  private LinkedList<Token> handleDirective (LinkedList<Token> tokens,
		ILineContext context) throws ParseException {
	if ((tokens == null) || tokens.isEmpty()) { return tokens; };
	Iterator<Token> iter = tokens.iterator();
	if (! (iter.hasNext() &&
			(iter.next().kind == ParserBaseConstants.DOLLAR))) {
		return tokens;
	};
	String directive = getNextDirectiveArg(iter);
	boolean isFixedDefine = false;
	switch (directive) {
	  case "else":
		inIf = false;
		inElse = true;
		break;
	  case "endif":
		inIf = false;
		inElse = false;
		break;
	  case "undef":	
		// $undef x: remove from definitions
		removeDefinition(getNextDirectiveArg(iter));
		break;
	  case "ifdef":
		// $ifdef x: if defined, ifCondition=true ; inIf = true
		ifConditionHolds = isDefined(getNextDirectiveArg(iter));
		inIf = true;
		break;
	  case "ifndef":
		// $ifndef x: flip above, inIf = true
		ifConditionHolds = ! isDefined(getNextDirectiveArg(iter));
		inIf = true;
		break;
	  case "include":
	  	// $include x
		includeFile(getNextDirectiveArg(iter), context);
		break;
	  case "error":
		// $error x
		String message = "";
		String word = "";
		while ((null != (word = getNextDirectiveOptionalArg(iter, true)))
			&& (! word.isEmpty())) {
		    message += word;
		}
		throw asParseException(message);
		// break;
	  case "line":
		// $line n filenameopt
		String linenumber = getNextDirectiveArg(iter);
		String filename = getNextDirectiveOptionalArg(iter, true);
		return tokenize("{ &line := " + "linenumber ;"
		    + (filename.isEmpty() ? "" : "&file=" + filename)
		    + "}" );
		// break;
	  case "defineFixed":
		isFixedDefine = true;
	  case "define":
		// $define x y : add to definitions
		String name = getNextDirectiveArg(iter);
		if (name.equals("in") || name.equals("new")) {
			throw asParseException(
				"Define error: reserved word: " + name);
		}

		// Take subset of tokens after $define name,
		//	and up to comment, for value.
		// Reset beginning and ending space to 0.
		LinkedList<Token> value = new LinkedList<Token>();
		Token token = null;
		Token lastToken = null;
		while (iter.hasNext() && (null != (token = iter.next()))
				&& (token.kind != SINGLE_LINE_COMMENT)
				&& (token.kind != SEMICOLON)) {
			if (lastToken == null) { token.beginColumn = 0; };
			value.add(token);
			lastToken = token;
		}
		if (lastToken != null) { lastToken.endColumn = 0; };
		addDefinition(name, value, isFixedDefine);
		break;
	  default:
		throw asParseException(
			"Parse error: invalid directive");
	}
	return null;
  }

  /**
   * Get next directive argument.
   * @return next argument.
   * @throws ParseException if no next argument.
   */
  private String getNextDirectiveArg (Iterator<Token> iter) 
		throws ParseException {
	return getNextDirectiveOptionalArg (iter, false);
  }

  /**
   * Get next directive argument.
   * @param isOptional if arg is optional
   * @return next argument, or empty string if no next argument. 
   * @throws ParseException if no next argument and one is required.
   */
  private String getNextDirectiveOptionalArg (Iterator<Token> iter,
		boolean isOptional) throws ParseException {
	String arg = null;
	if ((iter != null) && iter.hasNext()) {
		Token token = iter.next();
		if ((token.kind != SINGLE_LINE_COMMENT) 
				&& (token.kind != SEMICOLON)) {
			arg = token.image;
		}
	}
	if (arg == null) {
	   if (isOptional) {
		arg = ""; 
	   } else {
		throw asParseException(
			"Parse error: invalid directive number of arguments");
	   }
	}
	return arg;
  }

//============================================================================
// Include directive
//============================================================================

  /**
   * Includes file.  Will not re-include same file recursively.
   */
  private void includeFile (String filename, ILineContext includedFrom)
		throws ParseException {
	if ((filename == null) || filename.isEmpty()) { return; };
	ILineContext parent = includedFrom;
	while (parent != null) {	/* check context so not recurse */
		if ((parent.getFilename() != null) &&
			parent.getFilename().equals(filename)) {
		   return;
		}
		parent = parent.getFileIncludedFrom();
	}
	try {
		ILineContext context = new LineContext (filename,
			includedFrom);
		addTextToParse(FileCopy.fileToString(filename), context);
	} catch (IOException e) {
		throw asParseException(e);
	}
  }

//============================================================================
// Define directive
//============================================================================

  /**
   * Sees if is defined.
   */
  private boolean isDefined (String name) {
	if (name == null) { return false; };
	if (defns.containsKey(name)) { return true; };
	return false;
  }

  /**
   * Adds definition.
   * Substitutes new definition in previous definitions, and vice versa.
   * Each substitution is applied only once, to prevent recursion.
   */
  private void addDefinition (String name, String value, boolean noRecursion)
		throws ParseException {
	if ((name == null) || name.isEmpty()) { return; };
	if (value == null) { value = ""; };
	Substitution orig = new Substitution();
	orig_defns.put(name, orig);
	orig.name = name;
	orig.value = value;
	if (! value.isEmpty()) { orig.tokens = tokenize(value); }
	orig.isFixed = noRecursion;
	buildSubstitutedDefinition(orig);
  }

  /**
   * Adds definition for given tokens.
   * Substitutes new definition in previous definitions, and vice versa.
   * Each substitution is applied only once, to prevent recursion.
   */
  private void addDefinition (String name, LinkedList<Token> tokens,
		boolean noRecursion) {
	if ((name == null) || name.isEmpty()) { return; };
	Substitution orig = new Substitution();
	orig_defns.put(name, orig);
	orig.name = name;
	orig.value = tokensToString(tokens);
	orig.tokens = tokens;
	orig.isFixed = noRecursion;
	buildSubstitutedDefinition(orig);
  }

  /** 
   * Builds substituted definition for given entry.
   * Substitutes new definition in previous definitions, and vice versa.
   */
  private void buildSubstitutedDefinition (Substitution orig) {
	if (orig == null) { return; };
	Substitution recursed = new Substitution();
	recursed.name = orig.name;
	LinkedList<Token> tokens = orig.tokens;
	if (tokens == null) {		// Just $define, no value
		defns.put(orig.name, recursed);
		return;
	}

	LinkedList<Token> substituted = (LinkedList<Token>) tokens.clone();
	if (! orig.isFixed) {
	  // Apply other original definitions to new original
	  substituteFromDefinitions(substituted, orig_defns);

	  // Then apply new original to other substituted definitions
	  for (String name : defns.keySet()) {
		LinkedList<Token> defn_tokens = defns.get(name).tokens;
		substituteInIdentifiers(defn_tokens, name, tokens);
	  }
	}

	// Add new definition
	recursed.tokens = substituted;	// substituted definitions
	defns.put(orig.name, recursed);
  }

  /**
   * Removes definition.  Rebuilds all substituted definitions.
   */
  private void removeDefinition (String name) {
	if (name == null) { return; };
	orig_defns.remove(name);

	// rebuild all substituted definitions
	defns.clear();
	for (String key : orig_defns.keySet()) {
		buildSubstitutedDefinition(orig_defns.get(key));
	}
  }

  /**
   * Substitute definitions in identifier tokens.
   * Substitution is recursive but does not reapply definition on itself.
   * Substitution is not applied inside quotes or preprocessor directives.
   */
  public LinkedList<Token> substituteFromDefinitions (LinkedList<Token> tokens,
		Map<String, Substitution> defns) {
	if ((tokens == null) || (defns == null)) { return tokens; };
	for (String name : defns.keySet()) {
		LinkedList<Token> defn_tokens = defns.get(name).tokens;
		tokens = substituteInIdentifiers(tokens, name, defn_tokens);
	}
	return tokens;
  }
   
  /**
   * Substitute definition in identifier or operator tokens.
   * Inserts a copy of any substituted definition tokens.
   * Value can be null or empty, which deletes all occurrences of name.
   * Does not apply inside quotes.
   */
  private LinkedList<Token> substituteInIdentifiers (LinkedList<Token> tokens,
		String name, LinkedList<Token> value) {
	if ((tokens == null) || (name == null) || name.isEmpty()) {
		return tokens;		// no substitution
	}
	for (Token token : tokens.toArray(new Token[0])) {
		if (token == null) { continue; };
		if ((token.kind == IDENTIFIER) || substituteOperators) {
			String text = token.image;
			if (text.equals(name)) {
				int pos = tokens.indexOf(token);
				tokens.remove(pos);
				if ((value != null) && (! value.isEmpty())) {
				  Iterator<Token> rest = value.descendingIterator();
				  while (rest.hasNext()) {
				    Token val = rest.next();
				    if (val == null) { continue; };
				    tokens.add(pos, copyToken(val));
				  }
				}
			}
		}
	    
	}
	return tokens;
  }

//============================================================================
// Semicolon insertion
//============================================================================

  /**
   * See if need to append semicolon to previous line.
   * This holds if the previous line ended with a
   * statement-ender token and this line begins with a
   * beginner token indicating a new statement.
   */
  private boolean shouldAppendSemicolon (LinkedList<Token> tokens) {
	if (! doSemicolonInsertion) { return false; };
	if ((tokens == null) || tokens.isEmpty()) {
		prevLineEnder = false;	// for this line in next cycle
		return false;
	};

	// set thisLineBeginner
	boolean shouldAppend = false;
	boolean thisLineBeginner = false;
	Token token = tokens.getFirst();
	if ((token != null) && beginners.contains(token.kind)) {
		thisLineBeginner = true;
	}

	// set shouldAppend
	if (prevLineEnder && thisLineBeginner) {
		shouldAppend = true;
	}

	// set prevLineEnder for this line in next cycle
	prevLineEnder = false;
	token = tokens.getLast();
	if ((token != null) && enders.contains(token.kind)) {
		prevLineEnder = true;
	}
	return shouldAppend;
  }

  /**
   * Insert braces instead of semicolons after closing parameter
   * parenthesis for procedure, class, method, and initially constructs.
   * Otherwise insert semicolons at end of previous line if 
   * shouldAppendSemicolon, i.e., last line ended with an Ender token
   * and this line begins with a Beginner token that signals a new
   * statement.
   * This brings the syntax into a form 
   * compatible with MetaParser statement detection.
   * The rules for inserting braces, and not inserting semicolons,
   * are as follows:
   * <PRE>
   *	procedure id() ;|[^{] ==> procedure id() {
   *	[^abstract] method id() ;|[^{] ==> method id() {
   *	class id[supers]() [;]|[^{] ==> class id[supers]() {
   *	    subsumes: class id[supers]() ; end ==> class id[supers]() { }
   *	initially [()] ;|[^{] ==> initially () {
   *	end(after initially) ==> } }
   *	end ==> }
   * </PRE>
   * Be aware that the user may not be inserting above semicolons 
   * required by the grammar,
   * instead assuming that this preprocessor does.
   * @param tokens	List of tokens to scan
   * @param shouldAppendSemicolon	If should append semicolon
   *					after previous line and before this line
   */			// } } } } } preserve balance
  public LinkedList<Token> fixSemicolons (LinkedList<Token> tokens,
		boolean shouldAppendSemicolon) {

	if (tokens == null) { return null; };
	Token lastToken = null;
	LinkedList<Token> output = new LinkedList<Token>();

	// Strict Unicon syntax
	if (! isRelaxedSyntax) {
		if (! shouldAppendSemicolon) { return tokens; };
		output.add(createToken(SEMICOLON, ";", 0, 1));
		for (Token token : tokens) {
			if (token == null) { continue; };
			output.add(token);
			lastToken = token;
		}
		lastTokenPreviousLine = lastToken;
		return output;
	}

	// Do not prepend semicolon if relaxed syntax and
	//	after parenthesis in procedure, class, method, or initially.
	if ((afterParenthesis && (inProcedure || inClass || 
			(inMethod && (! inAbstract)) || inInitially)) ||
			(inInitially & (! hasParenthesisInInitially))) {
		shouldAppendSemicolon = false;
	}
	if (shouldAppendSemicolon) {
		output.add(createToken(SEMICOLON, ";", 0, 1));
	}

	// Relaxed Unicon syntax, with braces
	for (Token token : tokens) {
	    if (token == null) { continue; };	// skip tho can't happen

	    if (inClass && (! hasParenthesisInClass)) {
	      if (token.kind == LBRACE) {  // no ()
		//====
		// For class: if no (), insert it.
		// Only for relaxed: class D {}.
		//====
		output.add(createToken(LPAREN, "(", 1, 0)); // )(
		output.add(createToken(RPAREN, ")", 0, 1));
		afterParenthesis = true;
		hasParenthesisInClass = true;
	      } else if (token.kind == LPAREN) {
		hasParenthesisInClass = true;
	      }
	    }

	    if (inInitially && (! hasParenthesisInInitially)) {
	      if (token.kind != LPAREN) {  // no ()
		//====
		// For initially: if no (), insert it
		//====
		output.add(createToken(LPAREN, "(", 1, 0)); // )(
		output.add(createToken(RPAREN, ")", 0, 1));
		afterParenthesis = true;
	      }
	      hasParenthesisInInitially = true;
	    }

	    if (afterParenthesis && (inProcedure || inClass || 
			(inMethod && (! inAbstract)) ||
			(inInitially && (! hasBraceInInitially)))) {
		//====
		// Look at token after "procedure/class/method id()"
		// Change:     ; ^{ other ==> { other
		// Four cases: ; { other (if shouldAppendSemicolon && first)
		// // }}} preserve balance
		//====
		if (token.kind != LBRACE) {
		   if (token.kind == SEMICOLON) {  // change to brace
			token.kind = LBRACE;
			token.image = "{";	   // } preserve balance
		   } else {
			output.add(createToken(LBRACE, "{", 1, 1)); // }
		   }
		   if (inInitially) { hasInsertedBraceInInitially = true; }
		}
		if (inInitially) { hasBraceInInitially = true; }
	    }

	    if (afterParenthesis) { resetAfterParenthesis(); };

	    switch (token.kind) {
	      case RPAREN:	afterParenthesis = true;
				break;
	      case PROCEDURE:	inProcedure = true;
				break;
	      case ABSTRACT:	inAbstract = true;
				break;
	      case METHOD:	inMethod = true;
				break;
	      case CLASS:	inClass = true;
				resetClass();
				resetInitially();
				break;
	      case INITIALLY:	inInitially = true;
				break;
	      case END:		token.kind = RBRACE; // { preserve balance
				token.image = "}";
				if (inInitially) {  // add another brace
				  if (hasInsertedBraceInInitially) {
				    output.add(token);
				    token = copyToken(token);
				    // tokens.add(tokens.indexOf(token)
				    //		+ 1, copyToken(token))
				  }
				  resetInitially();
				};
				break;
	      default:		break;
	    }

	  output.add(token);
	  lastToken = token;
	}
	lastTokenPreviousLine = lastToken;
	return output;
  }

//============================================================================
// Fix quotes and numbers
//============================================================================

  /**
   * Fix \escapes in quotes to be compatible with Java.
   * Underscore quote continuations were already fixed in 
   * the quote fuser before this (quoteOnlyMode StatementDetector).
   */
  public LinkedList<Token> fixQuotes (LinkedList<Token> tokens) {
	if (tokens == null) { return null; }
	for (Token token : tokens) {
		if ((token == null) || (! ((token.kind == STRING_LITERAL)
				|| (token.kind == SINGLE_QUOTE_LITERAL)))) {
			continue;
		}
		if (token.image == null) { continue; };
		token.image = fixQuoteEscapes(token.image);
	}
	return tokens;
  }

  /**
   * Fix octal numbers to be compatible with Java
   * by translating 0n* to n* in integer and floating point numbers.
   */
  public LinkedList<Token> fixNumbers (LinkedList<Token> tokens) {
	if (tokens == null) { return null; }
	for (Token token : tokens) {
		if ((token == null) || (! ((token.kind == INTEGER_LITERAL)
				|| (token.kind == REAL_LITERAL)))) {
			continue;
		}
		if (token.image == null) { continue; };
		if (token.image.startsWith("0")) {	// strip leading 0's
		  token.image =
			stripLeadingZeros.matcher(token.image).replaceFirst("");
		  if (token.image.isEmpty() || token.image.startsWith(".")) {
			// oops - stripped away even a single 0 or 0.3
			token.image = "0" + token.image;
		  }
		}
	}
	return tokens;
  }

  /**
   * Translate a Unicon string to a Java string.
   * Must translate backslash escape sequences:
   * hexadecimal "\xdd" => unicode "\u00dd",
   * control character "\^c" (where @ <= c <= _) => unicode equivalent.
   * Other escapes such as octal "\ddd" and "\n" are are preserved.
   * Any unrecognized escape such as "\a" is left as "a".
   */
  private String fixQuoteEscapes (String inputLine) {
	char prevChar = 0; // last char, 0 if was escaped or line is empty
	boolean inEscape = false;
	boolean inQuote = false;
	char quoteChar = 0;	// which quote character found
	boolean inControlCode = false;	// have \^c escape code to output

	if ((inputLine == null) || inputLine.isEmpty()) { return inputLine; };
	int lenStr = inputLine.length();	
	StringBuilder output = new StringBuilder();

	for (int i = 0; i < lenStr; i++) {

	    char curChar = inputLine.charAt(i);	// inputLine.toCharArray
	    boolean isEscapeChar = false;

	    //====
	    // Check for escape.
	    // Escapes will reset prevchar to 0 before next pass.
	    //====
	    if (inQuote) {
	      if (inEscape) {	// Handle \x \n \^ \fnl otherwise output
		  if ((curChar >= '0') && (curChar <= '9')) { // Java octal escape
		    output.append('\\');
		    output.append(curChar);
		  } else switch (curChar) {
		    case 'b':	// Java escapes, so preserve them
		    case 'f':
		    case 'n':
		    case 'r':
		    case 't':
		    case '\'':
		    case '\"':
		    case '\\':
			output.append('\\');
			output.append(curChar);
			break;
		    case 'd':
			output.append("\\u007F");	// delete
			break;
		    case 'e':
			output.append("\\u001B");	// escape
			break;
		    case 'l':
			output.append("\\n");	// linefeed == newline
			break;
		    case 'v':
			output.append("\\u000B");	// vertical tab
			break;
		    case 'x':			
			output.append("\\u00");	// \xdd hex => \u00dd
			// Lookahead to pad unicode for hex \xd
			if ((i+1) < lenStr) {
			   if (isHex(inputLine.charAt(i+1))) {
				if (! (((i+2) < lenStr) &&
					isHex(inputLine.charAt(i+2)))) {
				    output.append("0");
				}
			   }
			}
			break;
		    case '^':			// \^c control char
			inControlCode = true;
			break;
		    default:
			output.append(curChar);	// \a => a
			break;
		  }
	      } else if (inControlCode) {
		char ctl = Character.toUpperCase(curChar);
		if ((ctl >= '@') && (ctl <= '_')) {
			ctl -= 0x40;
			output.append("\\u00");
			output.append(hexChar[(ctl >> 4) & 0xF]);
			output.append(hexChar[ctl & 0xF]);
		}; 
		//====
		// else if (ctl == '?') {	// delete
		//	output.append("\\u0074"); }
		//====
		inControlCode = false;
	      } else {
		switch (curChar) {    // Check for escape or end of quote
		  case '\\':
			isEscapeChar = true; // do not output this now
			break;
		  case '"':
		  case '\'':
			if (curChar == quoteChar) { inQuote = false; };
			output.append(curChar);
			break;
		  default:
			output.append(curChar);
			break;
		}
	      }
	    } else switch (curChar) {	// Check for quotes
		case '"':
		case '\'':
			inQuote = true;
			quoteChar = curChar;
		default:
			output.append(curChar);
			break;
	    }
			
	    if (inEscape) {
		prevChar = 0;
	    } else {
		prevChar = curChar;
	    };
	    inEscape = isEscapeChar;
	}
	return output.toString();
  }

  /**
   * Test if character is hexadecimal.
   */
  private boolean isHex (char c) {
	  return ( ((c >= '0') && (c <= '9')) ||
		((c >= 'a') && (c <= 'f')) ||
		((c >= 'A') && (c <= 'F')));
  }

}

//==== END OF FILE
