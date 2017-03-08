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

import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;

import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.function.BiFunction;

/**
 * Factory methods to take functions over values
 * into functions over IIconAtoms.
 * The returned function over atoms invokes the user-defined closure
 * over the dereferenced atoms to effect the operation.
 * If no operator is defined, returns FAIL.
 * <P>
 * IconOperator also provides support to memorize the association of
 * a symbol to its base function over values,
 * for separate use as a function for example using proc("+").
 * <P>
 * The returned value of operate makes no distinction between null
 * and an atom that holds null.
 * <P>
 * USAGE: IconOperator.overAtoms((x,y) -> x+y).
 *
 * @author Peter Mills
 */
public class IconOperator <V> {

  //==========================================================================
  // Constructors.
  //==========================================================================

  /**
   * No-arg constructor.
   */
  public IconOperator () { }

  //==========================================================================
  // Setters for dependency injection.
  //==========================================================================

  //==========================================================================
  // Promote function over values to over atoms.
  //==========================================================================

  /**
   * Create binary operator over atoms from function over values.
   */
  private static <T> BinaryOperator<IIconAtom<T>> binary (
		BinaryOperator<T> binaryOperator, boolean isBoolean) {
    return (IIconAtom<T> x, IIconAtom<T> y) -> {
	if (binaryOperator == null) { return FAIL; }

	T xvalue = null;
	T yvalue = null;
	if (x != null) { xvalue = x.deref(); }
	if (y != null) { yvalue = y.deref(); }

	T value = null;
	value = binaryOperator.apply(xvalue, yvalue);

	// Boolean operators return second arg if true, else fail
	if (isBoolean) {
		if (! toBoolean(value)) {
			return FAIL;
		} else {	// return second operand
			return y;
		}
	}
	if (value == null) { return null; }
	if (value == FAIL) { return FAIL; }

	// Promote returned value to an atom
	return IconValue.create(value);		// create()
    };
  }

  /**
   * Create unary operator over atoms from function over values.
   */
  private static <T> UnaryOperator<IIconAtom<T>> unary (
		UnaryOperator<T> unaryOperator, boolean isBoolean) {
    return (IIconAtom<T> x) -> {
	if (unaryOperator == null) { return FAIL; }

	T xvalue = null;
	if (x != null) { xvalue = x.deref(); }

	T value = null;
	value = unaryOperator.apply(xvalue);

	// Boolean operators return first arg if true, else fail
	if (isBoolean) {
		if (! toBoolean(value)) {
			return FAIL;
		} else {	// return first operand
			return x;
		}
	}
	if (value == null) { return null; }
	if (value == FAIL) { return FAIL; }

	// Promote returned value to an atom
	return IconValue.create(value);		// create()
    };
  }

  /**
   * Convert value to boolean.
   * If not Boolean or null, return false.
   */
  private static <T> boolean toBoolean (T value) {
	if (value instanceof Boolean) { return (Boolean) value;
	} else if (value == null) { return false;
	} else { return true; }
  }

  /**
   * Create binary operator over atoms from function over values.
   */
  public static <T> BinaryOperator<IIconAtom<T>> overAtoms (BinaryOperator<T> operator) {
	return binary(operator, false);
  }

  /**
   * Create unary operator over atoms from function over values.
   */
  public static <T> UnaryOperator<IIconAtom<T>> overAtoms (UnaryOperator<T> operator) {
	return unary(operator, false);
  }

  /**
   * Create binary boolean operator over atoms from function over values.
   * The resultant value is converted to false
   * if not Boolean or null, otherwise true.
   * The operator then returns the second argument if true, else fail.
   * True or false is thus converted to succeed and
   * fail, respectively, and if successful returns second operand.
   */
  public static <T> BinaryOperator<IIconAtom<T>> overAtomsBoolean (BinaryOperator<T> operator) {
	return binary(operator, true);
  }

  /**
   * Create unary boolean operator over atoms from function over values.
   * The resultant value is converted to false
   * if not Boolean or null, otherwise true.
   * The operator then returns the argument if true, else fail.
   * True or false is thus converted to succeed and
   * fail, respectively, and if successful returns second operand.
   */
  public static <T> UnaryOperator<IIconAtom<T>> overAtomsBoolean (UnaryOperator<T> operator) {
	return unary(operator, true);
  }

  /**
   * Handle binary boolean operators.
   * Binary boolean operators return second arg if true, else fail.
   */
  public static <T> IIconAtom<T> handleBoolean (boolean test,
		IIconAtom<T> x, IIconAtom<T> y) {
	if (test) { return y; }
	return FAIL;
  }

  /**
   * Handle unary boolean operators.
   * Unary boolean operators return first arg if true, else fail.
   */
  public static <T> IIconAtom<T> handleBoolean (boolean test,
		IIconAtom<T> x) {
	if (test) { return x; }
	return FAIL;
  }

  //==========================================================================
  // Reverse functions.
  //==========================================================================

  //==========================================================================
  // Create variadic function over values from operator over values.
  //==========================================================================

  /**
   * Create variadic function over values from binary operator over values.
   */
  private static <T> VariadicFunction<T,T> binaryFunction (
		BinaryOperator<T> operator, boolean isBoolean) {
    return (T... args) -> {
	if ((operator == null) || (args == null) || (args.length < 2)) {
		return (T) FAIL;
	}
	T result = operator.apply(args[0], args[1]);
	if (isBoolean) {
		if (! toBoolean(result)) {
			return (T) FAIL;
		} else {	// return second operand
			return args[1];
		}
	}
	if (result == null) { return null; }
	if (result == FAIL) { return (T) FAIL; }
	return result;
    };
  }

  /**
   * Create variadic function over values from unary operator over values.
   */
  private static <T> VariadicFunction<T,T> unaryFunction (
		UnaryOperator<T> operator, boolean isBoolean) {
    return (T... args) -> {
	if ((operator == null) || (args == null) || (args.length < 1)) {
		return (T) FAIL;
	}
	T result = operator.apply(args[0]);
	if (isBoolean) {
		if (! toBoolean(result)) {
			return (T) FAIL;
		} else {	// return first operand
			return args[0];
		}
	}
	if (result == null) { return null; }
	if (result == FAIL) { return (T) FAIL; }
	return result;
    };
  }

  /**
   * Create variadic function over values from binary operator over values.
   */
  public static <T> VariadicFunction<T,T> asFunction (
		BinaryOperator<T> operator) {
	return binaryFunction(operator, false);
  }

  /**
   * Create variadic function over values from unary operator over values.
   */
  public static <T> VariadicFunction<T,T> asFunction (
		UnaryOperator<T> operator) {
	return unaryFunction(operator, false);
  }

  /**
   * Create variadic function over values from boolean binary operator over values.
   */
  public static <T> VariadicFunction<T,T> asFunctionBoolean (
		BinaryOperator<T> operator) {
	return binaryFunction(operator, true);
  }

  /**
   * Create variadic function over values from boolean unary operator over values.
   */
  public static <T> VariadicFunction<T,T> asFunctionBoolean (
		UnaryOperator<T> operator) {
	return unaryFunction(operator, true);
  }

  //==========================================================================
  // Create variadic function over values from operator over atoms.
  //==========================================================================

  /**
   * Create variadic function over values from binary operator over atoms.
   */
  public static <T> VariadicFunction<T,T> asFunctionFromAtoms (
		BinaryOperator<IIconAtom<T>> operator) {
    return (T... args) -> {
	if ((operator == null) || (args == null) || (args.length < 2)) {
		return (T) FAIL;
	}
	IIconAtom<T> result = operator.apply(IconValue.create(args[0]),
				IconValue.create(args[1]));	// create()
	if (result == null) { return null; }
	if (result == FAIL) { return (T) FAIL; }
	return result.deref();
    };
  }

  /**
   * Create variadic function over values from unary operator over atoms.
   */
  public static <T> VariadicFunction<T,T> asFunctionFromAtoms (
		UnaryOperator<IIconAtom<T>> operator) {
    return (T... args) -> {
	if ((operator == null) || (args == null) || (args.length < 1)) {
		return (T) FAIL;
	}
	IIconAtom<T> result = operator.apply(IconValue.create(args[0])); // create()
	if (result == null) { return null; }
	if (result == FAIL) { return (T) FAIL; }
	return result.deref();
    };
  }

}

//==== Factory method alternatives
// return (new IconOperator().binary(blah))::binaryOperator
// return ((BinaryOperator) new IconOperator().binary(blah))
//====
// * IconOperator is stateless, except for setters,
// * so it can be used to create static operators.
//====

//==== END OF FILE
