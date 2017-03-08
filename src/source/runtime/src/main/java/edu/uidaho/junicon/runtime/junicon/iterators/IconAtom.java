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

//============================================================================
// ATOMS
//============================================================================

/**
 * Base class for reified variables and values, and function closures.
 *
 * @author Peter Mills
 */
public class IconAtom <T> implements IIconAtom <T> {

  //==========================================================================
  // Constructors.
  //==========================================================================
  public IconAtom () { }

  //==========================================================================
  // Setter and getter.
  //==========================================================================
  public T get () { return null; }

  public void set (T val) { }

  public T deref () { return get(); }

  //==========================================================================
  // Typed values.
  //==========================================================================
  public void setValue (IIconAtom<T> atom) {
	if (atom == null) { set(null);
	} else { set(atom.get()); }	// atom.get() == atom.getValue().get()
  }

  public IconValue getValue () {
	T value = get();
	if (value == null) { return EMPTY_VALUE; }
	return IconValue.create(value);
  }

  public Object getObject () {
	return get();
  }

  //==========================================================================
  // Temporaries.
  //==========================================================================
  public IIconAtom<T> getAtom () { return this; }

  public void setAtom (IIconAtom<T> atom) { }

  //==========================================================================
  // Filters.
  //==========================================================================
  public boolean lastSetFailed () { return false; }

  public IIconAtom<T> onReturn () { return this; }

  public IIconAtom<T> local () { return this; }

  //==========================================================================
  // Type attributes.
  //==========================================================================
  public boolean isTemporary () { return false; }

  public boolean isActivation () { return false; }

}

//==== END OF FILE
