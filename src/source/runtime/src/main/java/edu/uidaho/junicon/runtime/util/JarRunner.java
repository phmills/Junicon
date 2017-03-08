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
import java.util.jar.*;
import java.io.PrintStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Runs the main-class in the given jar file's manifest.
 * Allows an executable jar file to be run with a classpath and dependencies.
 * This workaround is because "java -jar" ignores the user classpath.
 *
 * @author Peter Mills
 */
public class JarRunner {

  //====================================================================
  // Constructors
  //====================================================================
  public JarRunner () {
  }

  //====================================================================
  // Main.
  //====================================================================
  /**
   * Runs the main-class in the given jar file's manifest.
   * If no manifest or main-class is found, uses the tail of the jarname
   * without its dot-extension as the classname.
   * If classname is "manifest" or omitted, JarRunner uses manifest as above,
   * otherwise overrides to run that class instead.
   * <BR>
   * Usage: edu.uidaho.junicon.runtime.util.JarRunner
   *		jarfile [classname|"manifest"] [args]
   */
  public static void main (String[] args) {
	String usage = "Usage: JarRunner jarfile [classname|manifest] [args]"
	    + "\n\t"
	    + "Runs the main-class in the given jar file's manifest.";

	// Process command-line arguments
	if ((args.length < 1) || (args[0] == null)) {
		exit(usage);
	}
	String jarname = args[0];
	String classname = null;
	String[] mainArgs = new String[0];
	if (args.length > 1) {
		classname = args[1];
		if (args.length > 2) {
		    mainArgs = Arrays.copyOfRange(args, 2, args.length);
		}
	}

	// Get classname from manifest if "manifest" or omitted
	if ((classname == null) || classname.equals("manifest")) {
	  String tail = getFilenameTail(jarname);
	  classname = getFilenameWithoutExtension(tail);

	  // Get class with main method from jar manifest, if it exists
	  Manifest manifest = null;
	  try {
		JarFile jar = new JarFile(jarname);
		manifest = jar.getManifest();
	  } catch (IOException e) {
		exit("Error loading jar file: " + jarname, e);
	  }
	  if (manifest != null) {
	    Attributes attributes = manifest.getMainAttributes();
	    String mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
	    if (mainClass != null) {
		classname = mainClass;
		// fatal("Jar file mainfest does not contain a Main-Class");
	    }
	  }
	}

	// Invoke main-class with arguments
	try {
		Class<?> clazz = Class.forName(classname);
		Method method = clazz.getMethod("main", String[].class);
		method.invoke(null, (Object) mainArgs);
	} catch (ClassNotFoundException e) {
		exit("Class not found: " + classname);
	} catch (NoSuchMethodException e) {
		exit("Class does not define a main method: " + classname);
	} catch (IllegalAccessException e) {
		exit("", e.getCause());
        } catch (InvocationTargetException e) {
		exit("", e.getCause());
        }
  }

  //====================================================================
  // Utilities.
  //====================================================================

  /**
   * Gets head of filename, i.e., prefix before first dot.
   * Strips path up to "/" or "\".
   */
  public static String getFilenameTail (String filename) {
	if ((filename == null) || filename.isEmpty()) { return filename; }
	int slash = filename.lastIndexOf("/");
	int backslash = filename.lastIndexOf("\\");
	if ((slash >= 0) || (backslash >= 0)) {
		int tailpos = ((slash > backslash) ? slash : backslash) + 1;
		if (tailpos > filename.length()) {
			filename = filename.substring(tailpos);
		}
	}
	return filename;
  }

  /**
   * Get filename extension, i.e., text after last dot.
   * Returns extension, or empty if no extension, or null if filename is null.
   */
  public static String getFilenameExtension (String filename) {
	if (filename == null) { return null; }
	int pos = filename.lastIndexOf(".");
	if ((pos < 0) || (pos >= filename.length())) { return ""; }
	return filename.substring(pos+1);
  }

  /**
   * Get filename root, i.e., text before last dot.
   */
  public static String getFilenameWithoutExtension (String filename) {
	if (filename == null) { return null; }
	int pos = filename.lastIndexOf(".");
	if (pos < 0) { return filename; }
	return filename.substring(0, pos);
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
