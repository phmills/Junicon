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
package edu.uidaho.junicon.runtime.util;

import edu.uidaho.junicon.runtime.util.ILogger.Trace;

import java.util.EnumSet;
import java.util.Properties;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Provides logging, debugging, and tracing capabilities common to interpreters.
 * Debugging will show full error messages and stacktraces.
 * Tracing will capture the parser and transform XML stages in log files.
 *
 * @author Peter Mills
 */
public interface ILogger {

  //======================================================================
  // Constant declarations: debug, trace, verbose enum types
  //======================================================================
  public enum Trace { OFF, ON, DETAIL,
		PREPROCESS, PARSE, NORMALIZE, TRANSFORM };
  public static final EnumSet<Trace> TRACE_ALL = EnumSet.allOf(Trace.class);

  //======================================================================
  // Input and output redirection.
  //======================================================================

  /**
   * Reassigns the "standard" input stream for this interpreter
   * and its substrate.
   */
  public void setIn (InputStream in);

  /**
   * Reassigns the "standard" output stream for this interpreter
   * and its substrate.
   * Wraps the stream as a printstream.
   */
  public void setOut (OutputStream out);

  /**
   * Reassigns the "standard" error stream for this interpreter
   * and its substrate.
   * Wraps the stream as a printstream.
   */
  public void setErr (OutputStream err);

  /**
   * Returns the standard input stream for this interpreter.
   */
  public InputStream getIn ();

  /**
   * Returns the standard output stream for this interpreter.
   */
  public PrintStream getOut ();

  /**
   * Returns the standard error stream for this interpreter.
   */
  public PrintStream getErr ();

  /**
   * Reassigns the standard input, output, and error streams for this logger.
   * Will not redirect an individual stream if its parameter is null.
   */
  public void redirect (InputStream in, OutputStream out, OutputStream err);

  //======================================================================
  // Stream map to resolve filenames.
  //======================================================================
  /**
   * Set properties map used to resolve stream names to filenames.
   */
  public void setStreamMap (Properties streamMap);

  /**
   * Get properties map used to resolve stream names to filenames.
   */
  public Properties getStreamMap ();

  /**
   * Get the PrintStream associated with a logger stream name.
   * If streamName is null or empty, or not found in the streamMap,
   * uses getOut().
   * If the logger stream is already open, uses it from the cache.
   * Otherwise, will use streamName to lookup the filename
   * in the streamMap and open it.
   */
  public PrintStream getPrintStream (String streamName)
		throws IOException;

  /**
   * Log a message.
   * @param debugStream Debug stream.
   */
  public void log (PrintStream debugStream, String message) 
		throws IOException;

  /**
   * Log a message.   Optionally appends timestamp to message.
   * @param debugStream Debug stream.
   */
  public void log (PrintStream debugStream, String message, 
			int level, boolean timestamp)
		throws IOException;

  //======================================================================
  // Debug and trace options.
  //======================================================================
  /**
   * Get if debugging is on.
   */
  public EnumSet<Trace> getIsDebug ();

  /**
   * Set debugging on or off.
   */
  public void setIsDebug (EnumSet<Trace> isDebug);

  /**
   * Get if tracing is on.
   */
  public EnumSet<Trace> getIsTrace ();

  /**
   * Set tracing on or off.
   */
  public void setIsTrace (EnumSet<Trace> isTrace);

  /**
   * Get if debug is on.
   */
  public boolean isDebug ();

  /**
   * Get if debug detail is on.
   */
  public boolean isDebugDetail ();

  /**
   * Get if trace is on.
   */
  public boolean isTrace ();

  /**
   * Get if trace detail is on.
   */
  public boolean isTraceDetail ();

  //======================================================================
  // Log a message.
  //======================================================================

  /**
   * Log a message to the standard output.
   */
  public void println (String message);

  /**
   * Log an empty line to the standard output.
   */
  public void println ();

  /**
   * Log a message to the standard output.
   */
  public void print (String message);

  /**
   * Flush the logger out stream.
   */
  public void flush ();

  /**
   * Close the logger out stream.
   */
  public void close ();

  /**
   * Log an information message.
   */
  public void info (String message);

  /**
   * Log a warning message.
   */
  public void warn (String message);

  /**
   * Log an error message.
   */
  public void error (String message);

  /**
   * Log an error message with an exception.
   */
  public void error (Throwable cause);

  /**
   * Log an error message with an exception.
   */
  public void error (String message, Throwable cause);

  /**
   * Log a message if debug is on.
   */
  public void debug (String message);

  /**
   * Log a message if debug detail is on.
   */
  public void debugDetail (String message);

  /**
   * Log a message if trace is on.
   */
  public void trace (String message);

  /**
   * Log a message if trace detail is on.
   */
  public void traceDetail (String message);

  /**
   * Print message on stderr, and throw runtime exception.
   */
  public void fatal (String message);

  /**
   * Print message and stacktrace on stderr, and throw runtime exception.
   */
  public void fatal (String message, Throwable e);

  /**
   * Print message on stderr, and exit.
   */
  public void exit (String message);

  /**
   * Print message and stacktrace on stderr, and exit.
   */
  public void exit (String message, Throwable e);

}

//==== END OF FILE
