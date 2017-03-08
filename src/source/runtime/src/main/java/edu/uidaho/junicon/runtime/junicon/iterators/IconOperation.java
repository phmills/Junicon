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

import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;

/**
 * IconOperation maps an operator over the product of its operands,
 * and lifts the result to an IconAtom.
 * All operators and control constructs take generators as operands,
 * and effect a similar map over cross-product, equivalent to a monad bind.
 * <P>
 * IconOperation is implemented using IconProduct which has a built-in map.
 * The operator is passed as a closure that performs the operation,
 * for example x+y, on primitive data types.
 * For example:
 * <PRE>
 *	I+J => IconProduct(I,J).map((x,y)->x+y) = (x in I) & (y in J) & !(x+y)
 * </PRE>
 * The operator can be set to return a boolean using setIsBoolean(), in which
 * case the iterator fails if the result is false.
 * <P>
 * IconOperation is variadic, in that it can accept any number of operands.
 * Assign, however, only has two operands, since it is right associative
 * and the grammar recurses for right associative rules.
 * <P>
 * The assignment operator can be augmented, e.g., x += y
 * <PRE>
 *	new IconAssign().augment(IconOperator.binary(op))
 * </PRE>
 * Can curry into a static factory for a given operator, for ease of use.
 * USAGE: static plus =
 *		IconOperation.curryOver({x,y -> x+y}).getCurry()
 *	  plus.over(x,y)
 *
 * @author Peter Mills
 */
public class IconOperation <T> extends IconComposition <T> {

  //==========================================================================
  // Setters for dependency injection.
  //==========================================================================

  //==========================================================================
  // Constructors
  //==========================================================================

  /**
   * No-arg constructor.
   */
  public IconOperation () {
	product();
  }

  /**
   * Curried unary operator.
   */
  public IconOperation (UnaryOperator<IIconAtom<T>> op) {
	product();
	map(op);
  }

  /**
   * Curried binary operator.
   */
  public IconOperation (BinaryOperator<IIconAtom<T>> op) {
	product();
	map(op);
  }

  /**
   * Unary operator.
   */
  public IconOperation (UnaryOperator<IIconAtom<T>> op, IconIterator<T> x) {
	super(x); product(); map(op);
  }

  /**
   * Binary operator.
   */
  public IconOperation (BinaryOperator<IIconAtom<T>> op, IconIterator<T> x,
		IconIterator<T> y) {
	super(x,y); product(); map(op);
  }

  /**
   * Variadic (n-ary) binary operator.
   */
  public IconOperation (BinaryOperator<IIconAtom<T>> op,
		IconIterator<T>... rest) {
	super(rest); product(); map(op);      // Left associative chain (x+y)+z
  }

  /**
   * Binary operator, with context.
   */
  public IconOperation (TriFunction<IIconIterator<T>, IIconAtom<T>> op,
		IconIterator<T> x, IconIterator<T> y) {
	super(x,y); product(); map(op);
  }

  /**
   * Variadic (n-ary) binary operator, with context.
   */
  public IconOperation (TriFunction<IIconIterator<T>, IIconAtom<T>> op,
		IconIterator<T>... rest) {
	super(rest); product(); map(op);      // Left associative chain (x+y)+z
  }

}

//==== END OF FILE
