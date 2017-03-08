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
 * Icon "until" construct.
 *
 * @author Peter Mills
 */
public class IconUntil <T> extends IconComposition <T> {
  public IconUntil (IconIterator<T> x, IconIterator<T> y) {
	IconComposition<T> do_y = new IconComposition<T>(y);
	if (x != null) { x.exists().not(); };	  // Test x
	do_y.bound().succeed();
	do_y.guard(x);
	do_y.continueBoundary();
	setX(new IconRepeatUntilEmpty<T>(new IconConcat<T>(x, do_y)));
	reduce();
  }

  public IconUntil (IconIterator<T> x) {
	this(x, new IconNullIterator());
  }

  public static <T> IconUntil<T> untilIt (IconIterator<T> x,
		IconIterator<T> y) {
	return new IconUntil<T>(x,y); }
}

//==== END OF FILE
