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

import java.util.*;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Immutable string value.
 * Can be monotonically refined into number.
 *
 * @author Peter Mills
 */
public class IconValueString extends IconValue <String> {
  String value = null;
  private boolean haveTriedStringAsNumber = false;
  private IconValueNumber number = null;	// String converted to number

  //==========================================================================
  // Constructors.
  //==========================================================================
  public IconValueString () { }

  public IconValueString (CharSequence value) {
	if (value == null) { return; }
	this.value = value.toString();
  }

  //==========================================================================
  // Setter and getter.
  //==========================================================================
  public String get () { return value; }

  public void set (String value) { }

  //==========================================================================
  // Methods by type.
  //==========================================================================
  public IconTypes getType () { return IconTypes.STRING; }

  public boolean isString () { return true; }

  public String getString () { return value; }

  public boolean isAsString () { return true; }

  public String getAsString () { return value; }

  public Object getObject () { return value; }

  //==========================================================================
  // Delegated number.
  //==========================================================================
  public boolean isNumber () {
	if (! haveTriedStringAsNumber) {
		number = IconValueNumber.toNumber(value);
		haveTriedStringAsNumber = true;
	}
	if (number == null) { return false; }
	return number.isNumber();
  }

  public Number getNumber () {
	if (number == null) { return null; }
	return number.getNumber();
  }

  public boolean isInteger () {
	if (! haveTriedStringAsNumber) { isNumber(); }
	if (number == null) { return false; }
	return number.isInteger();
  }

  public long getInteger () {
	if (number == null) { return 0; }
	return number.getInteger();
  }

  public boolean isReal () {
	if (! haveTriedStringAsNumber) { isNumber(); }
	if (number == null) { return false; }
	return number.isReal();
  }

  public double getReal () {
	if (number == null) { return 0; }
	return number.getReal();
  }

  public boolean isBigInteger () {
	if (! haveTriedStringAsNumber) { isNumber(); }
	if (number == null) { return false; }
	return number.isBigInteger();
  }

  public BigInteger getBigInteger () {
	if (number == null) { return null; }
	return number.getBigInteger();
  }

  public boolean isBigDecimal () {
	if (! haveTriedStringAsNumber) { isNumber(); }
	if (number == null) { return false; }
	return number.isBigDecimal();
  }

  public BigDecimal getBigDecimal () {
	if (number == null) { return null; }
	return number.getBigDecimal();
  }

}

//==== END OF FILE
