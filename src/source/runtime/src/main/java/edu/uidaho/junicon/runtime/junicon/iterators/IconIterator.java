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

import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;

import java.util.Iterator;
import java.util.Map;
import java.util.List;

import java.util.concurrent.Callable;

/**
 * Simple constant iterator that does not support composition.
 * Can be run as thread or future.
 *
 * @author Peter Mills
 */
public class IconIterator <T> implements IIconIterator <T> {

  public static boolean isDebug = false;

  //====
  // Identity for debugging and tracing
  //====
  String name = "";	// for debugging

  //====
  // Iterator state
  //====
  boolean isFailed = false;
  boolean isReturned = false;   // implied by isSuspended, isBreaked,isContinued
  boolean isSuspended = false;  // propagate up, but not past isMethod boundary
  boolean isContinuation = false; // suspend, propagate up past method boundary
  boolean isMethodReturned = false; // propagate up to method boundary
  boolean isEmpty = true;  // had no successful interations, i.e., count == 0
  int count = 0; // number of successful iterations, number of next()-1
  boolean haveDoneNext = false;
  IIconAtom<T> result = null;	// net result of last nextAtom()

  // Created atoms
  IIconAtom<T> lastResult = null;   // last non-failed result
  // IIconAtom<T> filterResult = null;   // last non-failed result

  // Continuation k(result)
  IIconAtom<T> continuationResult = null;
  boolean hasContinuationResult = false;

  // Loop
  boolean isBreaked = false;	// propagate up, but not past isLoop boundary
  boolean isContinued = false;	// propagate up, but not past isContinueBoundary

  // Lookahead
  boolean hasLookahead = false;
  IIconAtom<T> lookahead = null;

  // Undo
  IIconUndo<T> undo = null;

  //==========================================================================
  // Composition.  Only one of the following compositions can apply.
  //==========================================================================
  boolean isConstant = false;   // if is constant iterator, with no operands
  IIconAtom<T> constantValue = null; // for constant iterator
			// that returns value and never fails

  //====
  // Limit modifiers that can be attached to any iterator
  //====
  boolean isSingleton = false;	// limit 1, like bounded expression
  boolean isBounded = false;    // always fails on next, like reduced singleton
  boolean doNot = false;	// Flips fail and success
  boolean isAlwaysRestart = false; // exists(), alternative to isBounded
  boolean isAlwaysFail = false;    // fail without increasing nonempty count
  boolean isAlwaysSucceed = false; // succeed always, unless guarded
  boolean filterOnReturn = false; // filter result for return from method

  //====
  // Iterator parent
  //====
  IconIterator<T> parent = null;

  //====
  // Operands for composition
  //====
  IconIterator<T> x = null;  // first operand, if null then cannot suspend
  IconIterator<T> y = null;  // if unary, will be null
  boolean inLeft = true;		// Popped from left child
  boolean isComposition = false;	// If is IIconComposition

  //====
  // Cache for function body definition
  //====
  MethodBodyCache cache = null;
  String nameInCache = null; // uniqueified method name used in per-object cache
  VariadicFunction<T,T> unpack = null; // unpack args for variadic method

  //==========================================================================
  // next() methods
  //==========================================================================

  public IIconIterator<T> restart () {
	reset();
	afterRestart();
	return this;
  }

  public T next () {			// throws NoSuchElementException
	IIconAtom<T> value = nextAtom();
	if (value == null) { return null; }
	if (value == FAIL) { return (T) FAIL; }
	return value.deref();
  }

  public T nextOrNull () {		// throws NoSuchElementException
	IIconAtom<T> value = nextAtom();
	if (value == null) { return null; }
	if (value == FAIL) { return null; }
	return value.deref();
  }

  public IIconAtom<T> nextAtom () {	// throws NoSuchElementException
	// If already did hasNext(), just return lookahead
	if (hasLookahead) {
		hasLookahead = false;
		result = lookahead;
		return result;
	}

	// Handle continuation result: k(result).
	if (hasContinuationResult) {
		result = continuationResult;
		if (result == FAIL) { setIsFailed(true); }
		hasContinuationResult = false;
		nextEnd();
		return result;
	}

	nextBegin();
	if (useOnNextAdvice) { afterNextBegin(); } // advice: do even if failed
	if (! isFailed) {
		nextNoOperands();
		nextEnd();
	} else { nextEndFinally(); }
	if (useOnNextAdvice) { afterNextEnd(); }   // advice: do even if failed
	return result;	// Return result, not lastResult
  }

  //====
  // public IIconAtom<T> nextAtom (IIconAtom<T> continuationResult) {
  //	setContinuationResult(continuationResult);
  //	return nextAtom();
  // }
  //====

  public void nextBegin () {
	// Restart if failed, first next(), or always restart and not suspended.
	// 	If failed restart even if suspended, since do not resume on next
	if (isFailed || (! haveDoneNext) || isAlwaysRestart) {
		restart();
	}

	result = null;

	// Handle limit or singleton, i.e., bounded expression, unless suspended
	if ((isSingleton || isBounded) && haveDoneNext) {
		// Bounded/singleton/limit and guard trumps handleFailOverrides
		isFailed = true;
		result = FAIL;
		return;
	}

	// Handle undo if resumed, i.e., is undoable && !isEmpty.
	if (undo != null) {
		if (isEmpty) { undo.save(null);
		} else {
			undo.restore();
			isFailed = true;
			result = FAIL;
		}
	}
  }

  public void nextEnd () {
	haveDoneNext = true;

	// Handle fail overrides, not allowed with return/suspend/break/continue
	if (! isReturned) {
		handleFailOverrides(false);	// not ignore alwaysSucceed
	}

	// Handle failure
	if (isFailed) {
		result = FAIL;
		return;
	}

	// Handle succeed - return success result
	handleSucceed();
  }

  public void nextEndChildReturn () {
  }

  public void nextEndFinally () {
  }

  public void nextNoOperands () {
	result = provideNext();
	if (result == FAIL) { isFailed = true; };
  }
  //====
  // if (! (isFailed || isReturned)) {
  //	result = afterNextChild(result); // filterNext(), identity, can override
  //	if (result == FAIL) { isFailed = true; };
  // }
  //====

  /**
   * Begin nextAtom() for first operand x.
   * If unary, fails if either the child isFailed() or returns FAIL.
   * @return If should continue to process children, or just use this result.
   *    Default if not overridden is to process children.
   */
  public boolean nextChildBegin () {
	return true;
  }

  public IconIterator<T> nextChildEnd () {		// handlePoppedChild ()
	return null;
  }

  public IIconIterator<T> reset () {
	isFailed = false;
	isReturned = false;
	isSuspended = false;
	isContinuation = false;
	isMethodReturned = false;
	isBreaked = false;
	isContinued = false;

	isEmpty = true;
	count = 0;
	haveDoneNext = false; 
	result = null;

	lastResult = null;
	hasLookahead = false;

	continuationResult = null;
	hasContinuationResult = false;

	return this;
  }

  //==========================================================================
  // Iterator methods
  //==========================================================================
  public boolean hasNext () {
	if (! hasLookahead) {
		lookahead = nextAtom();
		hasLookahead = true;
	}
	return !isFailed;
  }
  //====
  // unlike Iterator, next() works until fail
  // hasNext() uses lookahead, like java iterator.
  //====

  public void remove () {
  }	// throws UnsupportedOperationException, IllegalStateException 

  //==========================================================================
  // Iteratable.
  //==========================================================================
  public java.util.Iterator<T> iterator () {
	return restart();
  }

  //==========================================================================
  // Helper methods.
  //==========================================================================

  /**
   * Handles succeed.  Sets isFailed, result.
   */
  void handleSucceed () {
	if (! isSuspended) {
		count++;
		isEmpty = false;
	}
	if (result != null) {
		if (filterOnReturn) { result = result.onReturn(); }
		//====
		// else if (filterOnNext) { result = result.onNext(); }
		//====
	}
if (isDebug) {
	if (name.isEmpty()) { name = this.getClass().getName(); }
	System.out.println("Succeed " + name + " " + result + (isSuspended()?" suspend":"") + (isReturned()?" return":""));
}

	lastResult = result;
  }

  /**
   * Handles fail.  Sets isFailed, result.
   */
  void handleFail () {
if (isDebug) {
	if (name.isEmpty()) { name = this.getClass().getName(); }
	System.out.println("Fail " + name + (isBounded()?" isBounded":"") + (isEmpty()?" isEmpty":""));
}
	isFailed = true;
	result = FAIL;
  }

  /**
   * Handle fail overrides.
   * If isGuarded then ignores isAlwaysSucceed.
   */
  void handleFailOverrides (boolean isGuarded) {
	    if (doNot) {
		if (isFailed) { result = null; }
		isFailed = ! isFailed;
	    }
	    // Guard trumps succeed -- if IsGuarded, use its value
	    if (isAlwaysSucceed && (! isGuarded)) {
		if (isFailed) { result = null; }
		isFailed = false;
	    }
	    if (isAlwaysFail) {
		isFailed = true;	// restart on next()
	    }
	    // isBounded not allowed with return/suspend/break/continue
	    if (isBounded && (! isAlwaysSucceed)) {
		// preserve non-empty status for guard
		if (! isFailed) {
			count++;
			isEmpty = false;
			lastResult = result;	// last non-failed result
		}
		isFailed = true;	// restart on next()
	    }
  }

  //==========================================================================
  // Override these methods for customized unary compositions.
  //==========================================================================

  public void afterRestart () {
  }

  public IIconAtom<T> provideNext () {
	if (! isConstant) { isFailed = true; }	// default is empty iterator
	return constantValue;			// null if not constant iterator
  }

  //==========================================================================
  // Advice to do on next, suspend, resume, or return.
  //==========================================================================

  IIconIterator<T> onSuspendAdvice = null;
  boolean useOnSuspendAdvice = false;
  boolean useOnNextAdvice = false;

  private IIconIterator<T> topLevel = null;

  public void setOnSuspendAdvice (IIconIterator<T> advice) {
	this.onSuspendAdvice = advice;
	this.useOnSuspendAdvice = (advice == null) ? false : true;
	if (parent != null) {
		getTopLevel().setOnSuspendAdvice(advice);
	}
  }

  public IIconIterator<T> getOnSuspendAdvice () {
	return onSuspendAdvice;
  }

  public IIconIterator<T> getTopLevel () {
	if (topLevel != null) { return topLevel; }	// cache top level
	topLevel = this;
	while (topLevel.getParent() != null) {
		topLevel = topLevel.getParent();
	}
	return topLevel;
  }

  public void setUseOnNextAdvice (boolean onoff) {
	useOnNextAdvice = onoff;
  }

  public boolean getUseOnNextAdvice () {
	return useOnNextAdvice;
  }

  public void afterSuspend () { }

  public void beforeResume () { }

  public void afterMethodReturn () { }

  public void afterNextBegin () { }

  public void afterNextEnd () { }

  //==========================================================================
  // Undo action.
  //==========================================================================
  public IIconIterator<T> undoable (IIconUndo<T> undo) {
	this.undo = undo;
	return this;
  }

  public IIconUndo<T> getUndoable () {
	return undo;
  }

  public boolean isUndoable () {
	return (undo != null);
  }

  //==========================================================================
  // Composition.  Only one of the following compositions can apply.
  //==========================================================================

  public boolean isComposition () { return isComposition; }

  public IIconComposition <T> asComposition () { return null; }

  //==========================================================================
  // Constructor Setters -- will return "this" to enable chaining
  //==========================================================================

  public IIconIterator<T> constant (T c) {
	this.constantValue = IconValue.create(c);	// create().setIsKeep()
	this.isConstant = true;
	return this;
  }

  public IIconIterator<T> constantAtom (IIconAtom<T> c) {
	this.constantValue = c;
	this.isConstant = true;
	return this;
  }

  public IIconAtom<T> getConstantAtom () {
	return constantValue;
  }

  public boolean isConstant () {
	return isConstant;
  }

  public IIconIterator<T> singleton (T c) {
	constant(c);
	singleton();
	return this;
  }

  public IIconIterator<T> singletonAtom (IIconAtom<T> c) {
	constantAtom(c);
	singleton();
	return this;
  }

  //====
  // Limit modifiers that can be attached to any composition.
  //====

  public IIconIterator<T> singleton () {
	this.isSingleton=true; return this; } // isBounded but non-empty

  public IIconIterator<T> bound () {
	this.isBounded=true; this.isSingleton=true; return this; }

  public IIconIterator<T> exists () {     // exists() is same as alwaysRestart()
	this.isAlwaysRestart=true; return this; }

  public boolean isBounded () {
	return isBounded; }

  public boolean isSingleton () {
	return (isSingleton || isBounded); }

  public IIconIterator<T> setIsSingleton (boolean b) {
	this.isSingleton = b; return this; }

  //====
  // Force fail, succeed, or not.
  //====

  public IIconIterator<T> fail () {
	this.isAlwaysFail = true; return this; }

  public IIconIterator<T> succeed () {
	this.isAlwaysSucceed = true; return this; } // if guarded use that

  public IIconIterator<T> not () {
	this.doNot = true; return this; }

  //====
  // Filter result for return from method or procedure
  //====
  public IIconIterator<T> filterOnReturn () {
	// Wrap result as IconValue if not already
	this.filterOnReturn = true;
	return this;
  }

  //====
  // Parent
  //====

  public IconIterator<T> getParent () { return parent; }
  public IIconIterator<T> setParent (IconIterator<T> parent) {
	this.parent = parent;
	return this;
  }

  //==========================================================================
  // Setters during evaluation
  //==========================================================================
  public boolean isFailed () { return isFailed; }

  public void setIsFailed (boolean failed) {
	isFailed = failed;
  }

  public boolean isEmpty () { return isEmpty; }

  public int getCount () { return count; }

  public IIconAtom<T> getLastResult () { return lastResult; }

  public IIconAtom<T> getResult () { return result; }

  public boolean getHaveDoneNext () { return haveDoneNext; }

  public boolean isReturned () { return isReturned; }

  public boolean isSuspended () { return isSuspended; }

  public void setIsSuspended () {
	isSuspended = true; 
	isReturned = true;
  }

  public boolean isContinuation () { return isContinuation; }

  public void setIsContinuation () {
	isContinuation = true; 
	isSuspended = true;
	isReturned = true;
  }

  public void setContinuationResult (IIconAtom<T> result) {
	continuationResult = result;
	hasContinuationResult = true;
  }

  public boolean isMethodReturned () { return isMethodReturned; }

  public void setIsMethodReturned () {
	isMethodReturned = true; 
	isReturned = true;
  }

  public boolean isBreaked () { return isBreaked; }

  public void setIsBreaked () {
	isBreaked = true; 
	isReturned = true;
  }

  public boolean isContinued () { return isContinued; }

  public void setIsContinued () {
	isContinued = true; 
	isReturned = true;
  }

  //==========================================================================
  // Name, i.e., identity for debugging and tracing
  //==========================================================================
  public String getName () { return name; }

  public IIconIterator<T> setName (String name) {
	if (name != null) { this.name = name; }
	return this;
  }

  //============================================================================
  // Thread support.
  //============================================================================

  public void run () {
	refresh().next();
  }

  public T call () {
	return refresh().next();
  }

  public IIconIterator<T> refresh () {
	return this;
  }

  //==========================================================================
  // Cache for function body definition
  //==========================================================================
  public MethodBodyCache getCache () { return cache; }

  public IIconIterator<T> setCache (MethodBodyCache cache,
		String name) {
	this.cache = cache;
	this.nameInCache = name; return this;
  }
  public String getNameInCache () { return nameInCache; }

  // Sets and invokes closure to unpack args from variadic method
  public IIconIterator<T> setUnpackClosure (VariadicFunction<T,T> unpack) {
	this.unpack = unpack;
	return this;
  }

  public IIconIterator<T> unpackArgs (T... args) {
	if (unpack != null) { unpack.apply(args); }
	return this;
  }

  //==========================================================================
  // Constructors
  //==========================================================================
  /**
   * Default is empty iterator that just fails.
   */
  public IconIterator () { }

  /**
   * Singleton iterator that returns a constant IconAtom.
   */
  public IconIterator (IIconAtom<T> v) {
	constantAtom(v);
	singleton();
  }

}

//==== END OF FILE
