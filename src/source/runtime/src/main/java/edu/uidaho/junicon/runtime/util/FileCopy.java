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
package edu.uidaho.junicon.runtime.util;

import java.util.Map;
import java.util.List;
import java.util.Properties;

import java.io.*;
import java.util.regex.Pattern;

/**
 * Provides methods to read and write files to and from strings,
 * and to copy files.
 */
public class FileCopy {
    private static Pattern splitPattern = Pattern.compile("\\n|\\r|\\r\\n");

    static String default_linesep = System.getProperty("line.separator", "\n");
    String linesep = default_linesep;

    String filename = null;
    InputStream resource = null;
    String contents = null;

//======================================================================
// Constructors.
//======================================================================

  public void FileCopy () {
  }

//======================================================================
// Setters for static defaults.
//	The setters are non-static for Spring dependency injection.
//======================================================================

  /**
   * Set default line separator.  Initially is line.separator system property.
   * The setters are non-static for Spring dependency injection.
   */
  public void setDefaultLineSeparator (String newline) {
	setStaticDefaultLineSeparator(newline);
  }

  /**
   * Get default line separator.
   * The setters are non-static for Spring dependency injection.
   */
  public String getDefaultLineSeparator () {
	return getStaticDefaultLineSeparator();
  }

  /**
   * Set default line separator.  Initially is line.separator system property.
   */
  public static void setStaticDefaultLineSeparator (String newline) {
	if (newline == null) { return; };
	default_linesep = newline;
  }

  /**
   * Get default line separator.
   */
  public static String getStaticDefaultLineSeparator () {
	return default_linesep;
  }

//======================================================================
// Setters for dependency injection.
//======================================================================

  /**
   * Set line separator.  Default is line.separator system property.
   */
  public String getLineSeparator () {
	return linesep;
  }

  /**
   * Get line separator.
   */
  public void setLineSeparator (String linesep) {
	if (linesep == null) { return; };
	this.linesep = linesep;
  }

//======================================================================
// Setters to read a Spring resource into this string.
// Spring resources are modeled by InputStreams, remember to close them!
//======================================================================

  /**
   * Sets the Spring resource to be read into a string.
   */
  public void setInput (InputStream resource)
		throws IOException {
	this.filename = null;
	this.resource = resource;
	this.contents = readTextFile(resource, linesep);
  }

  /**
   * Sets the filename to be read into a string.
   */
  public void setInputFile (String filename)
		throws IOException {
	this.filename = filename;
	this.contents = readTextFile(filename, linesep);
  }

  /**
   * Gets the text read in from the Spring resource.
   */
  public String getContents () {
	return contents;
  }

  /**
   * Gets the filename for the Spring resource.
   */
  public String getFilename () {
	return filename;
  }

  /**
   * Gets the text read in from the Spring resource.
   */
  public String toString () {
	return contents;
  }

//======================================================================
// Base methods to read text file into string.
//======================================================================

  /**
   * Reads a text file and returns its contents.
   * Line separators are changed to the given value.
   */
  public static String readTextFile (BufferedReader reader, String linesep)
		throws IOException
  {
	if (reader == null) { return null; };
	StringBuffer text = new StringBuffer();
	String line;

	try {
		while ((line = reader.readLine()) != null) {
			text.append(line);
			text.append(linesep);
		};
		reader.close();
	} catch (IOException e) {
		throw e;
		//====
		// throw new IOException(
		//    "Error occurred while reading text from file.", e);
		//====
	};
	return text.toString();
  }

  /**
   * Reads a text file and returns its contents.
   * Line separators are changed to the given value.
   */
  public static String readTextFile (String filename, String linesep)
		throws IOException 
  {
	if (filename == null) { return null; };
	String text = new String();
	try {
		text = readTextFile(
			new BufferedReader(new FileReader(filename)), linesep);
	} catch (FileNotFoundException e) {
		throw new IOException("File not found: " + filename, e);
	} catch (IOException e) {
		throw e;
		//====
		// throw new IOException(
		// "Error occurred while reading from file: " + filename,
		//	e.getCause());
		//====
	}
	return text;
  }

  /**
   * Reads a text file and returns its contents.
   * Line separators are changed to the given value.
   * @param in InputStream for Spring resource.
   */
  public static String readTextFile (InputStream in, String linesep)
		throws IOException 
  {
	if (in == null) { return null; };
	return readTextFile(
			new BufferedReader(new InputStreamReader(in)), linesep);
  }

  /**
   * Reads a text file and returns its contents.
   * Line separators are changed to the given value.
   */
  public static String readTextFile (File theFile, String linesep)
		throws IOException 
  {
	if (theFile == null) { return null; };
	return readTextFile(
			new BufferedReader(new FileReader(theFile)), linesep);
  }

//======================================================================
// Read text file into string.
//======================================================================

  /**
   * Reads a text file and returns its contents.
   * Line separators are changed to the default value.
   */
  public static String fileToString (BufferedReader reader)
		throws IOException
  {
	return readTextFile(reader, default_linesep);
  }

  /**
   * Reads a text file and returns its contents.
   * Line separators are changed to the default value.
   */
  public static String fileToString (String filename)
		throws IOException 
  {
	return readTextFile(filename, default_linesep);
  }

  /**
   * Reads a text file and returns its contents.
   * Line separators are changed to the default value.
   * @param in InputStream for Spring resource.
   */
  public static String fileToString (InputStream in)
		throws IOException 
  {
	return readTextFile(in, default_linesep);
  }

  /**
   * Reads a text file and returns its contents.
   * Line separators are changed to the default value.
   */
  public static String fileToString (File theFile)
		throws IOException 
  {
	return readTextFile(theFile, default_linesep);
  }

  /**
   * Reads a text file and splits it into lines.
   */
  public static String[] fileToLines (String filename, String linesep)
		throws IOException {
	String text = readTextFile(filename, default_linesep);
	if (text == null) { return null; }
	return splitPattern.split(text, -1); // keep empty lines
  }

  /**
   * Reads a text file and splits it into lines.
   */
  public static String[] fileToLines (String filename)
		throws IOException {
	return fileToLines(filename, default_linesep);
  }

//======================================================================
// Write text to file.
//======================================================================

  /**
   * Copies the contents of <code>data</code> to the given file.
   */
  public static void stringToFile (String data, File file)
	throws IOException
  {
	if ((data == null) || (file == null)) { return; };
	try {
	    FileOutputStream output = new FileOutputStream(file);
	    output.write(data.getBytes());
	    output.close();
	} catch (IOException e) {
		throw e;
		//  throw new IOException(e);
	};
  }

  /**
   * Writes a text file.
   */
  public static void stringToFile (String text, String filename)
	throws IOException
  {
	if ((text == null) || (filename == null)) { return; };
	try {
		File file = new File(filename);
		stringToFile(text, file);
	} catch (FileNotFoundException e) {
		throw new IOException("File not found: " + filename, e);
	} catch (IOException e) {
		throw e;
	}
  }

//======================================================================
// Static methods to write and copy files.
//======================================================================

  /**
   * Copies the data from <code>source</code> to <code>destination</code>.
   */
  public static void copyFile (File source, File destination) 
	throws IOException
  {
	if ((source == null) || (destination == null)) { return; };
	try {
	    FileInputStream inStream = new FileInputStream(source);
	    FileOutputStream outStream = new FileOutputStream(destination);

	    byte [] b = new byte[inStream.available()];

	    inStream.read(b);
	    outStream.write(b);
	    outStream.flush();
	    inStream.close();
	    outStream.close();
	} catch (IOException e) {
	    throw e;
	    // throw new IOException("Error occurred while copying file", e);
	}
  }

  /**
   * Saves the data in a file. If the file argument is 
   * <code>null</code>, a temp file is created.
   *
   * @param file the file or <code>null</code>
   * @param data the data to store in the file
   * @return the file
   *
   * @throws IOException
   */
  public static File saveDataToFile (File file, byte [] data)
	throws IOException
  {
	BufferedOutputStream output;

	if (data == null) { return null; };
	if (file == null) {
	    file = File.createTempFile("data", null, null);
	}

	output = new BufferedOutputStream(new FileOutputStream(file));
	output.write(data);
	output.close();

	return file;
  }

  /**
   * Reads the data in a file. 
   *
   * @param file the file or <code>null</code>
   * @return the data
   *
   * @throws IOException
   */
  public static byte[] readDataFromFile (File file)
		throws IOException
  {
	if (file == null) { return null; };

	//====
	// BufferedInputStream input = 
	//	new BufferedInputStream(new FileInputStream(file));
	//====

	FileInputStream input = new FileInputStream(file);
	byte [] data = new byte[input.available()];
	input.read(data);
	input.close();
	return data;
  }

}

//==== END OF FILE
