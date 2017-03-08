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
package edu.uidaho.junicon.substrates.groovy.iterators;

import edu.uidaho.junicon.runtime.junicon.iterators.*;

import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.concurrent.Callable;

import groovy.lang.Closure;

/**
 * Reified variable using closures.
 * Need isLocal flag since Icon procedure return
 * does not dereference unless declared local variable.
 * <P>
 * USAGE for x : new IconRefIterator({->x}, {y->x=y})
 *
 * @author Peter Mills
 */
public class IconRefIterator <T> extends IconVarIterator <T> { 

  /**
   * Default is empty iterator that just fails.
   */
  public IconRefIterator () {
  }

  /**
   * Singleton iterator using setter and getter.
   */
  public IconRefIterator (Closure<T> getter, Closure<T> setter) {
	constantAtom(new IconRef<T>(getter, setter));
	singleton();
  }

}

//==== END OF FILE
