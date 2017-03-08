//========================================================================
// Copyright (c) 2014 Orielle, LLC.  
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
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;

import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
// import java.util.concurrent.atomic.AtomicReference;

/**
 * A co-expression that can also be run as a thread, or
 * as a proxy for the generator running in a separate thread.
 * The proxy acts as either an in-place substitute, or
 * as a pipe that requires explicit activation.
 * IconCoExpression implements both Runnable and Callable,
 * so as to be able to function as either a thread or as a threaded future.
 * IconCoExpression also provides inbox and outbox channels for
 * thread communication.
 * <P>
 * <B>Co-expressions.</B>
 * A co-expression is a first-class iterator that uses an explicit
 * {@literal @} activation operator to step to the next iteration.
 * A co-expression creates a copy of its local environment,
 * i.e., it clones method local variables and parameters.
 * Aside from shadowing locals, a co-expression is similar to a first-class
 * iterator: in addition to explicit activation, it can be used
 * as any other first-class iterator using a ! operator.
 * <P>
 * Because a co-expression can transfer control to another co-expression
 * before it finishes an iteration, support must be added for continuations.
 * All iterators are already continuations at the method level since they
 * support a suspend operator: we provide support for expression continuations
 * by adding a continuation operation that is just 
 * a suspend that propagates past method boundaries.
 * <P>
 * <B>First class expressions.</B>
 * First class expressions are a subset of co-expressions, in which
 * {@literal @} just steps to the next iteration.
 * On each restart(), it must provide a new copy of the expression body,
 * i.e., ()->expr.
 * <P>
 * <B>Threads.</B>
 * A thread, on the other hand, runs a co-expression until failure, and
 * communicates via blocking channels.
 * Each thread has by default an inbox and an outbox channel.
 * If the thread isProducer, all results are put to the outbox.
 * If the thread isConsumer, at each iteration it takes a value from
 * the inbox, i.e., it waits until the inbox is non-empty,
 * and takes at least one value from the inbox.
 * If both isProducer and isConsumer,
 * the thread steps once on each inbox value, and produces to its outbox.
 * Lookahead is used on the inbox channel to make that trigger value available
 * to any other takes on the inbox.
 * Both the inbox and the outbox can be redirected to other channels
 * to form pipes.
 * The default for the thread is to run without waiting for a trigger,
 * but to always put the result in the outbox.
 * By default a thread is also run as a Future, so that it can be cancelled.
 * Co-expressions thus step as continuations, whereas threads step on messages.
 * <P>
 * <B>Proxies and pipes</B>
 * A proxy creates a generator that lazily communicates with 
 * the original generator that runs in a separate thread.
 * The proxy acts as either an in-place substitute for
 * the original expression (|>> e), or
 * as a pipe that requires explicit activation (|> e).
 * To simplify terminology
 * we use the term "proxy" for an in-place substitute,
 * the term "pipe" for a substitute that needs activation,
 * and the term "thread" for a coexpression that runs to failure in a separate
 * thread.
 * A proxy and a pipe are related by the equivalences
 * proxy=!pipe, and pipe={@literal <}>proxy,
 * where {@literal <}> denotes a first class expression.
 * <P>
 * A proxy creates a pair of generators: a consumer and a threaded producer.
 * On each iteration step, the producer puts into its outbox,
 * and the consumer takes from the producer outbox until failure.
 * The consumption of the thread result
 * is asynchronous: the producer lazily returns atoms
 * which are only consumed on demand by blocking take on the outbox.
 * The producer can optionally bound the outbox buffer size.
 * Bounding the outbox buffer size will throttle a threaded coexpression,
 * and effects burst mode or bulk transfer like a serial bus.
 * Bounding the buffer to 1 effects lockstep behavior, 
 * which creates a barrier at each iteration between a thread and its proxy.
 * <P>
 * <B>Messages.</B>
 * Co-expressions and threads share a concept of outbox in common.
 * Co-expressions support result transmission using message{@literal @}C,
 * which sets "message" as the next result for the originating iterator.
 * Specifically, message{@literal @}C puts "message" in the
 * current coexpression outbox.
 * The outbox result is later used on activation of that current coexpression
 * as the next iteration result if non-empty.
 * The {@literal @} operator can also be applied to threads.
 * If C is a thread, {@literal @} takes from the outbox.
 * Other than this commonality, co-expressions and threads behave differently.
 * Co-expressions step as continuations, whereas threads step on messages.
 * <P>
 * Both channels and co-expression result transmission are thread-safe.
 * A null message value for channels and results is the same as omitting it,
 * so that {@literal @}C == null{@literal @}C,
 * and null{@literal @}>C is ignored.
 * <P>
 * <B> Creation of co-expressions.</B>
 * A co-expression can act in two modes:
 * as a singleton creator of
 * a first-class coexpression, a thread, or a pipe,
 * or as an in-place proxy which on next() delegates over a thread 
 * to its underlying expression.
 * Activation is required for a coexpression and pipe,
 * while an in-place proxy and thread act like plain generators,
 * albeit a thread runs to completion.
 * Coexpression creation has the following meaning.
 * <PRE>
 *	refresh() = {(x,y,z) -> expr} ( {()->[x,y,z]}() )
 *	thread expr = new Thread (run() { refresh().next() } )
 * </PRE>
 * The create construct, thread construct, and pipe and proxy operators,
 * are translated as follows.
 * <PRE>
 * create e => new IconCoExpression(creator, getenv)
 *	where creator = rewrite {(x,y,z) -> e }			// Shadow locals
 *        getenv  = {() -> IconList.createArray(x_r.get(),...)} // Capture env
 *	  and x,y,z are all referenced locals			// {()->[x,y,z]}
 * |> e  => new IconCoExpression(creator,getenv).createPipe().next()
 * |>> e => new IconCoExpression(creator,getenv).createProxy().next()
 * thread e => new IconCoExpression(creator, getenv).createThread().next()
 * spawn(create e) = thread e
 * </PRE>
 * <P>
 * <B>Operators.</B>
 * <PRE>
 * Co-expression operators, C{ msg{@literal @}A {{@literal @}C uses msg}, are:
 *   ^C => C.refresh()					// Refresh
 *   *C => C.getCount()					// Count
 *   {@literal @}C => activate(this, null, C)		// Activate
 *   msg{@literal @}C => activate(this, msg, C)
 *
 *   msg {@literal @}> C     => C.getInbox().offer(msg)	// Send
 *   msg {@literal @}> null  => getOutbox().offer(msg)
 *   msg {@literal @}>> C    => C.getInbox().put(msg)	// Blocking send
 *   msg {@literal @}>> null => getOutbox().put(msg)
 *
 *   <{@literal @} C         => C.getOutbox().poll()	// Receive
 *   <{@literal @} null      => getInbox().poll()
 *   <<{@literal @} C        => C.getOutbox().take(msg)	// Blocking receive
 *   <<{@literal @} null     => getInbox().take(msg)
 *   n<<{@literal @} C       => C.getOutbox().poll(timeout)
 *   n<<{@literal @} null    => getInbox().poll(timeout)
 * </PRE>
 *
 * @author Peter Mills
 */
public class IconCoExpression <T> extends IconIterator <T>
	implements Runnable, Callable<T> {

  // Underlying iterator for co-expression
  private VariadicFunction<T, IIconIterator<T>> creator = null;
  private Callable<T[]> getenv = null;
  private IIconIterator<T> plainDelegate = null;  // Delegate to plain iterator
  private IIconIterator<T> iter = null;	// Derived iterator for co-expression
			// This is always a top-level outermost iterator
  private boolean isPlainDelegate = false;
  private IconCoExpression<T> invoker = null; // Who invoked this co-expression.
  private boolean isFrozen = false;

  // &main keyword support, as inherited top-level outermost co-expression
  private IconCoExpression<T> topLevel = null;

  // Thread support 
  private boolean isProducer = false;
  private boolean isConsumer = false;
  private boolean isRunToFailure = true;
  private Future<T> future = null;

  // Pipe and proxy support
  private boolean isCreateMode = false;
  private boolean isProxy = false;
  private boolean isPipe = false;
  private boolean isThread = false;
  private boolean isCoExpression = false;
  private boolean isFirstClass = false;

  private boolean isLockstep = false;
  private IconCoExpression<T> pipeTo = null;
  private boolean haveInitializedPipe = false;

  // Default channels
  private BlockingQueue<IIconAtom<T>> myInbox = new LinkedBlockingQueue();
  private BlockingQueue<IIconAtom<T>> myOutbox = new LinkedBlockingQueue();
  private int outboxSize = 0;	// > 0 to bound outbox.

  // Redirected channels
  private BlockingQueue<IIconAtom<T>> inbox = myInbox;
  private BlockingQueue<IIconAtom<T>> outbox = myOutbox;
  private boolean isRedirectedOut = false;

  // Message channel for activation results
  private BlockingQueue<IIconAtom<T>> messagebox = new LinkedBlockingQueue();

  // Inbox lookahead
  boolean haveInboxLookahead = false;
  IIconAtom<T> inboxLookahead = null;

  //==========================================================================
  // Thread local for current active co-expression
  //==========================================================================

  private static final ThreadLocal<IconCoExpression> currentCoexpr = 
	new ThreadLocal () {
	    @Override protected IconCoExpression initialValue() {
		 return null;	// return new IconCoExpression();
            }
  };

  public static IconCoExpression getCurrentCoexpr () {
	return currentCoexpr.get();
  }

  public static void setCurrentCoexpr (IconCoExpression coexpr) {
	currentCoexpr.set(coexpr);
  }

  //==========================================================================
  // Constructors
  //==========================================================================

  /**
   * No-arg constructor.
   */
  public IconCoExpression () {
  }

  /**
   * Constructor with creator and getenv, used to create a coexpression
   * that clones its local environment.
   * The local environment consists of all referenced method
   * local variables and parameters.
   * Delegates to creator(getenv()).
   */
  public IconCoExpression (VariadicFunction<T, IIconIterator<T>> creator,
		Callable<T[]> getenv) {
	setCoExpression(creator, getenv);
  }

  /**
   * Create coexpression from plain iterator.
   * The plain iterator will be wrapped as a coexpression that delegates to it.
   * A plain iterator may not refresh itself nor clone its local environment.
   */
  public IconCoExpression (IIconIterator<T> iterator) {
	delegateCoExpression(iterator);
  }

  //==========================================================================
  // Setters for dependency injection.
  //==========================================================================

  /**
   * Create co-expression from creator and getenv,
   * that clones its local environment.
   * Delegates to creator(getenv()).
   */
  public IconCoExpression<T> setCoExpression (
		VariadicFunction<T, IIconIterator<T>> creator,
		Callable<T[]> getenv) {
	this.creator = creator;
	this.getenv = getenv;
	return this;
  }

  /**
   * Create co-expression from plain iterator.
   * Delegates to the plain iterator.
   */
  public IconCoExpression<T> delegateCoExpression (IIconIterator<T> iterator) {
	this.plainDelegate = iterator;
	if (iterator != null) { isPlainDelegate = true; }
	return this;
  }

  /**
   * Returns the co-expression that invoked this co-expression,
   * or null if top-level.
   */
  public IconCoExpression<T> getInvoker () {
	return invoker;
  }

  /**
   * Returns the inherited top-level outermost co-expression.
   * Will be this coexpression if it was the first activation.
   */
  public IconCoExpression<T> getTopLevel () {
	return topLevel;
  }

  /**
   * Sets if puts results to outbox.
   */
  public IconCoExpression<T> setIsProducer (boolean onoff) {
	isProducer = onoff;
	return this;
  }

  /**
   * Gets if puts results to outbox.
   */
  public boolean isProducer () {
	return isProducer;
  }

  /**
   * Sets if takes from inbox at each next.
   */
  public IconCoExpression<T> setIsConsumer (boolean onoff) {
	isConsumer = onoff;
	return this;
  }

  /**
   * Gets if takes from inbox at each next.
   */
  public boolean isConsumer () {
	return isConsumer;
  }

  /**
   * Sets if thread runs the generator to failure, i.e., does an every over it.
   * Default is true.
   */
  public IconCoExpression<T> setIsRunToFailure (boolean onoff) {
	isRunToFailure = onoff;
	return this;
  }

  /**
   * Gets if thread runs the generator to failure, i.e., does an every over it.
   */
  public boolean isRunToFailure () {
	return isRunToFailure;
  }

  /**
   * Sets Future for thread.
   */
  public IconCoExpression<T> setFuture (Future<T> future) {
	this.future = future;
	return this;
  }

  /**
   * Gets Future for thread.
   */
  public Future<T> getFuture () {
	return future;
  }

  /**
   * Gets if thread will return Future instead of just a coexpression.
   * A Future can be used to wait until the thread completes,
   * and then returns a result.
   * A Future can also be used to attempt to cancel a thread.
   * Futures are creates using an ExecutorService submit instaed of execute.
   */
  public boolean isFuture () {
	return future != null;
  }

  /**
   * Sets if this coexpression is in separate thread.
   */
  public IconCoExpression<T> setIsThread (boolean onoff) {
	isThread = onoff;
	return this;
  }

  /**
   * Gets if this coexpression is in separate thread.
   */
  public boolean isThread () {
	return isThread;
  }

  /**
   * Sets if is pipe for thread.
   */
  public IconCoExpression<T> setIsPipe (boolean onoff) {
	isPipe = onoff;
	return this;
  }

  /**
   * Gets if is pipe for thread.
   */
  public boolean isPipe () {
	return isPipe;
  }

  /**
   * Sets if is coexpression.
   */
  public IconCoExpression<T> setIsCoExpression (boolean onoff) {
	isCoExpression = onoff;
	return this;
  }

  /**
   * Gets if is coexpression.
   */
  public boolean isCoExpression () {
	return isCoExpression;
  }

  /**
   * Sets if is first class.
   */
  public IconCoExpression<T> setIsFirstClass (boolean onoff) {
	isFirstClass = onoff;
	return this;
  }

  /**
   * Gets if is first class.
   */
  public boolean isFirstClass () {
	return isFirstClass;
  }

  /**
   * Runs proxy as an in-place generator instead of co-expression
   * that needs activation.
   * A proxy is a substitute for the original expression.
   */
  public IconCoExpression<T> setIsProxy (boolean onoff) {
	this.isProxy= onoff;
	return this;
  }

  /**
   * Gets if runs proxy as an in-place generator instead of co-expression
   * that needs activation.
   */
  public boolean isProxy() {
	return isProxy;
  }

  /**
   * Sets outbox size.
   * If this is a proxy or pipe, sets the outbox size of its thread.
   * Bounding the outbox buffer size will throttle a threaded coexpression,
   * and effects burst mode or bulk transfer like a serial bus.
   * Bounding the buffer to 1 effects lockstep behavior, 
   * which creates a barrier at each iteration between a thread and its proxy.
   * Any size < 1 means unbounded.
   * Does not affect a redirected outbox.
   * Default is unbounded.
   */
  public IconCoExpression<T> setOutboxSize (int size) {
	if (size == this.outboxSize) { return this; }
	this.outboxSize = size;
	if (isProxy || isPipe) {
	    if (pipeTo != null) { pipeTo.setOutboxSize(size); }
	    return this;
	}
	if (size > 0) { myOutbox = new LinkedBlockingQueue(size);
	} else { myOutbox = new LinkedBlockingQueue(); }
	if (! isRedirectedOut) { outbox = myOutbox; }
	return this;
  }

  /**
   * Gets outbox size.
   */
  public int getOutboxSize () {
	return outboxSize;
  }

  /**
   * Sets if the pipe or proxy runs in lockstep with the thread,
   * i.e., if there is a barrier between each iteration.
   * If on, bounds the outbox size to 1.
   * If off, unbounds the outbox size.
   * Default is true.
   */
  public IconCoExpression<T> setIsLockstep (boolean onoff) {
	isLockstep = onoff;
	if (isLockstep) { setOutboxSize(1);
	} else { setOutboxSize(0); }
	return this;
  }

  /**
   * Gets if the pipe or proxy runs in lockstep with the thread.
   */
  public boolean isLockstep () {
	return isLockstep;
  }

  /**
   * Sets pipe endpoint.
   */
  public IconCoExpression<T> setPipeTo (IconCoExpression<T> thread) {
	this.pipeTo = thread;
	return this;
  }

  /**
   * Gets pipe endpoint.
   */
  public IconCoExpression<T> getPipeTo () {
	return pipeTo;
  }

  //============================================================================
  // Creation of coexpression, thread, pipe, proxy, or first class.
  //============================================================================

  /**
   * Coexpression create.
   * On next() returns refreshed co-expression,
   * i.e., creates clone and sets its locals from the current environment.
   * Returns this, for setter chaining.
   */
  public IconCoExpression<T> createCoExpression () {
	isCreateMode = true;
	isCoExpression = true;
	return this;
  }

  /**
   * First class expression create.
   * On next() returns refreshed co-expression,
   * i.e., creates clone and sets its locals from the current environment.
   * Returns this, for setter chaining.
   */
  public IconCoExpression<T> createFirstClass () {
	isCreateMode = true;
	isFirstClass = true;
	return this;
  }

  /**
   * Thread create.
   * On next() creates thread for refreshed coexpression that runs to failure.
   */
  public IconCoExpression<T> createThread () {
	isCreateMode = true;
	isThread = true;
	return this;
  }

  /**
   * Pipe create.
   * On next() creates pipe to refreshed coexpression in separate thread.
   */
  public IconCoExpression<T> createPipe () {
	isCreateMode = true;
	isPipe = true;
	return this;
  }

  /**
   * Proxy create.
   * Creates in-place generator that is proxy for refreshed coexpression
   * in separate thread.
   */
  public IconCoExpression<T> createProxy () {
	isProxy = true;
	return this;
  }

  //==========================================================================
  // Input and output redirection.
  //==========================================================================

  /**
   * Redirect inbox to another channel.
   * If null, resets the inbox.
   */
  public IconCoExpression<T> redirectInbox (BlockingQueue<IIconAtom<T>> in) {
	if (in == null) {
		inbox = myInbox;
		inbox.clear();
	} else { inbox = in; };
	haveInboxLookahead = false;
	return this;
  }

  /**
   * Gets inbox.
   * A channel cannot contain null elements.
   */
  public BlockingQueue<IIconAtom<T>> getInbox () {
	return inbox;
  }

  /**
   * Redirect outbox to another channel.
   * If null, resets the outbox.
   * The size of the redirected channel will not be reset,
   * and so may not match any previous setOutbox request.
   */
  public IconCoExpression<T> redirectOutbox (BlockingQueue<IIconAtom<T>> out) {
	if (out == null) {
		isRedirectedOut = false;
		outbox = myOutbox;
		outbox.clear();
	} else {
		isRedirectedOut = true;
		outbox = out;
	}
	return this;
  }

  /**
   * Gets outbox.
   * A channel cannot contain null elements.
   */
  public BlockingQueue<IIconAtom<T>> getOutbox () {
	return outbox;
  }

  //==========================================================================
  // Inbox lookahead.
  //==========================================================================

  /**
   * Lookahead on inbox for next value.
   * If had existing lookahead, discards it.
   */
  private IIconAtom<T> inboxLookahead () {
	// if (haveInboxLookahead) { return inboxLookahead; }
	haveInboxLookahead = false;
	inboxLookahead = null;
	try {
		inboxLookahead = inbox.take();
	} catch (InterruptedException e) {
		return null;
	}
	haveInboxLookahead = true;
	return inboxLookahead;
  }

  /**
   * Take from inbox.
   * If isConsumer, must use this method instead of inbox.take(),
   * since lookahead may have been used to get next inbox value.
   */
  public IIconAtom<T> takeFromInbox () {
	if (haveInboxLookahead) {
		haveInboxLookahead = false;
		return inboxLookahead;
	}
	try {
		return inbox.take();
	} catch (InterruptedException e) {
		return FAIL;
	}
  }

  //==========================================================================
  // Refresh.
  //==========================================================================

  /**
   * Copies the local environment, and delegates to the resulting iterator.
   * iter = ([x,y,z]->expr)(->[x,y,z] ());
   */
  public IconCoExpression<T> freeze () {
	isFrozen = true;
	if (isPipe || isProxy) { return this; }	// Only freeze server not client
	if ((creator == null) || (getenv == null)) {
		return this;
	}
	try {
		iter = creator.apply(getenv.call());
	} catch (Exception e) {
		// WARNING: This catch will mask a transform logic error.
		// Should never occur.
		iter = null;
		return this;	// throw new RuntimeException(e);
	}
	return this;
  }
  //====
  // setX(null);
  // setX(iter);  // now ignores provideNext()
  //====

  /**
   * Clones this coexpression, and copies the local environment.
   */
  public IconCoExpression<T> refresh () {
	if (isPlainDelegate) {
		return new IconCoExpression(plainDelegate.refresh());
	}
	IconCoExpression coexpr = new IconCoExpression(creator,getenv);
	return coexpr.setIsPipe(isPipe).setIsProxy(isProxy).setIsCoExpression(isCoExpression).setIsFirstClass(isFirstClass).setIsLockstep(isLockstep).freeze();
  }
  //====
  // if isThread () { IconCoExpression(refresh()).runThread(); }
  //====

  private IconCoExpression<T> refreshThread () {
	return new IconCoExpression(creator,getenv).setIsThread(true).setIsLockstep(isLockstep).freeze();
  }

  //==========================================================================
  // AfterRestart.
  //   Create mode:
  //	 restart(): nothing
  //	 next(): return non-create clone, then fail
  //   Non-create mode:
  //	 restart(): if proxy runpipe off this ; clear
  //	 next(): next element
  // Context: method { body { new IconCoExpression ( lambdas ).create(type); } }
  //==========================================================================
  public void afterRestart () {
	if (isCreateMode) { return; }

	if (! isFrozen) { freeze(); }

	// WARNING: Should kill off old thread, or let it fail
	if (isProxy) {		// if (createProxy && (! haveInitializedPipe))
		haveInitializedPipe = true;
		runPipe(this);
	}

	// inbox.clear();
	outbox.clear();
	messagebox.clear();
	if (iter != null) { iter.restart(); }
  }

  //==========================================================================
  // Next.
  //==========================================================================
  /**
   * Override next().
   * In creation mode, next() will create
   * a refreshed coexpression, thread, or pipe.
   * Otherwise, within a plain co-expression, next() will 
   * just delegate to its expression iterator's next().
   * <P>
   * If the co-expression is a pipe or proxy,
   * next() just waits for the thread's next outbox message:
   * if thread isConsumer first tells it to step by
   * placing a message in its inbox.
   * If the co-expression is a thread, 
   * next() delegates to its expression iterator's next()
   * and then places the result in its outbox if isProducer.
   * If the thread isConsumer it first consumes from its inbox.
   * Co-expressions thus step as continuations, while threads step on messages.
   * <P>
   * Thread-safe.
   */
  public IIconAtom<T> provideNext () {

	// Create refreshed co-expression, thread, or pipe.
	// WARNING: Should kill off old thread, or let it fail
	if (isCreateMode) {
	  if (getHaveDoneNext()) {	// Singleton iterator
		// setIsFailed(true);	// nextAtom will do this on return FAIL
		return FAIL;
	  }
	  if (isPipe) {
	    return IconValue.create(runPipe(refresh()));
	  } else if (isThread) {
	    return IconValue.create(runThread());
	  } else if (isCoExpression || isFirstClass) {
	    return IconValue.create(refresh());
	  }
	}

	// Pipe or proxy just activates next step in threaded co-expression.
	if (isPipe || isProxy) {
	    IconCoExpression<T> thread = getPipeTo();
	    if (thread == null) { return FAIL; }

	    // Add trigger if consumer, tells it to step
	    if (thread.isConsumer()) {
		   try {
			thread.getInbox().put(
				IconValue.create((Object) null));
		   } catch (InterruptedException e) {
			return FAIL;
		   }
	    }

	    // Test for terminated thread, nothing will be forthcoming
	    if (thread.isFailed() && thread.getOutbox().isEmpty()) {
		return FAIL;
	    }

	    // Wait for thread result
	    return takeAtom(thread.getOutbox());
	}

	// Otherwise for thread or coexpression, will delegate to iter
	IIconAtom<T> nextAtom = FAIL;

	// Thread steps on inbox, produces to outbox
	if (isThread) {
	    if (isConsumer) {
		inboxLookahead();		// Null if interrupted
	    }
	    if (iter != null) {
		nextAtom = iter.nextAtom();
		if (iter.isFailed()) { nextAtom = FAIL; }
	    }
	    if (isProducer) {
		try {
		    outbox.put(nextAtom);
		} catch (InterruptedException e) {
		    return FAIL;
		}
	    }
	    return nextAtom;
	}

	// Otherwise is co-expression, plaindelegate, or first class
	if (iter == null) { return FAIL; }
	if (isCoExpression) {
	    // If outbox, use it, else iter.next()
	    nextAtom = messagebox.poll();   // Poll is null if channel is empty
	    if (nextAtom == null) { nextAtom = iter.nextAtom(); };
	} else {
	    nextAtom = iter.nextAtom();
	}

	// redundant for safety, nextAtom does this after nextChildOverride
	if (iter.isFailed()) { return FAIL; }
	return nextAtom;
  }
  //====
  // return IconValue.create(activate());
  //====

  /**
   * Performs take on blocking queue to get result.
   */
  private static <V> IIconAtom<V> takeAtom (BlockingQueue<IIconAtom<V>> outbox) {
	if (outbox == null) { return FAIL; }
	try {
		return outbox.take();
	} catch (InterruptedException e) {
		return FAIL;
	}
  }

  //==========================================================================
  // Activate.
  //==========================================================================

  /**
   * Activate the co-expression:
   * take its next iteration step,
   * and transfer control if another co-expression is activated.
   * Nested activations can occur until a next completes or a cycle is reached.
   * If no cycles occur, this is a purely recursive formulation of activation.
   * Co-expressions step as continuations, while threads step on messages.
   * <P>
   * First, if message is non-null, adds to currentCoExpression.messagebox.
   * <P>
   * For top-level activation, where there is no currentCoExpression,
   * does the next iteration of the co-expression.
   * If the result is a continuation, recurses on activation steps
   * using a continuation-passing-style
   * with the returned co-expression until it completes an iteration.
   * <P>
   * If is a nested activation, just returns the co-expression to activate
   * and turns the parent where activation occurs into a continuation.
   * Within a co-expression, its next() will delegate to its iterator's next().
   * <P>
   * If cycles occur in nested activation, the continuation result must be null.
   * A cycle occurs when we hit an activation expression
   * whose containing co-expression is already in a child activation.
   * In that case, we must set the continuation result to null,
   * since the activation does not yet have a result.
   * An easy solution is to by default set the continuation result to null
   * when marking a continuation.
   * <P>
   * The algorithm for activation in a continuation-passing style is as follows:
   * <PRE>
   * outermost @C (we are not a coexpression, so cannot cycle on me):
   * 	k = C.nextAtom()	// k is result, or toactivate & mark C as cont
   * 	return step(C, k)	// For nested activation
   * step(C, k)	// Operates on continuations as activated coexpressions
   * 	if k != @, return k
   * 	return C.nextAtom(step(k, k.nextAtom()))
   *	// If continuation, continuation result is null by default.
   * </PRE>
   * <PRE>
   * A co-expression is always a top-level outermost iterator,
   * and so is a continuation k.
   *	k = nextAtom(){continue k'} ; k(result)
   * Contrast this with a continuation-passing style call/cc:
   *	f(args,k) { ... k(result) }
   * </PRE>
   * <P>
   * <PRE>
   * EXAMPLE: C { x; @A; t },  A { y; @B; t },  B { z; @C; t }
   *	C := (create {write("C@A "); write(@A | "a"); write("Back@A"); 1});
   *	A := (create {write("A@B "); write(@B | "b"); write("Back@B"); 2});
   *	B := (create {write("B@C "); write(@C | "c"); write("Back@C"); 3});
   *	write(@C);
   * OUTPUT: C@A A@B B@C Back@A 1 Back@C 3 Back@B
   * ITERATOR NESTING: {coexpr { function-call { operation { @C }}}}
   * </PRE>
   * <P>
   * Thread-safe.
   * @param parent iterator performing this activate, ignored if top-level
   *	activation not inside any other coexpression.
   * @param message result of this coexpression if invoked inside another coexpr
   * @param coexpr coexpression to activate.
   * @return result of activation of the next step of the coexpression.
   */
  public static <V> IIconAtom<V> activate (IIconIterator<V> parent,
		V message, IconCoExpression<V> coexpr) {
	if (coexpr == null) { return FAIL; }
	if (! coexpr.isCoExpression) {	
		return coexpr.nextAtom();
	}

	IconCoExpression<V> invoker = getCurrentCoexpr();
	coexpr.invoker = invoker;

	// Add message to current.messagebox
	if ((invoker != null) && (message != null)) {
	    try {
		invoker.messagebox.put(IconValue.create(message));
	    } catch (InterruptedException e) {
		return FAIL;
	    }
	}

	// Handle co-expression
	// If not top level, return continuation for C
	if (invoker != null) {	// Another co-expression is active
		// Inherit top-level from invoker
		coexpr.topLevel = invoker.topLevel;

		// Mark parent as continuation
		parent.setIsContinuation();
		// Null is default if cycle in activation before result
		parent.setContinuationResult(null);

		// Return this coexpr as toActivate
		return new IconActivation<V>(coexpr, parent);
	}

	// Top level, chain until next completes, may have @ cycle back to me
	coexpr.topLevel = coexpr;
	setCurrentCoexpr(coexpr);
	// nextAtom: If did another @, returned coexpr to activate
	//      and marked coexpr as continuation
	IIconAtom<V> nextAtom = step(coexpr, coexpr.nextAtom());
	setCurrentCoexpr(null);		// Restore active parent
	return nextAtom;
  }

  /**
   * Activate plain iterator.
   * For plain iterators, msg{@literal @}C is just nextAtom().
   */
  public static <V> IIconAtom<V> activate (IIconIterator<V> parent,
		V message, IIconIterator<V> coexpr) {
	return coexpr.nextAtom();
  }

  /**
   * Step coexpr continuation using result of activating a child coexpression.
   * This is a variant of a continuation-passing style.
   * <BR>
   * USAGE: step(coexpr, coexpr.nextAtom())
   * <BR>
   * nextAtom: If an activated coexpr did another {@literal @}, will return
   * the {@literal @} to activate and mark coexpr as a continuation.
   */
  private static <V> IIconAtom<V> step (IconCoExpression<V> coexpr,
					IIconAtom<V> nextAtom) {
	if (coexpr == null) { return null; }
	if ((nextAtom == null) || (! nextAtom.isActivation())) {
		return nextAtom;
	}
	IconCoExpression<V> toActivate = (IconCoExpression<V>) nextAtom.get();
	if (toActivate == null) {
		return nextAtom;
	}
	setCurrentCoexpr(toActivate);
	//====
	// return coexpr.nextAtom(step(toActivate, toActivate.nextAtom()));
	//====
	IIconAtom<V> result = step(toActivate, toActivate.nextAtom());
	IIconIterator<V> parent = ((IconActivation<V>) nextAtom).getContinued();
	if (parent != null) { parent.setContinuationResult(result); }
	return coexpr.nextAtom();
  }

  //==========================================================================
  // Run iterator inside thread.
  //==========================================================================
  private volatile boolean isRunning = true;

  /**
   * Run interator inside thread.
   */
  private T runInThread () throws InterruptedException {
	IconCoExpression<T> runnable = this;	// refresh();
	T result = null;
	if (isRunToFailure) {
	    while (isRunning && (! runnable.isFailed())) {
		if (Thread.interrupted()) {	// Clears interrupted status
		       throw new InterruptedException();
		}
		result = runnable.next();
	    }
	} else {
	    result = runnable.next();
	}
	return result;
  }

  /**
   * Run method for thread using execute.
   */
  public void run () {
    try {
	runInThread();
    } catch (InterruptedException e) { 
	Thread.currentThread().interrupt();  // Restore interrupt status
    }
  }

  /**
   * Call method for thread using submit.
   */
  public T call () {
    try {
	return runInThread();
    } catch (InterruptedException e) { 
	Thread.currentThread().interrupt();  // Restore interrupt status
	return null;
    }
  }

  /**
   * Attempt to cancel thread.
   * Returns true if was able to cancel.
   */
  public boolean cancel () {
	if (future != null) {
		future.cancel(true);	// mayInterruptIfRunning
	}
	isRunning = false;
	return true;
  }

  //==========================================================================
  // Run co-expression as thread in ExecutorService.
  //==========================================================================

  private static boolean doShutdown = true;
  private static boolean doForkJoin = false;
  private static ExecutorService executor = null;

  /**
   * Run co-expression in separate thread under an ExecutorService.
   * If isFuture, will run as a future.
   */
  public static <V> IIconIterator<V> createThread (IconCoExpression<V> coexpr,
		boolean isFuture) {
	if (coexpr == null) { return null; }
	if (executor == null) {
		createExecutor();
	}
	//====
	// if (executor.isShutdown()) {	// Restart executor
	//	executor = Executors.newCachedThreadPool();  }
	//====
	if (isFuture) {
		Future<V> future = executor.submit((Callable<V>) coexpr);
		coexpr.setFuture(future);
	} else {
		executor.execute(coexpr);
	}
	return coexpr;
  }

  /**
   * Create executor service.
   */
  public static void createExecutor () {
	if (executor != null) { return; }
	if (doForkJoin) {
		executor = new ForkJoinPool();
	} else {
		executor = Executors.newCachedThreadPool();
	}
  }
  //====
  // private static ExecutorService executor = new ForkJoinPool();
  // private static ExecutorService executor = Executors.newCachedThreadPool();
  //	// newFixedThreadPool(Runtime.getRuntime.availableProcessor * 10);
  // private static ExecutorService executor = new ForkJoinPool();
  //====
  // ForkJoinPool(Runtime.getRuntime().availableProcessors(),
  //	ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, false);
  //====

  /**
   * Set whether to use thread pool or fork join pool.
   * Default is thread pool.
   */
  public static void setForkJoin (boolean onoff) {
	doForkJoin = onoff;
  }

  /**
   * Set whether the thread pool can be shutdown at end of main() methods.
   * Default is true.
   */
  public static void setDoShutdown (boolean onoff) {
	doShutdown = onoff;
  }

  /**
   * Shutdown the thread executor service, if doShutdown.
   */
  public static void shutdown () {
	if (doShutdown) { forceShutdown(); }
  }

  /**
   * Shutdown the thread executor service.
   */
  public static void forceShutdown () {
	if (executor == null) { return; }
	executor.shutdown();
	try {
	  if (! executor.awaitTermination(60, TimeUnit.SECONDS)) {
		executor.shutdownNow();
	  }
	} catch (InterruptedException e) {
		executor.shutdownNow();
		Thread.currentThread().interrupt();
	} finally {
		executor = null;
	}
  }

  //==========================================================================
  // Run refreshed co-expression in thread.
  //==========================================================================

  /**
   * Run refreshed copy of co-expression in separate thread.
   * Propagates isProducer, isConsumer, isRunToFailure properties to new thread.
   * If isFuture, will run as a future.
   */
  public IIconIterator<T> runThread (boolean isFuture) {
	IconCoExpression<T> thread = refreshThread();
	thread.setIsProducer(isProducer());
	thread.setIsConsumer(isConsumer());
	thread.setIsRunToFailure(isRunToFailure());
	return createThread(thread, isFuture);
  }

  /**
   * Run refreshed copy of co-expression in separate thread as future.
   */
  public IIconIterator<T> runThread () {
	return runThread(true);
  }

  //==========================================================================
  // Run refreshed co-expression as pipe.
  //==========================================================================

  /**
   * Run copy of co-expression as proxy to separate thread.
   * USAGE: runPipe(new IconCoExpression())
   */
  public IIconIterator<T> runPipe (IconCoExpression<T> proxy) {
	if (proxy == null) { proxy = new IconCoExpression(); };
	IconCoExpression<T> thread = refreshThread();
	proxy.setPipeTo(thread);
	thread.setIsProducer(true);
	thread.setIsRunToFailure(isRunToFailure());
	if (isLockstep) {
		proxy.setIsLockstep(true);
		thread.setIsLockstep(true);
		// thread.setIsConsumer(true);
	}
	createThread(thread, true);
	return proxy;
  }

}

//==== END OF FILE
