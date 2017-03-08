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

/**
 * Icon table.
 * Like map, but if key not found return default, i.e. table[undefined]=default
 *
 * @author Peter Mills
 */
public class IconMap <V> extends LinkedHashMap <V,V> {
  private V undefined = null;

  public IconMap () { super(); }

  /**
   * Create Map from collection literal of the form: [key, value, ...]
   * Keys without values are mapped to null.
   */
  public IconMap (V... args) {
	addMap(this, args);
  }

  public void setDefault (V d) { undefined = d; }

  public V getDefault () { return undefined; }

  public V get (Object x) {
	if (! containsKey(x)) { return undefined; }
	return super.get(x);
  }

  /**
   * Create Map from collection literal of the form: [key, value, ...]
   * Keys without values are mapped to null.
   */
  public static <T> Map<T,T> createMap (T... args) {
	Map<T,T> map = new LinkedHashMap<T,T>();
	addMap(map, args);
	return map;
  }

  private static <T> void addMap (Map<T,T> map, T... args) {
	if ((map == null) || (args == null)) { return; }
	int pos =0;
	while (pos < args.length) {
		T key = args[pos];
		T value = null;
		pos++;
		if (pos < args.length) {
			value = args[pos];
			map.put(key, value);
		}
		pos++;
	}
  }

}

//==== END OF FILE
