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
 * Immutable untyped value.
 * Deferred type is later monotonically refined as to what type it is not.
 *
 * @author Peter Mills
 */
public class IconValueUntyped extends IconValue <Object> {
  Object value = null;

  boolean isTyped = false;
  IconValue typedValue = null;		// If has been typed, is non-null
  boolean haveTriedAsString = false;
  boolean haveTriedAsNumber = false;
  boolean haveTriedAsCollection = false; // Includes list, set
  boolean haveTriedAsMap = false;
  boolean haveTriedAsIterator = false;	// Includes Java Iterator, IconIterator
  boolean haveTriedAsArray = false;
  // boolean haveTriedAsOther = false;	// Not needed, isOther if nothing else

  //==========================================================================
  // Constructors.
  //==========================================================================
  /**
   * No-arg constructor. Empty value.
   */
  public IconValueUntyped () {
	value = EMPTY_VALUE;
	typedValue = EMPTY_VALUE;
	isTyped = true;
  }

  /**
   * Constructor for immutable value of unknown type.
   * Type will be refined on demand.
   */
  public IconValueUntyped (Object value) {
	this.value = value;
	if (value == null) {		// Set type for constant values.
		value = EMPTY_VALUE;
		typedValue = EMPTY_VALUE;
		isTyped = true;
	} else if (value == FAIL) {
		typedValue = FAIL;
		isTyped = true;
	}
  }

  //==========================================================================
  // Setter and getter.
  // Delegate to typedValue if exists.
  //==========================================================================
  public Object get () {
	if (isTyped) { return (Object) typedValue.get(); }
	return value;
  }

  public void set (Object value) { }

  //==========================================================================
  // Typed values.
  // Delegate to typedValue if exists.
  //==========================================================================
  public void setValue (IconValue atom) { }

  public IconValue getValue () {
	if (isTyped) { return typedValue; }
	return this;
  }

  public boolean isTyped () { return isTyped; }

  public IconTypes getType () {
	if (isTyped) { return typedValue.getType(); }
	return IconTypes.UNTYPED;
  }

  //==========================================================================
  // Methods by type.
  // Delegate to typedValue if exists.
  // Otherwise, if haven't yet tried, test for given type.
  //==========================================================================
  public boolean isString () {
	if (isTyped) { return typedValue.isString(); }
	if (! haveTriedAsString) {
	    if (value instanceof CharSequence) {
		typedValue = new IconValueString((CharSequence) value);
		isTyped = true;
	    }
	    haveTriedAsString = true;
	}
	return isTyped;
  }

  public String getString () {
	if (isTyped) { return typedValue.getString(); }
	return null;
  }

  public boolean isNumber () {
	if (isTyped) { return typedValue.isNumber(); }
	if (! haveTriedAsNumber) {
	    if (value instanceof Number) {
		typedValue =  new IconValueNumber((Number) value);
		isTyped = true;
	    } else if (! haveTriedAsString) {
		if (value instanceof CharSequence) {
		    typedValue = new IconValueString((CharSequence) value);
		    isTyped = true;
		}
		haveTriedAsString = true;
	    }
	    haveTriedAsNumber = true;
	}
	if (isTyped) { return typedValue.isNumber(); }	// In case string
	return false;
  }

  public Number getNumber () {
	if (isTyped) { return typedValue.getNumber(); }
	return null;
  }

  public boolean isAsString () {
	if (isTyped) { return typedValue.isAsString(); }
	if (! haveTriedAsString) {
	    if (value instanceof CharSequence) {
		typedValue = new IconValueString((CharSequence) value);
		isTyped = true;
	    }
	    haveTriedAsString = true;
	}
	if (! isTyped) {
	  if (! haveTriedAsNumber) {
	    if (value instanceof Number) {
		typedValue =  new IconValueNumber((Number) value);
		isTyped = true;
	    }
	    haveTriedAsNumber = true;
	  }
	}
	return isTyped;
  }

  public String getAsString () {
	if (isTyped) { return typedValue.getAsString(); }
	return null;
  }

  public boolean isCollection () {
	if (isTyped) { return typedValue.isCollection(); }
	if (! haveTriedAsCollection) {
	    if (value instanceof Collection) {
		if (value instanceof List) {
		    typedValue = new IconValueList((List) value);
		} else if (value instanceof Set) {
		    typedValue = new IconValueSet((Set) value);
		} else {
		    typedValue = new IconValueCollection((Collection) value);
		}
		isTyped = true;
	    }
	    haveTriedAsCollection = true;
	}
	return isTyped;
  }

  public Collection getCollection () {
	if (isTyped) { return typedValue.getCollection(); }
	return null;
  }

  public boolean isList () {
	if ((! isTyped) && (! haveTriedAsCollection)) {
		isCollection();
	}
	if (isTyped) { return typedValue.isList(); }
	return false;
  }

  public List getList () {
	if (isTyped) { return typedValue.getList(); }
	return null;
  }

  public boolean isSet () {
	if ((! isTyped) && (! haveTriedAsCollection)) {
		isCollection();
	}
	if (isTyped) { return typedValue.isSet(); }
	return false;
  }

  public Set getSet () {
	if (isTyped) { return typedValue.getSet(); }
	return null;
  }

  public boolean isMap () {
	if (isTyped) { return typedValue.isMap(); }
	if (! haveTriedAsMap) {
	    if (value instanceof Map) {
		typedValue = new IconValueMap((Map) value);
		isTyped = true;
	    }
	    haveTriedAsMap = true;
	}
	return isTyped;
  }

  public Map getMap () {
	if (isTyped) { return typedValue.getMap(); }
	return null;
  }

  public boolean isIterator () {
	if (isTyped) { return typedValue.isIterator(); }
	if (! haveTriedAsIterator) {
	    if (value instanceof Iterator) {
		if (value instanceof IIconIterator) {
		    typedValue = new IconValueGenerator((IIconIterator) value);
		} else {
		    typedValue = new IconValueJavaIterator((Iterator) value);
		}
		isTyped = true;
	    }
	    haveTriedAsIterator = true;
	}
	return isTyped;
  }

  public Iterator getIterator () {
	if (isTyped) { return typedValue.getIterator(); }
	return null;
  }

  public boolean isGenerator () {
	if ((! isTyped) && (! haveTriedAsIterator)) {
		isIterator();
	}
	if (isTyped) { return typedValue.isGenerator(); }
	return false;
  }

  public IIconIterator getGenerator () {
	if (isTyped) { return typedValue.getGenerator(); }
	return null;
  }

  public boolean isArray () {
	if (isTyped) { return typedValue.isArray(); }
	if (! haveTriedAsArray) {
	    if (value instanceof Object[]) {
		typedValue = new IconValueArray((Object[]) value);
		isTyped = true;
	    }
	    haveTriedAsArray = true;
	}
	return isTyped;
  }

  public Object[] getArray () {
	if (isTyped) { return typedValue.getArray(); }
	return null;
  }

  public boolean isOther () {
	if (isTyped) { return typedValue.isOther(); }
	if (! haveTriedAsString) {
		if (isString()) { return false; }
	}
	if (! haveTriedAsNumber) {
		if (isNumber()) { return false; }
	}
	if (! haveTriedAsCollection) {
		if (isCollection()) { return false; }
	}
	if (! haveTriedAsMap) {
		if (isMap()) { return false; }
	}
	if (! haveTriedAsIterator) {
		if (isIterator()) { return false; }
	}
	if (! haveTriedAsArray) {
		if (isArray()) { return false; }
	}
	//====
	// How to test for function: instanceof Callable || VariadicFunction
	//====
	typedValue = new IconValueOther(value);
	isTyped = true;
	return true;
  }

  public Object getOther () {
	if (isTyped) { return typedValue.getOther(); }
	return value;
  }

  public Object getObject () {
	if (isTyped) { return typedValue.getObject(); }
	return value;
  }

  //==========================================================================
  // Number methods.
  // Delegate to typedValue if exists.
  //==========================================================================
  public IconTypes getNumberType () {
	if (isTyped) { return typedValue.getNumberType(); }
	return IconTypes.UNTYPED;
  }

  public boolean isInteger () {
	if (isTyped) { return typedValue.isInteger(); }
	return false;
  }

  public long getInteger () {
	if (isTyped) { return typedValue.getInteger(); }
	return 0;
  }

  public boolean isReal () {
	if (isTyped) { return typedValue.isReal(); }
	return false;
  }

  public double getReal () {
	if (isTyped) { return typedValue.getReal(); }
	return 0;
  }

  public boolean isBigInteger () {
	if (isTyped) { return typedValue.isBigInteger(); }
	return false;
  }

  public BigInteger getBigInteger () {
	if (isTyped) { return typedValue.getBigInteger(); }
	return null;
  }

  public boolean isBigDecimal () {
	if (isTyped) { return typedValue.isBigDecimal(); }
	return false;
  }

  public BigDecimal getBigDecimal () {
	if (isTyped) { return typedValue.getBigDecimal(); }
	return null;
  }

}

//==== END OF FILE
