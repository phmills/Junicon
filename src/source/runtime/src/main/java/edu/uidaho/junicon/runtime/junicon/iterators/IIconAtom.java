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

/**
 * Interface for reified variables and values.
 * A reified variable, or atom, has a getter and setter like a property,
 * and either holds a reference to an object,
 * or instead can internally just hold the object value.
 * In function terms, a getter is a Supplier.get() or Callable.call(),
 * and a setter is a Consumer.accept(), although here it is presented as set().
 * <P>
 * Reified variables are used in Junicon as follows.
 * Class fields are declared as plain values, and in addition have separate
 * reified variables that refer to them,
 * in order to support seamless interoperability with Java.
 * Method parameters, method/block locals, and temporaries are
 * declared as reified variables which just hold the value.
 * Temporaries that appear in the left-hand side of a bound iterator,
 * i.e., (tmp in e), hold an atom rather than a raw value.
 * For external references, we don't declare reified variables,
 * but just construct them in reified form as needed in expressions.
 *
 * @author Peter Mills
 */
public interface IIconAtom <T> {

  //==========================================================================
  // Types
  //==========================================================================

  /**
   * Failure atom.
   */
  public static IconValue FAIL = new IconValue();  // create()

  /**
   * Value types.
   */
  public enum IconTypes { OMIT, UNTYPED,	// Omitted argument in invoke
	STRING, NUMBER, COLLECTION, LIST, SET,	// Types
	MAP, ITERATOR, GENERATOR, ARRAY, OTHER, // Other if no other type
	INTEGER, REAL, BIGINTEGER, BIGDECIMAL }	// Refined types

  // Empty array used in OMITted argument handling
  public static final Object[] EMPTY_ARRAY = {};

  /**
   * Get empty array.
   */
  public static Object[] getEmptyArray () { return EMPTY_ARRAY; }

  /**
   * Empty value.
   */
  public static final IconValue EMPTY_VALUE = new IconValue();

  //==========================================================================
  // Setter and getter.
  //==========================================================================

  /**
   * Gets the value of the object referenced by this atom.
   * For temporaries which hold atoms, gets the held atom's value
   * by getTmp().get().
   */
  public T get ();

  /**
   * Sets the object referenced by this atom to the specified value.
   * For temporaries which hold atoms, sets the held atom's value
   * by getTmp().set(v).
   * A value atom is immutable.
   */
  public void set (T value);

  /**
   * Gets the value referenced by this atom.
   * For temporaries which hold atoms, gets the held atom's value
   * by getTmp().set(v).
   * Equivalent to get().
   */
  public T deref ();

  //==========================================================================
  // Typed values.
  //==========================================================================
  /**
   * Sets the value as an atom, from the given atom's getValue().
   * <PRE>
   * Equivalent to: set(atom.get())
   * </PRE>
   */
  public void setValue (IIconAtom<T> atom);

  /**
   * Gets the value as an atom.
   * Not guaranteed to be typed.
   * Always non-null, may be empty value.
   * <PRE>
   * Equivalent to: IconValue.create(get())
   * Invariant condition: atom.getValue().get() = atom.get()
   * </PRE>
   */
  public IconValue getValue ();

  /**
   * Gets the value as an object, regardless of type.
   * Equivalent to deref() or get().
   */
  public Object getObject ();

  //==========================================================================
  // Temporaries.
  //==========================================================================
  /**
   * Gets the atom held by this temporary.
   * If not temporary, just returns this.
   */
  public IIconAtom<T> getAtom ();

  /**
   * Sets the atom held by this temporary.
   * If not temporary, does nothing.
   */
  public void setAtom (IIconAtom<T> atom);

  //==========================================================================
  // Filters.
  //==========================================================================

  /**
   * Gets if the last set failed.   Used by &pos in assignment.
   */
  public boolean lastSetFailed ();

  /**
   * Perform postprocessing on this atom before being returned
   * by an iterators nextAtom(), if filterOnReturn() is set in that iterator.
   * Intended for use by method return and suspend statements.
   * For IconVar, if local then deferences the atom, i.e.,
   * gets the value referenced by this atom and promotes it to an atom.
   * For IconIndex and IconField,
   * clones and then freezes the value referenced by this atom.
   * Default is to just return this atom.
   */
  public IIconAtom<T> onReturn ();

  /**
   * Sets if is a method local that is dereferenced on return.
   * Returns this to allow setter chaining.
   */
  public IIconAtom<T> local ();

  //==========================================================================
  // Type attributes.
  //==========================================================================

  /**
   * Gets if is a temporary reified atom, which holds another atom.
   * <P>
   * For temporaries, deref() is equivalent to getTmp().get().
   * <P>
   * In the transforms, deref() is only used for temporaries.
   * An object reference with deref() will be of form:
   * o.deref().x.y | o.deref().
   * If an object reference ends in deref(),
   * it will be a singular atom that is a temporary.  Such an
   * o.deref() without .x.y can only occur inside a primary
   * as an index list, function name, or argument, but never standalone.
   */
  public boolean isTemporary ();

  /**
   * Gets if is an atom returned from a continuation,
   * which holds a co-expression to activate.
   */
  public boolean isActivation ();

}

//==== END OF FILE
