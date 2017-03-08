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

/**
 * IconIn
 * binds a reified variable to the result of an iterator.
 * <P>
 * USAGE: (x in e) ==> new IconInIterator(   // e should be IconIterator
 * 		new IconReifyVariable(get:{-> x}, set:{y -> x=y}), e)
 *
 * @author Peter Mills
 */
public class IconIn <T> extends IconComposition <T> {
  private IIconAtom<T> binding;
  private IconIterator<T> generator;

  public IconIn (IIconAtom<T> b, IconIterator<T> g) {
	setX(g);
	map();
	binding = b;
	generator = g;
  }

  /**
   * Override next().
   * IconIn will set the bound variable referenced by its setter
   * to the reified value returned from the iterator.
   * The transforms set it up this way, and deference the value later when used.
   * UnaryMap will not be executed if x.isFailed or x.isReturned.
   */
  public IIconAtom<T> unaryMap (IIconAtom<T> result) {
	if (binding != null) { binding.setAtom(result); }
	return result;
  }

  // Setters
  public IIconIterator<T> filterOnReturn () {
	if (generator != null) generator.filterOnReturn();
	return this;
  }

  // Factory
  public static <T> IconIn<T> inIt (IIconAtom<T> b, IconIterator<T> g) {
	return new IconIn<T>(b,g);
  }
}

//==== END OF FILE
