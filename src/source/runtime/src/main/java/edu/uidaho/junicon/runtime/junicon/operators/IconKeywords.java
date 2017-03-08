//========================================================================
// Copyright (c) 2014 Orielle, LLC.  
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
import edu.uidaho.junicon.runtime.junicon.constructs.IconPromote;
import edu.uidaho.junicon.runtime.junicon.constructs.IconScan;
import edu.uidaho.junicon.runtime.junicon.constructs.IconCoExpression;
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.BufferedOutputStream;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Defines the Unicon &amp;keywords.
 * Each &amp;keyword is a static field or method in this class.
 * The Junicon Spring configuration file maps symbols
 * to these fields using its property maps.
 * <PRE>
 * &subject => IconKeywords.subject
 *
 * SymbolAsValue transforms it to a literal value.
 * SymbolAsField transforms it to a field reference.
 * SymbolAsProperty transforms it to field treated as an IIconAtom property.
 * SymbolAsIterator transforms it to an iterator constructor.
 * OperatorAsFunction transforms it as a generator function call.
 * Default is: SymbolAsVariable.
 * </PRE>
 * <P>
 * For generator keywords that produce a sequence of results, 
 * the field should return an iterator.
 * <P>
 * ThreadLocal state is used for any local thread values.
 * &fail and &null are separately transformed to fail and null.
 *
 * @author Peter Mills
 * @author Rob Kleffner
 */
public class IconKeywords {

  //==========================================================================
  // Keywords defined as properties with getters and setters.
  // SEE: SymbolAsProperty (default) in spring_config.xml.
  //==========================================================================

  //====
  // String scanning
  //====

  /**
   * String scanning keywords: {@literal &}subject
   * {@literal &}subject is the text in the current scanning environment.
   * On set, i.e. on assignment, {@literal &}pos is set to 1.
   */
  public static IconAtom subject = new IconAtom() {
	public String get () {                 
	    IconScan env = IconScan.getScanEnv();	// Get current env
	    return env.getSubject();
	}
	public void set (Object val) {
	    if ((val == null) || (! (val instanceof String))) { return; }
	    IconScan env = IconScan.getScanEnv();
	    env.setSubject((String) val);
	    env.setPos(1);
	}
  };

  /**
   * String scanning keywords: {@literal &}pos
   * {@literal &}pos is the subject text position
   * in the current scanning environment.
   * On set, if not in range of the subject, the set fails.
   */
  public static IconAtom pos = new IconAtom() {
	boolean lastSetFailed = false;
	public Number get () {                 
	    IconScan env = IconScan.getScanEnv();
	    return env.getPos();
	}
	public void set (Object val) {
	    lastSetFailed = false;
	    Number num = IconNumber.toNumber(val);
	    if (val == null) {
		lastSetFailed = true;
		return;
	    }
	    IconScan env = IconScan.getScanEnv();
	    lastSetFailed = ! env.setPosWithin(num.longValue());
	}
	public boolean isLastSetFailed () {
	    return lastSetFailed;
	}
  };

  //====
  // Co-expressions
  //====

  /**
   * Current co-expression: &amp;current
   */
  public static IconAtom<IconCoExpression> current = new IconAtom() {
	public IconCoExpression get () {                 
	    IconCoExpression coexpr = IconCoExpression.getCurrentCoexpr();
	    return coexpr;
	}
	public void set (IconCoExpression coexpr) {
	    IconCoExpression.setCurrentCoexpr(coexpr);
	}
  };

  /**
   * Invoking co-expression: &amp;source
   */
  public static IconAtom<IconCoExpression> source = new IconAtom() {
	public IconCoExpression get () {                 
	    IconCoExpression coexpr = IconCoExpression.getCurrentCoexpr();
	    if (coexpr == null) { return null; };
	    return coexpr.getInvoker();
	}
	public void set (IconCoExpression coexpr) {
	}
  };

  /**
   * Main co-expression: &amp;main
   * Returns top-level outermost co-expression if it exists.
   */
  public static IconAtom<IconCoExpression> main = new IconAtom() {
	public IconCoExpression get () {                 
	    IconCoExpression coexpr = IconCoExpression.getCurrentCoexpr();
	    if (coexpr == null) { return null; }
	    return coexpr.getTopLevel();
	}
	public void set (IconCoExpression coexpr) {
	}
  };

  //====
  // Time
  //====

  /**
   * Time: &amp;time
   * Returns current cpu time in milliseconds,
   * including both user and system time.
   */
  public static IconAtom<Number> time = new IconAtom() {
	public Number get () {                 
		return getCpuTime()/1000;
	}
	public void set (Number coexpr) {
	}
  };

  //==========================================================================
  // Keywords defined as variables.
  // SEE: SymbolAsVariable in spring_config.xml
  //==========================================================================

  /**
   * &amp;digits
   */
  public static LinkedHashSet digits =
	new LinkedHashSet(IconList.createList(
		"0","1","2","3","4","5","6","7","8","9"));

  /**
   * {@literal &}cset set of 256 characters.
   */
  public static LinkedHashSet<String> cset =
	new LinkedHashSet(IntStream.range(0,256).mapToObj(
	(int x) -> String.valueOf((char) x)).collect(Collectors.toSet()));

  /**
   * Alias for standard input: &amp;input
   */
  public static BufferedReader input =
        new BufferedReader(new InputStreamReader(System.in));
  
  /**
   * Alias for standard output: &amp;output
   */
  public static OutputStream output = System.out;
        // new BufferedWriter(new OutputStreamWriter(System.out));

  //==========================================================================
  // Keywords defined as synthetic functions that return iterators.
  //	Typically return lists or maps promoted using !
  // SEE: OperatorAsFunction in spring_config.xml.
  //==========================================================================

  /**
   * &amp;features
   */
  public static IIconIterator features () {
	return new IconPromote(features);
  }

  private static Map features = IconMap.createMap(
		"Version", "1.0",
		"Language", "Junicon"
  );

  //==========================================================================
  // Keywords defined as generators.
  //       Symbols defined this way must map to a new or method invocation
  //       that returns a mutable iterator.
  //==========================================================================
  // SEE: SymbolAsIterator in spring_config.xml
  //=====

  //==========================================================================
  // Keywords defined as values.
  //==========================================================================
  // SEE: SymbolAsValue in spring_config.xml
  //=====

  //=========================================================================
  // Utility methods.
  //=========================================================================
  /**
   * Get CPU time in nanoseconds, including both system and user time.
   */
  public static long getCpuTime () {
	ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	return bean.isCurrentThreadCpuTimeSupported( ) ?
		bean.getCurrentThreadCpuTime( ) : 0L;
  }
 
  /**
   * Get user time in nanoseconds.
   */
  public static long getUserTime () {
	ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	return bean.isCurrentThreadCpuTimeSupported( ) ?
		bean.getCurrentThreadUserTime( ) : 0L;
  }

}

//==== END OF FILE
