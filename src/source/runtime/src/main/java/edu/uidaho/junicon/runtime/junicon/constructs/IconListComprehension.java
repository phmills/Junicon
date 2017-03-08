//========================================================================
// Copyright (c) 2015 Orielle, LLC.  
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
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.EMPTY_VALUE;

import java.util.List;
import java.util.ArrayList;

/**
 * List comprehension operator.
 * Singleton iterator that creates a list from the given iterator.
 *
 * @author Peter Mills
 */
public class IconListComprehension <T> extends IconComposition <T> {

  IIconAtom<T> initial = EMPTY_VALUE;
  IIconAtom<T> listAtom;
  List list;

  //==========================================================================
  // Constructors.
  //==========================================================================

  public IconListComprehension () {
	constantAtom(IconVar.createAsList(() -> (T) new ArrayList()));
	singleton();
  }

  public IconListComprehension (IconIterator<T> iconIter) {
	setX(iconIter);		// now ignores provideNext()
	reduce(this::listAggregator, initial);	  // Initial must be non-null
  }

  //==========================================================================
  // Override methods.
  //==========================================================================

  /**
   * Override what happens after restart().
   */
  public void afterRestart () {
	list = new ArrayList();
	listAtom = IconValue.create(list);
  }

  private IIconAtom<T> listAggregator (IIconAtom<T> sofar, IIconAtom<T> x) {
	if (x != null) { list.add(x.getValue().get()); }
	return listAtom;
  }

}
		
//==== END OF FILE
