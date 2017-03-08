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

/**
 * Reified temporary variable that holds another IconAtom.
 *
 * @author Peter Mills
 */
public class IconTmp <T> extends IconAtom <T> { 
  IIconAtom<T> atom = null;

  //==========================================================================
  // Constructors.
  //==========================================================================
  /**
   * No-arg constructor.
   */
  public IconTmp () { }

  //==========================================================================
  // Setter and getter.
  //==========================================================================
  public T get () {
	if (atom == null) { return null; }
	return atom.get();
  }

  public void set (T value) {
	if (atom == null) { return; }
	atom.set(value);
  }

  /**
   * deref does a get().get() to yield the value of the IconAtom.
   */
  public T deref () {
	if (atom == null) { return null; }
	return atom.get();
  }

  //==========================================================================
  // Typed values.
  //==========================================================================
  public void setValue (IIconAtom<T> value) {
	if (atom == null) { return; }
	atom.setValue(value);
  }

  public IconValue getValue () {
	if (atom == null) { return EMPTY_VALUE; }
	return atom.getValue();
  }

  //==========================================================================
  // Temporaries.
  //==========================================================================
  public IIconAtom<T> getAtom () {
	return atom;
  }

  public void setAtom (IIconAtom<T> atom) {
	this.atom = atom;
  }

  //==========================================================================
  // Filters.
  //==========================================================================

  /**
   * Never invoked.  Temporaries will not be returned.
   * Temporaries are, however, always local.
   */
  public IIconAtom<T> onReturn () {
	if (atom == null) { return null; }
	return atom.getValue();
  }

  //==========================================================================
  // Type attributes.
  //==========================================================================
  public boolean isTemporary () { return true; }

}

//==== END OF FILE
