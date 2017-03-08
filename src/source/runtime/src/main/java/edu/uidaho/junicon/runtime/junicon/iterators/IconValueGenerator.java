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

import java.util.*;

/**
 * Immutable generator value.
 *
 * @author Peter Mills
 */
public class IconValueGenerator <T> extends IconValue <IIconIterator<T>> {
  IIconIterator<T> value = null;

  //==========================================================================
  // Constructors.
  //==========================================================================
  public IconValueGenerator () { }

  public IconValueGenerator (IIconIterator<T> value) {
	this.value = value;
  }

  //==========================================================================
  // Setter and getter.
  //==========================================================================
  public IIconIterator<T> get () { return value; }

  public void set (IIconIterator<T> value) { }

  //==========================================================================
  // Methods by type.
  //==========================================================================
  public IconTypes getType () { return IconTypes.GENERATOR; }

  public boolean isGenerator () { return true; }

  public IIconIterator<T> getGenerator () { return value; }

  public boolean isIterator () { return true; }

  public Iterator<T> getIterator () { return value; }

  public Object getObject () { return value; }

}

//==== END OF FILE
