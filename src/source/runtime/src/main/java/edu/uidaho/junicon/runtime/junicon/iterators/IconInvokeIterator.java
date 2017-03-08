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

import java.util.concurrent.Callable;

/**
 * Singleton iterator over a function invocation
 * that is reified using closures.
 * The function invocation is wrapped as a Groovy closure.
 * For generator functions, delegates to the returned iterator.
 * For plain non-generator functions, if the result is FAIL it returns FAIL, 
 *	otherwise the result is wrapped as an IconValue
 *	and returned in a singleton iterator.
 * <P>
 * IconInvoke
 * takes the closure f and promotes its invocation f() to an
 *	iterator, and also reifies the generator result if needed.
 * If the result of f() is an IconIterator(generator), delegates to that,
 *	otherwise lifts the plain non-generator function result as a reified
 *	IconAtom in singleton iteration mode.
 * IconInvokeIterator is a method boundary, and
 *	stops any propagation of isSuspended at this method boundary.
 * <P>
 * USAGE for f(x): new IconInvoke({->f(x)})
 *
 * @author Peter Mills
 */
public class IconInvokeIterator <T> extends IconIterator <T> {

  private Callable<T> invocable = null;
  private Runnable runnable = null;
  private boolean forceJava = false;	// not treat result as generator

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * No-arg constructor.
   */
  public IconInvokeIterator () { }

  /**
   * Constructor with function as closure that returns value.
   */
  public IconInvokeIterator (Callable<T> invocable) {
	this.invocable = invocable;
	singleton();	// May be bounded later by parent IconSequence
	setUseOnNextAdvice(true);
  }

  /**
   * Constructor with function as closure that returns void.
   */
  public IconInvokeIterator (Runnable runnable) {
	// Under Groovy, Closure is both Runnable and Callable, chooses Runnable
	if (runnable instanceof Callable) {
		this.invocable = (Callable<T>) runnable;
	} else { this.runnable = runnable; }
	singleton();	// May be bounded later by parent IconSequence
	setUseOnNextAdvice(true);
  }

  //==========================================================================
  // Setters for dependency injection.
  //==========================================================================

  /**
   * Sets if not treat invoke result as generator, even if its IIconIterator.
   * Returns this for setter chaining.
   */
  public IconInvokeIterator setForceJava (boolean onoff) {
	this.forceJava = onoff;
	return this;
  }

  /**
   * Sets if not treat invoke result as generator.
   */
  public IconInvokeIterator setForceJava () {
	this.forceJava = true;
	return this;
  }

  /**
   * Gets if not treat invoke result as generator.
   */
  public boolean isForceJava () {
	return forceJava;
  }

  //==========================================================================
  // Singleton iterator extensions.
  //==========================================================================

  private Object invoked = null;
  private IconIterator<T> iter = null;	// null only if freed to cache
  private boolean isIterator = false;
  private IIconAtom<T> funcResult;
  private boolean hasReturned = false;	// last iteration was isMethoReturned

  /**
   * Override what happens after restart().
   * <P>
   * We must free the method body to cache on restart, on generator fail, and
   * also aggressively on generator return.
   * This is because IconInvokeIterator may be invoked only once if it
   * is bounded, e.g. in a sequence, in which case due to bounded
   * optimization it fails immediately.
   * IconInvokeIterator may also be invoked only once or before it
   * returns or fails, i.e. on suspend, because of a parent that fails.
   * Thus IconInvokeIterator will have a generator that is not guaranteed to run
   * to failure or even to return (which fails since IconReturn is a singleton).
   * Thus we must aggressively free the method body to cache
   * on restart or return, in addition to failure.
   * <P>
   * If we do not free the method body, it may cause Java garbage collection
   * to overload and run out of memory, since we explicitly reference the method
   * body in the generated code's instance method cache and
   * thus prevent its collection.
   * It will also cause slowdown since we are re-allocating the method body
   * on each invocation, instead of reusing it for the given class instance.
   * Note that the method body cannot be statically cached as it may contain
   * references to local instance variables.
   */
  public void afterRestart () {
	//====
	// This is sufficient to reclaim method body, but not aggressively.
	//====
	if (iter != null) {	// Return method body to cache
		MethodBodyCache cache = iter.getCache();
		if (cache != null) {
		    cache.addFree(iter.getNameInCache(), iter);
		}
		iter = null;
	}
	hasReturned = false;

	if (invocable != null) {
	  try {
		invoked = invocable.call();
	  } catch (Exception e) {
		throw new RuntimeException(e);
	  }
	} else if (runnable != null) {
	  try {
		invoked = null;
		runnable.run();
	  } catch (Exception e) {
		throw new RuntimeException(e);
	  }
	} else { return; };

	if ((invoked != null) && (! forceJava) &&
			(invoked instanceof IconIterator)) {
		iter = (IconIterator) invoked;
		isIterator = true;	// delegate next to iter
		setIsSingleton(false);
	} else {
		isIterator = false;
		if (isUndoable()) {
		  setIsSingleton(false); // will undo and fail on resume
		} else {
		  setIsSingleton(true);	// force singleton for Java method
		}
	}
  }

  /**
   * Override next().
   * Stops any propagation of isSuspended at this method boundary.
   */
  public IIconAtom<T> provideNext () {
	if (isIterator) { 
		if (hasReturned) {	// true only if (iter == null)
		    //====
		    // Method returned on last iteration, so skip nextAtom()
		    //   since will fail at top level anyway, both through
		    //   IconComposition's testing and IconReturn's singleton.
		    //====
		    setIsFailed(true);
		} else {
		    funcResult = iter.nextAtom();
		    boolean failed = iter.isFailed();
		    hasReturned = iter.isMethodReturned();

		    // Propagate failure and continuation past method boundary
		    if (failed) { setIsFailed(true); };
		    if (iter.isContinuation()) { setIsContinuation(); }

		    // Free method cache
		    if (failed || hasReturned) {
			// Return method body to cache (aggressively reclaim)
			MethodBodyCache cache = iter.getCache();
			if (cache != null) {
			    cache.addFree(iter.getNameInCache(), iter);
			}
			iter = null;
		    }
		}
	} else {
		if (invoked == FAIL) { return FAIL; }
		funcResult = IconValue.create(invoked);  // create()
		//====
		// For plain function, if atom just return it
		// if (invoked instanceof IIconAtom) {
		// funcResult = (IIconAtom<T>) invoked;
		// } else {
		// funcResult = IconValue.create(invoked).setCreator(this); }
		//====
	}
	return funcResult;
  }

  public void afterNextEnd () {
    if (isFailed && isIterator && (iter != null)) {
	// Return method body to cache (aggressively reclaim)
	MethodBodyCache cache = iter.getCache();
	if (cache != null) {
	    cache.addFree(iter.getNameInCache(), iter);
	}
	iter = null;
    }
  }

}

//==== END OF FILE
