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
package edu.uidaho.junicon.runtime.junicon.operators;

import edu.uidaho.junicon.runtime.junicon.iterators.*;
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collection;

/**
 * Create character set from string.
 *
 * @author Peter Mills
 */
public class IconSet <V> extends LinkedHashSet <V> {

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * No-arg constructor.
   */
  public IconSet () {
	super();
  }

  /**
   * Create character set from string.
   */
  public IconSet (CharSequence str) {
	if (str == null) { return; }
	addAll((ArrayList) IconIndex.stringToList(str.toString()));
  }

  /**
   * Create set from collection literal of the form: {x,y,z}.
   */
  public IconSet (V... args) {
	if (args == null) { return; };
	addAll(Arrays.asList(args));
  }

  //==========================================================================
  // Factory methods.
  //==========================================================================

  /**
   * Create set from collection literal of the form: {x,y,z}.
   */
  public static <T> Set<T> createSet (T... args) {
	if (args == null) { return new LinkedHashSet<T>(); };
	return new LinkedHashSet<T>(Arrays.asList(args));
  }

  /**
   * Factory method to
   * convert single quote string to set of its characters as singleton strings.
   */
  public static Set<String> createCset (CharSequence str) {
	if (str == null) { return null; }
	return new LinkedHashSet(IconIndex.stringToList(str.toString()));
  }

}

//==== END OF FILE
