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
import edu.uidaho.junicon.runtime.junicon.constructs.*;
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.FAIL;
import static edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.EMPTY_VALUE;
import edu.uidaho.junicon.runtime.junicon.iterators.IIconAtom.IconTypes;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.function.BiFunction;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Defines many of the Unicon operators.
 * For efficiency, each operator is a static field that implements
 * IIconOperator.
 * The Junicon Spring configuration file then correlates operator symbols
 * to these static fields using its OperatorOverAtoms property.
 * <P>
 * For operators as well as an iterator's nextAtom(),
 * there is no difference in behavior between null
 * and an atom that holds null, and null is not treated as failure.
 * <P>
 * Note that
 * IconNumber.toNumber() returns null if input is not number or null.
 *
 * @author Peter Mills
 */
public class IconOperators {

  //==========================================================================
  // Memorization.
  //==========================================================================
  private static Map<String, UnaryOperator>
			symbolToOperatorUnary = new HashMap();
  private static Map<String, BinaryOperator>
			symbolToOperatorBinary = new HashMap();

  /**
   * Memorize function over values associated to a given symbol,
   * converting it to a variadic function.
   */
  public static <V> BinaryOperator<V> memorize (String symbol,
		BinaryOperator<V> operator) {
	if ((symbol == null) || (operator == null)) { return null; }
	symbolToOperatorBinary.put(symbol, operator);
	return operator;
  }
		
  /**
   * Get memorized function over values associated to a given symbol.
   */
  public static <V> BinaryOperator<V> getMemorizedBinary (
			String symbol) {
	return symbolToOperatorBinary.get(symbol);
  }

  /**
   * Memorize function over values associated to a given symbol,
   * converting it to a variadic function.
   */
  public static <V> UnaryOperator<V> memorize (String symbol,
		UnaryOperator<V> operator) {
	if ((symbol == null) || (operator == null)) { return null; }
	symbolToOperatorUnary.put(symbol, operator);
	return operator;
  }
		
  /**
   * Get memorized function over values associated to a given symbol.
   */
  public static <V> UnaryOperator<V> getMemorizedUnary (
		String symbol) {
	return symbolToOperatorUnary.get(symbol);
  }

  //==========================================================================
  // Arithmetic
  //==========================================================================

  private static java.util.Random randomNumber = new java.util.Random();

  /**
   * x+y, +x
   */
  public static BinaryOperator<IIconAtom> plus = memorize("+", 
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isNumber()) || (! y.isNumber())) {	// return FAIL;
	    throw new RuntimeException("Error code 102: not number");
	}
	// Align to: INTEGER, REAL, BIGINTEGER, BIGDECIMAL
	IconTypes align = IconValueNumber.align(x,y);
	if (align == IconTypes.BIGINTEGER) {
		return IconValue.create(
			(x.getBigInteger()).add(y.getBigInteger()) );
	}
	if (align == IconTypes.BIGDECIMAL) {
		return IconValue.create(
			(x.getBigDecimal()).add(y.getBigDecimal()) );
	}
	if (align == IconTypes.REAL) {
		return IconValue.create( x.getReal() + y.getReal() );
	}
	return IconValue.create( x.getInteger() + y.getInteger() );
    });

  public static UnaryOperator<IIconAtom> plusUnary = memorize("+",
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	IconValue x = xatom.getValue();
	if (! x.isNumber()) {	// return FAIL;
	    throw new RuntimeException("Error code 102: not number");
	}
	// Coerce to number, might have been string
	return IconValue.create( x.getNumber() );
    });

  /**
   * x-y, -x
   */
  public static BinaryOperator<IIconAtom> minus = memorize("-",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isNumber()) || (! y.isNumber())) {	// return FAIL;
	    throw new RuntimeException("Error code 102: not number");
	}
	// Align to: INTEGER, REAL, BIGINTEGER, BIGDECIMAL
	IconTypes align = IconValueNumber.align(x,y);
	if (align == IconTypes.BIGINTEGER) {
		return IconValue.create(
			(x.getBigInteger()).subtract(y.getBigInteger()) );
	}
	if (align == IconTypes.BIGDECIMAL) {
		return IconValue.create(
			(x.getBigDecimal()).subtract(y.getBigDecimal()) );
	}
	if (align == IconTypes.REAL) {
		return IconValue.create( x.getReal() - y.getReal() );
	}
	return IconValue.create( x.getInteger() - y.getInteger() );
    });

  public static UnaryOperator<IIconAtom> minusUnary = memorize("-",
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	IconValue x = xatom.getValue();
	if (! x.isNumber()) {	// return FAIL;
	    throw new RuntimeException("Error code 102: not number");
	}
	if (x.isBigInteger()) {
		return IconValue.create( (x.getBigInteger()).negate() );
	}
	if (x.isBigDecimal()) {
		return IconValue.create( (x.getBigDecimal()).negate() );
	}
	if (x.isReal()) {
		return IconValue.create( - x.getReal() );
	}
	return IconValue.create( - x.getInteger() );
    });

  /**
   * x/y
   * In Icon, in integer division the remainder is truncated.
   * In Groovy, by default integer division yields a BigDecimal or Double.
   * To realize Icon semantics, we must force integer division.
   */
  public static BinaryOperator<IIconAtom> division = memorize("/",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isNumber()) || (! y.isNumber())) {	// return FAIL;
	    throw new RuntimeException("Error code 102: not number");
	}
	// Align to: INTEGER, REAL, BIGINTEGER, BIGDECIMAL
	IconTypes align = IconValueNumber.align(x,y);
	if (align == IconTypes.BIGINTEGER) {
		return IconValue.create(
			(x.getBigInteger()).divide(y.getBigInteger()) );
	}
	if (align == IconTypes.BIGDECIMAL) {
		return IconValue.create(
			(x.getBigDecimal()).divide(y.getBigDecimal()) );
	}
	if (align == IconTypes.REAL) {
		return IconValue.create( x.getReal() / y.getReal() );
	}
	// Integer divide
	if (IconNumber.getIsIntegerPrecision()) {
		return IconValue.create(
			(x.getBigInteger()).divide(y.getBigInteger()) );
	}
	return IconValue.create( x.getInteger() / y.getInteger() );
    });

  /**
   * x*y, *x
   */
  public static BinaryOperator<IIconAtom> times = memorize("*",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isNumber()) || (! y.isNumber())) {	// return FAIL;
	    throw new RuntimeException("Error code 102: not number");
	}
	// Align to: INTEGER, REAL, BIGINTEGER, BIGDECIMAL
	IconTypes align = IconValueNumber.align(x,y);
	if (align == IconTypes.BIGINTEGER) {
		return IconValue.create(
			(x.getBigInteger()).multiply(y.getBigInteger()) );
	}
	if (align == IconTypes.BIGDECIMAL) {
		return IconValue.create(
			(x.getBigDecimal()).multiply(y.getBigDecimal()) );
	}
	if (align == IconTypes.REAL) {
		return IconValue.create( x.getReal() * y.getReal() );
	}
	return IconValue.create( x.getInteger() * y.getInteger() );
    });

  /**
   * *x as size.
   */
  public static UnaryOperator<IIconAtom> timesUnary = memorize("*",
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	IconValue x = xatom.getValue();
	int size = 0;
	if (x.isString()) {
		size = (x.getString()).length();
	} else if (x.isNumber()) {
		size = 0;	// (x.getNumber()).size();
	} else if (x.isCollection()) {
		size = (x.getCollection()).size();
	} else if (x.isMap()) {
		size = (x.getMap()).size();
	} else if (x.isGenerator()) {		// or IconCoExpression
		size = (x.getGenerator()).getCount();
	} else if (x.isIterator()) {
		size = 0;			// Unsupported
	} else if (x.isArray()) {
		size = (x.getArray()).length;
	} else { // If not Map, Collection, CharSequence, then is class/record.
	    	List names = IconField.objectAsNames(x.getObject());
	    	if (names == null) { size = 0;
		} else { size = names.size(); }
	}
	return IconValue.create(size);
    });
  //====
  // Error: 112 not cset, string, co-expression, or a structure
  // return 0;
  //====

  /**
   * x%y
   * If either x or y is BigDecimal or BigInteger, uses its remainder method.
   */
  public static BinaryOperator<IIconAtom> remainder = memorize("%",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isNumber()) || (! y.isNumber())) {	// return FAIL;
	    throw new RuntimeException("Error code 102: not number");
	}
	// Align to: INTEGER, REAL, BIGINTEGER, BIGDECIMAL
	IconTypes align = IconValueNumber.align(x,y);
	if (align == IconTypes.BIGINTEGER) {
		return IconValue.create(
			(x.getBigInteger()).remainder(y.getBigInteger()) );
	}
	if (align == IconTypes.BIGDECIMAL) {
		return IconValue.create(
			(x.getBigDecimal()).remainder(y.getBigDecimal()) );
	}
	if (align == IconTypes.REAL) {
		return IconValue.create( x.getReal() % y.getReal() );
	}
	return IconValue.create( x.getInteger() % y.getInteger() );
    });

  /**
   * x^y numeric power of.
   */
  public static BinaryOperator<IIconAtom> powerOf = memorize("^",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isNumber()) || (! y.isNumber())) {	// return FAIL;
	    throw new RuntimeException("Error code 102: not number");
	}

	// Integer power
	if (y.isInteger() || y.isBigInteger()) {
		int power = y.getNumber().intValue();
		if (power >= 0) {
		  if (IconNumber.getIsIntegerPrecision() && x.isBigInteger()) {
		    return IconValue.create( (x.getBigInteger()).pow(power) );
		  }
		  if (IconNumber.getIsRealPrecision() && x.isBigDecimal()) {
		   return IconValue.create( (x.getBigDecimal()).pow(power) );
		  } 
		}
		return IconValue.create( Math.pow(x.getReal(), power) );
	}

	// Decimal power
	return IconValue.create( Math.pow(x.getReal(), y.getReal()) );
    });

  //==========================================================================
  // Comparison operations.
  //==========================================================================

  /**
   * x>y	Numeric greater than.
   */
  public static BinaryOperator<IIconAtom> greaterThan = memorize(">",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }

	// Return second argument if true, else fail
	return IconOperator.handleBoolean(
		numberCompareTo(xatom,yatom) > 0,
		xatom, yatom);
    });

  /**
   * x>=y	Numeric greater than or equals.
   */
  public static BinaryOperator<IIconAtom> greaterThanOrEquals = memorize(">=",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	return IconOperator.handleBoolean(
		numberCompareTo(xatom,yatom) >= 0,
		xatom, yatom);
    });

  /**
   * x<y	Numeric less than.
   */
  public static BinaryOperator<IIconAtom> lessThan = memorize("<",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	return IconOperator.handleBoolean(
		numberCompareTo(xatom,yatom) < 0,
		xatom, yatom);
    });

  /**
   * x<=y	Numeric less than or equals.
   */
  public static BinaryOperator<IIconAtom> lessThanOrEquals = memorize("<=",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	return IconOperator.handleBoolean(
		numberCompareTo(xatom,yatom) <= 0,
		xatom, yatom);
    });

  /**
   * x>>y	String greater than.
   */
  public static BinaryOperator<IIconAtom> stringGreaterThan = memorize(">>",
    (IIconAtom xatom, IIconAtom yatom) -> {
	return IconOperator.handleBoolean(
		stringCompareTo(xatom,yatom) > 0,
		xatom, yatom);
    });

  /**
   * x>>=y	String greater than or equals.
   */
  public static BinaryOperator<IIconAtom> stringGreaterThanOrEquals = memorize(">>=",
    (IIconAtom xatom, IIconAtom yatom) -> {
	return IconOperator.handleBoolean(
		stringCompareTo(xatom,yatom) >= 0,
		xatom, yatom);
    });

  /**
   * x<<y	String less than.
   */
  public static BinaryOperator<IIconAtom> stringLessThan = memorize("<<",
    (IIconAtom xatom, IIconAtom yatom) -> {
	return IconOperator.handleBoolean(
		stringCompareTo(xatom,yatom) < 0,
		xatom, yatom);
    });

  /**
   * x<<=y	String less than or equals.
   */
  public static BinaryOperator<IIconAtom> stringLessThanOrEquals = memorize("<<=",
    (IIconAtom xatom, IIconAtom yatom) -> {
	return IconOperator.handleBoolean(
		stringCompareTo(xatom,yatom) <= 0,
		xatom, yatom);
    });

  /**
   * x=y	Test for numeric equality.
   * Convert both to numbers if either is a number or a number in string form.
   * It is an error if one is a number and the other is not.
   * SO: "1"="1.0", but 1="hello" gives an error.
   */
  public static BinaryOperator<IIconAtom> sameNumberAs = memorize("=",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	return IconOperator.handleBoolean(
		numberCompareTo(xatom,yatom) == 0,
		xatom, yatom);
    });

  /**
   * x==y	Test for string equality.
   * Converts arguments to strings using toString().
   */
  public static BinaryOperator<IIconAtom> sameStringAs =  memorize("==",
    (IIconAtom xatom, IIconAtom yatom) -> {
	return IconOperator.handleBoolean(
		stringCompareTo(xatom,yatom) == 0,
		xatom, yatom);
    });

  /**
   * x===y	Test for value equality.
   * Tests if arguments are equal using equals().
   * Value comparison fails if x and y do not have the same type.
   */
  public static BinaryOperator<IIconAtom> sameValueAs = memorize("===",
    (IIconAtom xatom, IIconAtom yatom) -> {
	return IconOperator.handleBoolean(
		objectCompareTo(xatom,yatom),
		xatom, yatom);
    });

  /**
   * x~=y	Test for numeric inequality.
   */
  public static BinaryOperator<IIconAtom> notSameNumberAs = memorize("~=",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	return IconOperator.handleBoolean(
		numberCompareTo(xatom,yatom) != 0,
		xatom, yatom);
    });

  /**
   * x~==y	Test for string inequality.
   */
  public static BinaryOperator<IIconAtom> notSameStringAs = memorize("~==",
    (IIconAtom xatom, IIconAtom yatom) -> {
	return IconOperator.handleBoolean(
		stringCompareTo(xatom,yatom) != 0,
		xatom, yatom);
    });

  /**
   * x~===y	Test for value inequality.
   */
  public static BinaryOperator<IIconAtom> notSameValueAs = memorize("~===",
    (IIconAtom xatom, IIconAtom yatom) -> {
	return IconOperator.handleBoolean(
		! objectCompareTo(xatom,yatom),
		xatom, yatom);
    });

  //==========================================================================
  // Null test, and dereference.
  //==========================================================================

  /**
   * \x    Null test unary operator.   Fails if x is null.
   */
  public static UnaryOperator<IIconAtom> failIfNull =
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	if (xatom.deref() == null) { return FAIL; }
	return xatom;
    };

  /**
   * /x    Non-null test unary operator.   Fails if x is non-null.
   */
  public static UnaryOperator<IIconAtom> failIfNonNull = 
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	if (xatom.deref() != null) { return FAIL; }
	return xatom;
    };

  /**
   * .x dereference value, just identity function.
   */
  public static UnaryOperator<IIconAtom> dereference = 
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	return xatom.getValue();
    };

  //==========================================================================
  // Random.
  //==========================================================================

  /**
   * x?y, ?x    Random or string search operator.
   *
   * ?list gives an updatable index reference using a random position.
   * ?collection or ?set gives a randomly selected value.
   * ?string gives an index reference using a random position, which
   *	is updatable if the operand is a variable, and replaces the variable.
   * ?map gives an updatable index reference using a random key.
   * ?record or ?object gives an updatable reference to a random public field.
   * ?integer gives a random integer j where 1<=j<=integer.
   * ?0 gives a random real r where 0<=r<=1.
   *
   * string?operator gives results of string matching.
   */
  public static UnaryOperator<IIconAtom> questionMarkUnary = 
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	IconValue x = xatom.getValue();
	if (x.isCollection()) {
		int size = x.getCollection().size();
		return new IconIndex().origin(0).setIndex(x,
			randomNumber.nextInt(size));
	}
	if (x.isString()) {
		int size = x.getString().length();
		return new IconIndex().origin(0).setIndex(xatom,
			randomNumber.nextInt(size));
	}
	if (x.isMap()) {
		List names = new ArrayList<Object>(x.getMap().keySet());
		if ((names == null) || (names.size() == 0)) { return FAIL; }
		Object fieldName = 
		    names.get(randomNumber.nextInt(names.size()));
		return new IconIndex().setMapIndex(x, fieldName);
	}
	if (x.isNumber()) {
		int num = x.getNumber().intValue();
		if (num == 0) {
		  return IconValue.create(randomNumber.nextDouble());
		}
		return IconValue.create(1 + randomNumber.nextInt(num));
	}
	if (x.isArray()) {
		int size = x.getArray().length;
		return new IconIndex().origin(0).setIndex(xatom,
			randomNumber.nextInt(size));
	}
	if (x.isOther()) {
		// Return random field of object
		List<String> names = IconField.objectAsNames(x.getObject());
		if ((names == null) || (names.size() == 0)) { return FAIL; }
		String fieldName = 
			names.get(randomNumber.nextInt(names.size()));
		return new IconField(x, fieldName);
	}
	return EMPTY_VALUE;
    };

  //==== Use IconScan instead
  // public static BinaryOperator<IIconAtom> questionMark = 
  //    (IIconAtom xatom, IIconAtom yatom) -> {
  //	return yatom;
  // };
  //====

  //============================================================================
  // Coexpression and thread operators.
  //============================================================================
  
  /**
   * ^C  (refresh)
   * Returns clone of coexpression, or this if plain iterator,
   * or empty atom if null, or fail otherwise.
   */
  public static UnaryOperator<IIconAtom> refresh = memorize("^",
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	Object coexpr = xatom.getObject();
	if (coexpr == null) { return EMPTY_VALUE; }
	if (! (coexpr instanceof IIconIterator)) { return FAIL; }
	return IconValue.create( ((IIconIterator) coexpr).refresh() );
    });

  /**
   * {@literal @}C, msg{@literal @}C  (activate)
   */
  public static TriFunction<IIconIterator, IIconAtom> activate =
      (IIconIterator iter, IIconAtom msgAtom, IIconAtom coexprAtom) -> {
	// On unary, msgAtom is null 
	return activateAtom(iter, msgAtom, coexprAtom);
    };

  /**
   * Activate coexpression using atom arguments.
   * For other iterators, is just nextAtom().
   * Otherwise FAIL.
   */
  private static IIconAtom activateAtom (IIconIterator iter, IIconAtom msgAtom,
			IIconAtom coexprAtom) {
	if ((iter == null) || (coexprAtom == null)) {
	    return FAIL;
	}
	Object coexpr = coexprAtom.deref();
	if (coexpr == null) { return FAIL; }
	if (! (coexpr instanceof IconCoExpression)) {
		if (! (coexpr instanceof IIconIterator)) { return FAIL; }
		// return ((IIconIterator) coexpr).nextAtom();
		return IconCoExpression.activate(iter,
			null, (IIconIterator) coexpr);
	}
	Object msg = null;
	if (msgAtom != null) { msg = msgAtom.deref(); }
	return IconCoExpression.activate(iter,
		msg, (IconCoExpression) coexpr);
  }

  /**
   * msg {@literal @}> C  (send, returns size of queue)
   */
  public static BinaryOperator<IIconAtom> send = memorize("@>",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	Object msg = xatom.getObject();
	Object coexpr = yatom.getObject();
	BlockingQueue channel = getChannel(coexpr, true);
	if (channel == null) { return FAIL; }
	if (msg != null) {
		if (! channel.offer(msg)) { return FAIL; }
	}
	return IconValue.create( channel.size() );
    });

  /**
   * msg {@literal @}>> C  (blockingSend, returns size of queue)
   */
  public static BinaryOperator<IIconAtom> blockingSend = memorize("@>>",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	Object msg = xatom.getObject();
	Object coexpr = yatom.getObject();
	BlockingQueue channel = getChannel(coexpr, true);
	if (channel == null) { return FAIL; }
	if (msg != null) {
	    try {
		channel.put(msg);
	    } catch (InterruptedException e) {
		return FAIL;
	    }
	}
	return IconValue.create( channel.size() );
    });

  /**
   * <{@literal @} C  (receive)
   */
  public static UnaryOperator<IIconAtom> receiveUnary = memorize("@",
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	Object coexpr = xatom.getObject();
	BlockingQueue channel = getChannel(coexpr, false);
	if (channel == null) { return FAIL; }
	Object msg = channel.poll();
	if (msg == null) { return FAIL; }
	return IconValue.create( msg );
    });

  /**
   * <<{@literal @} C, timeout <<{@literal @} C  (blockingReceive)
   */
  public static BinaryOperator<IIconAtom> blockingReceive = memorize("<<@",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue timeout = xatom.getValue();
	Object coexpr = yatom.getObject();
	BlockingQueue channel = getChannel(coexpr, false);
	if (channel == null) { return FAIL; }
	if (! timeout.isNumber()) { return FAIL; }
	long timeNum = timeout.getInteger();
	Object msg = null;
	try {
	  msg = channel.poll(timeNum, TimeUnit.MILLISECONDS);
	} catch (InterruptedException e) {
	  return FAIL;
	}
	if (msg == null) { return FAIL; }
	return IconValue.create( msg );
    });

  public static UnaryOperator<IIconAtom> blockingReceiveUnary = memorize("<<@",
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	Object coexpr = xatom.getObject();
	BlockingQueue channel = getChannel(coexpr, false);
	if (channel == null) { return FAIL; }
	Object msg = null;
	try {
	    msg = channel.take();
	} catch (InterruptedException e) {
	    return FAIL;
	}
	if (msg == null) { return FAIL; }
	return IconValue.create( msg );
    });

  /**
   * Get channel from given co-expression or channel,
   *	or current co-expression if null.
   * If channel is null, uses current co-expression:
   *	if isSend uses outbox, else uses inbox.
   * If channel is a BlockingQueue, i.e., is a channel, just use it.
   * Otherwise, channel is a co-expression:
   *	if isSend uses inbox, else uses outbox.
   */
  public static BlockingQueue getChannel (Object x, boolean isSend) {
	IconCoExpression coexpr = null;
	if (x == null) {
		coexpr = IconCoExpression.getCurrentCoexpr();
		if (coexpr == null) { return null; }
		if (isSend) { return coexpr.getOutbox(); }
		return coexpr.getInbox();
	}
	if (x instanceof IconCoExpression) {
		coexpr = (IconCoExpression) x;
		if (isSend) { return coexpr.getInbox(); }
		return coexpr.getOutbox();
	}
	if (x instanceof BlockingQueue) { return (BlockingQueue) x; }
	return null;
  }

  //==========================================================================
  // String matching operations.
  //==========================================================================

  /**
   * =c abbreviation for tab(match(string)).
   */
  public static UnaryOperator<IIconAtom> tabMatch = memorize("=",
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	IconValue x = xatom.getValue();
	if (! x.isString()) {
		throw new RuntimeException("Error code 103: not string");
	}
	return IconValue.create(
		IconFunctions.tab(IconFunctions.match(x.getString())) );
    });

  //==========================================================================
  // Set operations.
  //==========================================================================

  /**
   * ++ set union.
   */
  public static BinaryOperator<IIconAtom> setUnion = memorize("++",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isSet()) || (! y.isSet())) {
		return FAIL;
	}
	LinkedHashSet result = new LinkedHashSet(x.getSet());
	result.addAll(y.getSet());
	return IconValue.create( result );
    });

  /**
   * -- set difference.
   */
  public static BinaryOperator<IIconAtom> setDifference = memorize("--",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isSet()) || (! y.isSet())) {
		return FAIL;
	}
	LinkedHashSet result = new LinkedHashSet(x.getSet());
	result.removeAll(y.getSet());
	return IconValue.create( result );
    });

  /**
   * ** set intersection.
   */
  public static BinaryOperator<IIconAtom> setIntersection = memorize("**",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isSet()) || (! y.isSet())) {
		return FAIL;
	}
	LinkedHashSet result = new LinkedHashSet(x.getSet());
	result.retainAll(y.getSet());
	return IconValue.create( result );
    });

  /**
   * ~c character set complement with respect to {@literal &}cset.
   * Equivalent to {@literal &}cset--c.
   */
  public static UnaryOperator<IIconAtom> csetComplement = memorize("~",
    (IIconAtom xatom) -> {
	if (xatom == null) { return FAIL; }
	IconValue x = xatom.getValue();
	if (! x.isSet()) {
		return FAIL;
	}
	LinkedHashSet result = new LinkedHashSet(IconKeywords.cset);
	result.removeAll(x.getSet());
	return IconValue.create( result );
    });

  //==========================================================================
  // Concatenation operations.
  //==========================================================================

  /**
   * || string concatenation.
   */
  public static BinaryOperator<IIconAtom> stringConcat = memorize("||",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isAsString()) || (! y.isAsString())) {	// return FAIL;
	    throw new RuntimeException("Error code 103: not string");
	}
	return IconValue.create( x.getAsString() + y.getAsString() );
    });

  /**
   * ||| list concatenation.
   */
  public static BinaryOperator<IIconAtom> listConcat = memorize("|||",
    (IIconAtom xatom, IIconAtom yatom) -> {
	if ((xatom == null) || (yatom == null)) { return FAIL; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if ((! x.isList()) || (! y.isList())) {		// return FAIL;
	    throw new RuntimeException("Error code 108: not list");
	}
	ArrayList result = new ArrayList(x.getList());
	result.addAll(y.getList());
	return IconValue.create( result );
    });

  //==========================================================================
  // Utility methods used in operators.
  //==========================================================================

  /**
   * Compare two numbers. For BigDecimal and BigIntegers, uses x.compareTo(y).
   * Returns -1 if x<y, 0 if x=y, 1 if x>y, where null is least value.
   * Throws exception if arguments are not numbers.
   */
  private static int numberCompareTo (IIconAtom xatom, IIconAtom yatom) {
	if (xatom == null) {
		if (yatom == null) { return 0; }
		return -1;
	} else if (yatom == null) { return 1; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if (x.getObject() == null) {
		if (y.getObject() == null) { return 0; }
		return -1;
	} else if (y.getObject() == null) { return 1; }
	if ((! x.isNumber()) || (! y.isNumber())) {	// return FAIL;
	    throw new RuntimeException("Error code 102: not number");
	}
	// Align to: INTEGER, REAL, BIGINTEGER, BIGDECIMAL
	IconTypes align = IconValueNumber.align(x,y);
	if (align == IconTypes.BIGINTEGER) {
		return (x.getBigInteger()).compareTo(y.getBigInteger());
	}
	if (align == IconTypes.BIGDECIMAL) {
		return (x.getBigDecimal()).compareTo(y.getBigDecimal());
	}
	if (align == IconTypes.REAL) {
		if (x.getReal() == y.getReal()) {
			return 0;
		} else if (x.getReal() < y.getReal()) {
			return -1;
		}
		return 1;
	}
	if (x.getInteger() == y.getInteger()) {
		return 0;
	} else if (x.getInteger() < y.getInteger()) {
		return -1;
	}
	return 1;
  }

  /**
   * Compare two strings, using x.compareTo(y).
   * Null strings are lexically less than or equal to other strings.
   * Returns -1 if x<y, 0 if x=y, 1 if x>y, where null is least value.
   * Throws exception if arguments are not strings.
   */
  private static int stringCompareTo (IIconAtom xatom, IIconAtom yatom) {
	if (xatom == null) {
		if (yatom == null) { return 0; }
		return -1;
	} else if (yatom == null) { return 1; }
	IconValue x = xatom.getValue();
	IconValue y = yatom.getValue();
	if (x.getObject() == null) {
		if (y.getObject() == null) { return 0; }
		return -1;
	} else if (y.getObject() == null) { return 1; }
	if ((! x.isAsString()) || (! y.isAsString())) {	// return FAIL;
	    throw new RuntimeException("Error code 103: not string");
	}

	return (x.getAsString()).compareTo(y.getAsString());
  }

  /**
   * Compare two objects for equality.
   */
  private static boolean objectCompareTo (IIconAtom xatom, IIconAtom yatom) {
	if (xatom == null) {
		if (yatom == null) { return true; }
		return false;
	} else if (yatom == null) { return true; }
	Object x = xatom.deref();
	Object y = yatom.deref();
	if (x == y) { return true; }
	if ((x == null) || (y == null)) { return false; }
	return x.equals(y);
  }

}

//==== END OF FILE
