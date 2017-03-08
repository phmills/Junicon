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

import edu.uidaho.junicon.grammars.common.*;
import edu.uidaho.junicon.grammars.document.*;
import edu.uidaho.junicon.interpreter.parser.*;
import edu.uidaho.junicon.interpreter.transformer.*;
import edu.uidaho.junicon.runtime.util.ILogger;
import edu.uidaho.junicon.runtime.util.ILogger.Trace;

import java.util.Properties;
import java.io.InputStream;
import java.io.OutputStream;

// For transform
import javax.xml.transform.TransformerException;
import javax.xml.stream.XMLStreamException;

// For DOM building
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TransformInterpreter implements a base transformational interpreter 
 * that performs multi-stage transforms on its input
 * and then executes the result on a script engine substrate.
 *
 * @author Peter Mills
 */
public class TransformInterpreter extends AbstractShell 
	implements ITransformInterpreter {
	
  // Transform stylesheets
  private String normalizerText = null;		// normalize
  private String transformerText = null;	// main transform
  private String deconstructorText = null;	// output as XML
  private String formatterText = null;		// to text
  private String correlateFormatterText = null; // to XML, for correlate
  private String normalizeFormatterText = null; // for tracing
  private static String exporterText = null;	// for export

  // Transforms
  private Transform normalizer = null;		// normalize
  private Transform transformer = null;		// main transform
  private Transform deconstructor = null;	// output as XML
  private Transform formatter = null;		// to text
  private Transform correlateFormatter = null;	// to XML, for correlate
  private Transform normalizeFormatter = null;	// for tracing
  private Transform exporter = null;		// for export

  // Interpreter infrastructure
  private Object envLock = new Object();	// lock for getEnv, setEnv
  
  // Intermediate transform results
  private Document decorated = null;
  private Document normalized = null;
  private String   normalizedText = null;
  private Document transformed = null;
  private Document deconstructed = null;
  private String   formatted = null;
  private Document correlated = null;

  // Document handler to print to debug stream
  private DocumentHandler dochandler = null;	// new DocumentHandler();

  // CorrelateSource concrete syntax nodes
  private String[] concreteSyntaxNodes = { "IDENTIFIER", "LITERAL",
	"KEYWORD", "OPERATOR", "DELIMITER" };
  //====
  // Concrete syntax is carried by "@ID" attribute or text node.
  //====

  //======================================================================
  // Constructors
  //======================================================================

  /**
   * Empty constructor.
   * Delegates properties to System.getProperties().
   */
  public TransformInterpreter () throws InterpreterException {
	super( null, null, System.getProperties());
	init();
  }

  /**
   * Constructor with input and output stream redirection.
   * Delegates properties to System.getProperties().
   */
  public TransformInterpreter (InputStream in, OutputStream out, 
		OutputStream err) throws InterpreterException {
	super( null, null, System.getProperties());
	getLogger().redirect(in, out, err);
	init();
  }

  /**
   * Constructor with delegated environment and properties,
   * and input and output stream redirection.
   */
  public TransformInterpreter (IEnvironment delegateEnvironment,
		Properties defaultProperties,
		InputStream in, OutputStream out, OutputStream err)
			throws InterpreterException {
	super( null, delegateEnvironment, defaultProperties );
	getLogger().redirect(in, out, err);
	init();
  }

  /**
   * Initialize the transformational sub-interpreter.
   */
  private void init () throws InterpreterException {
	try {
		dochandler = new DocumentHandler();
	} catch (XMLStreamException e) {
		throw new InterpreterException(e);
	}
  }

  //======================================================================
  // Setters for dependency injection : transforms.
  //======================================================================

  public void setNormalizeTransform (String transform) {
	normalizerText = transform;
	normalizer = null;
	if ((transform == null) || transform.isEmpty()) { return; };
	try {
		normalizer = new Transform(transform, getCompileTransforms());
	} catch (TransformerException e) {
		printException(e);
	}
  }

  public String getNormalizeTransform () {
	return normalizerText;
  }

  public void setCodeTransform (String transform) {
	transformerText = transform;
	transformer = null;
	if ((transform == null) || transform.isEmpty()) { return; };
	try {
		transformer = new Transform(transform, getCompileTransforms());
	} catch (TransformerException e) {
		printException(e);
	}
  }

  public String getCodeTransform () {
	return transformerText;
  }

  public void setDeconstructTransform (String transform) {
	deconstructorText = transform;
	deconstructor = null;
	if ((transform == null) || transform.isEmpty()) { return; };
	try {
		deconstructor = new Transform(transform, getCompileTransforms());
	} catch (TransformerException e) {
		printException(e);
	}
  }

  public String getDeconstructTransform () {
	return transformerText;
  }

  public void setFormatTransform (String transform) {
	formatterText = transform;
	formatter = null;
	if ((transform == null) || transform.isEmpty()) { return; };
	try {
		formatter = new Transform(transform, getCompileTransforms());
	} catch (TransformerException e) {
		printException(e);
	}
  }

  public String getFormatTransform () {
	return transformerText;
  }

  public void setCorrelateFormatTransform (String transform) {
	correlateFormatterText = transform;
	correlateFormatter = null;
	if ((transform == null) || transform.isEmpty()) { return; };
	try {
		correlateFormatter = new Transform(transform, getCompileTransforms());
	} catch (TransformerException e) {
		printException(e);
	}
  }

  public String getCorrelateFormatTransform () {
	return correlateFormatterText;
  }

  public void setNormalizeFormatTransform (String transform) {
	normalizeFormatterText = transform;
	normalizeFormatter = null;
	if ((transform == null) || transform.isEmpty()) { return; };
	try {
		normalizeFormatter = new Transform(transform, getCompileTransforms());
	} catch (TransformerException e) {
		printException(e);
	}
  }

  public String getNormalizeFormatTransform () {
	return normalizeFormatterText;
  }

  public void setExportTransform (String transform) {
	exporterText = transform;
	exporter = null;
	if ((transform == null) || transform.isEmpty()) { return; };
	try {
		exporter = new Transform(transform, getCompileTransforms());
	} catch (TransformerException e) {
		printException(e);
	}
  }

  public String getExportTransform () {
	return exporterText;
  }

  //======================================================================
  // Setters for dependency injection : correlate source.
  //======================================================================

  public String[] getConcreteSyntaxNodes () {
	return concreteSyntaxNodes;
  }

  public void setConcreteSyntaxNodes (String[] nodes) {
	this.concreteSyntaxNodes = nodes;
  }

  //====================================================================
  // Interpretive loop steps: decorate, filterException.
  //====================================================================

  public void resetTransformed () {
	decorated = null;
	normalized = null;
	normalizedText = null;
	transformed = null;
	deconstructed = null;
	formatted = null;
	correlated = null;
  }

  public Document decorate (String inputSource, Document parseTree,
		ILineContext context) throws InterpreterException {
	decorated = parseTree;
	printIfTrace(inputSource, "INPUT", Trace.PREPROCESS);
	if ((inputSource == null) || (parseTree == null)) {
		return parseTree;
	}
	CorrelateSource corrSource = new CorrelateSource(inputSource,
		getLineSeparator());
	corrSource.setConcreteSyntaxNodes(concreteSyntaxNodes);
	decorated = corrSource.decoratePosition(parseTree);
	return decorated;
  }

  /**
   * Filter exception into readable format.
   * Does not print, as this is handled by the outer CommandShell.
   */
  public InterpreterException filterException (Throwable cause) {
	if (cause == null) { return null; };
	if (cause instanceof InterpreterException) {
		InterpreterException ie = (InterpreterException) cause;
		if (ie.getHaveFiltered()) { return ie; };
	};

	String inputSource = getLastParseInput();
	String substrateSource = formatted;
	if (inputSource == null) { inputSource = ""; };
	if (substrateSource == null) { substrateSource = inputSource; };
	CorrelateSource sourceCorr = new CorrelateSource(inputSource,
		getLineSeparator());
	sourceCorr.setConcreteSyntaxNodes(concreteSyntaxNodes);
	CorrelateSource substrateCorr = new CorrelateSource(
		substrateSource, getLineSeparator());	// formattedLinesep
	substrateCorr.setConcreteSyntaxNodes(concreteSyntaxNodes);
	int line = -1;
	int columnStart = -1;
	int columnEnd = -1;
			
	if (cause instanceof SubstrateException) {
		String message = cause.toString();
		SubstrateException substrateErr =
			(SubstrateException) cause;
		
		if (message == null) {
			message = "Unknown script error.";
		}; 

		String lineKey = "line ";
		int lineKeyStart = message.indexOf(lineKey);
		if (lineKeyStart >= 0) {
			lineKeyStart += lineKey.length();
			int lineKeyEnd1 = message.indexOf(",", lineKeyStart);
			int lineKeyEnd = message.indexOf("\n", lineKeyStart);
			if (lineKeyEnd1 < lineKeyEnd) { lineKeyEnd = lineKeyEnd1; };
			try {
				line = Integer.parseInt(message.substring
					(lineKeyStart, lineKeyEnd).trim());
			} catch (NumberFormatException e) {
				line = -1;
			};
		};

		line--;	// Line index origin 1; CorrelateSource 0

		
		String substrateLine = substrateCorr.getSourceLine(line);
		if (line >= 0) {	// otherwise get whole source
			if ((substrateLine != null) && (substrateLine.length() > 0)) { 
				String frontTrimmed =
					substrateLine.replaceFirst("\\s*","");
				if (columnStart < 0) {
					columnStart = substrateLine.length()
						- frontTrimmed.length();
				};
				if (columnEnd < 0) {
					String trimmed = frontTrimmed.trim();
					columnEnd = substrateLine.length() -
						(1 + frontTrimmed.length()
						- trimmed.length());
				};
			};
			if (columnStart < 0) { columnStart = 0; };
			if (columnEnd < 0) { columnEnd = 0; };
		};

		int[] span = substrateCorr.findDecoratedSpan(correlated,
			line, columnStart, line, columnEnd);
		// span is null if correlated is null

		String sourceErrorLines = null;
		int errorLine = -1;		// line # of error, origin "0"
		if (span != null) {
		    sourceErrorLines = sourceCorr.extractDecoratedLines(
			span[0], span[1], span[2], span[3]);
			errorLine = span[0];
		}
		if (sourceErrorLines == null) {
			sourceErrorLines = inputSource;
		};
		
		if (errorLine >= 0) { 
			message = "Error at line "+ (errorLine+1) + ": \n" + sourceErrorLines + "\n";
		} else {
			message = "Error at: \n" + sourceErrorLines + "\n";
		};
		if (getShowRawSubstrateErrors()) {
			message += "Source line is: \n" + substrateLine
				+ "\n";
		}
		message += substrateErr.getMessage();
		return (SubstrateException) new SubstrateException(message,
			substrateErr.getCause()).setLine(line+1).setColumn(columnStart).setHaveFiltered(true);
	};

	if (cause instanceof ParseException) {
		ParseException parseErr = (ParseException) cause; 
		line = parseErr.getLine();
		columnStart = parseErr.getColumn();
		
		line--;	// Parse line index origin 1; CorrelateSource 0

		String sourceErrorLines = sourceCorr.extractDecoratedLines(
			line, columnStart, line, columnStart);
		if (sourceErrorLines == null) {
			sourceErrorLines = inputSource;
		};
		
		String message = null;
		if (line >= 0) { 
			message = "Error at line "+ (line+1) + ": \n" + sourceErrorLines + "\n";
		} else {
			message = "Error at: \n" + sourceErrorLines + "\n";
		};

		// Strip off javacc detail of "Was expecting one of:"
		String parseMessage = parseErr.getMessage();
		int expecting = parseMessage.indexOf("Was expecting one of:");
		if (expecting >= 0) {
			parseMessage = parseMessage.substring(0, expecting);
		}

		message += parseMessage;
		return (ParseException) new ParseException(message, parseErr.getCause()).setLine(line+1).setColumn(columnStart).setHaveFiltered(true);
	};
	if (cause instanceof InterpreterException) {
		return ((InterpreterException) cause).setHaveFiltered(true);
	};
	return new ExecuteException(cause).setHaveFiltered(true);
  }
	
  //====================================================================
  // Interpretive loop steps: normalize, filter, transform, execute.
  //====================================================================
  public Document normalize (Document parseTree, ILineContext context)
		throws InterpreterException
  {
	normalized = parseTree;
	printIfTrace(parseTree, "PARSE AST", Trace.PARSE);

	if ((parseTree == null) || (normalizer == null)) { return parseTree; };

	setCurrentInterpreter(this);
	try {
		normalized = normalizer.transform(parseTree);
	} catch (TransformerException e) {
		throw new InterpreterException(e);
	}

	printIfTrace(normalized, "NORMALIZED AST", Trace.NORMALIZE);

	return normalized;
  }

  public String normalize (String inputSource,
		Document normalized, ILineContext context)
		throws InterpreterException {
	normalizedText = inputSource;

	// isNormalizeArtifact()
	if ((getJustNormalize() || isVerboseDetail()) && (normalized != null)
		  && (deconstructor != null) && (normalizeFormatter != null)) {
	    setCurrentInterpreter(this);
	    try {
	    	Document deconstruct = deconstructor.transform(normalized);
		if (deconstruct != null) {
	          normalizedText = normalizeFormatter.deconstruct(deconstruct);
	        }
	    } catch (TransformerException e) {
		throw new InterpreterException(e);
	    }
	}
	return normalizedText;
  }

  public boolean filter (String source, Document parseDom,
			ILineContext context)
    		throws InterpreterException
  {
	if ((source == null) || (parseDom == null)) { return false; };
	return true;
  }

  private String transform (String inputSource, Document parseTree, 
		ILineContext context, boolean export) 
		throws InterpreterException {
	transformed = parseTree;
	deconstructed = null;
	formatted = null;
	correlated = null;

	if ((inputSource == null) || (transformer == null) ||
			(parseTree == null)) {
		return inputSource;
	};

	setCurrentInterpreter(this);
	try {

	synchronized (transformer) {
	    //====
	    // transformer.setParameter(0, "isInterpretive",
	    //		Boolean.valueOf(getIsInterpretive()));
	    // transformer.setParameter(0, "title", (String) title);
	    //====
	    transformed = transformer.transform(parseTree);
	}
	printIfTrace(transformed, "TRANSFORMED AST", Trace.TRANSFORM);

	if ((transformed != null) && (deconstructor != null)) { 
		deconstructed = deconstructor.transform (transformed);
	};

	if (deconstructed != null) {
	    if (formatter != null) { 
		formatted = formatter.deconstruct (deconstructed);
	    }
	    if (correlateFormatter != null) {
		correlated = correlateFormatter.transform(deconstructed);
	    }
	};

	} catch (TransformerException e) {
		throw new InterpreterException(e);
	}

	printIfTrace(formatted, "SUBSTRATE CODE", Trace.ON);
	
	return formatted;	
  }
    
  public String transform (String inputSource, Document parseTree,
		ILineContext context) throws InterpreterException {
	return transform(inputSource, parseTree, context, false);
  }

  //====================================================================
  // Debug and trace support.
  //====================================================================

  /**
   * Prints text to logfile if getTrace().
   */
  private void printIfTrace(String text, String title, Trace traceType) {
	if (title == null) { title = ""; };
	if (getLogger().isTrace() ||
			getLogger().getIsTrace().contains(traceType)) {
		getLogger().info("****** START " + title + "******");
		if (text != null) {
			getLogger().info(text);
		};
		getLogger().info("****** END " + title + "******");
		getLogger().info("");
	}
  }

  /**
   * Prints document to logfile if getTrace().
   */
  private void printIfTrace(Document document, String title, Trace traceType) {
	String text = null;
	if (document != null) {
		text = dochandler.document_to_string(document);
	};
	printIfTrace(text, title, traceType);
  }

  //====================================================================
  // Environment overrides
  //====================================================================

  public Object getEnv (String name) {
	if (name == null) { return null; };
	ISubstrate substrate = getSubstrate();
	if (substrate == null) { return null; };
	Object obj = null;
	synchronized (envLock) {
	  try {
		obj = substrate.getEnv(name);
	  } catch (Exception e) { return null; }  
	};
	return obj;
  }

  public void setEnv (String name, Object value) {
	if (name == null) { return; };
	ISubstrate substrate = getSubstrate();
	if (substrate == null) { return; };
	synchronized (envLock) {
		substrate.setEnv(name, value);
	};
  }

  public String[] getEnvNames () {
	ISubstrate substrate = getSubstrate();
	if (substrate == null) { return null; };
	String[] names = null;
	synchronized (envLock) {
		names = substrate.getEnvNames().toArray(new String[0]);
	};
	return names;
  }

}

//==== END OF FILE
