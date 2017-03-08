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

import java.io.*;
import java.util.*;
// import java.util.EnumSet;
// import java.util.Set;

/** 
 * The LoggerFactory class createas a logger
 * to manage error, info, and trace messages.
 *
 * @author Peter Mills
 */
public class LoggerFactory implements ILogger {

  //====
  // The shell's standard input, output, error streams
  //====
  private final RedirectOut redirectedOut = new RedirectOut(System.out);
  private final RedirectOut redirectedErr = new RedirectOut(System.err);
  public final RedirectIn in = new RedirectIn(System.in);
  public final PrintStream out = new PrintStream(redirectedOut);
  public final PrintStream err = new PrintStream(redirectedErr);

  // For error messages before creating Logger instance
  // private static PrintStream err = System.err;

  //====
  // Debug, trace, verbose enum types
  //====
  // Static debug flags
  private static EnumSet<Trace> defaultIsDebug = EnumSet.of(Trace.OFF);
  private static EnumSet<Trace> defaultIsTrace = EnumSet.of(Trace.OFF);

  // Debug flags
  private EnumSet<Trace> isDebug = defaultIsDebug.clone();
  private EnumSet<Trace> isTrace = defaultIsTrace.clone();

  // Debug streams
  private Properties streamMap = null;

  // Cache of open PrintStreams, in synchronized HashTable
  private Map<String,PrintStream> debugStreams =
	new HashMap<String,PrintStream>(); // Map of printStreams by name

  // Default factory
  private static ILogger factory = new LoggerFactory();

  //======================================================================
  // Constructor.
  //======================================================================

  /**
   * Construct a Logger object.
   * Uses System.Properties for mapping logStream names to filenames.
   */
  public LoggerFactory () {
	this(System.getProperties());
  }

  /**
   * Construct a Logger object with given map for stream names.
   */
  public LoggerFactory (Properties streamMap) {
	setStreamMap(streamMap);
  }

  //======================================================================
  // Static factory.
  //======================================================================
  /**
   * Factory to create a logger.
   */
  public static ILogger getLogger () {
	return factory;
  }

  /**
   * Factory to create a logger.
   */
  public static ILogger getLogger (Properties streamMap) {
	return new LoggerFactory(streamMap);
  }

  //====================================================================
  // Setters for static defaults.
  //    The setters are non-static for Spring dependency injection.
  //====================================================================
 
  public static void setDefaultIsDebug (EnumSet<Trace> isDebug) {
	if (isDebug == null) { return; }
	defaultIsDebug = isDebug;
  }

  public static EnumSet<Trace> getDefaultIsDebug () {
	return defaultIsDebug;
  }

  public static void setDefaultIsTrace (EnumSet<Trace> isTrace) {
	if (isTrace == null) { return; }
	defaultIsTrace = isTrace;
  }

  public static EnumSet<Trace> getDefaultIsTrace () {
	return defaultIsTrace;
  }

  //======================================================================
  // Setters for dependency injection.
  //======================================================================

  public EnumSet<Trace> getIsDebug () {
	return isDebug;
  }

  public void setIsDebug (EnumSet<Trace> isDebug) {
	if (isDebug == null) { return; }
	this.isDebug = isDebug;
  }

  public EnumSet<Trace> getIsTrace () {
	return isTrace;
  }

  public void setIsTrace (EnumSet<Trace> isTrace) {
	if (isTrace == null) { return; }
	this.isTrace = isTrace;
  }

  public boolean isDebug () {
	return isDebug.contains(Trace.ON);
  }

  public boolean isDebugDetail () {
	return isDebug.contains(Trace.DETAIL);
  }

  public boolean isTrace () {
	return isTrace.contains(Trace.ON);
  }

  public boolean isTraceDetail () {
	return isTrace.contains(Trace.DETAIL);
  }

  //======================================================================
  // Input and output stream redirection.
  //======================================================================

  public final void setIn (InputStream stream) {
	if (stream == null) { return; };
	if (stream == this.in) { return; };	// Minimalist loop prevention
	this.in.setIn(stream);
  }

  public final void setOut (OutputStream stream) {
	if (stream == null) { return; };
	if (stream == this.out) { return; };	// Minimalist loop prevention
	redirectedOut.setOut(stream);
  }

  public final void setErr (OutputStream stream) {
	if (stream == null) { return; };
	if (stream == this.err) { return; };	// Minimalist loop prevention
	redirectedErr.setOut(stream);
  }

  public final InputStream getIn () {
	return (InputStream) in;
  }

  public final PrintStream getOut () {
	return out;
  }

  public final PrintStream getErr () {
	return err;
  }

  public final void redirect (InputStream in, OutputStream out,
		OutputStream err) {
	if (in != null) { setIn(in); };
	if (out != null) { setOut(out); };
	if (err != null) { setErr(err); };
  }

  /**
   *  InputStream supporting redirection.
   */
  protected class RedirectIn extends FilterInputStream {
	public RedirectIn (InputStream in) {
		super(in);
	}

	public void setIn (InputStream in) {
		if (in == null) { return; }
		this.in = in;
	}
  }

  /**
   *  OutputStream supporting redirection.
   */
  protected class RedirectOut extends FilterOutputStream {
	public RedirectOut (OutputStream out) {
		super(out);
	}

	public void setOut (OutputStream out) {
		if (out == null) { return; }
		this.out = out;
	}
  }

  //======================================================================
  // Log to stream.
  //======================================================================

  public Properties getStreamMap () {
	return streamMap;
  }

  public void setStreamMap (Properties streamMap) {
	if (streamMap == null) { streamMap = new Properties(); };
	this.streamMap = streamMap;
  }

  public PrintStream getPrintStream (String streamName)
		throws IOException {
    if ((streamName == null) || streamName.isEmpty()) {
	return getOut();
    }
    PrintStream debugStream = null;
    synchronized (debugStreams) {
	debugStream = debugStreams.get(streamName);
	if (debugStream != null) { return debugStream; };
	String filename = streamMap.getProperty(streamName);
	if ((filename == null) || filename.isEmpty()) {
		debugStreams.put(streamName, getOut());
		return getOut();
	}
	try {
		debugStream = new PrintStream(
			new FileOutputStream(filename, true));
		debugStreams.put(streamName, debugStream);
	} catch (FileNotFoundException e) {
		throw new IOException (
			"Could not open log file: " + filename, e);
	} catch (IOException e) {
		throw new IOException (
			"IO error on log file: " + debugStream, e);
	}
    }
    return debugStream;
  }

  public void log (PrintStream debugStream, String message) 
		throws IOException {
	log(debugStream, message, 0, false);
  }

  public void log (PrintStream debugStream, String message, 
			int level, boolean timestamp)  {
	if ((debugStream == null) || (message == null)) {
		return;
	}
	if (timestamp) {
		Date now = new Date();
		debugStream.println(now.toString());
	}
	debugStream.println(message);
  }

  //======================================================================
  // Log a message.
  //======================================================================

  public void println (String message) {
	if (message == null) { return; }
	getOut().println(message);
  }

  public void println () {
	getOut().println();
  }

  public void print (String message) {
	if (message == null) { return; }
	getOut().print(message);
  }

  public void flush () {
	getOut().flush();
  }

  public void close () {
	getOut().close();
  }

  public void info (String message) {
	if (message == null) { return; }
	getErr().println(message);
  }

  public void warn (String message) {
	if (message == null) { return; }
	getErr().println(message);
  }

  public void error (String message) {
	if (message == null) { return; }
	getErr().println(message);
  }

  public void error (Throwable cause) {
	if (cause == null) { return; }
	cause.printStackTrace(getErr());
  }

  public void error (String message, Throwable cause) {
	if (message != null) { getErr().println(message); }
	if (cause != null) { cause.printStackTrace(getErr()); }
  }

  public void debug (String message) {
	if (message == null) { return; }
	if (getIsDebug().contains(Trace.ON)) { getErr().println(message); }
  }

  public void debugDetail (String message) {
	if (message == null) { return; }
	if (getIsDebug().contains(Trace.DETAIL)) { getErr().println(message); }
  }

  public void trace (String message) {
	if (message == null) { return; }
	if (getIsTrace().contains(Trace.ON)) { getErr().println(message); }
  }

  public void traceDetail (String message) {
	if (message == null) { return; }
	if (getIsTrace().contains(Trace.DETAIL)) { getErr().println(message); }
  }

  public void fatal (String message) {
	if (message != null) { getErr().println(message); }
	if (message == null) { throw new RuntimeException(); }
	throw new RuntimeException(message);
  }

  public void fatal (String message, Throwable e) {
	if (message != null) { getErr().println(message); }
	if (e != null) { e.printStackTrace(getErr()); }
	if (message == null) {
		if (e == null) { throw new RuntimeException(); }
		throw new RuntimeException(e);
	}
	if (e == null) { throw new RuntimeException(message); }
	throw new RuntimeException(message, e);
  }

  public void exit (String message) {
	if (message != null) { getErr().println(message); }
	System.exit(1);
  }

  public void exit (String message, Throwable e) {
	if (message != null) { getErr().println(message); }
	if (e != null) { e.printStackTrace(getErr()); }
	System.exit(1);
  }

}

//==== END OF FILE
