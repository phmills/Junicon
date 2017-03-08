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
import java.util.Map;
import java.util.List;

import java.util.concurrent.Callable;

/**
 * Interface for a simple iterator that returns an IconAtom.
 * The default behavior is an empty iterator that just fails.
 * It can be set to a constant iterator
 * that returns a constant value and never fails.
 * If limited to be a singleton,
 * returns the constant value, and then fails, and then
 * on subsequent next() will restart.
 * <P>
 * Bounded, singleton, limit, and guard take priority over always succeed,
 * with guard taking higher precedence over other modifiers.
 * <P>
 * For operators as well as an iterator's nextAtom(),
 * there is no difference in behavior between null
 * and an atom that holds null, and null is not treated as failure.
 * <P>
 * This interface provides a subset of the full capabilities of IIComposition
 * for composing failure-driven and suspendable iterators.
 * <P>
 * An IIconIterator to be run as a thread or a future,
 * since it implements Runnable and Callable.
 * It also has a notion of clone in refresh().
 *
 * @author Peter Mills
 */
public interface IIconIterator <T> extends java.util.Iterator<T>,
			Iterable<T>, Callable<T>, Runnable {

  //==========================================================================
  // Restartable iterator.
  //==========================================================================

  /**
   * Restart iterator.
   * Invokes reset on operands, and invokes any afterRestart() advice.
   */
  public IIconIterator<T> restart ();

  /**
   * Produces the next dereferenced value in the iterator,
   * or fails and returns FAIL.
   * Equivalent to nextAtom().deref().
   * Returns dereferenced next atom, or FAIL if nextAtom is null or fails.
   */
  public T next ();			// throws NoSuchElementException

  /**
   * Produces the next dereferenced value in the iterator,
   * or fails and returns null.
   * Equivalent to nextAtom().deref().
   * Returns dereferenced next atom, or null if nextAtom is null or fails.
   */
  public T nextOrNull ();		// throws NoSuchElementException

  /**
   * Produces the next reified value in the iterator, or fails and returns FAIL.
   * The default action is an empty iterator that just fails,
   * or if constant to return a value but never fail.
   * NextAtom can return null: null is not treated as failure, and 
   * for operators as well as an iterator's nextAtom(),
   * there is no difference in behavior between null
   * and an atom that holds null.
   * In composition, operators will still be invoked using null
   * instead of a dereferenced value if their atom arguments are null.
   * Returns IconAtom, or null.
   * <P>
   * nextAtom is equationally defined as follows.
   * <PRE>
   * nextAtom () {
   *   nextBegin();
   *   if (useOnNextAdvice) { afterNextBegin(); }
   *   if (! isFailed) {
   *     if (no operands) { nextNoOperands() { provideNext(); }
   *     } else { nextChildBegin(); operands; unaryBinaryMap(); nextChildEnd();}
   *	 if (child return) { nextEndChildReturn();
   *     } else { nextEnd(); }
   *   } else { nextEndFinally(); }
   *   if (useOnNextAdvice) { afterNextEnd(); }	// do even if failed
   * }
   * </PRE>
   * Behavior can be customized by overriding afterRestart(), provideNext(),
   * and unary/binaryMap(), as well as methods that begin with
   * "on", "after", or "before".
   * These include afterRestart(), afterSuspend(), afterResume(), and
   * afterMethodReturn().
   * It is not recommended to override other nextAtom methods,
   * since they contain composition logic.
   */
  public IIconAtom<T> nextAtom ();	// throws NoSuchElementException

  //====
  // * Sets continuation result before nextAtom.
  // * Overrides result of last nextAtom().
  // * Be careful using this method: the point of continuation
  // * may not be a top-level iterator.
  //====
  // public IIconAtom<T> nextAtom (IIconAtom<T> continuationResult);
  //====

  /**
   * Returns true if next() will not fail.
   * Stores result as lookahead.
   * Lookahead only works for the outermost iterator.
   * <P>
   * Unlike java.util.Iterator, IconIterator's
   * next() can be invoked until it fails.
   * In other words, an IconIterator is terminated when next() fails
   * and returns FAIL, and a following next() will restart the iterator.
   * Thus, to maintain consistency with java.util.Iterator,
   * hasNext() must use lookahead of next() to see if it fails.
   * Be aware that, since next() can have side-effects,
   * such lookahead can pose interference.
   */
  public boolean hasNext ();

  /**
   * Removes the most recently iterated item.
   * Not implemented.
   */
  public void remove ();
	// throws UnsupportedOperationException, IllegalStateException 

  /**
   * Reset iterator state to empty and not failed.
   */
  public IIconIterator<T> reset ();

  //==========================================================================
  // Iteratable.
  //==========================================================================
  /**
   * Return restart of this iterator.
   */
  public java.util.Iterator<T> iterator ();

  //==========================================================================
  // Override these methods for customized unary compositions.
  //==========================================================================

  /**
   * Executed at end of restart().
   */
  public void afterRestart ();

  /**
   * Get next() for no operands.
   * Called from nextNoOperands().
   * Default is an empty iterator that just fails,
   * or if constant to return a value but never fail.
   */
  public IIconAtom<T> provideNext ();

  //==========================================================================
  // Advice around next(), suspend(), resume(), and method return().
  //==========================================================================

  /**
   * Sets advice to do after suspend, before resume, or after return.
   * Sets top-level parent iterator to use this advice;
   * the top-level iterator is the executor that loops through nested iterators.
   * If advice is non-null, will do
   * advice.afterSuspend(), advice.beforeResume(), advice.afterMethodReturn()
   * after each of these events.
   * Equivalent to getTopLevel().setOnSuspendAdvice(advice).
   */
  public void setOnSuspendAdvice (IIconIterator<T> advice);

  /**
   * Returns iterator to use for suspend/resume/return advice.
   * Only does advice if non-null.
   */
  public IIconIterator<T> getOnSuspendAdvice ();

  /**
   * Advice to perform after suspend.
   */
  public void afterSuspend ();

  /**
   * Advice to perform before resume.
   */
  public void beforeResume ();

  /**
   * Advice to perform after return.
   */
  public void afterMethodReturn ();

  /**
   * Sets if use advice after nextBegin() and nextEnd().
   */
  public void setUseOnNextAdvice (boolean onoff);

  /**
   * Gets if use advice after nextBegin() and nextEnd().
   */
  public boolean getUseOnNextAdvice ();

  /**
   * Advice to perform after nextBegin().
   * Does advice if useOnNextAdvice is true, even if failed.
   */
  public void afterNextBegin ();

  /**
   * Advice to perform after nextEnd().
   * Does advice if useOnNextAdvice is true, even if failed.
   */
  public void afterNextEnd ();

  /*
   * Get top-level outermost parent.
   */
  public IIconIterator<T> getTopLevel ();

  //==========================================================================
  // nextAtom() customization.  Can override these methods, but not recommended.
  //==========================================================================

  /**
   * Begin nextAtom().
   * Sets isFailed, result.
   * Can fail if exceeds limit.
   */
  public void nextBegin ();

  /**
   * Do nextAtom() for no operands.
   * Sets isFailed, result.
   */
  public void nextNoOperands ();

  /**
   * Begin nextAtom() for first operand x.
   * If unary, fails if either the child isFailed() or returns FAIL.
   * @return If should continue to process children, or just use this result.
   *	Default if not overridden is to process children.
   */
  public boolean nextChildBegin ();

  /**
   * Handle popped child.
   * Returns another child to be pushed, or null.
   */
  public IconIterator<T> nextChildEnd();	// handlePoppedChild ();

  /**
   * End nextAtom(), if not bound or undo in nextBegin(), and not child return.
   * Sets isFailed, result.
   */
  public void nextEnd ();

  /**
   * End nextAtom() if composition and child return.
   */
  public void nextEndChildReturn ();

  /**
   * End nextAtom(), if nextEnd() is not executed, and not child return.
   * Either nextEnd(), nextEndIfBound(), or nextEndChildReturn()
   * is always executed.
   */
  public void nextEndFinally ();

  //==========================================================================
  // Composition.  Only one of the following compositions can apply.
  //==========================================================================

  /**
   * Returns true if this is a composition, i.e., also an IconComposition.
   * The default behavior if no operands is an empty iterator
   * that just fails; or can be set to a constant iterator that never fails;
   * otherwise the default is concatenation.
   */
  public boolean isComposition ();

  /**
   * Returns this iterator as a composition, or null if not a composition.
   */
  public IIconComposition <T> asComposition ();

  /**
   * Constant iterator.
   * Returns an atom, i.e., property, holding the given value,
   * and never fails unless limited or singleton.
   */
  public IIconIterator<T> constant (T c);

  /**
   * Constant iterator that returns the given atom,
   * and never fails unless limited or singleton.
   */
  public IIconIterator<T> constantAtom (IIconAtom<T> c);

  /**
   * Singleton iterator that returns the given value, then fails.
   * Equivalent to constant(v).singleton().
   * Returns this.
   */
  public IIconIterator<T> singleton (T c);

  /**
   * Singleton iterator that returns the given atom, then fails.
   * Equivalent to constantAtom(v).singleton().
   * Returns this.
   */
  public IIconIterator<T> singletonAtom (IIconAtom<T> c);

  /**
   * Get if is constant iterator, with no operands.
   */
  public boolean isConstant();

  /**
   * If constant, get constant value.
   */
  public IIconAtom<T> getConstantAtom();

  //==========================================================================
  // Limit modifiers that can be attached to any iterator.
  //==========================================================================

  /**
   * Bound the iterator.
   * Sets limit to 1, i.e. sets is singleton,
   * but remembers if it was non-empty, i.e., had a successful iteration.
   * Optimized by forcing to always fail.
   */
  public IIconIterator<T> bound ();

  /**
   * Set limit to 1.  Will iterate once, then fail.
   */
  public IIconIterator<T> singleton ();

  /**
   * Does next(), then remembers if was non-empty, i.e., was successful.
   * Optimized by forcing to always restart.
   */
  public IIconIterator<T> exists ();

  /**
   * Returns if is bounded.
   */
  public boolean isBounded ();

  /**
   * Returns if limited to 1 iteration, i.e., is singleton.
   * This occurs if limit of 1, singleton, or bounded was set.
   */
  public boolean isSingleton ();

  /**
   * Turn singleton on or off.
   */
  public IIconIterator<T> setIsSingleton (boolean on);

  //==========================================================================
  // Force fail, succeed, or not.
  //==========================================================================

  /**
   * Forces to always fail.
   */
  public IIconIterator<T> fail ();

  /**
   * Forces to always succeed.
   * Typically used in conjunction with limit.
   * Bounded, singleton, limit, and guard take priority over always succeed,
   * with guard taking higher precedence than other modifiers.
   * In other words, limit and guard trump always succeed, and
   * the iterator will fail after the limit is exceeded or if the guard fails.
   */
  public IIconIterator<T> succeed ();

  /**
   * At each iteration, flips fail and succeed.
   */
  public IIconIterator<T> not ();

  //==========================================================================
  // Filter result.
  //==========================================================================

  /**
   * Perform postprocessing on an atom before being returned
   * by nextAtom(), by invoking that atom's onReturn().
   * Intended for use on return from method or procedure.
   */
  public IIconIterator<T> filterOnReturn ();

  //==========================================================================
  // Undo action.
  //==========================================================================

  /**
   * Sets the undo action.
   * If non-null, makes next() undoable, i.e.,
   * will undo and fail on resume.
   * Will save the environment on a next(),
   * and restore the environment and fail if it is resumed,
   * i.e., if next() occurs and it is non-empty with count > 0.
   * Returns this.
   */
  public IIconIterator<T> undoable (IIconUndo<T> undo);

  /**
   * Gets the undo action.
   * If non-null, next() is undoable, and will be reversed if it is resumed.
   */
  public IIconUndo<T> getUndoable ();

  /**
   * Gets if is undoable.
   * True if getUndoable() is non-null.
   */
  public boolean isUndoable ();

  //============================================================================
  // Thread and co-expression support.
  //============================================================================

  /**
   * Run iterator in thread.
   * Default is to perform refresh().next().
   */
  public void run ();

  /**
   * Run iterator in thread as a future.
   * Default is to return refresh().next().
   */
  public T call ();

  /**
   * Clone iterator to run in separate thread.
   * Default is to just return this.
   */
  public IIconIterator<T> refresh ();

  //====
  // * Freeze referenced environment.
  // * Default is to do nothing.
  //====
  // public IIconIterator<T> freeze ();
  //====
  // * It also has a notion of clone in refresh().
  // * and of freezing its local environment in freeze().
  // * Clone iterator.  Default is to just freeze().
  //====

  //==========================================================================
  // Parent.
  //==========================================================================

  /**
   * Get parent iterator, which uses this as an operand in composition.
   */
  public IconIterator<T> getParent ();

  /**
   * Set parent iterator, which uses this as an operand in composition.
   * Returns this.
   */
  public IIconIterator<T> setParent (IconIterator<T> p);

  //==========================================================================
  // Setters during evaluation.
  //==========================================================================

  /**
   * Get last non-failed result from next().
   */
  public IIconAtom<T> getLastResult ();

  /**
   * Get last result from next().  Can be FAIL if isFailed.
   */
  public IIconAtom<T> getResult ();

  /**
   * Get if had no successful iterations.
   */
  public boolean isEmpty ();

  /**
   * Get number of successful iterations.
   */
  public int getCount ();

  /**
   * Get is have done a next since last restart.
   */
  public boolean getHaveDoneNext ();

  /**
   * Get if failed.
   */
  public boolean isFailed ();

  /**
   * Sets if failed.
   */
  public void setIsFailed (boolean isFailed);

  /**
   * Get if is returned, i.e.,
   * an iterator child issued a return, suspend, break, or continue statement.
   * Return can fail:
   * can fail and return or break or continue, but cannot fail and suspend.
   */
  public boolean isReturned ();

  /**
   * Get if is suspended,
   * i.e., an iterator child issued a suspend.
   */
  public boolean isSuspended ();

  /**
   * Sets if is suspended.
   * Also sets isReturned.
   */
  public void setIsSuspended ();

  /**
   * Get if is a continuation.
   * A continuation is like a suspend, except propagates path method boundaries.
   * If isContinuation, then isSuspended.
   */
  public boolean isContinuation ();

  /**
   * Sets if is a continuation.
   * Also sets isSuspended, isReturned.
   */
  public void setIsContinuation ();

  /**
   * Sets continuation result.
   * Overrides result of last nextAtom().
   * Intended to be used with setIsContinuation().
   * <B>
   * USAGE: nextAtom { setIsContinuation(); return coexpr; }
   * <PRE>
   * Note that each outermost iterator is a continuation k.
   *	k = nextAtom(){continue k'} ; k(result)
   * Contrast with continuation-passing style call/cc:
   *	f(args,k) { ... k(result) }
   * </PRE>
   */
  public void setContinuationResult (IIconAtom<T> result);
  //====
  // Continuation k(result)
  // Handle continuation result: k(result).
  //====
  // * <PRE>
  // * USAGE: k = k(){continue k'} ; k(result)
  // *		where k()=nextAtom(), i.e., iterator is a continuation k.
  // *	nextAtom { setIsContinuation(); return coexpr; }
  // *	nextAtom(result) = { k(result); }
  // * Contrast with continuation-passing style call/cc:
  // *	f(args,k) { ... k(result) }
  // * </PRE>
  //====

  /**
   * Get if is a method return,
   * i.e., an iterator child issued a return.
   */
  public boolean isMethodReturned ();

  /**
   * Sets if is a method return.
   * Also sets isReturned.
   */
  public void setIsMethodReturned ();

  /**
   * Get if is a break statement,
   * i.e., an iterator child outside of a loop issued a break.
   */
  public boolean isBreaked ();

  /**
   * Sets if is a break statement.
   * Also sets isReturned.
   */
  public void setIsBreaked ();

  /**
   * Get if is a continue statement,
   * i.e., an iterator child outside of a loop issued a continue.
   */
  public boolean isContinued ();

  /**
   * Sets if is a continue statement.
   * Also sets isReturned.
   */
  public void setIsContinued ();

  //==========================================================================
  // Cache for function body definition
  //==========================================================================

  /**
   * Sets class cache used for function body definitions,
   * and sets this iterator's name to be used in the cache.
   * Returns this.
   */
  public IIconIterator<T> setCache (MethodBodyCache cache, String name);

  /**
   * Get class cache for function body definitions.
   */
  public MethodBodyCache getCache ();

  /**
   * Gets this iterator's name in cache for function body definitions.
   */
  public String getNameInCache ();

  /**
   * Sets closure to unpack args from variadic method.
   * Returns this.
   */
  public IIconIterator<T> setUnpackClosure (VariadicFunction<T,T> unpack);

  /**
   * Invokes closure to unpack args from variadic method.
   * Returns this.
   */
  public IIconIterator<T> unpackArgs (T... args);

}
 
//==== END OF FILE
