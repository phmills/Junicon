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

import java.util.function.Function;
import java.util.concurrent.Callable;

import groovy.lang.Closure;

/**
 * Converts Groovy closures to Java lambda expressions, using Function.
 * Wraps Groovy Closure as Function that delegates to it.
 * For use in ConcurrentHashMap.computeIfAbsent or AtomicReference.getAndUpdate.
 *
 * @author Peter Mills
 */
public class IconFunction <T,R> implements Function <T,R> { 

  Closure<?> closure = null;

  /**
   * No-arg constructor.
   */
  public IconFunction () { }

  /**
   * Constructor with closure.
   */
  public IconFunction (Closure<?> closure) {
	this.closure = closure;
  }

  public R apply (T arg) {
	if (closure == null) { return null; }
	return (R) closure.call(arg);
  }
  //====
  // Groovy closure is: V call(Object... args) 
  //====
}

//==== END OF FILE
