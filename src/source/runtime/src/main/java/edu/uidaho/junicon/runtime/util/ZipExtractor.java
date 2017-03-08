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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.IOException;

import java.util.stream.Stream;
import java.util.function.*;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/**
 * Extracts files from a zip archive.
 *
 * @author Peter Mills
 */
public class ZipExtractor {

  //====================================================================
  // Constructors
  //====================================================================
  public ZipExtractor () {
  }

  //====================================================================
  // Extractor.
  //====================================================================
  /**
   * Extracts files with a given prefix from a zip archive
   * to a destination directory.
   * Returns the path of the destination directory, or null on error.
   */
  public static Path extract (String zipFilename, String classname,
		String destination) {
    if ((zipFilename == null) || zipFilename.isEmpty() ||
		(classname == null) || classname.isEmpty() ||
		(destination == null) || (destination.isEmpty())) {
	return null;
    }
    try {
	Path destDir = Paths.get(destination);
	if (! Files.exists(destDir)) {
		Files.createDirectory(destDir);
	}

	ZipFile zip = new ZipFile(zipFilename);
	Stream<? extends ZipEntry> zipstream = zip.stream();
	Path prefix = Paths.get(classname);

	zipstream.filter( (ZipEntry z) -> {
		Path zpath = Paths.get(z.getName());
		return zpath.startsWith(prefix);
	} ).forEachOrdered ( (ZipEntry z) -> {
	    try {
		Path net = destDir.resolve(z.getName());
		if (z.isDirectory()) {
			Files.createDirectories(net);
		} else {
			InputStream is = zip.getInputStream(z);
			Files.copy(is, net);
			is.close();
		}
	    } catch (IOException e) {
		throw new UncheckedIOException(e);
	    }
	} );
	return destDir;
    } catch (InvalidPathException e) {
	fatal("Invalid filename format", e);
	return null;
    } catch (UncheckedIOException e) {
	fatal("IOException unzipping archive", e);
	return null;
    } catch (IOException e) {
	fatal("IOException unzipping archive", e);
	return null;
    }
  }

  /**
   * Extracts a single file from a zip archive to a string.
   * Returns null on error.
   */
  public static String extract (String zipFilename, String classname) {
    if ((zipFilename == null) || zipFilename.isEmpty() ||
		(classname == null) || classname.isEmpty()) {
	return null;
    }
    try {
	ZipFile zip = new ZipFile(zipFilename);
	ZipEntry z = zip.getEntry(classname);
	InputStream is = zip.getInputStream(z);
	return FileCopy.fileToString(is);
    } catch (IOException e) {
	return null;
    }
  }

  //====================================================================
  // Main.
  //====================================================================
  /**
   * Extracts files with the given prefix from a zip archive to a directory.
   * <BR>
   * Usage: ZipExtractor zipFilename classname outputDirectory
   */
  public static void main (String[] args) {
	String usage = "Usage: ZipExtractor jarFilename classname outputDirectory"
	    + "\n\t"
	    + "Extracts files with the given prefix from a zip archive.";

	// Process command-line arguments
	if (args.length < 3) {
		exit(usage);
	}
	String jarFilename = args[0];
	String classname = args[1];
	String outputDirectory = args[2];

	extract(jarFilename, classname, outputDirectory);
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
