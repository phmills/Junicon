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
package edu.uidaho.junicon.runtime.junicon.iterators;

import edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.IconTypes;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Reified variable using closures.
 * Can optionally just hold the variable's value,
 * instead of providing a reference to the variable.
 * Need isLocal flag since Icon procedure return
 * does not dereference unless it is a declared method local or parameter.
 * <P>
 * USAGE for x : new IconVar({->x}, {y->x=y})
 *
 * @author Peter Mills
 */
public class IconVar <T> extends IconAtom <T> { 
  Callable<T> getter = null;		// closure for getter
  Consumer<T> setter = null;		// closure for setter
  protected boolean isHolder = true;	// Holds own value, setter/getter==null
  protected T value = null;
  protected IconValue<T> valueAtom = null;
  boolean isLocal = false;		// Do not keep reify on return
  IconTypes type = null;		// Immutable variable of known type

  //==========================================================================
  // Constructors.
  //==========================================================================
  /**
   * No-arg constructor.
   */
  public IconVar () { }

  /**
   * Constructor with getter and setter.
   * For immutable atoms, setter is null.
   */
  public IconVar (Callable<T> getter, Consumer<T> setter) {
	this.getter = getter;
	this.setter = setter;
	if (getter != null) { this.isHolder = false; }
  }

  /**
   * Create an immutable variable.
   */
  public IconVar (Callable<T> getter) {
	this.getter = getter;
	if (getter != null) { this.isHolder = false; }
  }

  //==========================================================================
  // Setters for dependency injection.
  //==========================================================================
  /**
   * Set known type.
   * Returns this for setter chaining, i.e., build pattern.
   */
  public IconVar<T> setType (IconTypes type) {
	this.type = type;
	return this;
  }

  /**
   * Get known type.
   */
  public IconTypes getType () {
	if (type == null) { return IconTypes.UNTYPED; }
	return type;
  }

  //==========================================================================
  // Setter and getter.
  //==========================================================================
  public T get () {
	if (isHolder) { return value; };
	try {
	    return getter.call();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
  }

  public void set (T val) {
	valueAtom = null;	// Void previous cached value
	if (isHolder) {
		value = val;
	} else if (setter != null) {
		setter.accept(val);
	}
  }

  //==========================================================================
  // Typed values.
  //==========================================================================
  public void setValue (IIconAtom<T> atom) {
	if (isHolder) {  // Optimization if isHolder
		if (atom == null) {
			valueAtom = null;
			value = null; 
		} else {
			valueAtom = atom.getValue();
			if (valueAtom == null) { value = null;
			} else { value = valueAtom.get(); }
		}
	} else {  // Must keep in sync with plain variable
		if (atom == null) {
			valueAtom = null;
			set(null);
		} else {
			valueAtom = atom.getValue();
			if (valueAtom == null) { set(null);
			} else { set(valueAtom.get()); }

		}
	}
  }

  public IconValue getValue () {
	if (isHolder) {
		if (valueAtom == null) {
			if (value == null) { valueAtom = EMPTY_VALUE;
			} else {
			   if (type != null) {
				valueAtom = IconValue.createTyped(value, type);
			   } else {
				valueAtom = IconValue.create(value);
			   }
			}
		}
	} else {	// Must keep in sync with plain variable
		value = get();
		if ((valueAtom == null) || (valueAtom.get() != value)) {
			// valueAtom is unset or changed
			if (value == null) { valueAtom = EMPTY_VALUE;
			} else {
			    if (type != null) {
				valueAtom = IconValue.createTyped(value, type);
			    } else {
				valueAtom = IconValue.create(value);
			    }
			}
		}
	}
	return valueAtom;
  }

  //==========================================================================
  // Filters.
  //==========================================================================
  public IconVar<T> local () {
	isLocal = true;
	return this;
  }

  public IIconAtom<T> onReturn () {
	if (isLocal) { return getValue(); }
	return this;
  }
  //====
  // if (isLocal) { return IconValue.create(get()); }
  //====

  //==========================================================================
  // Factory methods.
  //==========================================================================
  /**
   * Create an immutable variable of known type.
   */
  public static <V> IconVar<V> create (Callable<V> getter, IconTypes type) {
	return new IconVar(getter).setType(type);
  }

  /**
   * Create an immutable variable of type list.
   */
  public static <V> IconVar<V> createAsList (Callable<V> getter) {
	return new IconVar(getter).setType(IconTypes.LIST);
  }

  /**
   * Create an immutable variable of type set.
   */
  public static <V> IconVar<V> createAsSet (Callable<V> getter) {
	return new IconVar(getter).setType(IconTypes.SET);
  }

  /**
   * Create an immutable variable of type map.
   */
  public static <V> IconVar<V> createAsMap (Callable<V> getter) {
	return new IconVar(getter).setType(IconTypes.MAP);
  }

}

//==== END OF FILE
