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
package edu.uidaho.junicon.runtime.junicon.constructs;

import edu.uidaho.junicon.runtime.junicon.iterators.*;

import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Promotes map, list, collection, array, string, or Java Iterator to an
 * iterator. For any other type of object, produces an iterator over the
 * Object's fields.
 * The iterator produces updatable index references into the collection,
 * i.e., c[i].
 * <P>
 * For maps, the iterator produces indexes by key: when dereferenced,
 * the index produces the value for that key.
 * For lists, arrays, and strings, i.e., CharSequence,
 * the iterator produces indexes by position.
 * For a number, the iterators produces 1 to number.
 * For other collections or Java iterators, the iterator produces
 * values, not indexes.
 * For any other type of object, i.e., record, the iterator produces
 * its public fields as updatable variables, in order.
 * If "listAsValue" is set, overrides list to produce values, not indexes.
 * The operators that depend on the above types include:
 * promote, random, and size, as well as index operations.
 * <P>
 * Promote, in addition to "to", is a prototypical generator function,
 * that operates over values and returns an iterator.
 * The constructor should be invoked using normalized
 * values, not as an iterator composition over iterators.
 * <P>
 * USAGE: ! iterator
 *
 * @author Peter Mills
 */
public class IconPromote <T> extends IconComposition <T> {
  private IIconAtom<T> listAtom = null;	// Needed for updatable string
  private IconValue<T> list = null;	// Derived list atom value
  private int size = 0;
  private int pos = -1;
  private Object obj = null;		// For field
  private List<String> fieldNames = null;
  private boolean asValue = false;	// Override to produce index
  private Iterator<T> clonedIter = null;

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * Promote Java Iterator.
   */
  public IconPromote (Iterator javaIterator) {
	if (javaIterator == null) { return; }
	list = IconValue.create(javaIterator);
  }

  /**
   * Promote Java list.
   */
  public IconPromote (List list) {
	if (list == null) { return; }
	this.list = IconValue.create(list);
  }

  /**
   * Promote Java collection other than list, e.g., set.
   */
  public IconPromote (Collection collection) {
	if (collection == null) { return; }
	list = IconValue.create(collection);
  }

  /**
   * Promote Java map.
   */
  public IconPromote (Map map) {
	if (map == null) { return; }
	list = IconValue.create(map);
  }

  /**
   * Promote Java array.
   */
  public IconPromote (Object[] array) {
	if (array == null) { return; }
	list = IconValue.create(array);
  }

  /**
   * Promote non-updatable character sequence.
   */
  public IconPromote (CharSequence str) {
	if (str == null) { return; }
	list = IconValue.create(str);
  }

  /**
   * Promote number to (1 to number).
   */
  public IconPromote (Number num) {
	if (num == null) { return; }
	list = IconValue.create(num);
  }

  /**
   * Promote IconIterator to itself.
   */
  public IconPromote (IconIterator<T> iconIter) {
	setX(iconIter);		// now ignores provideNext()
  }

  /**
   * Promote any other type to iterator over Object's fields.
   */
  public IconPromote (Object obj) {
	if (obj == null) { return; }
	list = IconValue.create(obj);
  }

  /**
   * Promote an atom to an iterator over its value.
   */
  public IconPromote (IIconAtom<T> listAtom) {
	this.listAtom = listAtom;
  }

  //==========================================================================
  // Setters for dependency injection.
  //==========================================================================

  /**
   * Set if override list to return values, not indexes.
   * Returns this.
   */
  public IconComposition<T> setListAsValue () {
	this.asValue = true;
	return this;
  }

  /**
   * Get if override list to return values, not indexes.
   */
  public boolean isListAsValue () {
	return asValue;
  }

  //==========================================================================
  // Override methods.
  //==========================================================================

  /**
   * Override what happens after restart().
   */
  public void afterRestart () {
	// Reset
	clonedIter = null;
	pos = -1;
	size = 0;

	// Derive list value from atom
	if (listAtom != null) {
		list = listAtom.getValue();
	}
	if (list == null) { return; }

	// Get type: string, number, collection, map, iterator, array, other
	if (list.isString()) {
		size = list.getString().length();
	} else if (list.isNumber()) {
		size = (int) list.getInteger();
	} else if (list.isList()) {
		clonedIter = list.getList().iterator();
	} else if (list.isCollection()) {
		clonedIter = list.getCollection().iterator();
	} else if (list.isMap()) {
		clonedIter = list.getMap().keySet().iterator();
	} else if (list.isArray()) {
		size = list.getArray().length;
	} else if (list.isOther()) {
		obj = list.getOther();
		if (obj != null) {
			fieldNames = IconField.objectAsNames(obj);
			if (fieldNames != null) {
				size = fieldNames.size();
			}
		}
	} else if (list.isIterator()) {
		clonedIter = list.getIterator();  // cannot clone iterator
	}
  }

  /**
   * Override next().
   * If hasNext(), invokes java.next(), else fails.
   */
  public IIconAtom<T> provideNext () {
	if (list == null) { 
		setIsFailed(true);
		return null;
	}
	if (isConstant()) { return getConstantAtom(); }	// Object

	pos++;		// Starts at 0, since was first -1
	if (list.isString()) {		// Index into string by position
	    if (pos < size) {	// Use frozen string for getter, atom for setter
		return new IconIndex<T>().origin(0).setIndex(
			(listAtom == null) ? list : listAtom, pos);  // create()
	    }
	} else
	if (list.isNumber()) {		// 1 to number
	    if (pos < size) {
		return IconValue.create(pos+1);
	    }
	} else
	if (list.isArray()) {		// Index into array by position
	    if (pos < size) {
		return new IconIndex<T>().origin(0).setIndex(
			(listAtom == null) ? list : listAtom, pos);  // create()
	    }
	} else
	if (list.isOther()) {
	    if (pos < size) {
		return new IconField<T>(obj, fieldNames.get(pos)); // create()
	    }
	} else
    	if ((clonedIter != null) && clonedIter.hasNext()) {
	    if (list.isList()) {	// Index into list by position
		if (asValue) {
			return IconValue.create(clonedIter.next()); // create()
		}
		clonedIter.next();	// skip to next, but return index
		return new IconIndex<T>().origin(0).setIndex(
			list, pos);  // create()
	    }
	    if (list.isMap()) {	// Index into map by key
		return new IconIndex<T>().origin(0).setMapIndex(list,
			clonedIter.next());			// create()
	    }
	    if (list.isCollection() || list.isIterator()) { // Just return value
		return IconValue.create(clonedIter.next());	// create()
	    }
	}
	setIsFailed(true);
	return null;
  }

}

//==== END OF FILE
