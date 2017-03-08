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

/**
 * Reified variable using Groovy closures
 * to hold a reference to the variable.
 * For interactive Groovy, this is a faster alternative to 
 * using IconVar, which is pure Java with no Groovy dependencies,
 * and having the transforms bridge to it by wrapping setters "as Consumer".
 * An isLocal flag is needed since Icon procedure return
 * does not dereference unless it is a declared method local or parameter.
 * <P>
 * USAGE for x: new IconRef({-> x}, {rhs -> x=rhs})
 *
 * @author Peter Mills
 */
public class IconRef <T> extends IconVar <T> { 
  Closure<T> cgetter = null;		// closure for getter
  Closure<T> csetter = null;		// closure for setter

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * No-arg constructor.
   */
  public IconRef () { }

  /**
   * Constructor with getter and setter.
   * For immutable atoms, setter is null.
   */
  public IconRef (Closure<T> getter, Closure<T> setter) {
	this.cgetter = getter;
	this.csetter = setter;
	if (cgetter != null) { this.isHolder = false; }
  }

  //==========================================================================
  // Setter and getter.
  //==========================================================================

  public T get () {
	if (isHolder) { return value; };
	try {
	    return cgetter.call();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
  }

  public void set (T val) {
	valueAtom = null;
	if (isHolder) {
		value = val;
	} else if (csetter != null) {
		csetter.call(val);
	}
  }

}

//==== END OF FILE
