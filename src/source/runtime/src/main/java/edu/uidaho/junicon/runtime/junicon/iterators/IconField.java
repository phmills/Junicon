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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.concurrent.Callable;

import java.lang.IndexOutOfBoundsException;	// List get - RuntimeException
import java.util.NoSuchElementException;   // Iterator next - RuntimeException
import java.lang.UnsupportedOperationException;	// Collection - RuntimeException
import java.lang.NoSuchFieldException;		// Field get
import java.lang.IllegalArgumentException;	// Field get - RuntimeException
import java.lang.IllegalAccessException;	// Class getField

/**
 * Provides setters and getters to access object fields by name,
 * or by index into list of declared public non-synthetic field names.
 * Uses Java reflection.
 *
 * @author Peter Mills
 */
public class IconField <T> extends IconAtom <T> {

  // Defines object reference
  Object obj = null;
  IIconAtom<T> setter = null;
  String[] names = null;
  int index = -1;
  boolean isIndex = false;

  // Frozen object reference
  boolean isFrozen = false;
  Object fieldObject = null;	// frozen object to which field applies
  boolean haveField = false;	// have gotten field
  Field field = null;		// frozen field

  List<Field> fields = null;	// declared public non-synthetic fields
  boolean haveAllFieldNames = false;	// have gotten names

//============================================================================
// Constructors
//============================================================================

  /**
   * No-arg constructor.
   */
  public IconField () {
  }

  /**
   * Construct setter/getter for the given object and sequence of field names.
   */
  public IconField (Object obj, String... names) {
	this.obj = obj;
	this.names = names;
  }

  /**
   * Construct setter/getter for the given object and sequence of field names.
   */
  public IconField (IIconAtom<T> setter, String... names) {
	this.setter = setter;
	this.names = names;
  }

  /**
   * Construct setter/getter for the given object and field index.
   * The index is into the list of declared public non-synthetic field names.
   * An index of < 0 indexes from end of list of field names.
   */
  public IconField (Object obj, int index) {
	this.obj = obj;
	this.index = index;
	this.isIndex = true;
  }

  /**
   * Construct setter/getter for the given object and field index.
   * The index is into the list of declared public non-synthetic field names.
   * An index of < 0 indexes from end of list of field names.
   */
  public IconField (IIconAtom<T> setter, int index) {
	this.setter = setter;
	this.index = index;
	this.isIndex = true;
  }

//============================================================================
// Setters for dependency injection.
//============================================================================
  /**
   * Set object for field access.
   */
  public IconField<T> setFieldContainer (Object obj) {
	this.obj = obj;
	haveField = false;	// reset
	haveAllFieldNames = false;
	return this;
  }

  /**
   * Set atom holding object for field access.
   */
  public IconField<T> setFieldContainer (IIconAtom<T> setter) {
	this.setter = setter;
	haveField = false;	// reset
	haveAllFieldNames = false;
	return this;
  }

  /**
   * Set cascading sequence of field names.
   */
  public IconField<T> setFieldNames (String... names) {
	this.names = names;
	isIndex = false;
	haveField = false;	// reset
	return this;
  }

  /**
   * Get sequence of field names.
   */
  public String[] getFieldNames () {
	// if (names == null) { return new String[] { name } }
	return names;
  }

  /**
   * Set field index.   If set, takes priority over name.
   */
  public IconField<T> setFieldIndex (int index) {
	this.index = index;
	names = null;
	isIndex = true;
	haveField = false;	// reset
	return this;
  }

  /**
   * Get field index.   If set, takes priority over name.
   */
  public int getFieldIndex () {
	return index;
  }

//============================================================================
// Access methods.
//============================================================================

  /**
   * Sets object from setter or getter if non-null.
   */
  private void deriveObject () {
    if (setter != null) {
	Object newobj = setter.deref();	// Deref also handles temporary.
	if (newobj != obj) { setFieldContainer(newobj); }
    }
  }

  /**
   * Sets fieldObject and field, used for chained fields.
   * <PRE>
   * fieldObject = obj[name[1]]...[name[last-1]]
   * field = fieldObject[name[last]]
   * </PRE>
   */
  private void deriveField () {
    fieldObject = obj;
    if (obj == null) { return; }
    if (isIndex) {
	if (! haveField) {
	    field = getFieldFromIndex();
	    haveField = true;
	}
	return;
    }
    if ((names == null) || (names.length == 0)) { return; }
    if (names.length > 1) {	// Have to re-evaluate up to last field
	fieldObject = getFieldValue(obj, names.length-1, names);
	field = getField(fieldObject, names[names.length-1]);
    } else if (! haveField) {
	field = getField(obj, names[0]);
	haveField = true;
    }
  }

  /**
   * Get object field by name, or by index if name is null or empty.
   */
  public T get () {
    if (! isFrozen) { freeze(); }
    if ((! isIndex) && ((names == null) || (names.length == 0))) {
	// Treat objref as variable
	return (T) fieldObject;	// from getter in deriveObject
    }
    if (field == null) {
	throw new IndexOutOfBoundsException("No such field " + 
	    (isIndex ? Integer.toString(index) :
	    (((names == null) && (names.length == 0)) ? "" : names[0])));
    }
    Object value = null;
    try {
	value = field.get(fieldObject);
    } catch (IllegalArgumentException e) {
	throw e;
    } catch (IllegalAccessException e) {
	throw new IllegalArgumentException(e);
    }
    return (T) value;
  }

  /**
   * Set object field by name, or by index if name is null or empty.
   */
  public void set (T value) {
    if (! isFrozen) { freeze(); }
    if ((! isIndex) && ((names == null) || (names.length == 0))) {
	// Treat objref as variable
	if (setter != null) { setter.set(value); }
	return;
    }
    if (field == null) {
	throw new IndexOutOfBoundsException("No such field " + 
	    (isIndex ? Integer.toString(index) :
	    (((names == null) && (names.length == 0)) ? "" : names[0])));
    }
    try {
	field.set(fieldObject, value);
    } catch (IllegalArgumentException e) {
	throw e;
    } catch (IllegalAccessException e) {
	throw new IllegalArgumentException(e);
    }
  }

  //=========================================================================
  // freezeReference is invoked by the singleton field iterator on next().
  //=========================================================================

  public IIconAtom<T> onReturn () { return freezeReference(); }

  /**
   * Return a clone of the field with frozen object and field values.
   */
  public IconField<T> freezeReference () {
	if (isIndex) {
		return new IconField<T>(setter, index).setFieldContainer(obj).freeze(); // create()
	}
	return new IconField<T>(setter, names).setFieldContainer(obj).freeze(); // create()
  }

  private IconField<T> freeze () {
	deriveObject();
	deriveField();
	isFrozen = true;
	return this;
  }

//============================================================================
// Field index access methods.
//============================================================================

  /**
   * Gets size of declared fields.
   */
  public int getNumberFields () {
    if (! isFrozen) {
        deriveObject();
    }
    if (! haveAllFieldNames) {
	fields = objectAsList(obj);
	haveAllFieldNames = true;
    }
    if (fields == null) { return 0; }
    return fields.size();
  }

  /**
   * Gets field for given object using index if set.
   * An index of < 0 indexes from end of list of field names.
   */
  private Field getFieldFromIndex () {
    // For numeric index into record, change to field name.
    if (! haveAllFieldNames) {
	fields = objectAsList(obj);
	haveAllFieldNames = true;
    }
    if ((fields == null) || fields.isEmpty()) {	  // No fields
	return null;
    }
    int size = fields.size();
    if (index < 0) { index += size; }		// Index from end
    if ((index < 0) || (index >= size)) {	// Index error
	return null;
    }
    Field field = fields.get(index);
    if (! Modifier.isPublic(obj.getClass().getModifiers())) {
	field.setAccessible(true);
    }
    return field;
  }

//============================================================================
// Static field methods.
//============================================================================

  /**
   * Gets field value for given object using sequence of field names.
   * If size >= 0, only uses sublist of names up to that size.
   * If any name is null or empty, returns null.
   */
  private static Object getFieldValue (Object obj, int size, String... names) {
      if ((obj == null) || (names == null)) { return obj; }
      if ((size < 0) || (size > names.length)) { size = names.length; }
      Field field = null;
      Class clazz = null;
      for (int i=0; i<size; i++) {
	String name = names[i];
	if ((name == null) || name.isEmpty()) { return null; }
	try {		// Get field for next name
	    clazz = obj.getClass();
	    field = clazz.getDeclaredField(name);
	} catch (NoSuchFieldException e) {
	    return null;
	}
	if (field == null) { return null; }
	try {		// Cascade to next field
	    if (! Modifier.isPublic(clazz.getModifiers())) {
		field.setAccessible(true);
	    }
	    obj = field.get(obj);
	} catch (IllegalArgumentException e) {
	    throw e;
	} catch (IllegalAccessException e) {
	    throw new IllegalArgumentException(e);
	}
      }
      return obj;
  }

  /**
   * Gets field for given object and name.
   * Sets field as accessible if not public.
   */
  private static Field getField (Object obj, String name) {
	if ((obj == null) || (name == null) || name.isEmpty()) {
		return null;
	}
	Field field = null;
	Class clazz = null;
	try {
	    clazz = obj.getClass();
	    field = clazz.getDeclaredField(name);
	    if (! Modifier.isPublic(clazz.getModifiers())) {
		field.setAccessible(true);
	    }
	} catch (NoSuchFieldException e) {
	    return null;
	}
	return field;
  }

  /**
   * Gets field value for given object.
   * Sets field as accessible if not public.
   */
  private static Object getFieldValue (Object obj, Field field) {
	if ((obj == null) || (field == null)) { return null; }
	Object value = null;
	try {
	    if (! Modifier.isPublic(obj.getClass().getModifiers())) {
		field.setAccessible(true);
	    }
	    value = field.get(obj);
	} catch (IllegalArgumentException e) {
	    throw e;
	} catch (IllegalAccessException e) {
	    throw new IllegalArgumentException(e);
	}
	return value;
  }

//============================================================================
// Static field access methods.
//============================================================================

  /**
   * Gets field value for given object using sequence of field names.
   */
  public static Object getFieldValue (Object obj, String... names) {
	return getFieldValue(obj, -1, names);
  }

  /**
   * Gets field value for given atom using sequence of field names.
   */
  public static <V> Object getFieldValue (IIconAtom<V> setter, String... names) {
	if (setter == null) { return null; }
	Object obj = setter.deref();
	return getFieldValue(obj, names);
  }

//============================================================================
// Utilities
//============================================================================

  /**
   * Create list of object fields,
   * for public fields declared in this class only.
   */
  public static List<Field> objectAsList (Object obj) {
    if (obj == null) { return null; }
    Field[] fields = obj.getClass().getDeclaredFields();
    ArrayList list = new ArrayList();
    if (fields == null) { return null; }
    for (Field field : fields) {
	// Screen out Groovy synthetic fields, as well as transient or static
	//   NOT: if (field.getModifiers() == java.lang.reflect.Modifier.PUBLIC)
	int mod = field.getModifiers();
	if (Modifier.isPublic(mod) && (! (field.isSynthetic() ||
		Modifier.isTransient(mod) || Modifier.isStatic(mod)))) {
	    list.add(field);
	}
    }
    return list;
  }

  /**
   * Create list of object field names,
   * for public fields declared in this class only.
   */
  public static List<String> objectAsNames (Object obj) {
    List<Field> fields = objectAsList(obj);
    if (fields == null) { return null; }
    ArrayList<String> names = new ArrayList<String>();
    for (Field field : fields) {
	names.add(field.getName());
    }
    return names;
  }

  /**
   * Create map of object field names to values,
   * for public fields declared in this class only.
   */
  public static Map<String, Object> objectAsMap (Object obj) {
    List<Field> fields = objectAsList(obj);
    if (fields == null) { return null; }
    LinkedHashMap<String,Object> map = new LinkedHashMap<String,Object>();
    for (Field field : fields) {
	try {
	    map.put(field.getName(), field.get(obj));
	} catch (IllegalAccessException e) {
	    return null;
	}
    }
    return map;
  }

}

//==== END OF FILE
