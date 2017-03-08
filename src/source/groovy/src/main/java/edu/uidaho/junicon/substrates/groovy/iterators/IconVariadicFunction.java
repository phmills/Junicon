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
package edu.uidaho.junicon.substrates.groovy.iterators;

import edu.uidaho.junicon.runtime.junicon.iterators.*;

import java.util.concurrent.Callable;
// import java.util.function.Consumer;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.MethodClosure;

/**
 * Converts Groovy closures to Java lambda expressions, using VariadicFunction.
 * Wraps Groovy Closure as VariadicFunction that delegates to it.
 * For use by unpackArgs() in IconIterator.
 *
 * @author Peter Mills
 */
public class IconVariadicFunction <T,R> implements VariadicFunction <T,R> { 

  Closure<?> closure = null;

  /**
   * No-arg constructor.
   */
  public IconVariadicFunction () { }

  /**
   * Constructor with closure.
   */
  public IconVariadicFunction (Closure<?> closure) {
	this.closure = closure;
  }

  public R apply (T... args) {
	if (closure == null) { return null; }
	return (R) closure.call(args);
  }
  //====
  // Groovy closure is: V call(Object... args) 
  //====

  //==========================================================================
  // Static factory methods
  //==========================================================================

  /**
   * Convert Java variadic function to Groovy closure.
   */
  public static Closure asClosure (VariadicFunction func) {
	if (func == null) { return null; }
	return new MethodClosure(func, "apply");
  }
  //====
  // if (method instanceof VariadicFunction) { return { Object args... -> 
  //		((VariadicFunction) method).apply(args); }; }
  //====

}

//==== END OF FILE
