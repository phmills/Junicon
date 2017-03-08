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
package edu.uidaho.junicon.runtime.junicon.iterators;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * An atom returned from a continuation,
 * which holds a co-expression to activate.
 *
 * @author Peter Mills
 */
public class IconActivation <T> extends IconValue <T> { 

  IIconIterator<T> toActivate = null;	 // Coexpression to activate
  IIconIterator<T> continuation = null;	 // Containing continuation

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * No-arg constructor.
   */
  public IconActivation () {
  }

  /**
   * Constructor with coexpression and containing continuation.
   */
  public IconActivation (IIconIterator<T> coexpression,
		IIconIterator<T> continuation) {
	this.toActivate = coexpression;
	this.continuation = continuation;
  }

  //==========================================================================
  // Setter and getter.
  //==========================================================================
  public T get () { return (T) toActivate; };

  public IIconIterator<T> getToActivate () { return toActivate; }

  public IIconIterator<T> getContinued () { return continuation; }

  //==========================================================================
  // Attributes.
  //==========================================================================
  public boolean isActivation () { return true; }

}

//==== END OF FILE
