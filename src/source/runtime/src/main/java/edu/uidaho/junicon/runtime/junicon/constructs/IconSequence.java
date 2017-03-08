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

//============================================================================
// CONTROL CONSTRUCTS
//============================================================================

/**
 * Icon "x;y" sequence construct.
 *
 * @author Peter Mills
 */
public class IconSequence <T> extends IconComposition <T> {
  public IconSequence () { super(); }
  public IconSequence (IconIterator<T>  x) { super(x); }
  public IconSequence (IconIterator<T>  x, IconIterator<T>  y) {
	super(x, y); concat();
	x.bound();
  }
  public IconSequence (IconIterator<T> ... rest) {
	concat();
	if ((rest == null) || (rest.length == 0)) { return; }
	if (rest.length == 1) {
	    setX(rest[0]);		// this(rest[0]);
	} else if (rest.length == 2) {
	    setX(rest[0]);		// this(rest[0], rest[1]);
	    setY(rest[1]);
	    if (rest[0] != null) {
		rest[0].bound();		// WARNING: altering iterator
	    }
	} else {
	    IconSequence<T> seq = new IconSequence<T>(
		Arrays.copyOfRange(rest, 0, rest.length-1));
	    setX(seq, true);	// getX().setIsInChain();
	    seq.getY().bound();			// WARNING: altering iterator
	    // Leave last unbounded
	    setY(rest[rest.length-1]);
	}
  }
  public static <T> IconSequence<T> sequence (IconIterator<T> x,
		IconIterator<T> y) {
	return new IconSequence<T>(x,y); }
  public static <T> IconSequence<T> sequence (IconIterator<T>... rest) {
	return new IconSequence<T>(rest); }
}

//==== END OF FILE
