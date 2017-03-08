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

import java.util.*;
import java.math.BigInteger;
import java.math.BigDecimal;

/**
 * Singleton iterator over
 * literal, i.e., raw value, not captured as a closure.
 * <P>
 * USAGE for v: new IconValueIterator(v)
 *
 * @author Peter Mills
 */
public class IconValueIterator <T> extends IconIterator <T> {

  //=========================================================================
  // Constructors.
  //=========================================================================
  /**
   * Default is empty iterator that just fails.
   */
  public IconValueIterator () {
  }

  //====
  // * Singleton iterator using atom.
  // public IconValueIterator (IIconAtom<T> atom) {
  //	constantAtom(atom);
  //	singleton();
  // }
  //====
  // * Singleton iterator using value.
  // public IconValueIterator (T value) {
  //	constant(value);
  //	singleton();
  // }
  //====

  //=========================================================================
  // Typed constructors for immutable values.
  //=========================================================================

  /**
   * Typed constructors for immutable values.
   */
  public IconValueIterator (CharSequence value) {
	constantAtom(IconValue.create(value));
	singleton();
  }

  public IconValueIterator (CharSequence value, int radix) {
	constantAtom(IconValue.create(value, radix));
	singleton();
  }

  public IconValueIterator (Number value) {
	constantAtom(IconValue.create(value));
	singleton();
  }

  public IconValueIterator (int value) {
	constantAtom(IconValue.create(value));
	singleton();
  }

  public IconValueIterator (long value) {
	constantAtom(IconValue.create(value));
	singleton();
  }

  public IconValueIterator (float value) {
	constantAtom(IconValue.create(value));
	singleton();
  }

  public IconValueIterator (double value) {
	constantAtom(IconValue.create(value));
	singleton();
  }

  public IconValueIterator (BigInteger value) {
	constantAtom(IconValue.create(value));
	singleton();
  }

  public IconValueIterator (BigDecimal value) {
	constantAtom(IconValue.create(value));
	singleton();
  }

}

//==== END OF FILE
