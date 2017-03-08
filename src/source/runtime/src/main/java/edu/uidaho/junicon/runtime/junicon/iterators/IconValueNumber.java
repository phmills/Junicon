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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.regex.Pattern;

/**
 * Immutable number value.
 *
 * @author Peter Mills
 */
public class IconValueNumber extends IconValue <Number> {
  private Number number = null;
  private IconTypes numberType = null;
  private BigInteger bigInteger = null;
  private BigDecimal bigDecimal = null;
  private boolean haveTriedAsInteger = false;
  private boolean haveTriedAsReal = false;
	// Have tried instanceof Float or Double
   	// Is float or double; otherwise is long, short, or byte
  private boolean haveTriedAsBigInteger = false;
  private boolean haveTriedAsBigDecimal = false;

  private static Pattern isIntegerPattern = Pattern.compile("[+-]?\\d+");
  private static NumberFormat numberFormatter = NumberFormat.getInstance();
	//====
	// A Number can be truncated to an integer using intValue().
	// Alternatively one could use NumberFormat.setParseIntegerOnly().
	//====

  //==========================================================================
  // Constructors.
  //==========================================================================
  /**
   * No-arg constructor.
   */
  public IconValueNumber () { }

  /**
   * Create number of unknown type.
   */
  public IconValueNumber (Number number) {
	this.number = number;
  }

  /**
   * Create number of known type.
   * Used for literal number wrappers in source code.
   * Must lift input to arbitrary precision value if needed.
   */
  public IconValueNumber (int number) {
	this((long) number);
  }

  /**
   * Create number of known type.
   */
  public IconValueNumber (long number) {
	if (IconNumber.getIsIntegerPrecision()) {
		bigInteger = BigInteger.valueOf(number);
		this.number = bigInteger;
		this.numberType = IconTypes.BIGINTEGER;
	} else {
		this.number = Long.valueOf(number);
		this.numberType = IconTypes.INTEGER;
	}
  }

  /**
   * Create number of known type.
   */
  public IconValueNumber (float number) {
	this((double) number);
  }

  /**
   * Create number of known type.
   */
  public IconValueNumber (double number) {
	if (IconNumber.getIsRealPrecision()) {
		bigDecimal = BigDecimal.valueOf(number);
		this.number = bigDecimal;
		this.numberType = IconTypes.BIGDECIMAL;
	} else {
		this.number = Double.valueOf(number);
		this.numberType = IconTypes.REAL;
	}
  }

  /**
   * Create number of known type.
   */
  public IconValueNumber (BigInteger number) {
	if (number == null) { return; }
	this.number = number;
	this.numberType = IconTypes.BIGINTEGER;
	bigInteger = number;
  }

  /**
   * Create number of known type.
   */
  public IconValueNumber (BigDecimal number) {
	if (number == null) { return; }
	this.number = number;
	this.numberType = IconTypes.BIGDECIMAL;
	bigDecimal = number;
  }

  /**
   * Create number of known type.
   * Used when converting number from string.
   */
  public IconValueNumber (Number number, IconTypes type) {
	if (number == null) { return; }
	this.number = number;
	this.numberType = type;
	if (type == IconTypes.BIGINTEGER) {
		bigInteger = (BigInteger) number;
	} else if (type == IconTypes.BIGDECIMAL) {
		bigDecimal = (BigDecimal) number;
	}
  }

  //==========================================================================
  // Setter and getter.
  //==========================================================================
  public Number get () { return number; };

  public void set (Number number) { }

  //==========================================================================
  // Methods by type.
  //==========================================================================
  public IconTypes getType () { return IconTypes.NUMBER; }

  public boolean isNumber () { return (number != null); }

  public Number getNumber () { return number; }

  public Object getObject () { return number; }

  //==========================================================================
  // Number methods.
  //==========================================================================
  public IconTypes getNumberType () {
	if (numberType == null) { return IconTypes.UNTYPED; }
	return numberType;
  }

  public boolean isInteger () {
    if (numberType != null) { return (numberType == IconTypes.INTEGER); }
    if (number == null) { return false; }
    if (! haveTriedAsInteger) {
	haveTriedAsInteger = true;
	if ((number instanceof Integer) || (number instanceof Long)) {
		numberType = IconTypes.INTEGER;
		return true;
	}
    }
    return false;
  }

  public boolean isReal () {
    if (numberType != null) { return (numberType == IconTypes.REAL); }
    if (number == null) { return false; }
    if (! haveTriedAsReal) {
	haveTriedAsReal = true;
	if ((number instanceof Float) || (number instanceof Double)) {
		numberType = IconTypes.REAL;
		return true;
	}
    }
    return false;
  }

  public boolean isBigInteger () {
    if (numberType != null) { return (numberType == IconTypes.BIGINTEGER); }
    if (number == null) { return false; }
    if (! haveTriedAsBigInteger) {
	haveTriedAsBigInteger = true;
	if (number instanceof BigInteger) {
		numberType = IconTypes.BIGINTEGER;
		bigInteger = (BigInteger) number;
		return true;
	}
    }
    return false;
  }

  public boolean isBigDecimal () {
    if (numberType != null) { return (numberType == IconTypes.BIGDECIMAL); }
    if (number == null) { return false; }
    if (! haveTriedAsBigDecimal) {
	haveTriedAsBigDecimal = true;
	if (number instanceof BigDecimal) {
		numberType = IconTypes.BIGDECIMAL;
		bigDecimal = (BigDecimal) number;
		return true;
	}
    }
    return false;
  }

  public long getInteger () {
    if (number == null) { return 0; }
    return number.longValue();
  }

  public double getReal () {
    if (number == null) { return 0; }
    return number.doubleValue();
  }

  public BigInteger getBigInteger () {
    if (bigInteger != null) { return bigInteger; }
    if (isBigInteger()) { return bigInteger; }
    // if (isBigDecimal()) { bigInteger = BigDecimal.toBigInteger(bigDecimal); }
    bigInteger = BigInteger.valueOf(number.longValue());
    return bigInteger;
  }

  public BigDecimal getBigDecimal () {
    if (bigDecimal != null) { return bigDecimal; }
    if (isBigDecimal()) { return bigDecimal; }
    if (isBigInteger()) {
	bigDecimal = new BigDecimal(bigInteger);
    } else {
	// if (isReal()) { bigDecimal = new BigDecimal(number.longValue()); }
	bigDecimal = new BigDecimal(number.doubleValue());
    }
    return bigDecimal;
  }

  public boolean isAsString () {
    if (number == null) { return false; }
    return true;
  }

  public String getAsString () {
    if (number == null) { return ""; }
    return number.toString();
  }

  /**
   * Convert string to number. 
   * Strings are converted to BigDecimal, or to BigInteger if integer format.
   * If isIntegerPrecision is off, instead converts integers to Long,
   * and if isRealPrecision is off, converts decimal numbers to Double.
   * @return null if string is not a number, or if null input.
   */
  public static IconValueNumber toNumber (String str) {
    if (str == null) { return null; }
    str = str.trim();
    boolean isInteger = isIntegerPattern.matcher(str).matches();
    try {
	if (isInteger) {
	    if (IconNumber.getIsIntegerPrecision()) {
		return new IconValueNumber(new BigInteger(str),
			IconTypes.BIGINTEGER);
	    }
	    return new IconValueNumber(numberFormatter.parse(str),
			IconTypes.INTEGER);
	}
	if (IconNumber.getIsRealPrecision()) {
		return new IconValueNumber(new BigDecimal(str),
			IconTypes.BIGDECIMAL);
	}
	return new IconValueNumber(numberFormatter.parse(str),
			IconTypes.REAL);
    } catch (NumberFormatException e) {
	return null;
    } catch (java.text.ParseException e) {
	return null;
    }
  }

  /**
   * Derives the aligned number type of two atoms
   * for use in arithmetic operations.
   * If either is a Float or Double, aligns both to REAL, i.e.,
   * a matching Double, to preserve floating point arithmetic.
   * Otherwise if either is a BigDecimal, aligns both to BIGDECIMAL,
   * or to REAL if isRealPrecision is off.
   * Otherwise if either is a BigInteger, aligns both to a BigInteger,
   * or to INTEGER is isIntegerPrecision is off.
   * Otherwise, aligns both to INTEGER.
   * The above matches Groovy math.
   * <P>
   * Returned type is: BIG_INTEGER if both are BigInteger, 
   * BIG_DECIMAL if both are BigDecimal,
   * INTEGER if both are Integer or Long and should use longValue(), and
   * otherwise DOUBLE if either is Float or Double and should use doubleValue().
   * @return aligned numeric type, or null
   * if either argument is null or not a number.
   */
  public static IconTypes align (IconValue x, IconValue y) {
	if ((x == null) || (y == null)) { return null; }
	if (! (x.isNumber() && y.isNumber())) { return null; }
	x = x.getValue();
	y = y.getValue();
	IconTypes result = IconTypes.INTEGER;	// Both are BigInteger, downcast
	if (IconNumber.getIsIntegerPrecision()) {
		result = IconTypes.BIGINTEGER;
	}
	// Most common case will be BigInteger and then BigDecimal
	if (x.isBigInteger()) {
		if (y.isBigInteger()) {
			// Do nothing
		} else if (y.isBigDecimal()) {
			if (IconNumber.getIsRealPrecision()) {
				result = IconTypes.BIGDECIMAL;
			} else {	// No coersion to big numbers
				result = IconTypes.REAL;
			}
		} else if (y.isReal()) {
			result = IconTypes.REAL;
		}	// else: yn is Integer, Long, or Byte
	} else if (x.isBigDecimal()) {
		if (IconNumber.getIsRealPrecision()) {
			result = IconTypes.BIGDECIMAL;
			if (y.isReal()) {
				result = IconTypes.REAL;
			}
		} else {
			result = IconTypes.REAL;
		}
	// xn is not BigInteger or BigDecimal
	} else if (y.isBigInteger()) {
		if (x.isReal()) {
			result = IconTypes.REAL;
		}	//  else: xn is Integer, Long, or Byte
	} else if (y.isBigDecimal()) {
		if (IconNumber.getIsRealPrecision()) {
			result = IconTypes.BIGDECIMAL;
			if (x.isReal()) {
				result = IconTypes.REAL;
			}
		} else {
			result = IconTypes.REAL;
		}
	// Neither xn or yn is BigInteger or BigDecimal
	} else if (x.isReal() || y.isReal()) {
		result = IconTypes.REAL;
	} else {
		result = IconTypes.INTEGER;
	}
	return result;
  }
  //====
  // In Groovy, if either is Double or Float, sets both to that.
  //====

}

//==== END OF FILE
