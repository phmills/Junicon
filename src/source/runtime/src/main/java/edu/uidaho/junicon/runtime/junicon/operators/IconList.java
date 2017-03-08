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
package edu.uidaho.junicon.runtime.junicon.operators;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;

/**
 * Icon list.
 *
 * @author Peter Mills
 */
public class IconList <V> extends ArrayList <V> {

  /**
   * No-arg constructor.
   */
  public IconList () { super(); }

  /**
   * Create List from collection literal of the form: [x,y,z].
   */
  public IconList (V... args) {
	if (args == null) { return; };
	addAll(Arrays.asList(args));
  }

  /**
   * Create List from collection literal of the form: [x,y,z].
   */
  public static <T> List<T> createList (T... args) {
	if (args == null) { return new ArrayList<T>(); };
	return new ArrayList<T>(Arrays.asList(args));
  }

  /**
   * Create Array from varargs.
   */
  public static <T> T[] createArray (T... args) {
	if (args == null) { return (T[]) new Object[0]; };
	return args;
  }

  /**
   * Convert list or any collection to array.
   * If object is not a list, returns empty array.
   */
  public static <T> T[] listToArray (Object list) {
	if (! (list instanceof Collection)) {
		return (T[]) new Object[0];
	}
	return (T[]) ((Collection) list).toArray();
  }

}

//==== END OF FILE
