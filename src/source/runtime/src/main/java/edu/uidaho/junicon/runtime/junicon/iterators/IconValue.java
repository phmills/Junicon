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
import java.math.BigInteger;
import java.math.BigDecimal;

/**
 * Immutable typed value.
 *
 * @author Peter Mills
 */
public class IconValue <T> extends IconAtom <T> {

  //==========================================================================
  // Constructors.
  //==========================================================================
  public IconValue () { }

  //==========================================================================
  // Setter and getter.
  //==========================================================================
  public T get () { return null; };

  public void set (T value) { }

  //==========================================================================
  // Typed values.
  //==========================================================================
  /**
   * Immutable value set at creation, so cannot later change it.
   */
  public void setValue (IconValue<T> atom) { }

  public IconValue<T> getValue () { return this; }

  /**
   * Gets if the value has been typed.
   * Deferred typing monotonically refines the type as to what type it is not.
   */
  public boolean isTyped () { return true; }

  /**
   * Gets the original type of the value.
   * If the value is a number converted from a string,
   * getType() will be string, and both isNumber() and isString() will be true.
   * The refined type of the number can be obtained by getNumberType().
   */
  public IconTypes getType () { return IconTypes.UNTYPED; }

  //==========================================================================
  // Methods by type.
  //==========================================================================
  /**
   * Returns if the original value is a string.
   * If the value is untyped, and haven't yet tried, tests for the given type.
   * If true, getString() is non-null.
   */
  public boolean isString () { return false; }

  /**
   * Gets string if is a string, otherwise returns null.
   */
  public String getString () { return null; }

  /**
   * Gets if is convertable to a string, i.e., is a string or number.
   */
  public boolean isAsString () { return false; }

  /**
   * Gets string if is a string or number, otherwise returns null.
   */
  public String getAsString () { return null; }

  /**
   * Returns if is a number, or can be converted to a number from a string. 
   * If the value is untyped, and haven't yet tried, tests for the given type.
   * If the original value is a string, tries to convert it to a number.
   * If true, getNumber() is non-null.
   */
  public boolean isNumber () { return false; }

  /**
   * Gets number if is a number, otherwise returns null.
   */
  public Number getNumber () { return null; }

  /**
   * Returns if is a collection. May also be list or set.
   * If the value is untyped, and haven't yet tried, tests for the given type.
   * If true, getCollection() is non-null.
   */
  public boolean isCollection () { return false; }

  /**
   * Gets collection if is a collection, otherwise returns null.
   */
  public Collection getCollection () { return null; }

  /**
   * Returns if is a list.
   * If the value is untyped, and haven't yet tried, tests for the given type.
   * If true, getList() is non-null.
   */
  public boolean isList () { return false; }

  /**
   * Gets list if is a list, otherwise returns null.
   */
  public List getList () { return null; }

  /**
   * Returns if is a set.
   * If the value is untyped, and haven't yet tried, tests for the given type.
   * If true, getSet() is non-null.
   */
  public boolean isSet () { return false; }

  /**
   * Gets set if is a set, otherwise returns null.
   */
  public Set getSet () { return null; }

  /**
   * Returns if is a map.
   * If the value is untyped, and haven't yet tried, tests for the given type.
   * If true, getMap() is non-null.
   */
  public boolean isMap () { return false; }

  /**
   * Gets map if is a map, otherwise returns null.
   */
  public Map getMap () { return null; }

  /**
   * Returns if is a Java Iterator. May also be an IconIterator.
   * If the value is untyped, and haven't yet tried, tests for the given type.
   * If true, getIterator() is non-null.
   */
  public boolean isIterator () { return false; }

  /**
   * Gets Iterator if is a Java Iterator, otherwise returns null.
   */
  public Iterator getIterator () { return null; }

  /**
   * Returns if is a generator, i.e., IconIterator.
   * If the value is untyped, and haven't yet tried, tests for the given type.
   * If true, getGenerator() is non-null.
   */
  public boolean isGenerator () { return false; }

  /**
   * Gets IconIterator if is a generator, otherwise returns null.
   */
  public IIconIterator getGenerator () { return null; }

  /**
   * Returns if is an array.
   * If the value is untyped, and haven't yet tried, tests for the given type.
   * If true, getArray() is non-null.
   */
  public boolean isArray () { return false; }

  /**
   * Gets array if is an array, otherwise returns null.
   */
  public Object[] getArray () { return null; }

  /**
   * Returns if is otherwise an object of unknown type, called a record in Icon.
   * If the value is untyped, tests for all remaining untried types.
   * Returns true only if the value is no other type.
   * If true, getOther() is non-null.
   */
  public boolean isOther () { return false; }

  /**
   * Gets value as an object.
   * The value is other only if it is no other type.
   */
  public Object getOther () { return null; }

  //==========================================================================
  // Number methods.
  //==========================================================================
  /**
   * Gets type of number.
   * Number type is one of: UNTYPED, INTEGER, REAL, BIGINTEGER, BIGDECIMAL.
   */
  public IconTypes getNumberType () { return IconTypes.UNTYPED; }

  /**
   * Returns if the number is integer.
   * Integer includes int and long, but not BigInteger.
   */
  public boolean isInteger () { return false; }

  /**
   * Returns long version of number if is a number.
   * If not number, returns 0.
   */
  public long getInteger () { return 0; }

  /**
   * Returns if the number is real instead of integer.
   * Real includes float and double, but not BigDecimal.
   */
  public boolean isReal () { return false; }

  /**
   * Returns double version of number if is a number.
   * If not number, returns 0.
   */
  public double getReal () { return 0; }

  /**
   * Returns if the number is a BigInteger.
   */
  public boolean isBigInteger () { return false; }

  /**
   * Gets BigInteger version of number if is a number.
   */
  public BigInteger getBigInteger () { return null; }

  /**
   * Returns if the number is a BigDecimal.
   */
  public boolean isBigDecimal () { return false; }

  /**
   * Gets BigDecimal version of number if is a number.
   */
  public BigDecimal getBigDecimal () { return null; }

  //==========================================================================
  // Factory to create a value with a given type.
  //==========================================================================
  /**
   * Factory to create a value with a given type.
   */
  public static <E> IconValue createTyped (Object val, IconTypes type) {
	if (val == null) { return EMPTY_VALUE; }
	if (val == FAIL) { return FAIL; }
	if (type == null) { return new IconValueUntyped(val); }
	switch (type) {
	    case STRING:
		return new IconValueString((CharSequence) val);
	    case NUMBER:
		return new IconValueNumber((Number) val);
	    case LIST:
		return new IconValueList<E>((List<E>) val);
            case SET:
		return new IconValueSet<E>((Set<E>) val);
            case MAP:
		return new IconValueMap<E,E>((Map<E,E>) val);
            case GENERATOR:
		return new IconValueGenerator<E>((IIconIterator<E>) val);
            case ITERATOR:
		return new IconValueJavaIterator<E>((Iterator<E>) val);
            case ARRAY:
		return new IconValueArray<E>((E[]) val);
	    //====
	    // How to test for function: instanceof Callable || VariadicFunction
	    //====
            case OTHER:
		return new IconValueOther((Object) val);
	}
	return new IconValueUntyped(val);
  }

  //==========================================================================
  // Factory to create a value of unknown type.
  //==========================================================================
  /**
   * Factory to create a value of unknown type, i.e., an untyped object value.
   * Deferred type is later monotonically refined as to what type is it not.
   */
  public static IconValue create (Object val) {
	//==== Instead of fully type at create, use deferred type.
	if (val == null) { return EMPTY_VALUE; }
	if (val == FAIL) { return FAIL; }
	return new IconValueUntyped(val);
  }

  //==========================================================================
  // Factory to create a value of known type.
  //==========================================================================
  /**
   * Factory to create a string value.
   */
  public static IconValue create (CharSequence val) {
	return new IconValueString(val);
  }

  /**
   * Factory to create a numeric value from a string and radix.
   */
  public static IconValue create (CharSequence val, int radix) {
	return create(IconNumber.create(val, radix));
  }

  /**
   * Factory to create a numeric value.
   */
  public static IconValue create (Number val) {
	return new IconValueNumber(val);
  }

  /**
   * Factory to create an integer value.
   */
  public static IconValue create (int val) {
	return new IconValueNumber(val);
  }

  /**
   * Factory to create an integer value.
   */
  public static IconValue create (long val) {
	return new IconValueNumber(val);
  }

  /**
   * Factory to create a real value.
   */
  public static IconValue create (float val) {
	return new IconValueNumber(val);
  }

  /**
   * Factory to create a real value.
   */
  public static IconValue create (double val) {
	return new IconValueNumber(val);
  }

  /**
   * Factory to create a BigInteger value.
   */
  public static IconValue create (BigInteger val) {
	return new IconValueNumber(val);
  }

  /**
   * Factory to create a BigDecimal value.
   */
  public static IconValue create (BigDecimal val) {
	return new IconValueNumber(val);
  }

  /**
   * Factory to create a collection value.
   */
  public static <E> IconValue create (Collection<E> val) {
	return new IconValueCollection<E>(val);
  }

  /**
   * Factory to create a list value.
   */
  public static <E> IconValue create (List<E> val) {
	return new IconValueList<E>(val);
  }

  /**
   * Factory to create a set value.
   */
  public static <E> IconValue create (Set<E> val) {
	return new IconValueSet<E>(val);
  }

  /**
   * Factory to create a map value.
   */
  public static <K,V> IconValue create (Map<K,V> val) {
	return new IconValueMap<K,V>(val);
  }

  /**
   * Factory to create an iterator value.
   */
  public static <E> IconValue create (Iterator<E> val) {
	return new IconValueJavaIterator<E>(val);
  }

  /**
   * Factory to create an IconIterator generator value.
   */
  public static <E> IconValue create (IconIterator<E> val) {
	return new IconValueGenerator<E>(val);
  }

  /**
   * Factory to create an array value.
   */
  public static <E> IconValue create (E[] val) {
	return new IconValueArray<E>(val);
  }

}

//====
// protected T value = null;
// public IconValue (T value) { }
// public IconValue (T value) { this.value = value; }
// public T get () { return value; };
//====

//==== END OF FILE
