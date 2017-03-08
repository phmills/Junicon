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

import java.util.Arrays;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.io.IOException;

/**
 * Runs a command file given as a string, and waits for it to finish.
 * Typical usage is to run a Windows bat file contained within
 * an executable jar file.
 * An executable jar file is formed from the concatenation of
 * jarrunner.exe and a self-contained jar file without dependencies
 * that contains a manifest of the main-class to run.
 * BatRunner is intended to be invoked last, as a Java tail recursion.
 *
 * @author Peter Mills
 */
public class BatRunner {

  //====================================================================
  // Constructors
  //====================================================================
  public BatRunner () {
  }

  /**
   * Runs a command file given as a string.
   * First copies the command text to a temporary file
   * with the given filename prefix and suffix.
   * Then runs it as an ProcessBuilder call with the given arguments,
   * prepended with the given first argument if non-null.
   * @return Error code, or 0 if no errors.
   */
  public static int runCommand (String commandText, String tempPrefix,
		String tempSuffix, String argument0, String[] args) {

	if ((commandText == null) || commandText.isEmpty()) {
	    fatal("Empty command file");
	    return 1;
	}

	// Process command-line arguments: Prepend argument0 to args
	if (args == null) { args = new String[0]; }
	String[] mainArgs = args;
	if (argument0 != null) {
	    String[] first = {argument0};
	    mainArgs = concatArrays(first, args);
	}

	// Copy commandText to temporary bat file
	try {
	    Path temp = Files.createTempFile(tempPrefix, tempSuffix);
	    temp.toFile().deleteOnExit();
	    InputStream is = new ByteArrayInputStream(commandText.getBytes());
	    long numbytes = Files.copy(is, temp);
	} catch (IOException e) {
	    fatal("IO exception creating temporary bat file");
	    return 1;
	}

	// Run temporary bat file
	ProcessBuilder pb = new ProcessBuilder(mainArgs);
	pb.inheritIO();
	try {
		Process p = pb.start();
		p.waitFor();
	} catch (IOException e) {
		fatal("Error running command shell", e);
		return 1;
	} catch (InterruptedException e) {
		fatal("Interruption running command shell", e);
		return 1;
	}

	return 0;
  }

  //====================================================================
  // Main.
  //====================================================================
  /**
   * Runs a command file contained within the given jar file.
   * If the command filename is omitted, 
   * it defaults by convention to "config/bin/windows/batrunner.bat".
   * The jar filename is prepended as the first argument to the command.
   * <BR>
   * Usage: BatRunner jarFilename [commandFilename] [args]
   */
  public static void main (String[] args) {

	String usage = "Usage: BatRunner jarFilename [commandFilename] [args]"
	    + "\n\t"
	    + "Extracts commandFilename from the given jar file and runs it";

	if ((args == null) || args.length < 1) {
		exit(usage);
	}

	String jarFilename = args[0];
	String commandFilename = "config/bin/windows/batrunner.bat";
	if ((args.length > 1) && (args[1] != null) && (! args[1].isEmpty())) {
		commandFilename = args[1];
	}

	String tail = "";
	try {
		Path commandPath = Paths.get(commandFilename);
		tail = commandPath.getFileName().toString();
	} catch (InvalidPathException e) {
		exit("Invalid filename " + commandFilename);
	}
	String root = JarRunner.getFilenameWithoutExtension(tail);
	String extension = JarRunner.getFilenameExtension(tail);
	String commandText = ZipExtractor.extract(jarFilename, commandFilename);
	if (commandText == null) {
		exit("Empty command file");
	}

	runCommand(commandText, root, extension, null, args);
  }

  //====================================================================
  // Utilities.
  //====================================================================
  /**
   * Concatenate two arrays.
   */
  public static <T> T[] concatArrays (T[] first, T[] second) {
	if ((first == null) || (second == null)) { return null; }
	T[] result = Arrays.copyOf(first, first.length + second.length);
	System.arraycopy(second, 0, result, first.length, second.length);
	return result;
  }

  /**
   * Get last filename in path.
   */
  public static String getFilenameTail (String filename) {
	if (filename == null) { return null; }
	String tail = null;
	try {
		Path path = Paths.get(filename);
		tail = path.getFileName().toString();
	} catch (InvalidPathException e) {
		return null;
	}
	return tail;
  }

  //====================================================================
  // Logger.
  //====================================================================
  private static PrintStream err = System.err;

  static void fatal (String message) {
        err.println(message);
  }

  static void fatal (String message, Throwable e) {
        err.println(message);
	e.printStackTrace();
  }

  static void exit (String message) {
	fatal(message);
        System.exit(1);
  }

  static void exit (String message, Throwable e) {
	fatal(message, e);
        System.exit(1);
  }

}

//==== END OF FILE
