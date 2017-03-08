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
package edu.uidaho.junicon.runtime.junicon.iterators;

import edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.IconTypes;

import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Reified variable using closures.
 * Need isLocal flag since Icon procedure return
 * does not dereference unless declared local variable.
 * <P>
 * USAGE for x : new IconVarIterator({->x}, {y->x=y})
 *
 * @author Peter Mills
 */
public class IconVarIterator <T> extends IconIterator <T> { 

  /**
   * Default is empty iterator that just fails.
   */
  public IconVarIterator () {
  }

  /**
   * Singleton iterator using setter and getter.
   */
  public IconVarIterator (Callable<T> getter, Consumer<T> setter) {
	constantAtom(new IconVar<T>(getter, setter));
	singleton();
  }

  /**
   * Singleton iterator using atom.
   */
  public IconVarIterator (IconAtom<T> atom) {
	constantAtom(atom);
	singleton();
  }

  /**
   * Set if is local variable, to be dereferenced on return from function.
   */
  public IconVarIterator<T> local () {
	IIconAtom<T> atom = getConstantAtom();
	if (atom != null) { atom.local(); }
	return this;
  }

  //==========================================================================
  // Factory methods.
  //==========================================================================
  /**
   * Create an iterator over an immutable variable.
   */
  public static <V> IconVarIterator<V> create (Callable<V> getter) {
	return new IconVarIterator(new IconVar(getter));
  }

  /**
   * Create an iterator over an immutable variable of type list.
   */
  public static <V> IconVarIterator<V> createAsList (Callable<V> getter) {
	return new IconVarIterator(new IconVar(getter).setType(IconTypes.LIST));
  }

  /**
   * Create an iterator over an immutable variable of type set.
   */
  public static <V> IconVarIterator<V> createAsSet (Callable<V> getter) {
	return new IconVarIterator(new IconVar(getter).setType(IconTypes.SET));
  }

  /**
   * Create an iterator over an immutable variable of type map.
   */
  public static <V> IconVarIterator<V> createAsMap (Callable<V> getter) {
	return new IconVarIterator(new IconVar(getter).setType(IconTypes.MAP));
  }

}

//==== END OF FILE
