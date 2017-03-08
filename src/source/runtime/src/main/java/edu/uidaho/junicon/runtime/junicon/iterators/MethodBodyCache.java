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

//============================================================================
// Method body cache.
//============================================================================

/**
 * Cache of method bodies for a given class.
 * Methods, procedures, and closures are transformed to lambda expressions
 * where the body becomes a constructed iterator:
 * <PRE>
 *   method f(x,y) {locals=init; body} =>
 *       def f = { x,y -> locals; return new IconIterator({init; body; fail}) }
 * </PRE>
 * Instead of doing a new allocation every time f is invoked,
 * we optimize using a cache for the iterator body:
 * <PRE>
 *   class foo {
 *       def cache = new MethodBodyCache();
 *       def f = {x,y -> locals; return cache_uniq.getFree("f") ?: 
 *          new IconIterator(init;body;fail).setCache(cache, "f"); }
 *       // IconInvokeIterator will add back to cache upon return
 *   }
 * </PRE>
 * The transforms will uniqueify both the cache name and the method string
 * if necessary in order to prevent ambiguity or conflict.
 *
 * @author Peter Mills
 */
public class MethodBodyCache {
  // Threadlocal cache of method bodies
  private final ThreadLocal<HashMap<String, List<IconIterator<?>>>>
    threadCache = new ThreadLocal() {
     @Override protected HashMap<String, List<IconIterator<?>>> initialValue() {
	return new HashMap<String, List<IconIterator<?>>> ();
     }
  };

  public MethodBodyCache () { }

  public IconIterator<?> getFree (String name) {
	HashMap<String, List<IconIterator<?>>> cache = threadCache.get();
	List<IconIterator<?>> stack = cache.get(name);
	if ((stack == null) || stack.isEmpty()) { return null; }
	return stack.remove(stack.size()-1);	// pop from end
  }
  public void addFree (String name, IconIterator<?> body) {
	HashMap<String, List<IconIterator<?>>> cache = threadCache.get();
	List<IconIterator<?>> stack = cache.get(name);
	if (stack == null) {
		stack = new ArrayList<IconIterator<?>>();
		cache.put(name, stack);
	}
	stack.add(body);
  }
}

//==== END OF FILE
