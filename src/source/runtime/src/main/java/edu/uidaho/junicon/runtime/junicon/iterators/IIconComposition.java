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

//============================================================================
// IconComposition : extends Java.util.Iterator
//	for failure-driven and suspendable evaluation
//	The iterator is restartable and optionally revertible on failure.
//============================================================================

/**
 * Core logic for composing failure-driven and suspendable iterators.
 * The iterator is restartable and optionally revertible on failure.
 * <P>
 * Summary of compositions over iterators:
 * <PRE>
 * Only one of the compositions may apply: concat, product, reduce, repeat.
 * Modifiers to any composition: map, guard, limit, bounded, singleton, exists.
 * Failure overrides: always succeed fail, always fail, not.
 * Precedence: (((guard -> (x|y x&y)).map).reduce.repeat).
 *			limit.bounded.singleton.exists.succeed.fail.not
 * Guard and limit take priority over always succeed, i.e.,
 *	if the guard fails then always succeed is ignored.
 * Thus (g -> y;succeed:1)* = (y.guard(g).bound().succeed()).repeat().
 * </PRE>
 * <P>
 * An IconIterator is failure-driven in that
 * next() produces a value until it fails.
 * In other words, an IconIterator is terminated when next() fails
 * and returns FAIL, and a following next() will then restart the iterator.
 * <P>
 * IconComposition extends IconIterator for the
 * composition of one or more iterators using such forms as product or
 * concatenation. If there are no operands, by default
 * it is an empty iterator that just fails, or can be set to
 * a constant iterator that never fails.
 * IconComposition is suspendable in that if next() suspends,
 * on the subsequent next() it will resume that next() at the 
 * point of suspension within the tree of composed iterators.
 * <P>
 * To support Icon's lazy dereference semantics,
 * IconIterator always returns an IconAtom 
 * that holds a reified variable or value.
 * This behavior is encapsulated in nextAtom(), which
 * produces the next reified value in the iterator or fails and returns FAIL,
 * while next() returns the dereferenced value of nextAtom()
 * to maintain consistency with native types returned by Java iterators.
 *
 * @author Peter Mills
 */
public interface IIconComposition <T> extends IIconIterator <T> {

  //==========================================================================
  // Operands.
  //==========================================================================

  /**
   * Gets first operand.
   */
  public IconIterator<T> getX ();

  /**
   * Sets first operand, and whether 
   * it is a sub-expression in a left-associative
   * chain of binary operations, e.g. (x & y) & z.
   * Returns this.
   * Propagates certain composition type and modifiers, including
   * any map, reduce, or product attributes,
   * to x if IsInChain, i.e., is in the decomposition of a
   * left-associative chain: x & y & z => (x & y) + z.
   */
  public IIconComposition<T> setX (IconIterator<T> x, boolean isInChain);

  /**
   * Sets first operand. Returns this.
   * Propagates certain composition and modifiers to x if it IsInChain.
   */
  public IIconComposition<T> setX (IconIterator<T> x);

  /**
   * Gets second operand.
   */
  public IconIterator<T> getY ();

  /**
   * Sets second operand.  Returns this.
   */
  public IIconComposition<T> setY (IconIterator<T> y);

  /**
   * Set operands to apply over.  Returns this.  Equivalent to setX.
   */
  public IconComposition<T> over (IconIterator<T> x);

  /**
   * Set operands to apply over.  Returns this.  Equivalent to setX and setY.
   */
  public IconComposition<T> over (IconIterator<T> x, IconIterator<T> y);

  /**
   * Set operands with variable number of arguments, i.e., variadic.
   * Returns this.
   * Assumes a left associative chain, and decomposes operands into
   * a tree of binary operations: (x & y & z) => ((x & y) & z).
   * Recursively sets XIsInChain attribute on decomposed left binary operations,
   * e.g. x & y, and propagates certain composition and modifiers to them.
   * Map, product, and reduce will propagate down the chain: e.g.,
   *	map will apply map on left child if (x & y).isInChain.
   * The methods over(), setX(), and propagatable
   * composition and modifier setters may be applied in any order.
   */
  public IconComposition<T> over (IconIterator<T>... rest);

  /**
   * Gets if first operand is sub-expression in 
   * left-associative chain, e.g., (x & y) & z.
   */
  public boolean getXIsInChain ();

  //==========================================================================
  // Composition.  Only one of the following compositions can apply.
  //==========================================================================

  /**
   * Concatenation over x | y, i.e., {for each x ; for each y}.
   * If unary over x, then this is delegation to x.
   * Returns this.
   * <P>
   * Only one of the following iterator compositions can apply:
   * concat, product, zip, reduce, repeat, not, constant, return,
   * suspend, break, or continue.
   * <P>
   * Default if operands and no composition is like unary concat,
   * ignores y child.
   */
  public IIconComposition<T> concat ();
  //====
  // * Default is concat if there are operands.
  //====

  /**
   * Product over x & y, i.e., for each x { for each y }.
   * If left child is in associative chain,
   * will apply any mapped operation to its children.
   * Returns this.
   */
  public IIconComposition<T> product ();

  /**
   * Zip over x % y, i.e., for each (x, y).
   * Iterates over x and y step-by-step in parallel.
   * Does not extend the shorter list but instead fails when its end is reached.
   * Returns this.
   */
  public IIconComposition<T> zip ();

  /**
   * Reduce operator over x.   This is a unary composition.
   * Reduce iteratively applies an operator between elements,
   * and returns a singleton result, or fails if the operator fails
   * or if the operand sequence is empty and there is no initial reduce value.
   * If no operator, i.e. null operator, then reduce always fails.
   * If reduce returns a result, then on next iteration it fails.
   * Returns this.
   * <P>
   * This aggregate operation over iterators is fold left, since
   * it applies an operator between each result of next().
   * If left child is in associative chain,
   * will apply this operation to its children.
   */
  public IIconComposition<T> reduce (BinaryOperator<IIconAtom<T>> operator);

  /**
   * Reduce operator over x, with an initial value.
   * Returns this.
   */
  public IIconComposition<T> reduce (BinaryOperator<IIconAtom<T>> operator,
		IIconAtom<T> initial);

  /**
   * Reduce with no operator.
   * If operator is null is equivalent to forall or every, i.e.,
   * iterates through the entire sequence but does not combine results,
   * and always fails.
   * Returns this.
   */
  public IIconComposition<T> reduce ();

  /**
   * Repeat iterator until it produces an empty sequence.
   * Returns this.
   */
  public IIconComposition<T> repeat ();

  /**
   * Return from this iterator.
   * Returns this.
   */
  public IIconComposition<T> doReturn ();

  /**
   * Suspend this iterator.
   * Returns this.
   */
  public IIconComposition<T> doSuspend ();

  /**
   * Raise a break condition.
   * Implies return to loop boundary.
   * Returns this.
   */
  public IIconComposition<T> doBreak ();

  /**
   * Raise a continue condition.
   * Implies return to loop's continue boundary.
   * Returns this.
   */
  public IIconComposition<T> doContinue ();

  //==========================================================================
  // Modifiers.  Modifiers can be attached to any composition.
  //==========================================================================

  /**
   * Map an operator over the operands in a composition.
   * If a unary composition or concat,
   * applies operator to each element of the operands,
   * i.e., over each result of nextAtom().
   * If a binary composition, applies operator to the pair of results.
   * <P>
   * Map is innermost, so it is applied over the operand results but before 
   * other compositions such as product and reduce.
   * So if the mapped operation returns fail, the operand or pair fails,
   * and for example in product the search will continue.
   * If the left child is in associative chain, for example (x & y & z).map(o),
   * map will also apply to its binary children,
   * e.g., ((x & y).map(o)) & z).map(o).
   * <P>
   * Map is equivalent to product with a trailing operation, as follows:
   * <PRE>
   * map(op).over(x) == (i in x) & op(i)
   * map(op).over(x,y).product() == (i in x) & (j in y) & op(i,j)
   * map(op).over(x,y).concat() == (i in (x | y)) & op(i)
   * </PRE>
   * <P>
   * Modifiers such as map, bound, exists, succeed, fail, limit, and guard
   * can be attached to any composition.
   * In particular,
   * map can be combined with reduce since they use different operators,
   * but map is innermost.
   * Modifiers are propagated down a left-associative chain if
   * the x operand isInChain, e.g., (x | y | z) => (x | y) | z.
   * <P>
   * Returns this.
   */
  public IIconComposition<T> map (BinaryOperator<IIconAtom<T>> operator);

  /**
   * Map using unary operator.
   * Returns this.
   */
  public IIconComposition<T> map (UnaryOperator<IIconAtom<T>> operator);

  /**
   * Map using binary operator with context.
   * Invoked as
   *	operate(IIconIterator<T> context, IIconAtom<T> x, IIconAtom<T> y).
   * Can be used for either binary or unary operations: is unary if y is null.
   * Returns this.
   */
  public IIconComposition<T> map (TriFunction<IIconIterator<T>,
		IIconAtom<T>> operator);

  /**
   * Override unaryMap or binaryMap to filter next result.
   * Returns this.
   */
  public IIconComposition<T> map ();

  /**
   * Get unary operator for map.
   */
  public UnaryOperator<IIconAtom<T>> getUnaryOperator ();

  /**
   * Get binary operator for map.
   */
  public BinaryOperator<IIconAtom<T>> getBinaryOperator ();

  /**
   * Get binary operator for map, with context.
   */
  public TriFunction<IIconIterator<T>, IIconAtom<T>> getContextOperator ();

  //==========================================================================
  // Filter methods.
  //==========================================================================
  /**
   * Unary map over results for operands, if isMap.
   * Executed after x.next() if have operands.
   * Will not execute if x.isFailed or x.isReturned.
   * Treats return value of FAIL as failure for x operand.
   * Default is identity function.
   */
  public IIconAtom<T> unaryMap (IIconAtom<T> x);

  /**
   * Binary map over results for operands, if isMap.
   * Executed after x.next() and y.next() if have operands.
   * Will not execute if x or y isFailed or isReturned.
   * Treats return value of FAIL as failure.
   * Default is to return x.
   */
  public IIconAtom<T> binaryMap (IIconAtom<T> x, IIconAtom<T> y);

  //==========================================================================
  // Limit modifiers.
  //==========================================================================

  /**
   * Limit iteration with number.
   * Returns this.
   */
  public IIconComposition<T> limit (long n);

  /**
   * Limit iteration with result of another iterator.
   * Returns this.
   */
  public IIconComposition<T> limitWithIterator (IconIterator<?> limit);

  //==========================================================================
  // Guarded choice.
  //==========================================================================

  /**
   * Guards iterator to effect guarded choice g->x|y.
   * Assumes guard has been separately evaluated.
   * Will choose a course of action between the two operands of the
   * current iterator based on if guard.exists().
   * Guarding will bypass (or fail if unary) the nextAtom() of the x operand 
   * if the guard failed and is empty.
   * For example, in a concat, it will choose 
   * the entirety of either the first or second operand based on this test.
   * In a product, it will ignore x if the guard fails, and just return y.
   * Returns this.
   */
  public IIconComposition<T> guard (IconIterator<?> g);

  /**
   * Get if composition is guarded.
   */
  public boolean isGuarded ();

  /**
   * Test guard for composition.
   */
  public boolean testGuard ();

}

//==== END OF FILE
