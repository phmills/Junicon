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

import java.util.Iterator;
import java.util.Map;
import java.util.List;

import java.text.NumberFormat;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import java.lang.IllegalArgumentException;
import java.lang.ArithmeticException;

/**
 * IconNumber provides string to number conversion utilities,
 * as well as setters for index origin and arbitrary precision arithmetic,
 * and default charset used for write operations.
 * Provides configurable support for enforcing arbitrary precision
 * in converting strings to numbers, and in coercion of operator results.
 * If either operand is a BigInteger or BigDecimal,
 * it will coerce up to big number computations.
 * Index origin is used for index operations c[i], and for string operations.
 * <P>
 * Setters to drive runtime of Java are as follows.
 * <PRE>
 * IconNumber.setIsIntegerPrecision(true);
 * IconNumber.setIsRealPrecision(false);
 * IconNumber.setIndexOrigin(1);
 * </PRE>
 * The above static defaults are initially set by System.properties 
 * junicon.isIntegerPrecision, junicon.isRealPrecision, and junicon.indexOrigin.
 * The defaults are "true", "false", and "1", respectively.
 * <P>
 * Setters to drive compile-time transformation are as follows.
 * <PRE>
 * {@literal @}{@literal <}index origin="1"/{@literal >}
 * </PRE>
 *
 * @author Peter Mills
 */
public class IconNumber {

  // Arbitrary precision arithmetic settings
  static boolean isIntegerPrecision =
      (System.getProperty("junicon.isIntegerPrecision","true")).equals("true");
  static boolean isRealPrecision =
      (System.getProperty("junicon.isRealPrecision","false")).equals("true");
  static int indexOrigin = (int)
      stringToInteger(System.getProperty("junicon.indexOrigin","1"), 1);

  // Types of Numbers
  public static Number INTEGER = Integer.valueOf(0);
  public static Number REAL = Integer.valueOf(1);
  public static Number BIGINTEGER = Integer.valueOf(2);
  public static Number BIGDECIMAL = Integer.valueOf(3);

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * Empty constructor.
   */
  public IconNumber () { }

  //==========================================================================
  // Setters for dependency injection.
  //==========================================================================

  /**
   * Sets precision for converting strings to numbers and operator results.
   * If on, converts integer strings to a BigInteger;
   * otherwise converts strings to a Long.
   * For binary operators, if on, if neither operand is real,
   * aligns both operands to a BigInteger if either is.
   * Sets the static property used by all IconNumber instances.
   * This is a non-static method to allow Spring dependency injection.
   * Default is on.
   */
  public void setDefaultIsIntegerPrecision (boolean onoff) {
	isIntegerPrecision = onoff;
  }

  /**
   * Gets precision for converting strings to numbers and operator results.
   * Gets the static property used by all IconNumber instances.
   */
  public boolean getDefaultIsIntegerPrecision () {
	return isIntegerPrecision;
  }

  /**
   * Sets precision for converting strings to numbers and operator results.
   * If on, converts decimal strings to a BigDecimal,
   * otherwise converts strings to a Double.
   * For binary operators, if on, aligns both operands to BigDecimal if either
   * is a BigDecimal or a Float or Double.
   * Sets the static property used by all IconNumber instances.
   * This is a non-static method to allow Spring dependency injection.
   * Default is on.
   */
  public void setDefaultIsRealPrecision (boolean onoff) {
	isRealPrecision = onoff;
  }

  /**
   * Gets precision for converting strings to numbers and operator results.
   * Gets the static property used by all IconNumber instances.
   */
  public boolean getDefaultIsRealPrecision () {
	return isRealPrecision;
  }

  /**
   * Sets index origin for string operations. 
   * This is a non-static method to allow Spring dependency injection.
   */
  public void setDefaultIndexOrigin (int origin) {
	indexOrigin = origin;
  }

  /**
   * Gets index origin for string operations. 
   */
  public int getDefaultIndexOrigin () {
	return indexOrigin;
  }

  //==========================================================================
  // Static setters and getters
  //==========================================================================

  /**
   * Sets precision for converting strings to numbers and operator results.
   * Sets the static property used by all IconNumber instances.
   */
  public static void setIsIntegerPrecision (boolean onoff) {
	isIntegerPrecision = onoff;
  }

  /**
   * Gets precision for converting strings to numbers and operator results.
   * Gets the static property used by all IconNumber instances.
   */
  public static boolean getIsIntegerPrecision () {
	return isIntegerPrecision;
  }

  /**
   * Sets precision for converting strings to numbers and operator results.
   * Sets the static property used by all IconNumber instances.
   */
  public static void setIsRealPrecision (boolean onoff) {
	isRealPrecision = onoff;
  }

  /**
   * Gets precision for converting strings to numbers and operator results.
   * Gets the static property used by all IconNumber instances.
   */
  public static boolean getIsRealPrecision () {
	return isRealPrecision;
  }

  /**
   * Sets index origin for string operations. 
   * Default is 1.
   */
  public static void setIndexOrigin (int origin) {
	indexOrigin = origin;
  }

  /**
   * Gets index origin for string operations. 
   */
  public static int getIndexOrigin () {
	return indexOrigin;
  }

  //==========================================================================
  // Charset for write.
  //==========================================================================
  private static Charset defaultCharset = StandardCharsets.ISO_8859_1;
	// Charset.defaultCharset();	// StandardCharsets.UTF_8;

  /**
   * Sets charset for write().
   * Default is Charset.defaultCharset().
   */
  public static void setDefaultCharset (Charset charset) {
	if (charset == null) { return; }
	defaultCharset = charset;
  }

  /**
   * Sets charset for write().
   * Default is Charset.defaultCharset().
   */
  public static void setDefaultCharset (String charsetName) {
	if (charsetName == null) { return; }
	Charset charset = null;
	try {
	    charset = Charset.forName(charsetName);
	} catch (IllegalCharsetNameException e) {
	    throw new RuntimeException(e);
	} catch (UnsupportedCharsetException e) {
	    throw new RuntimeException(e);
	}
	defaultCharset = charset;
  }

  /**
   * Gets charset for write().
   */
  public static Charset getDefaultCharset () {
	return defaultCharset;
  }

  /**
   * Gets charset name for write().
   */
  public static String getDefaultCharsetName () {
	return defaultCharset.name();
  }

  //==========================================================================
  // Convert string to a small number.
  //==========================================================================
  private static Pattern isIntegerPattern = Pattern.compile("-?\\d+");
  private static NumberFormat numberFormatter = NumberFormat.getInstance();
	//====
	// A Number can be truncated to an integer using intValue().
	// Alternatively one could use NumberFormat.setParseIntegerOnly().
	//====

  /**
   * Converts any number in string format into a small Number.
   * Yields a Double or Long, but not a BigDecimal or BigInteger.
   * @return null if the string is not a number, or if null input.
   */
  public static Number stringToNumber (CharSequence str) {
    if (str == null) { return null; }
    try {
	return numberFormatter.parse(str.toString().trim());
    } catch (java.text.ParseException e) {
	return null;
    }
  }

  /**
   * Converts a string into an integer.
   * @return default if the string is not a number, or if null input.
   */
  public static long stringToInteger (CharSequence str, int defaultNum) {
    if (str == null) { return defaultNum; }
    try {
	return Long.parseLong(str.toString().trim());
    } catch (java.lang.NumberFormatException e) {
	return defaultNum;
    }
  }

  /**
   * Converts any number or number in string format into a small Number.
   * Strings are converted to Double or Integer,
   * while numbers are left unchanged.
   * Note that this method as well as others
   * could be split into different parameter types of Object or Number,
   * and rely on Groovy dynamic dispatch.
   * However, it is borderline faster to use instanceof in compiled Java.
   * @return null if the object is not a Number or number in string format,
   * or if null input.
   */
  public static Number toNumber (Object str) {
    if (str == null) { return null; }
    if (str instanceof Number) { return (Number) str; }
    if (! (str instanceof CharSequence)) { return null; }
    try {
	return numberFormatter.parse(((CharSequence) str).toString().trim());
    } catch (java.text.ParseException e) {
	return null;
    }
  }

  //==========================================================================
  // Big numbers.
  //==========================================================================
  /**
   * Converts any Number or number in string format into a Big number.
   * Strings are converted to BigDecimal, or to BigInteger if integer format,
   * while Numbers are left unchanged.
   * If isIntegerPrecision is off, instead converts integers to Long,
   * and if isRealPrecision is off, converts decimal numbers to Double.
   * Works with any CharSequence such as String, StringBuffer, or StringBuilder.
   * @return null if the object is not a Number or number in string format,
   * or if null input.
   */
  public static Number toBigNumber (Object obj) {
    if (obj == null) { return null; }
    if (obj instanceof Number) { return (Number) obj; }
    if (! (obj instanceof CharSequence)) { return null; }
    String str = ((CharSequence) obj).toString().trim();
    boolean isInteger = isIntegerPattern.matcher(str).matches();
    try {
	if (isInteger) {
	    if (isIntegerPrecision) {
		return new BigInteger(str);
	    }
	    // return Long.parseLong(str);
	    return numberFormatter.parse(str);
	}
	if (isRealPrecision) {
	    return new BigDecimal(str);
	}
	// return Double.parseDouble(str);
	return numberFormatter.parse(str);
    } catch (NumberFormatException e) {
	return null;
    } catch (java.text.ParseException e) {
	return null;
    }
  }

  /**
   * Creates a number in the given base.
   * If isIntegerPrecision, creates a BigInteger; otherwise creates a Long.
   * If radix < 0, assumes radix is in front of the string in the form: 16r3FF
   */
  public static Number toBigRadix (CharSequence chars, int radix) {
    if (chars == null) { return null; }
    String str = chars.toString().trim().toLowerCase();
    		// Radix: 2-36, Number: a-z, lower case.
    try {
	if (radix < 0) {
	    int pos = str.indexOf('r');
	    if (pos < 0) { return null; }
	    String rad = str.substring(0,pos);
	    str = str.substring(pos + 1);
	    radix = Integer.parseInt(rad);
	}
	if (isIntegerPrecision) {
	    return new BigInteger(str, radix);
	}
	return Long.parseLong(str, radix);
    } catch (NumberFormatException e) {
	return null;
    }
  }

  //==========================================================================
  // Align two big numbers.
  //==========================================================================

  /**
   * Converts arguments to numbers if they are Numbers or numeric strings,
   * and aligns both to a matching level of precision.
   * If either is a Float or Double, aligns both to REAL, i.e.,
   * a matching Double, to preserve floating point arithmetic.
   * Otherwise if either is a BigDecimal, aligns both to BIGDECIMAL,
   * or to REAL if isRealPrecision is off.
   * Otherwise if either is a BigInteger, aligns both to a BigInteger,
   * or to INTEGER is isIntegerPrecision is off.
   * Otherwise, aligns both to INTEGER.
   * The above matches Groovy math.
   * <P>
   * Returns [x,y,isBig], an array of converted numbers, with an extra
   * last element for type of results.
   * If onlyInteger, returns BIGDECIMAL or REAL if either is not an integer,
   * but performs no conversion in this case.
   * Type is: BIGINTEGER if both are BigInteger, 
   * BIGDECIMAL if both are BigDecimal,
   * INTEGER if both are Integer or Long and should use longValue(), and
   * otherwise REAL if either is Float or Double and should use doubleValue().
   * Returns null if either argument is null or not a number.
   */
  public static Number[] alignBigNumbers (Object x, Object y,
			boolean onlyInteger) {
	if ((x == null) || (y == null)) { return null; }
	Number xn = toBigNumber(x);	// null if not number
	Number yn = toBigNumber(y);
	if ((xn == null) || (yn == null)) { return null; }

	// Most common case will be BigInteger and then BigDecimal
	// Could optimize: for string, we know if is BigDecimal from toBigNumber
	Number isBig = INTEGER;		// Both are BigInteger, downcast.
	if (isIntegerPrecision) { isBig = BIGINTEGER; }

	if (xn instanceof BigInteger) {
		if (yn instanceof BigInteger) {
			// Do nothing
		} else if (yn instanceof BigDecimal) {
			if (isRealPrecision) {
				isBig = BIGDECIMAL;
				if (! onlyInteger) {
					xn = new BigDecimal((BigInteger) xn);
				}
			} else {	// No coersion to big numbers
				// xn = xn.doubleValue();  // will coerce later
				isBig = REAL;
			}
		} else if ((yn instanceof Float) || (yn instanceof Double)) {
			// No coersion to big numbers
			// xn = xn.doubleValue();  // will coerce later
			isBig = REAL;
		} else {	// yn is Integer, Long, or Byte
			if (isIntegerPrecision) {
				yn = BigInteger.valueOf(yn.longValue());
			}
		}
	} else if (xn instanceof BigDecimal) {
		if (isRealPrecision) {
		    isBig = BIGDECIMAL;
		    if (! onlyInteger) {
			if (yn instanceof BigInteger) {
				yn = new BigDecimal((BigInteger) yn);
			} else if (yn instanceof BigDecimal) {
				// Do nothing
			} else if ((yn instanceof Float) ||
					(yn instanceof Double)) {
				// No coersion to big numbers
				isBig = REAL;
			} else {	// yn is integer
				yn = new BigDecimal(yn.longValue());
			}
		    }
		} else {
		    isBig = REAL;
		}
	// xn is not BigInteger or BigDecimal
	} else if (yn instanceof BigInteger) {
		if ((xn instanceof Float) || (xn instanceof Double)) {
			// No coersion to big numbers
			// yn = yn.doubleValue();  // will coerce later
			isBig = REAL;
		} else {	// xn is Integer, Long, or Byte
			if (isIntegerPrecision) {
				xn = BigInteger.valueOf(xn.longValue());
			}
		}
	} else if (yn instanceof BigDecimal) {
		if (isRealPrecision) {
		    isBig = BIGDECIMAL;
		    if (! onlyInteger) {
			if ((xn instanceof Float) || (xn instanceof Double)) {
				// No coersion to big numbers
				// yn = yn.doubleValue();  // will coerce later
				isBig = REAL;
			} else {	// xn is Integer, Long, or Byte
				xn = new BigDecimal(xn.doubleValue());
			}
		    }
		} else {
		    isBig = REAL;
		}
	// Neither xn or yn is BigInteger or BigDecimal
	} else if ((xn instanceof Float) || (xn instanceof Double) ||
		   (yn instanceof Float) || (yn instanceof Double)) {
		isBig = REAL;
	} else {
		isBig = INTEGER;
	}
	Number[] nums = {xn, yn, isBig};
	return nums;
  }
  //====
  // In Groovy, if either is Double or Float, sets both to that.
  //====

  /**
   * Converts arguments to integer or real numbers if they are
   * Numbers or numeric strings.
   * Aligns both to a BigDecimal, or Double if isRealPrecision is off,
   * if either is BigDecimal or Float or Double;
   * otherwise aligns both to a BigInteger,
   * or Long if isIntegerPrecision is off, if either is.
   */
  public static Number[] alignBigNumbers (Object x, Object y) {
	return alignBigNumbers(x, y, false);
  }

  /**
   * Converts arguments to integer numbers if they are
   * Numbers or numeric strings, and aligns both to a BigInteger if either is.
   */
  public static Number[] alignBigIntegers (Object x, Object y) {
	return alignBigNumbers(x, y, true);
  }

  //==========================================================================
  // Conversion to integer or real.
  //==========================================================================

  /**
   * Converts a number to an integer.
   * Will downcast from BigInteger if not isIntegerPrecision.
   */
  public static Number asInteger (Number r) {
	if (r == null) { return null; }
	if (r instanceof BigInteger) {
	    if (isIntegerPrecision) {
		return r;
	    }
	} else if (r instanceof BigDecimal) {
	    if (isIntegerPrecision) {
		return ((BigDecimal) r).toBigInteger();
	    }
	}
	return Long.valueOf(r.longValue());
  }
  
  /**
   * Converts a number to a real.
   * Will downcast from BigDecimal if not isRealPrecision.
   */
  public static Number asReal (Number r) {
	if (r == null) { return null; }
	if (r instanceof BigInteger) {
	    if (isRealPrecision) {
		return new BigDecimal((BigInteger) r);
	    }
	} else if (r instanceof BigDecimal) {
	    if (isRealPrecision) {
		return r;
	    }
	}
	return Double.valueOf(r.doubleValue());
  }
  
  //==========================================================================
  // Literal number wrappers for insertion in source code.
  //==========================================================================

  /**
   * Converts a literal input number to an arbitrary precision value.
   */
  public static Number create (int num) {
	if (isIntegerPrecision) {
		return BigInteger.valueOf(num);
	}
	return Long.valueOf(num);
  }

  /**
   * Converts a literal input number to an arbitrary precision value.
   */
  public static Number create (long num) {
	if (isIntegerPrecision) {
		return BigInteger.valueOf(num);
	}
	return Long.valueOf(num);
  }


  /**
   * Converts a literal input number to an arbitrary precision value.
   */
  public static Number create (float num) {
	if (isRealPrecision) {
		return BigDecimal.valueOf(num);
	}
	return Double.valueOf(num);
  }

  /**
   * Converts a literal input number to an arbitrary precision value.
   */
  public static Number create (double num) {
	if (isRealPrecision) {
		return BigDecimal.valueOf(num);
	}
	return Double.valueOf(num);
  }

  /**
   * Converts a literal input number to an arbitrary precision value.
   */
  public static Number create (BigInteger num) {
	return num;
  }

  /**
   * Converts a literal input number to an arbitrary precision value.
   */
  public static Number create (BigDecimal num) {
	return num;
  }

  /**
   * Creates a numeric value from a string and radix.
   */
  public static Number create (CharSequence val, int radix) {
	return toBigRadix(val, radix);
  }

}

//==== END OF FILE
