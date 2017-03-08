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
package edu.uidaho.junicon.runtime.junicon.operators;

import edu.uidaho.junicon.runtime.junicon.iterators.*;
import edu.uidaho.junicon.runtime.junicon.constructs.IconScan;
import edu.uidaho.junicon.runtime.junicon.constructs.IconCoExpression;
import edu.uidaho.junicon.runtime.junicon.constructs.IconPromote;
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;

import java.util.Collections;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.io.Reader;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import static java.sql.Types.INTEGER;

/**
 * Defines the built-in Unicon functions.
 * The built-in functions are exposed as method references.
 * <P>
 * USAGE: When compiling to Java: import static UniconFunctions.*;
 *
 * @author Rob Kleffner
 * @author Peter Mills
 */
public class UniconFunctions {

  //====
  // Input-output functions.
  //====
  public static VariadicFunction flush = UniconFunctions::flush;
  public static VariadicFunction getch = UniconFunctions::getch;
  public static VariadicFunction getche = UniconFunctions::getche;
  public static VariadicFunction read = UniconFunctions::read;
  public static VariadicFunction reads = UniconFunctions::reads;
  public static VariadicFunction write  = UniconFunctions::write;
  public static VariadicFunction writes = UniconFunctions::writes;
  
  //====
  // File functions.
  //====
  public static VariadicFunction close = UniconFunctions::close;
  public static VariadicFunction open = UniconFunctions::open;
  public static VariadicFunction remove = UniconFunctions::remove;
  public static VariadicFunction rename = UniconFunctions::rename;
  public static VariadicFunction seek = UniconFunctions::seek;
  public static VariadicFunction where = UniconFunctions::where;
  
  //====
  // Database functions.
  //====
  public static VariadicFunction sql = UniconFunctions::sql;
  public static VariadicFunction fetch = UniconFunctions::fetch;

  //====
  // Reflective functions.
  //====
  public static VariadicFunction copy = UniconFunctions::copy;
  public static VariadicFunction image = UniconFunctions::image;
  public static VariadicFunction type = UniconFunctions::type;

  //====
  // System functions.
  //====
  public static VariadicFunction collect = UniconFunctions::collect;
  public static VariadicFunction delay = UniconFunctions::delay;
  public static VariadicFunction exit = UniconFunctions::exit;
  public static VariadicFunction runerr = UniconFunctions::runerr;
  public static VariadicFunction stop = UniconFunctions::stop;
  public static VariadicFunction system = UniconFunctions::system;
  
  //==========================================================================
  // Input-output functions.
  //==========================================================================

  /**
   * Flushes any accumulated output for file f.
   * IMPLEMENTATION NOTE: currently write and writes always flush their
   * output, since RandomAccessFile does not have a flush method
   * and BufferedWriter is really only used for standard output, which
   * should be flushed on every line written for the expected semantics.
   * @return The number of bytes written
   */
  public static Object flush (Object... args) {
      if (args == null || args.length == 0) { return FAIL; }
      try {
          if (args[0] instanceof OutputStream) {
              ((OutputStream) args[0]).flush();
          }
          return 0;
      } catch (IOException e) {
          throw new RuntimeException("Error code 214: input/output error");
      }
  }
  
  /**
   * Produces the next character entered from the keyboard as a one-character string.
   * The character is not displayed. Fails on end of file.
   * @return A single character string
   */
  public static Object getch (Object... args) {
      try {
          int result = IconKeywords.input.read();
          if (result == -1) { return FAIL; }
          return Character.toChars(result);
      } catch (IOException e) {
          throw new RuntimeException("Error code 214: input/output error");
      }
  }
  
  /**
   * Produces the next character entered from the keyboard as a one-character string.
   * The character is displayed. Fails on end of file.
   * @return A single character string
   */
  public static Object getche (Object... args) {
      if (args == null || args.length == 0) { return FAIL; }
      try {
          int result = IconKeywords.input.read();
          if (result == -1) { return FAIL; }
          System.out.println(Character.toChars(result));
          return Character.toChars(result);
      } catch (IOException e) {
          throw new RuntimeException("Error code 214: input/output error");
      }
  }
  
  /**
   * Produces the next line from file f. Fails on end of file.
   * When called with no argument, will read from standard input.
   * @return A line of input or fail
   */
  public static Object read (Object... args) {
      // default to standard input
      try {
          if (args == null || args.length == 0) {
              String result = IconKeywords.input.readLine();
              if (result == null) { return FAIL; }
              return result;
          }

          if (args[0] instanceof RandomAccessFile) {
              String result = ((RandomAccessFile) args[0]).readLine();
              if (result == null) { return FAIL; }
              return result;
          }
          if (args[0] instanceof BufferedReader) {
              String result = ((BufferedReader) args[0]).readLine();
              if (result == null) { return FAIL; }
              return result;
          }
      } catch (IOException e) {
          throw new RuntimeException("Error code 212: file not open for reading");
      }
      throw new RuntimeException("Error code 105: not a file");
  }
  
  /**
   * Produces a string consisting of the next i characters from f, or the
   * remaining characters if fewer than i remain. Fails on end of file.
   * f defaults to standard input, while i defaults to 1.
   * USAGE: reads(f,i)
   * @return A string of i or fewer characters
   */
  public static Object reads (Object... args) {
      try {
          // no arguments? just read from stdin
          if (args == null || args.length == 0) {
              int ch = IconKeywords.input.read();
              if (ch == -1) { return FAIL; }
              return Character.toChars(ch);
          }
          
          // read from file
          if (args[0] instanceof RandomAccessFile) {
              int length = 1;
              // get length to read
              if (args.length > 1) {
                  Number r = IconNumber.toNumber(args[1]);
                  if (r == null) {
                      throw new RuntimeException("Error code 101: not integer");
                  }
                  length = r.intValue();
                  if (length <= 0) {
                      throw new RuntimeException("Error code 205: i <= 0");
                  }
              }
              
              byte[] buffer = new byte[length];
              int result = ((RandomAccessFile) args[0]).read(buffer);
              if (result == -1) { return FAIL; }
              return String.valueOf(buffer);
          }
          
          // attempt to read specified number of bytes from stdin
          Number r = IconNumber.toNumber(args[1]);
          if (r == null) {
              throw new RuntimeException("Error code 101: not integer");
          }
          if (r.intValue() <= 0) {
              throw new RuntimeException("Error code 205: i <= 0");
          }
          char[] buffer = new char[r.intValue()];
          int result = IconKeywords.input.read(buffer);
          if (result == -1) { return FAIL; }
          return String.valueOf(buffer);
          
      } catch (IOException e) {
          throw new RuntimeException("Error code 212: file not open for reading");
      }
  }
  
  /**
   * Write out arguments, and print newline.
   * If args is null, just prints newline.
   * @return The last item written out, defaults to empty string
   */
  public static Object write (Object... args) {
        Object last = "";
        // default writing to stdout
        OutputStream file = IconKeywords.output;
	RandomAccessFile rfile = null;
	boolean isRandom = false;
        try {
            if (args != null && args.length > 0) {
                for (Object i : args) {
                    // is argument a file? redirect further output to it
                    if (i instanceof RandomAccessFile) {
                        rfile = (RandomAccessFile) i;
			isRandom = true;
                        rfile.writeBytes("\n");
                        continue;
                    }
                    if (i instanceof OutputStream) {
                        file = (OutputStream) i;
			isRandom = false;
                        // ((BufferedOutputStream) file).newLine();
			file.write('\n');
                        file.flush();
                        continue;
                    }
                    if (i instanceof Reader) {
                        throw new RuntimeException("Error code 212: file not open for reading");
                    }

                    // not a file, write it out
		    if (isRandom) {
                        rfile.writeBytes(i.toString());
                    } else {
                        file.write(i.toString().getBytes(
				IconNumber.getDefaultCharset()));
                        file.flush();
                    }

                    last = i;
                }
                
		if (isRandom) {
			rfile.writeBytes("\n");
                } else {
			file.write('\n');
			file.flush();
                }
            } else {
		file.write('\n');
                file.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error code 212: file not open for reading");
        }
	return last;
  }
  
  /**
   * Write out arguments, without printing newline.
   * @return The last item written out, defaults to empty string
   */
  public static Object writes (Object... args) {
	Object last = "";
        // default writing to stdout
        OutputStream file = IconKeywords.output;
	RandomAccessFile rfile = null;
	boolean isRandom = false;
        try {
            if (args != null && args.length > 0) {
                for (Object i : args) {
                    // is argument a file? redirect further output to it
                    if (i instanceof RandomAccessFile) {
                        rfile = (RandomAccessFile) i;
			isRandom = true;
                        continue;
                    }
                    if (i instanceof OutputStream) {
                        file = (OutputStream) i;
			isRandom = false;
                        continue;
                    }
                    if (i instanceof Reader) {
                        throw new RuntimeException("Error code 212: file not open for reading");
                    }

                    // argument not a file, write it out
		    if (isRandom) {
                        rfile.writeBytes(i.toString());
                    } else {
                        file.write(i.toString().getBytes(
				IconNumber.getDefaultCharset()));
                        file.flush();
                    }

                    last = i;
                }
            } else {
		file.write('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException("Error code 212: file not open for reading");
        }
	return last;
  }
  
  //==========================================================================
  // File functions.
  //==========================================================================
  
  /**
   * Changes the current directory to s but fails if there is no such directory
   * or if the change cannot be made.
   * NOTE: NOT YET IMPLEMENTED
   * @return TODO: only returns fail currently
   */
  public static Object chdir (Object... args) {
      // TODO: find a way to emulate chdir simply and efficiently
      return FAIL;
  }
  
  /**
   * Closes the file f.
   * @return The closed file handle
   */
  public static Object close (Object... args) {
      if (args == null || args.length == 0) { return FAIL; }
      try {
          if (args[0] instanceof RandomAccessFile) {
              ((RandomAccessFile) args[0]).close();
              return args[0];
          }
          if (args[0] instanceof OutputStream) {
              ((OutputStream) args[0]).close();
              return args[0];
          }
          if (args[0] instanceof Reader) {
              ((Reader) args[0]).close();
              return args[0];
          }
          if (args[0] instanceof Connection) {
              ((Connection) args[0]).close();
              return args[0];
          }
          throw new RuntimeException("Error code 105: not file");
      } catch (IOException | SQLException e) {
          throw new RuntimeException("Error code 214: input/output error");
      }
  }
  
  /**
   * Produces a file resulting from opening the path p with options o.
   * Fails if the file cannot be opened.
   * Options for opening the file are:
   * - "r": open for reading
   * - "w": open for writing
   * - "a": open for writing in append mode
   * - "b": open for reading and writing
   * - "p": open a pipe
   * - "c": create
   * - "t": translate line termination sequences to linefeeds
   * - "u": do not translate line termination sequences to linefeeds
   * - "o": opens a SQL database connection
   * The default mode is to translate line termination sequences to linefeeds
   * on input, and vice-versa on output.
   * USAGE: open(p,o)
   * @return The opened file, or fail.
   */
  public static Object open (Object... args) {
      if (args == null || args.length == 0) { return FAIL; }
      if (!(args[0] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: not string");
      }

      // acquire the options
      String option = "r";
      if (args.length > 1) {
          if (args[1] instanceof CharSequence) {
              option = args[1].toString();
          } else {
              throw new RuntimeException("Error code 103: not string");
          }
      }
      
      // TODO: support Unicon options
      if (!option.matches("[abcdgnmoprtuw]|n(a|au|l|u)|gl")) {
          throw new RuntimeException("Error code 209: invalid option");
      }
      
      try {
          // TODO: perhaps there is a better way to match Icon/Unicon semantics
          // write only is not supported by RandomAccessFile, but BufferedWriter
          // has no seek method. RandomAccessFile has no append mode, and while
          // BufferedWriter has an append method, we would need to track whether
          // the BufferedWriter was created in append mode or write only mode.
          if (option.contains("r")) {
              return new RandomAccessFile(args[0].toString(), "r");
          }
          if (option.contains("w")) {
              return new RandomAccessFile(args[0].toString(), "rwd");
          }
          if (option.contains("a")) {
              RandomAccessFile raf = new RandomAccessFile(args[0].toString(), "rwd");
              raf.seek(raf.length());
              return raf;
          }
          if (option.contains("b")) {
              return new RandomAccessFile(args[0].toString(), "rwd");
          }
          
          // MySQL database support
          if (option.contains("o")) {
              if (args.length < 4) {
                  throw new RuntimeException("Error code 103: not string");
              }
              if (!(args[2] instanceof CharSequence) || !(args[3] instanceof CharSequence)) {
                  throw new RuntimeException("Error code 103: not string");
              }
              Class.forName("com.mysql.jdbc.Driver");
              return DriverManager.getConnection(args[0].toString() + "?user=" + args[2].toString() + "&password=" + args[3].toString());
          }
          
          // TODO: support other Icon and Unicon options
          return FAIL;
      } catch (IOException | SQLException | ClassNotFoundException e) {
          throw new RuntimeException("Error code 214: input/output error " + e.getLocalizedMessage());
      }
  }
  
  /**
   * Seeks to position i in file f. Fails if the seek cannot be performed.
   * The first byte in the file is at position 1. i = 0 seeks to the end
   * of the file.
   * @param args
   */
  public static Object seek (Object... args) {
      if (args == null || args.length == 0) { return FAIL; }
      if (!(args[0] instanceof RandomAccessFile)) {
          if (args[0] instanceof OutputStream || args[0] instanceof Reader) {
              throw new RuntimeException("Error code 214: input/output error");
          }
          throw new RuntimeException("Error code 105: not file");
      }

      Number r = IconNumber.toBigNumber(args[1]);
      if (r == null) {
          throw new RuntimeException("Error code 101: not integer");
      }
      
      try {
          ((RandomAccessFile) args[0]).seek(r.longValue());
          return args[0];
      } catch (IOException e) {
          throw new RuntimeException("Error code 214: input/output error");
      }
  }
  
  /**
   * Deletes the file named s. Fails if s cannot be removed.
   * @return null
   */
  public static Object remove (Object... args) {
      if (args == null || args.length == 0) { return FAIL; }
      if (!(args[0] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: not string");
      }
      
      try {
          Files.delete(FileSystems.getDefault().getPath(args[0].toString()));
      } catch (IOException e) {
          return FAIL;
      }
      return null;
  }
  
  /**
   * Renames the file s1 to have name s2. Fails if the renaming cannot be done.
   * @return null
   */
  public static Object rename (Object... args) {
      if (args == null || args.length < 2) { return FAIL; }
      if (!(args[0] instanceof CharSequence) || !(args[1] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: not string");
      }
      
      try {
          Path s1 = FileSystems.getDefault().getPath(args[0].toString());
          Path s2 = FileSystems.getDefault().getPath(args[1].toString());
          Files.move(s1, s2);
      } catch (IOException e) {
          return FAIL;
      }
      
      return null;
  }
  
  /**
   * Produces the current byte position in file f. The first in the file is
   * at position 1.
   * @return An integer
   */
  public static Object where (Object... args) {
      if (args == null || args.length == 0) { return FAIL; }
      if (!(args[0] instanceof RandomAccessFile)) {
          if (args[0] instanceof OutputStream || args[0] instanceof Reader) {
              throw new RuntimeException("Error code 214: input/output error");
          }
          throw new RuntimeException("Error code 105: not file");
      }
      try {
          return ((RandomAccessFile) args[0]).getFilePointer() + 1;
      } catch (IOException e) {
          throw new RuntimeException("Error code 214: input/output error");
      }
  }

  //==========================================================================
  // Database functions.
  //==========================================================================
  
  /**
   * Submit a query on the given database connection.
   * @return The result set if there is one, null otherwise.
   */
  public static Object sql (Object... args) {
      if ((args == null) || (args.length < 2)) { return FAIL; }
      
      if (!(args[0] instanceof Connection)) {
          throw new RuntimeException("Error code 105: not database file");
      }
      if (!(args[1] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: string expected");
      }
      
      try {
          Statement stmt = ((Connection) args[0]).createStatement();
          if (stmt.execute(args[1].toString())) {
              ResultSet rs = stmt.getResultSet();
              stmt.closeOnCompletion();
              return rs;
          } else {
              stmt.close();
              return null;
          }
      } catch (SQLException e) {
          throw new RuntimeException("Error code 214: input/output error " + e.getLocalizedMessage());
      }
  }
  
  /**
   * Fetch the next row from the given database query result set. If the optional
   * second argument is given, it is used as the key to select a specific row.
   * @return The row as a LinkedHashMap
   */
  public static Object fetch (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      
      if (!(args[0] instanceof ResultSet)) {
          throw new RuntimeException("Error code 105: not database result set");
      }
      if (args.length > 1 && !(args[1] instanceof CharSequence)) {
          throw new RuntimeException("Error code 103: string expected");
      }
      
      ResultSet rs = (ResultSet)args[0];
      try {
          if (!rs.next()) {
              rs.close();
              return FAIL;
          }
          
          Object o = new Object();
          LinkedHashMap row = new LinkedHashMap<>();
          ResultSetMetaData rmd = rs.getMetaData();
          for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
              String colname = rmd.getColumnName(i);
              switch (rmd.getColumnType(i)) {
                  case Types.BIT:
                  case Types.INTEGER:
                  case Types.TINYINT:
                  case Types.SMALLINT:
                  case Types.BIGINT:
                      row.put(colname, rs.getLong(i));
                      break;
                  
                  case Types.DOUBLE:
                  case Types.FLOAT:
                  case Types.REAL:
                      row.put(colname, rs.getDouble(i));
                      break;
                  
                  default:
                      row.put(colname, rs.getString(i));
              }
          }
          
          return row;
      } catch (SQLException e) {
          throw new RuntimeException("Error code 214: input/output error " + e.getLocalizedMessage());
      }
  }

  //==========================================================================
  // Reflective functions.
  //==========================================================================
  
  /**
   * 
   * @return The copied value or object
   */
  public static Object copy (Object... args) {
      if (args == null || args.length == 0) { return FAIL; }
      if (args[0] instanceof List) {
      	ArrayList list = new ArrayList((List) args[0]);
	return list;
      }
      if (args[0] instanceof Map) {
      	Map list = new LinkedHashMap((Map) args[0]);
	return list;
      }
      if (args[0] instanceof Set) {
      	Set list = new LinkedHashSet((Set) args[0]);
	return list;
      }
      try {
          return args[0].getClass().getMethod("clone").invoke(args[0]);
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException n) {
          return args[0];
      }
  }
  
  public static Object image (Object... args) {
      // TODO: string concatenation in this function can be made more efficient
      if (args == null || args.length == 0) { return "&null"; }
      if (args[0] instanceof CharSequence) {
          return "\"" + args[0].toString().replace("\t", "\\t")
                                .replace("\n", "\\n").replace("\0", "\\0")
                                .replace("\b", "\\b").replace("\"", "\\\"") + "\"";
      }
      if (args[0] instanceof List) {
          return "list_(" + ((List) args[0]).size() + ")";
      }
      if (args[0] instanceof Set) {
          return "set_(" + ((Set) args[0]).size() + ")";
      }
      if (args[0] instanceof Map) {
          return "table_(" + ((Map) args[0]).size() + ")";
      }
      if (args[0] instanceof RandomAccessFile) {
          return "file";
      }
      //====
      // TODO: get method name
      //==== No groovy allowed
      // if (args[0] instanceof MethodClosure) {
      // return "function " + ((MethodClosure) args[0]).getMethod();
      // }
      //====
      if ((args[0] instanceof Callable) ||
	  	(args[0] instanceof VariadicFunction)) {
	  return "function";
      }
      return "record " + args[0].getClass().getSimpleName() + "_(" + args[0].getClass().getFields().length + ")";
  }
  
  public static Object type (Object... args) {
      if (args == null || args.length == 0) { return FAIL; }
      if (args[0] == null) return "null";
      if (args[0] instanceof Integer || args[0] instanceof BigInteger) return "integer";
      if (args[0] instanceof Number) return "real";
      if (args[0] instanceof CharSequence) return "string";
      if (args[0] instanceof List) return "list";
      if (args[0] instanceof Set) return "set";
      if (args[0] instanceof Map) return "table";
      if (args[0] instanceof Reader) return "file";
      if (args[0] instanceof OutputStream) return "file";
      if (args[0] instanceof RandomAccessFile) return "file";
      if (args[0] instanceof IconCoExpression) return "co-expression";
      if (args[0] instanceof Method) return "procedure";
      return args[0].getClass().getSimpleName();
  }
  
  //==========================================================================
  // System functions.
  //==========================================================================
  
  /**
   * Sends a hint to program that the garbage collector should be run.
   * @return null
   */
  public static Object collect (Object... args) {
      System.gc();
      return null;
  }
  
  /**
   * Pauses the program for the specified number of milliseconds.
   * @return null
   */
  public static Object delay (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      Number xn = IconNumber.toBigNumber(args[0]);
      if (xn == null) {
          throw new RuntimeException("Error code 101: not integer");
      }
      
      if (xn instanceof BigDecimal) {
          throw new RuntimeException("Error code 101: not integer");
      }
      try {
          Thread.sleep(xn.longValue());
      } catch (InterruptedException e) {
          throw new RuntimeException("Error code 500: program interrupted while delayed");
      }
      return null;
  }
  
  /**
   * Exits the program with the given exit status code. 0 is the default exit code
   * if none is supplied.
   * @return Does not return
   */
  public static Object exit (Object... args) {
      if ((args == null) || (args.length < 1)) { Runtime.getRuntime().exit(0); }
      Number xn = IconNumber.toBigNumber(args[0]);
      if (xn == null) {
          throw new RuntimeException("Error code 101: not integer");
      }
      
      if (xn instanceof BigDecimal) {
          throw new RuntimeException("Error code 101: not integer");
      }
      Runtime.getRuntime().exit(xn.intValue());
      return null;
  }
  
  /**
   * Terminates program execution with an error code and offending value.
   * @return Does not return
   */
  public static Object runerr (Object... args) {
      if ((args == null) || (args.length < 2)) { return FAIL; }
      throw new RuntimeException(args[0].toString() + "\n" + args[1].toString());
  }
  
  /**
   * Terminates program execution with a message.
   * @return Does not return
   */
  public static Object stop (Object... args) {
      UniconFunctions.write(args);
      Runtime.getRuntime().exit(1);
      return null;
  }
  
  /**
   * Executes the string or list of strings argument as a shell command.
   * Returns the exit value of the called process.
   * @return An integer
   */
  public static Object system (Object... args) {
      if ((args == null) || (args.length < 1)) { return FAIL; }
      
      String command;
      if (args[0] instanceof CharSequence) {
          command = args[0].toString();
      } else if (args[0] instanceof List) {
          command = ((List<String>) args[0]).stream().map(Object::toString).collect(Collectors.joining(" "));
      } else {
          throw new RuntimeException("Error code 110: string or list expected");
      }
      
      try {
          Process p = Runtime.getRuntime().exec(command);
          return p.waitFor();
      }
      catch(IOException e1) {
          throw new RuntimeException("Error code 500: system call not executed");
      }
      catch(InterruptedException e2) {
          throw new RuntimeException("Error code 500: system call not executed");
      }
  }
  
}

//==== END OF FILE
