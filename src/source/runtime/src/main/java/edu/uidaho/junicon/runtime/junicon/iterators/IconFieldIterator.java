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
 * Singleton iterator over an object reference.
 * Iteration returns an assignable reference to the field.
 * The reference is frozen on first next to match Unicon semantics.
 * In other words, "o.x.y" will derive the value of "o.x", and use
 * that reference with field "z" in future get and set operations.
 *
 * @author Peter Mills
 */
public class IconFieldIterator <T> extends IconIterator <T> {
  private IconField<T> field = null;

  /**
   * No-arg constructor.
   */
  public IconFieldIterator () {
  }

  /**
   * Construct setter/getter for the given object and sequence of field names.
   */
  public IconFieldIterator (IIconAtom<T> setter, String... names) {
	field = new IconField<T>(setter, names);	// create()
	constantAtom(field);
	singleton();
  }

  /**
   * Constructor using atom.
   */
  public IconFieldIterator (IconField<T> field) {
	if (field == null) { field = new IconField<T>(); }
	this.field = field;
	constantAtom(field);
	singleton();
  }

  //=========================================================================
  // Override next.
  //=========================================================================
  public IIconAtom<T> provideNext () {
	// Freeze value on next
	return field.freezeReference();
  }

}

//==== END OF FILE
