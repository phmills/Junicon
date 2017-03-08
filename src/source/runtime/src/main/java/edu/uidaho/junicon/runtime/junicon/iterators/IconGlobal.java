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
package edu.uidaho.junicon.runtime.junicon.iterators;

import edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.IconTypes;

import java.util.Map;
import java.util.HashMap;

/**
 * Global field with redirection to static map of globals to value atoms.
 *
 * @author Peter Mills
 */
public class IconGlobal <T> extends IconAtom <T> { 
  String name = "";

  //==========================================================================
  // Constructors.
  //==========================================================================
  /**
   * No-arg constructor.
   */
  public IconGlobal () {
  }

  /**
   * Constructor with global variable name.
   */
  public IconGlobal (String name) {
	if (name == null) { return; }
	this.name = name;
  }

  //==========================================================================
  // Setter and getter.
  //==========================================================================
  public T get () {
	IconValue<T> atom = getGlobal(name);
	if (atom == null) { return null; }
	return atom.get();
  }

  public void set (T value) {
	setGlobal(name, IconValue.create(value));
  }

  //==========================================================================
  // Typed values.
  //==========================================================================
  public void setValue (IIconAtom<T> atom) {
	if (atom == null) {
		setGlobal(name, EMPTY_VALUE);
	} else {
		setGlobal(name, atom.getValue());
	}
  }

  public IconValue getValue () {
	IconValue<T> atom = getGlobal(name);
	if (atom == null) { return EMPTY_VALUE; }
	return atom;
  }

  //==========================================================================
  // Thread local for global variable map.
  //==========================================================================
  private static final ThreadLocal<Map<String, IconValue>> globalMap = 
	new ThreadLocal () {
	    @Override protected Map<String, IconValue> initialValue () {
		 return new HashMap<String, IconValue>();
            }
  };

  private IconValue getGlobal (String name) {
	return globalMap.get().get(name);
  }

  private IconValue setGlobal (String name, IconValue value) {
	if ((name == null) || name.isEmpty()) { return null; }
	return globalMap.get().put(name, value);
  }

  //====
  // public static Map getGlobalMap () { return globalMap.get(); }
  // public static void setGlobalMap (Map map) { globalMap.set(map); }
  //====

}

//==== END OF FILE
