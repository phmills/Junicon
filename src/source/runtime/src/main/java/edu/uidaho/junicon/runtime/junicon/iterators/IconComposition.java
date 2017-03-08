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
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;

/**
 * Core logic for composing failure-driven and suspendable iterators.
 * The iterator is restartable and optionally revertible on failure.
 * <P>
 * An IconIterator is failure-driven in that
 * next() produces a value until it fails.
 * In other words, an IconIterator is terminated when next() fails
 * and returns FAIL, and a following next() will then restart the iterator.
 * <P>
 * An IconIterator can either be a constant node, or is 
 * the composition of one or two iterators using such forms as product or
 * concatenation.
 * IconIterator is suspendable in that if next() suspends,
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
public class IconComposition <T> extends IconIterator <T>
		implements IIconComposition <T> {

  //====
  // State of iterator
  //====
  boolean haveXresult = false;	// nextX() produced result, implies !x.isFailed
		// May be false even if ! x.isFailed(), if not pass guard
  IIconAtom<T> lastXresult = null;	// from nextX
  boolean xIsEmpty = true;	// is empty after map in nextX  (map might fail)
  boolean haveYresult = false;
  IIconAtom<T> lastYresult = null;

  // Reduce
  private boolean haveReducedOnce = false;
  private boolean haveReduceResult = false;	// Have non-fail reduce result
  private IIconAtom<T> reduceInitial = null;

  // Created atoms: operators, reduce
  private IIconAtom<T> reduceResult = null;
  //====
  // private IIconAtom<T> mapResult = null;
  // // private IIconAtom<T> filterResult = null;	// In IconIterator
  // // private IIconAtom<T> lastResult = null;		// In IconIterator
  //====

  // Never restart
  private boolean haveRestartedOnce = false;  // for isNeverRestart

  // Map info = new HashMap(); // debug info:line/col/source, if in suspend chain

  //====
  // Only one of the following compositions can apply
  //====
  private boolean isProduct = false;	// can only be true if binary
  private boolean isConcat = false;  // default if operands and no composition
				     // is like unary concat, ignores y child.
		// XX: WAS: default is concat if binary: isConcat iff !isProduct
  private boolean isZip = false;	// interleave
  private boolean isReduce = false;	// instead of map, with operator
  private boolean isRepeat = false;	// can only be true if isUnary
  private boolean isSuspendStatement = false; // causes suspend event (isSuspended)
  private boolean isContinuationStatement = false; // causes continuation event
  private boolean isBreakStatement = false;    // causes break event (isBreak)
  private boolean isContinueStatement = false; // causes skip event (isContinue)
  private boolean isReturnStatement = false; // causes return event (isReturned)

  //====
  // Limit modifiers that can be attached to any composition
  //====
  private boolean isNeverRestart = false;  // consumable resource, never renews

  //====
  // Modifiers that can be attached to any composition
  //====
  private boolean isMap = false; // map if operator != null, isMap iff !isReduce
  private boolean isUnaryMap = true;  // if isMap, map over each of x and y
  private long upperlimit = -1;	// if >= 0, limits number of iterations to at most this
  private IconIterator<?> limitWithIterator = null;  // limits iteration to last result
  private IconIterator<?> guard = null; // uses testGuard() method, set by guard's last next
  private boolean isGuarded = false;	// if guard is non-null
  private boolean isGuardFailed = false;
  private boolean isLoop = false;	// loop boundary, for break
  private boolean isContinueBoundary = false; // continue boundary, for continue

  // Set by constructor
  private boolean isUnary = true;  // default is unary: isBinary iff y!=null
  boolean xIsInChain = false;   // left operand in associative chain (x & y) & z

  // Operator for map and reduce : null if no operation
  private UnaryOperator<IIconAtom<T>> unaryOperator = null;
  private BinaryOperator<IIconAtom<T>> binaryOperator = null;
  private TriFunction<IIconIterator<T>, IIconAtom<T>> contextOperator = null;
  private BinaryOperator<IIconAtom<T>> reduceOperator = null;

  // nextAtom state
  IconIterator<T> current = null;	// Current place in stack
  boolean isPopped = false;		// Must handle child result

  //==========================================================================
  // Iterator methods.
  //==========================================================================

  //====
  // Restart is invoked onFirstNext, and invokes reset on operands
  //	Restart differs from reset in that it invokes afterRestart, which
  //	for example will perform function invocation for IconInvokeIterator.
  //====
  public IIconIterator<T> restart () {
	if (isNeverRestart && haveRestartedOnce) { return this; };
	reset();
	if (x != null) x.reset();  // if (x != null) x.restart();
	if (y != null) y.reset();  // if (y != null) y.restart();
	deriveLimit();		// from limit iterator if specified
	afterRestart();
	haveRestartedOnce = true;
	return this;
  }

  //====
  // Produces the next reified value in the iterator, or fails and returns FAIL.
  // The default action is an empty iterator that just fails,
  // or if constant to return a value but never fail.
  // If the iterator is a composition over other iterator operands,
  // it interprets a child as failing if either it isFailed() or returns FAIL.
  // Returns IconAtom, or null.
  //====
  public IIconAtom<T> nextAtom () {	// throws NoSuchElementException
    boolean isDone = false;
    IconIterator<T> topush = null;
    IconIterator<T> prev = null;

    // Use lookahead is already did hasNext()
    // Lookahead only works for outermost iterator
    if (hasLookahead) {
	hasLookahead = false;
	result = lookahead;
	return result;
    }

    // If suspended, just use current.   Otherwise, make sure are top-level.
    if ((! isSuspended) || (current == null)) {
	current = this;
	current.parent = null;
    }
    if (isSuspended && useOnSuspendAdvice) { onSuspendAdvice.beforeResume(); }

    // Reset state
    isSuspended = false;
    isContinuation = false;
    isReturned = false;
    isPopped = false;
    isDone = false;

    // Outermost next: chain through composition until succeed, fail, or suspend
    while (! isDone) {
	// Process child result, for composition
	if (isPopped) {
   	  isPopped = false;
	  topush = current.nextChildEnd();	// handlePoppedChild();
	  if ((topush != null) && (! current.isFailed)) {
		// Push another child
		prev = current;
		current = topush;
		current.parent = prev;		// Set parent, just to be safe
		continue;
	  }
	  current.nextEnd();	// if (isReturned) { handleChildReturn(); }
	  if (current.useOnNextAdvice) { current.afterNextEnd(); } // even if fail
	} else if (current.hasContinuationResult) {
	  // Handle continuation result: k(result).
	  current.nextBegin();
	  current.result = current.continuationResult;
	  if (current.result == FAIL) { current.setIsFailed(true); }
	  current.hasContinuationResult = false;
	  current.nextEnd();	
	  //====
	  // haveDoneNext = true;
	  // if (! isFailed) { count++; isEmpty = false; }
	  // if (! isFailed) { lastResult = result; }
	  //====
	} else {
	  // Next(): push child, pop to parent, or top-level return
	  if (current.isComposition) {
	    current.nextBegin();
	    if (current.useOnNextAdvice) { current.afterNextBegin(); } // even if fail
	    if (! current.isFailed) {
	      if (current.x == null) {		// No operands
		current.nextNoOperands();
	      } else {				// Composition, push X or Y
		boolean doChildren = current.nextChildBegin();
		if ((! current.isFailed) && doChildren) {
			// Can fail from guard if unary
		  prev = current;		// Push X or Y
		  if (current.inLeft) { current = current.x;
		  } else { current = current.y; }
		  current.parent = prev;	// Set parent, just to be safe
		  continue;
		}
	      }
	      current.nextEnd();
	    } else { current.nextEndFinally(); }
	    if (current.useOnNextAdvice) { current.afterNextEnd(); } // even if fail
	  } else {
	    current.nextAtom();
	  }
	}

	// Slave back to parent
	if (current.parent != null) {
	  //====
	  // Handle suspend or method return, but ignore break and continue.
	  // Break and continue will just cycle back up to loop point.
	  //	isReturned = suspend | method return | break | continue
	  //====
	  if (current.isReturned && handleChildReturn(current)) {
		nextEndChildReturn();
		if (current.isMethodReturned && useOnSuspendAdvice) {
			onSuspendAdvice.afterMethodReturn();
		} else if (current.isSuspended && useOnSuspendAdvice) {
			onSuspendAdvice.afterSuspend();
		}
		return current.result;
	  }
	  // Pop to parent, otherwise
	  current = current.parent;
	  isPopped = true;
	  continue;
        }
	// At outermost top level, have done nextEnd, so just return next result
	return current.result;	// Return result, not lastResult (parent==this)
    }
    return current.result;	// Never gets here
  }

  //====
  // Reset iterator state.
  //====
  public IIconIterator<T> reset () {
	super.reset();

	haveReducedOnce = false;	// must do here since may suspend
	haveReduceResult = false;
	reduceResult = null;

	haveXresult = false;
	lastXresult = null;
	xIsEmpty = true;
	inLeft = true;

	return this;
  }

  //==========================================================================
  // Child iterator methods.
  //==========================================================================

  public IconIterator<T> nextChildEnd () { 		// handlePoppedChild ()
    if (inLeft) {  // Handle nextX()
	result = x.result;

	//====
	// Handle break and continue for loop, these are unary.
	// Already handled suspend and methodReturn in outer loop.
	// Continue should cycle to next without fail (skip).
	//====
	if (x.isReturned) {
		if (filterChildReturn(x)) {
			lastXresult = result;
			return x;				// Push x
		}
		lastXresult = result;
		return null;
	}

	if (! x.isFailed) {
	        //====
	        // filterNext() if not failed or returned
	        //====
		// result = afterNextChild(result); // identity, can override
		// if (result == FAIL) { return x; }		// Push x
		//====

		//====
		// Handle undo if is undoable.
		//====
		if (undo != null) {
			undo.save(result);
		}

		//====
		// Map if isMap -- map is innermost
		//====
		if (isMap && (isUnary || isUnaryMap)) {
			result = unaryMap(result);
			//====
			// Skip if fail since map(op).over(x) = (i in x) & op(i)
			//====
			if (result == FAIL) { return x; }	// Push x
		}
		xIsEmpty = false;
	}

	//====
	// The following will only occur for unary reduce and repeat
	// (where continue will cycle at loop boundary).
	//====
	if (isRepeat && (x.isFailed && (! xIsEmpty))) {
		// x.reset();
		xIsEmpty = true;
		return x;					// Push x
	}

	if (isReduce) {		// Reduce is unary, so can cause isFailed.
		if ((! haveReducedOnce) && (reduceInitial == null)) {
			reduceResult = result;
			haveReducedOnce = true;
		} else {
		    if (! haveReducedOnce) {
			reduceResult = reduceInitial;
			haveReducedOnce = true;
		    }
		    if ((! x.isFailed) && (reduceOperator != null)) {
			// Left fold
			//====
			// if (reduceOperator.isUseContext()) {
			// reduceResult = reduceOperator.operateWithContext(this, reduceResult, result);
			// } else { ... }
			//====
			reduceResult = reduceOperator.apply(reduceResult, result);
			// Boolean operators return second arg if true else fail
			if (reduceResult == FAIL) {
				isFailed = true;
				reduceResult = null;
			}
		    }
		}
	} else {
		if (! x.isFailed) { haveXresult = true; }
	}

	//====
	// Continue reduce if x not failed.  For reduce, may fail on boolean.
	// nextX(): while ((! x.isFailed) && (! haveXresult) && (! isFailed) 
	//	&& (x.isSuspended() || ((! isGuarded) || testGuard())))
	//====
	// Reduce returns singleton or fails; if null operator always fails
	//====
	if (isReduce) {		// End of reduce : reduce is unary
		if ((! x.isFailed) && (! haveXresult) && (! isFailed)) {
			return x;				// Push x
		}
		result = reduceResult;
		if ((reduceOperator == null) || (! haveReducedOnce)) {
			isFailed = true;
		} else {
			haveXresult = true;
			haveReduceResult = true;
		}
	} else if ((x.isFailed || (! haveXresult)) && isUnary) {
		isFailed = true;
	}
	lastXresult = result;

	//====
	// End of nextX().   We have a result, or x.isFailed.
	//	Will only have isFailed if unary, and reduce is unary.
	// WATCH OUT: Product can be unary, so must exclude unary first.
	//====
	if (isUnary) {
	  if (isFailed) { result = FAIL; }
	  return null;		// DO NOT push x.  Let parent handle result.
	}
	//====
	// if ((! isFailed) && (! isUnary)) {
	//	result = nextYSearch();
	//	if (isReturned) { return handleChildReturn(); } }
	//====

	//====
	// Get next() for operands besides first x : if unary, we are done.
	// Pass nextX() to product, zip, concat, then unary.
	//	Do this even if x.isFailed.
	//====
	// Binary concatenation (sequence): for each x ; for each y
  	// Binary product: for each x { for each y }
	//	Does search, if y fails, backtracks to next x
	// Binary zip: for each (x,y), step-by-step in parallel
	//	Does y iterator in parallel
	//====
	if (isProduct ||  isZip) {
	    if (! haveXresult) {
		isFailed = true;	// filterChildFail()
		result = FAIL;
		return null; 
	    }
	    haveYresult = false;
	    lastYresult = null;
	    inLeft = false;
	    return y;						// Push y
	}

	if (isConcat) {
	  if (! isUnary) {		// Default is concat
	   if (haveXresult) {
		result = lastXresult;
		return null;
	   }
	   if (isGuarded && testGuard()) {			// Skip y
		isFailed = true;	// filterChildFail()
		result = FAIL;
		return null;
	   }
	   haveYresult = false;
	   lastYresult = null;
	   inLeft = false;
	   return y;						// Push y
	  }
	}

	//====
	// Should never get here, unless operands and no composition.
	//====
	if (x.isFailed || (! haveXresult)) { isFailed = true; }
	if (isFailed) { result = FAIL; }
	return null;	// Should never get here.   DO NOT push.
    }

    //====
    // inRight : nextY()
    //====
    result = y.result;

    //====
    // Handle break and continue for loop, these are unary.
    // Already handled suspend and methodReturn in outer loop.
    //====
    if (y.isReturned) {
	if (filterChildReturn(y)) {
		lastYresult = result;
		return y;					// Push y
	}
	lastYresult = result;
	return null;
    }

    // Handle product, zip, concat
    if (isProduct) {
	if (isMap && (! y.isFailed)) {
		result = binaryMap(lastXresult, result);
		//====
		// Skip if fail since
		//	map(op).product(x,y) = (i in x) & (j in y) & op(i,j)
		//====
		if (result == FAIL) { return y; }		// Push y
	}
	if (y.isFailed) {
		if (x.isFailed) {
			isFailed = true;	// filterChildFail()
			result = FAIL;
			return null;
	  	}
		inLeft = true;
		haveXresult = false;
		lastXresult = null;
		return x;					// Push x
	}
	haveYresult = true;
	lastYresult = result;
    	return null;		// DO NOT push y.  Let parent handle result.
    }

    if (isZip) {
	if (isMap && (! y.isFailed)) {
		result = binaryMap(lastXresult, result);
		//====
		// Skip if fail since
		//	map(op).product(x,y) = (i in x) & (j in y) & op(i,j)
		//====
		if (result == FAIL) { return y; }		// Push y
	}
	if (y.isFailed) {
		isFailed = true;	// filterChildFail()
		result = FAIL;
		return null;
	}
	inLeft = true;
	haveXresult = false;
	lastXresult = null;
    	return x;			// Push x
    }

    if (isConcat) {
      if (! isUnary) {		// Default is concat
	if (isMap && (! y.isFailed)) {
		result = unaryMap(result);	// y.getResult()
		//====
		// Skip if fail since map(op).over(x) = (i in x) & op(i)
		//====
		if (result == FAIL) { return y; }		// Push y
	}
	if (y.isFailed) {
		isFailed = true;	// filterChildFail()
		result = FAIL;
		return null;
	}
	haveYresult = true;
	lastYresult = result;
    	return null;		// DO NOT push y.  Let parent handle result.
      }
    }

    //====
    // Should never get here, unless operands and no composition.
    //====
    if (isFailed) { result = FAIL; }
    return null;	// Should never get here.  DO NOT push.
  }

  /**
   * Begin nextAtom() for first operand x.
   * If isGuarded and guard fails, if unary then fails, otherwise skips to y.
   * Later on, in nextChildEnd (handlePoppedChild),
   * if unary, fails if either the child isFailed() or returns FAIL.
   * @return If should continue to process children, or just use this result.
   *	Default if not overridden is to process children.
   */
  public boolean nextChildBegin () {
    if (y == null) { isUnary = true; }	// Just to be sure, so no null pointer
    if (inLeft) {
	haveXresult = false;
	lastXresult = null;
	if (isGuarded && (! testGuard())) {
		if (isUnary) {
			isFailed = true;
			result = FAIL;
		} else {			// Go to Y
			inLeft = false;
		}
	}
    }
    if (! inLeft) {
	haveYresult = false;
	lastYresult = null;
    }

if (isDebug) {
	if (name.isEmpty()) { name = this.getClass().getName(); }
	if (inLeft) { System.out.println("X next " + name);
	} else { System.out.println("Y next " + name); }
}

  return true;

  }

  public void nextBegin () {
	// Restart if failed, first next(), or always restart and not suspended.
	// 	If failed restart even if suspended, since do not resume on next
	if (isFailed || (! haveDoneNext) ||
			(isAlwaysRestart && (! isSuspended))) {
		restart();
		if (isFailed) { // for neverRestart
			result = FAIL;		// handleFail()
			return;
		}
	}

if (isDebug) {
	if (name.isEmpty()) { name = this.getClass().getName(); }
	System.out.println("Next " + name + (isSuspendStatement?" SuspendStatement":""));
}

	result = null;

	// Handle limit or singleton (i.e., bounded expression) unless suspended
	//	IconReturn is bounded, so will fail after returns, but can avoid
	// 	re-descending into iterator by below isMethodReturned check.
	if (! isSuspended) {
	  if (((isSingleton || isBounded) && haveDoneNext)
			|| ((upperlimit >= 0) && (count >= upperlimit))
			|| isMethodReturned) {
		haveDoneNext = true;
		isFailed = true;	// handleFail()
		result = FAIL;
		return;
	  }

	  // If reduce returned a result last time, then fail this time
	  if (isReduce && haveReduceResult) {
		isFailed = true;
		result = FAIL;
		return;
	  }

	  // Handle undo if resumed, i.e., is undoable && !isEmpty.
	  if (undo != null) {
		if (!isEmpty) {
			undo.restore();
			isFailed = true;
			result = FAIL;
			return;
		}
	  }
	  //====
	  // if (isEmpty) { undo.save(x); } else { undo.restore(); return fail;}
	  //====

	}

	isReturned = false;	// Implied by isSuspended, isMethodReturned, 
	isSuspended = false;	// 	isBreaked, isContinued
	isContinuation = false; // Implies isSuspended
	isMethodReturned = false;
	isBreaked = false;
	isContinued = false;
  }

  public void nextEnd () {
	haveDoneNext = true;

	//====
	// // After NextBegin:
	// if (isFailed) { result = FAIL; return FAIL; } // No nextEnd()
	// // After nextX, or nextYSearch, with filterChildReturn
	// if (isReturned) { return handleChildReturn(); } // No nextEnd()
	//====

	// HandleMyReturn: handle return/suspend/break/continue statement
	if (! isReturned) {
	    if (isReturnStatement) {
		isMethodReturned = true; isReturned = true;
	    } else if (isSuspendStatement & (! isFailed)) {
		isSuspended = true; isReturned = true;
	    } else if (isContinuationStatement & (! isFailed)) {
		isContinuation = true; isSuspended = true; isReturned = true;
	    } else if (isBreakStatement) { isBreaked = true; isReturned = true;
	    } else if (isContinueStatement) {
		isContinued = true; isReturned = true;
	    }
	}

	// Handle fail overrides, not allowed with return/suspend/break/continue
	if (! isReturned) {
	    // Guard trumps alwaysSucceed -- if IsGuarded, use its value
	    if (isAlwaysSucceed) {	     // isGuarded trumps alwaysSucceed
 		if (isGuarded) { isFailed = ! testGuard(); }
	    }
	    handleFailOverrides(isGuarded); // ignore alwaysSucceed if guarded
	}

	// Return failure or success result
	if (isFailed) {
		result = FAIL;		// return handleFail();
		return;
	}
	handleSucceed();
  }

  public void nextEndChildReturn () {
	haveDoneNext = true;

	if (! (isFailed || isSuspended)) {
		count++;
		isEmpty = false;
	}
	if (! isFailed) {
		lastResult = result;
	}
  }

  /**
   * Handle continue or break for child, e.g., x or y.
   * Propagates state to parent.
   * Returns true if continue is at loop boundary, and should push next child.
   */
  private boolean filterChildReturn (IconIterator<T> child) {
	if (child == null) { return false; }
	if (child.isBreaked()) {
		if (isLoop) {	// This is a break at loop boundary, so fails.
			isFailed = true;	// return filterChildFail();
			result = FAIL;
		} else {
			isBreaked = true;
			isReturned = true;
		}
	} else if (child.isContinued()) {
		if (isContinueBoundary) {
			isFailed = true;
			result = FAIL;
		} else {			// Propagate up to boundary
			isContinued = true;
			isReturned = true;
		}
	}
	if (child.isFailed && isUnary) {
		isFailed = true;
		result = FAIL;
	}
	return false;				// NOT: Push x or y
  }

  /**
   * Handle suspend or method return for child, e.g., x or y.
   * Propagates return state.
   * Returns true if suspend or method return for child, 
   * but ignores break and continue.
   * Can fail and return or break or continue, but cannot fail and suspend.
   * USAGE: if (child.isReturned && handleChildReturn(child)) { return result; }
   */
  private boolean handleChildReturn (IconIterator<T> child) {
	if (child == null) { return false; }
	if (child.isSuspended) {
		isSuspended = true;
		isReturned = true;
		if (child.isContinuation) { isContinuation = true; }
	} else if (child.isMethodReturned()) {
		isMethodReturned = true;
		isReturned = true;
	} else { return false; }

	result = child.result;
	isFailed = child.isFailed;

if (isDebug) {
	if (name.isEmpty()) { name = this.getClass().getName(); }
	System.out.println("Return " + name + " " + result);
}
	return true;
  }

  //====
  // testGuard returns false if guard fails, i.e., isEmpty
  //====
  public boolean testGuard () {
	isGuardFailed = false;
	if (isGuarded) {
		if (guard.isSingleton()) {
			if (guard.isEmpty()) { isGuardFailed = true; }
		} else if (guard.isFailed()) { isGuardFailed = true; }
	}
	return ! isGuardFailed;
  }

  /**
   * Derive upperlimit from limitWithIterator's last result, if non-null.
   * Handles either numbers or strings, which are converted to integers.
   */
  long deriveLimit () {
	upperlimit = -1;
	if (limitWithIterator != null) {
	    IIconAtom<?> iteratorLimit =
		limitWithIterator.getLastResult();	// getResult();
	    if (iteratorLimit != null) {
		IconValue value = iteratorLimit.getValue();
		if (value.isNumber()) {
			upperlimit = value.getNumber().longValue();
		}
	    }
	}
	return upperlimit;
  }

  //==========================================================================
  // Filter methods.
  //==========================================================================

  /**
   * Binary map over results for operands, if isMap.
   */
  public IIconAtom<T> binaryMap (IIconAtom<T> x, IIconAtom<T> y) {
	if (binaryOperator != null) {		// && (x != null) && (y != null)
		result = binaryOperator.apply(x, y); // x|y.getResult()
	} else if (contextOperator != null) {
		result = contextOperator.apply(this, x, y);
	} else {
		result = x;
	}
	return result;
  }

  /**
   * Unary map over results for operands, if isMap.
   */
  public IIconAtom<T> unaryMap (IIconAtom<T> x) {
	if (unaryOperator != null) {			// && (x != null)
		result = unaryOperator.apply(x);	// x.getResult()
	} else if (contextOperator != null) {
		result = contextOperator.apply(this, null, x); // x@y or @y
	} else {
		result = x;
	}
	return result;
  }

  //==========================================================================
  // Composition.  Only one of the following compositions can apply.
  //==========================================================================

  public boolean isComposition () { return isComposition; }

  public IIconComposition <T> asComposition () { return this; }

  public IIconComposition<T> product () {
	this.isProduct = true;
	this.isUnaryMap = false;
	if ((x != null) && getXIsInChain()) {
		IIconComposition <T> comp = x.asComposition();
		if (comp != null) { comp.product(); }
	}
	return this;
  }

  public IIconComposition<T> concat () {
	this.isConcat = true;
	// this.isProduct = false;
	this.isUnaryMap = true;
	if ((x != null) && getXIsInChain()) {
		IIconComposition <T> comp = x.asComposition();
		if (comp != null) { comp.concat(); }
	}
	return this;
  }

  public IIconComposition<T> zip () {
	this.isZip = true;
	this.isUnaryMap = false;
	if ((x != null) && getXIsInChain()) {
		IIconComposition <T> comp = x.asComposition();
		if (comp != null) { comp.zip(); }
	}
	return this;
  }

  // Watch out: if getXIsInChain, have decomposed: x + y + z => (x + y) + z
  //	So reduce's initial goes to left child
  public IIconComposition<T> reduce (BinaryOperator<IIconAtom<T>> o,
		IIconAtom<T> initial) {
	this.reduceOperator = o;
	this.reduceInitial = initial;
	this.isReduce=true;	// this.isMap=false;
	if ((x != null) && getXIsInChain()) {
		IIconComposition <T> comp = x.asComposition();
		if (comp != null) { comp.reduce(o, initial); }
		reduceInitial = null;
	};
	return this;
  }

  public IIconComposition<T> reduce (BinaryOperator<IIconAtom<T>> o) {
	return reduce(o,null); }
  public IIconComposition<T> reduce () { return reduce(reduceOperator, null); }
  public IIconComposition<T> repeat () { this.isRepeat=true; return this; }

  public IIconComposition<T> doReturn () { this.isReturnStatement = true; return this; }
  public IIconComposition<T> doSuspend () { this.isSuspendStatement = true; return this; }
  public IIconComposition<T> doContinuation () { this.isContinuationStatement = true; return this; }
  public IIconComposition<T> doBreak () { this.isBreakStatement = true; return this; }
  public IIconComposition<T> doContinue () { this.isContinueStatement = true; return this; }	// skip

  //====
  // Modifiers that can be attached to any composition
  //====
  public IIconComposition<T> map (UnaryOperator<IIconAtom<T>> o) {
	this.unaryOperator = o;
	this.isMap=true;	// this.isReduce=false;
	if ((x != null) && getXIsInChain()) {
		IIconComposition <T> comp = x.asComposition();
		if (comp != null) { comp.map(o); }
	}
	return this;
  }

  public IIconComposition<T> map (BinaryOperator<IIconAtom<T>> o) {
	this.binaryOperator = o;
	this.isMap=true;	// this.isReduce=false;
	if ((x != null) && getXIsInChain()) {
		IIconComposition <T> comp = x.asComposition();
		if (comp != null) { comp.map(o); }
	}
	return this;
  }

  public IIconComposition<T> map (TriFunction<IIconIterator<T>,
		IIconAtom<T>> o) {
	this.contextOperator = o;
	this.isMap=true;	// this.isReduce=false;
	if ((x != null) && getXIsInChain()) {
		IIconComposition <T> comp = x.asComposition();
		if (comp != null) { comp.map(o); }
	}
	return this;
  }

  public IIconComposition<T> map () {
	this.isMap=true;	// this.isReduce=false;
	return this;
  }

  public UnaryOperator<IIconAtom<T>> getUnaryOperator () {
	return unaryOperator; 
  }

  public BinaryOperator<IIconAtom<T>> getBinaryOperator () {
	return binaryOperator; 
  }

  public TriFunction<IIconIterator<T>, IIconAtom<T>>
		getContextOperator () {
	return contextOperator;
  }

  //====
  // Limit modifiers that can be attached to any iterator
  //====
  public IIconComposition<T> limit (long n) { this.upperlimit=n; return this; }

  public IIconComposition<T> limitWithIterator (IconIterator<?> i) {
	this.limitWithIterator=i; return this; }

  public IIconIterator<T> neverRestart () {
	this.isNeverRestart = true; return this; } // resource

  public boolean isSingleton () {
	return (isSingleton || isBounded || (upperlimit == 1));
  }

  //====
  // Guarded choice
  //====
  public IIconComposition<T> guard (IconIterator<?> g) {
	this.guard = g;
	this.isGuarded = false;
	if (g != null) { this.isGuarded = true; }
	return this;
  }
  public IIconComposition<T> loopBoundary () { this.isLoop=true; return this; }

  public IIconComposition<T> continueBoundary () {
	this.isContinueBoundary = true;
	return this;
  }

  public boolean isGuarded () { return isGuarded; }

  //====
  // Operands
  //====
  public IconIterator<T> getX () { return x; }

  //====
  // Watch out: if getXIsInChain, have decomposed: x + y + z => (x + y) + z
  //	So reduce's initial goes to left child
  //====
  public IIconComposition<T> setX (IconIterator<T> x, boolean isInChain) {
	this.x = x;
	if (x == null) { return this; }
	this.xIsInChain = isInChain;
	x.setParent(this);
	IIconComposition <T> comp = x.asComposition();
	if (getXIsInChain() && (comp != null)) {
		if (isProduct) { comp.product(); }
		if (isConcat) { comp.concat(); }
		if (isZip) { comp.zip(); }
		if (isReduce) {
			comp.reduce(reduceOperator, reduceInitial);
			reduceInitial = null;
		}
		// if (isMap) { comp.map(operator); }
		if (isMap) {
		  if (binaryOperator != null) { comp.map(binaryOperator); }
		  if (unaryOperator != null) { comp.map(unaryOperator); }
		  if (contextOperator != null) { comp.map(contextOperator); }
		}
	}
	return this;
  }
  public IIconComposition<T> setX (IconIterator<T> x) {
	return setX(x, false);
  }
  public IconIterator<T> getY () { return y; }
  public IIconComposition<T> setY (IconIterator<T> y) {
	this.y = y;
	if (y != null) { y.setParent(this); isUnary = false; } ;
	return this; }

  //====
  // Is sub-expression in left-associative chain, e.g., (x & y) & z
  //====
  public boolean getXIsInChain () { return xIsInChain; }

  //==========================================================================
  // Constructors
  //==========================================================================
  /**
   * Default is empty iterator that just fails.
   */
  public IconComposition () {
	isComposition = true;
  }

  /**
   * Unary operator, or delegate.
   */
  public IconComposition (IconIterator<T> x) {
	isComposition = true;
	setX(x);
  }

  /**
   * Binary product or concatenation.
   */
  public IconComposition (IconIterator<T> x, IconIterator<T> y) {
	isComposition = true;
	setX(x);
	setY(y);
  }

  /**
   * Product or concatenation with variable number of arguments, i.e., variadic.
   * Assumes left associative chain: (x & y & z) => ((x & y) & z).
   * Map, product, and reduce will propagate down chain: e.g.,
   *	map will apply map on left child if (x & y).isInChain.
   */
  public IconComposition (IconIterator<T>... rest) {
	isComposition = true;
	over(rest);
  }

  //==========================================================================
  // Operands
  //==========================================================================

  public IconComposition<T> over (IconIterator<T> x) {
	setX(x);
	return this;
  }

  public IconComposition<T> over (IconIterator<T> x, IconIterator<T> y) {
	setX(x);
	setY(y);
	return this;
  }

  public IconComposition<T> over (IconIterator<T>... rest) {
	if ((rest == null) || (rest.length == 0)) { return this; }
	if (rest.length == 1) {
	    setX(rest[0]);	// this(rest[0]);
	} else if (rest.length == 2) {
	    setX(rest[0]);	// this(rest[0], rest[1]);
	    setY(rest[1]);
	} else {
	    setX(new IconComposition<T>(
		Arrays.copyOfRange(rest, 0, rest.length-1)),
		true);	// getX().setIsInChain();
	    setY(rest[rest.length-1]);
	}
	return this;
  }

}

//==== END OF FILE
