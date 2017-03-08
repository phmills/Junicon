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

import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.concurrent.Callable;

/**
 * Singleton iterator over a list or map index operation.
 * Iteration returns an assignable reference to the indexed element,
 * reified using closures.
 * Works on any collection or map in general, but slicing is limited to lists
 * and not sets or queues.
 * <P>
 * Reifies the index operation c[i] and promotes its evaluation to a
 *	singleton iterator which returns an assignable variable.
 *	A reified index c[i] is represented using closures for retreival
 *	and assignment, and which upon evaluation in an iterator,
 *	freezes the separate c and i fields used in the closures
 *	so as to fix the reference.
 * <P>
 * The reified index will be evaluated to an assignable reference
 *	upon its occurrence in (o in c[i]), by dereferencing the c and i fields.
 *	The reference "o" will later be dereferenced to a value
 *	by performing the index operation. Dereferencing occurs when used as
 *	an argument to a function or operator.
 *	An argument c[i] in function calls is passed call-by-value
 *	and cannot be assigned to or further altered.
 * <P>
 * ORIGIN: Index origin is 0 or 1.
 *	Origin 0 uses Groovy rules; origin 1 uses Unicon index & slicing rules. 
 *	In both cases, if index is < 0 will select from right.
 * <P>
 * USAGE  index c[i] => new IconIndexIndexIterator(
 *				new IconIndex({->c}, {->i}))
 *	  slice c[b..e] => new IconIndexIndexIterator(
 *				new IconIndex({->c}, {->b}, {->e}))
 *
 * @author Peter Mills
 */
public class IconIndexIterator <T> extends IconIterator <T> {
  private IconIndex<T> indexClosure = null;	// list index using closures

  /**
   * No-arg constructor.
   */
  public IconIndexIterator () {
  }

  /**
   * Constructor for subscript c[i].
   * Index origin must be 0 or 1; defaults to 0.
   */
  public IconIndexIterator (IIconAtom<T> listAtom, IIconAtom<T> beginAtom) {
	indexClosure = new IconIndex<T>(listAtom, beginAtom);  // create()
	constantAtom(indexClosure);
	singleton();
  }

  /**
   * Constructor for slice c[b..e].
   * Index origin must be 0 or 1; defaults to 0.
   */
  public IconIndexIterator (IIconAtom<T> listAtom,
		IIconAtom<T> beginAtom, IIconAtom<T> endAtom) {
	indexClosure = new IconIndex<T>(listAtom, beginAtom, endAtom);//create()
	constantAtom(indexClosure);
	singleton();
  }

  /**
   * Constructor using atom.
   */
  public IconIndexIterator (IconIndex<T> indexClosure) {
	if (indexClosure == null) { indexClosure = new IconIndex<T>(); }
	this.indexClosure = indexClosure;
	constantAtom(indexClosure);
	singleton();
  }

  //=========================================================================
  // Setters for dependency injection.
  //=========================================================================

  /**
   * Set index origin to 0 or 1.
   * Ignores argument if not 0 or 1.
   * Returns this.
   */
  public IconIndexIterator<T> origin (int indexOrigin) {
	if (indexClosure != null) {
		indexClosure.origin(indexOrigin);
	}
	return this;
  }

  /**
   * Set slice end as additive for c[b +: e].
   * Returns this.
   */
  public IconIndexIterator<T> plus () {
	if (indexClosure != null) {
		indexClosure.plus();
	}
	return this;
  }

  /**
   * Set slice end as subtractive for c[b -: e].
   * Returns this.
   */
  public IconIndexIterator<T> minus () {
	if (indexClosure != null) {
		indexClosure.minus();
	}
	return this;
  }

  //=========================================================================
  // Override next.
  //=========================================================================
  public IIconAtom<T> provideNext () {
	// Freeze value on next
	return indexClosure.freezeReference();
  }

}

//==== END OF FILE
