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

import java.math.BigInteger;

/**
 * Range operator for (x to y by z), which returns an iterator.
 * <P>
 * This is a prototypical generator function,
 * that operates over values and returns an iterator.
 * The constructor must be invoked from inside an operator, 
 * not as an iterator composition in a method body,
 * since it operates over values and not iterators.
 * <P>
 * Normalize will first turn "to" into a product over iterator arguments:
 * (e to e1 by e2) =>to(e,e1,e2) => (x in e) & (y in e1) & (z in e2) & to(x,y,z)
 * Transform then transforms: to(x,y,z) => new IconToIterator(x,y,z)
 * Alternatively, could code range operator as a function using yield (suspend),
 * but this is very inefficient for a base operator.
 * <P>
 * USAGE: x to y [by z]
 *
 * @author Peter Mills
 */
public class IconToIterator <T> extends IconIterator <T> {
  private long current;
  private long lower = 0;
  private long upper = 0;
  private long by = 1;
  private IIconAtom<T> xatom = null;
  private IIconAtom<T> yatom = null;
  private IIconAtom<T> zatom = null;

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * Constructor from x to y.
   */
  public IconToIterator (long x, long y) {
	lower = x;
	upper = y;
  }

  /**
   * Constructor from x to y by z.
   */
  public IconToIterator (long x, long y, long z) {
	lower = x;
	upper = y;
	by = z;
  }

  /**
   * Constructor from x to y, using atoms.
   */
  public IconToIterator (IIconAtom xatom, IIconAtom yatom) {
	this.xatom = xatom;
	this.yatom = yatom;
  }

  /**
   * Constructor from x to y by z, using atoms.
   */
  public IconToIterator (IIconAtom xatom, IIconAtom yatom, IIconAtom zatom) {
	this.xatom = xatom;
	this.yatom = yatom;
	this.zatom = zatom;
  }
  //==========================================================================
  // Override methods.
  //==========================================================================

  /**
   * Override what happens after restart().
   */
  public void afterRestart () {
	// Derive bounds from atom
	if (xatom != null) {
	    IconValue x = xatom.getValue();
	    if (x.isNumber()) { lower = x.getInteger(); }
	}
	if (yatom != null) {
	    IconValue y = yatom.getValue();
	    if (y.isNumber()) { upper = y.getInteger(); }
	}
	if (zatom != null) {
	    IconValue z = zatom.getValue();
	    if (z.isNumber()) { by = z.getInteger(); }
	}

	current = lower;
  }

  /**
   * Override next().
   */
  public IIconAtom<T> provideNext () {
	if (getHaveDoneNext()) { current = current + by; }
	if ((by == 0) || ((by > 0) && (current > upper)) || 
			((by < 0) && (current < upper))) {
		setIsFailed(true);
	}
	return IconValue.create(current);
	//====
	// // Must return BigInteger or will coerce down Groovy arithmetic.
	// return IconValue.create(IconNumber.asBigInteger(current));
	//====
  }

  public static <T> IconToIterator<T> fromTo (long x, long y) {
	return new IconToIterator(x,y); }
  public static <T> IconToIterator<T> fromTo (long x, long y, long z) {
	return new IconToIterator(x,y,z); }
}

//==== END OF FILE
