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
package edu.uidaho.junicon.runtime.junicon.iterators;

import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;

import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;

/**
 * Assignment operation.
 * Maps the assignment operator over iterator operands.
 * Supports augmented assign with another operator.
 * Undo is invoked from IconIterator as required.
 * The Junicon grammar makes assignment right associative rather than iterative,
 * so assignment chains have already been broken into binary operations.
 * For example, x:=y:=z is already parsed into x:=(y:=z).
 * <P>
 * USAGE: new IconAssign(augmentOp, isSwap).undoable().over(args)
 *
 * @author Peter Mills
*/
public class IconAssign <T> extends IconComposition <T> {

  private BinaryOperator<IIconAtom<T>> assignOperator = null;
  private BinaryOperator<IIconAtom<T>> augmentOp = null;
  private boolean isSwap = false;

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * No-arg constructor.
   */
  public IconAssign () {
	assignOperator = this::assignOperator;
	product();
	map(assignOperator);	// setOperator(this); // would also work
  }

  //==========================================================================
  // Setters for dependency injection.
  //==========================================================================
  public IconAssign<T> augment (BinaryOperator<IIconAtom<T>> op) {
	augmentOp = op;
	return this;
  }

  public IconAssign<T> swap () {
	isSwap = true;
	return this;
  }

  public IconAssign<T> undoable () {
	if (assignOperator == null) { return this; };
	super.undoable( new IIconUndo<T>() {
	    IIconAtom<T> var = null;
	    IconValue value = null;
	    public void save (IIconAtom<T> var) {
		this.var = var;
		if (var != null) { value = var.getValue(); }
	    }
	    public void restore () {
		if (var == null) { return; }
		var.setValue(value);
	    }
	});
	return this;
  }

  //==========================================================================
  // Assign operator.
  //==========================================================================
  public IIconAtom<T> assignOperator (IIconAtom<T> lhs, IIconAtom<T> rhs) {
	if ((lhs == null) || (rhs == null)) { return null; }
        IconValue savedLhs = null;
	if (isSwap) { savedLhs = lhs.getValue(); }
	if (augmentOp != null) {
		IIconAtom<T> result = augmentOp.apply(lhs, rhs);
		if (result == FAIL) { return FAIL; }
		lhs.setValue((result==null)?null:result.getValue());
		if (lhs.lastSetFailed()) { return FAIL; }
	} else {
		lhs.setValue(rhs.getValue());
		if (lhs.lastSetFailed()) { return FAIL; }
	}
	if (isSwap) {
		rhs.setValue(savedLhs);
		if (rhs.lastSetFailed()) { return FAIL; }
	}
	return lhs;
  }

}

//==== END OF FILE
