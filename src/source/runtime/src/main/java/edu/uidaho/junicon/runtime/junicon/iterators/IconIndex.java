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
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.lang.reflect.Array;

import java.lang.IndexOutOfBoundsException;	// List get - RuntimeException
import java.util.NoSuchElementException;   // Iterator next - RuntimeException
import java.lang.UnsupportedOperationException;	// Collection - RuntimeException

import java.util.concurrent.Callable;

/**
 * Produces an updatable reference c[i] or slice c[i:j]
 * into a map, list, collection, array, string, or object's fields.
 * Works on arrays, although they are not native to Icon.
 * Works on any collection in general, such as sets and queues, not just lists;
 * however, other collections cannot be updated, and
 * slicing is limited to lists, arrays, and strings.
 * <P>
 * Maps are indexed by key:
 * when dereferenced, the index produces the value for that key.
 * Lists, arrays, and strings, i.e., CharSequence,
 * are indexed by numeric position,
 * and can be sliced with a begin and end range.
 * Other collections can be accessed by position, but not updated or sliced.
 * For any other type of object, i.e. record,
 * indexing produces an updatable reference into the Object's fields.
 * Fields in objects can be accessed by field name or numeric index into
 * declared public non-synthetic class members.
 * Slicing will replace the range with a single element
 * if the replacment is not a collection, and otherwise adds all the elements.
 * <P>
 * Closures are used to capture setters and getters for the indexed collection.
 * To allow lazy dereferencing, upon evaluation will freeze, i.e., copy
 * the separate map and index references used in the setters.
 * These frozen values make the list index an assignable reference
 * that can be passed around, and later either assigned or dereferenced
 * using the setter or getter respectively.
 * <P>
 * Subscripting c[i] and slicing c[i:e] 
 * by default use Icon rules to derive the endpoints, with index origin 1.
 * Index origin may also be set to 0 for Java slicing rules.
 * For index origin 1, applies Icon rules for slicing,
 * which is up to but not including the end index,
 * and ignores reversal if the begin is after the end.
 * Otherwise, Java and Groovy rules for slicing apply, which includes the
 * end index and reverses results if the begin is after the end.
 * In both cases, an index < 0 is from the end of the list.
 * <P>
 * Only adjusts the index values for a List, Collection, Array, CharSequence,
 * or numeric field, but not for Maps.
 * The index can be any type of number or a string,
 * which are converted to integers for both origins 0 and 1.
 * <P>
 * Strings are handled differently when setting them with an updated value.
 * To match Unicon semantics, the string is converted to a list of characters,
 * then updated, the converted back to a string, which is assigned to
 * the variable reference.
 * <P>
 * USAGE  index c[i]    => new IconIndex({->c}, {->i})
 *	  slice c[b..e] => new IconIndex({->c}, {->b}, {->e})
 * USAGE: IconIndex(listAtom, begin, end)
 *
 * @author Peter Mills
 */
public class IconIndex <T> extends IconAtom <T> {
  IIconAtom<T> listAtom = null;  	// Input atom from setter
		// Used to freeze and set c in c[i].
		// The setter for a listAtom is only needed for strings.
		// Otherwise, the list reference suffices for updating.
		// ListAtom may be a reified temporary which in turn holds an
		// IconAtom reference. In this case, the list can be derived
		// from the temporary variable by deref() which does get().get()
  IIconAtom<T> beginAtom = null;  // Used to freeze i in c[i]
  IIconAtom<T> endAtom = null;	// Used to freeze e in c[b..e]
  IconValue<T> list = null;	// Frozen c in c[i] -- Derived list atom
  int begin = -1;		// Frozen b in c[b] -- Adjusted for Java
  int end = -1;			// Frozen e in c[b..e] -- Adjusted for Java
  Object beginObject = null;	// Map or record index value, object not number
  int origin = IconNumber.getIndexOrigin();		// Index origin: 0 or 1
  boolean isSlice = false;	// Is slicing operation, i.e., end != null
  boolean isOpen = false;	// Use open slicing, i.e., l[b..<e]
  boolean isFrozen = false;	// Is dereferenced
  boolean haveBeginAtom = false; // Use begin, end directly, from setIndex()
  boolean isNumber = false;	// Input index is number
  IconField field = null;	// For record, i.e., other object type
  int plusMinus = 0;	   // if >0, add begin to slice end, if <0, subtract it.
  int[] eUpdate = {-1};

  //=========================================================================
  // Constructors.
  //=========================================================================

  /**
   * No-arg constructor.
   */
  public IconIndex () {
  }

  /**
   * Constructor for subscript c[i].
   * Index origin must be 0 or 1; defaults to 0.
   */
  public IconIndex (IIconAtom<T> listAtom, IIconAtom<T> beginAtom) {
	this.listAtom = listAtom;
	this.beginAtom = beginAtom;
	this.haveBeginAtom = true;
  }

  /**
   * Constructor for slice c[b..e].
   * Index origin must be 0 or 1; defaults to 0.
   */
  public IconIndex (IIconAtom<T> listAtom, IIconAtom<T> beginAtom,
		IIconAtom<T> endAtom) {
	this.listAtom = listAtom;
	this.beginAtom = beginAtom;
	this.endAtom = endAtom;
	this.isSlice = true;
	this.haveBeginAtom = true;
  }

  //=========================================================================
  // Setters for dependency injection.
  //=========================================================================

  /**
   * Set index origin to 0 or 1.
   * Ignores argument if not 0 or 1.
   * Default index origin is IconNumber.getIndexOrigin().
   * Returns this.
   */
  public IconIndex<T> origin (int indexOrigin) {
	if ((origin == 0) || (origin == 1)) {
		this.origin = indexOrigin;
	}
	isOpen = (origin == 1);	// use open slicing, i.e., l[b..<e]
	return this;
  }

  /**
   * Set slice end as additive for c[b +: e].
   * Returns this.
   */
  public IconIndex<T> plus () {
	plusMinus = 1;
	return this;
  }

  /**
   * Set slice end as subtractive for c[b -: e].
   * Returns this.
   */
  public IconIndex<T> minus () {
	plusMinus = -1;
	return this;
  }

  //=========================================================================
  // Setters for frozen list.
  //=========================================================================

  /**
   * Set list and begin for frozen subscript c[i].
   * The already dereferenced values of c and i will be used in indexing.
   * Index origin must previously have been set to be 0 or 1; defaults to 0.
   * If origin is 1, adjusts begin.
   * Returns this.
   */
  public IconIndex<T> setIndex (IIconAtom<T> listAtom, int begin) {
	this.listAtom = listAtom;
	this.begin = begin;
	this.isNumber = true;
	beginAtom = IconValue.create(begin);  // For freezereference if return
	beginObject = beginAtom.deref();
	freeze();
	return this;
  }

  /**
   * Set list and begin/end for frozen slice c[b..e].
   * The already dereferenced values of c, b, and e will be used in indexing.
   * Index origin must previously have been set to 0 or 1; defaults to 0.
   * If origin is 1, adjusts begin and end.
   * Returns this.
   */
  public IconIndex<T> setSlice (IIconAtom<T> listAtom, int begin, int end) {
	this.listAtom = listAtom;
	this.begin = begin;
	this.end = end;
	isSlice = true;
	this.isNumber = true;
	beginAtom = IconValue.create(begin);  // For freezereference if return
	endAtom = IconValue.create(end);
	beginObject = beginAtom.deref();
	freeze();
	return this;
  }

  /**
   * Set map and begin for frozen subscript c[i].
   * Returns this.
   */
  public IconIndex<T> setMapIndex (IIconAtom<T> listAtom, Object beginObject) {
	this.listAtom = listAtom;
	this.beginObject = beginObject;
	this.isNumber = false;
	beginAtom = IconValue.create(beginObject); // For freezereference
	freeze();
	return this;
  }

  //=========================================================================
  // Override setter, getter, and onReturn, from IconAtom.
  //=========================================================================
  public T get () {
	if (! isFrozen) { freeze(); }
	if (list == null) { return null; };

	// Map index is object, not integer
	if (list.isMap()) {
	    return (T) list.getMap().get(beginObject);
	}

	if (list.isOther()) {
	    if (field == null) { return null; }
	    return (T) field.get();
	}

	// begin and end integers are used for remaining index
	if ((begin < 0) || (isSlice && (end < 0))) {
		throw new IndexOutOfBoundsException(
			"Index out of bounds " + begin + " : " + end);
	}

	if (list.isCollection()) {
	  if (list.isList()) {
	    if (isSlice) {
		return (T) new ArrayList(list.getList().subList(begin, end));
	    }
	    return (T) list.getList().get(begin);
	  }

	  // Collection: get iterator, scoot to nth element. Slice not supported
	  Iterator iter = list.getCollection().iterator();
	  T result = null;
	  int i=-1; 
	  while ((iter.hasNext()) && (i < begin)) {
		result = (T) iter.next();
		i++;
	  }
	  if (i < begin) {
		throw new IndexOutOfBoundsException("Index out of bounds "
			+ begin);
	  }
	  return (T) result;
	}

	if (list.isArray()) {
	    if (isSlice) {
		return (T) Arrays.copyOfRange(list.getArray(), begin, end);
	    }
	    return (T) list.getArray()[begin];
	}

	if (list.isString()) {
	    return (T) list.getString().substring(begin,end);
	}

	return null;	// Non-indexible type, e.g., IconIterator
  }

  public void set (T rhs) {
	if (! isFrozen) { freeze(); }
	if (list == null) { return; };

	// Map index is object, not integer
	if (list.isMap()) {
	    list.getMap().put(beginObject, rhs);
	    return;
	}

	if (list.isOther()) {
	    if (field == null) { return; }
	    field.set(rhs);
	    return;
	}

	// begin and end are used for remaining index
	if ((begin < 0) || (isSlice && (end < 0))) {
		throw new IndexOutOfBoundsException(
			"Index out of bounds " + begin + " : " + end);
	}

	if (list.isCollection()) {
	  if (list.isList()) {
	    if (isSlice) {
		List range = list.getList().subList(begin,end);
		range.clear();
		if (rhs instanceof Collection) {
			range.addAll((Collection) rhs);
		} else {
			range.add(rhs);
		}
	    } else {
		list.getList().set(begin, rhs);
	    }
	  } else {
		throw new UnsupportedOperationException(
			"Cannot update index in Collection");
	  }
	  return;
	}

	// Array update must set the listAtom, not just update the value
	if (list.isArray()) {
	    if (isSlice) {
		List fromArray = new ArrayList(Arrays.asList(list.getArray()));
		List range = fromArray.subList(begin, end);
		range.clear();
		if (rhs instanceof Collection) {
			range.addAll((Collection) rhs);
		} else if (rhs instanceof Object[]) {
			range.addAll(Arrays.asList((Object[]) rhs));
		} else {
			range.add(rhs);
		}
		listAtom.set((T) fromArray.toArray());
	    } else {
		list.getArray()[begin] = rhs;
	    }
	    return;
	}

	// String update must set the listAtom, not just update the value
	if (list.isString()) {
	    // Convert rhs non-string to string
	    String rhsString = null;
	    if (rhs != null) { rhsString = rhs.toString(); }

	    // Convert charSequence to list
	    ArrayList<String> chars = stringToList(list.getString());

	    // Update list.
	    if (isSlice) {		// Slice using lists
		ArrayList<String> rhsChars = stringToList(rhsString);
		List range = chars.subList(begin, end);
		range.clear();
		range.addAll(rhsChars);
	    } else {
		chars.set(begin, rhsString);
	    }

	    // Convert list back to string
	    String str = listToString(chars);

	    // Update variable with result
	    listAtom.set((T) str);
	}
  }

  public IIconAtom<T> onReturn () { return freezeReference(); }

  //=========================================================================
  // freezeReference is invoked by the singleton index iterator on next().
  //=========================================================================

  /**
   * Return a clone of the index with frozen list and index values.
   * Also creates the getter and setter, and the frozen values.
   */
  public IconIndex<T> freezeReference () {
	if (isSlice) {		
	    return (new IconIndex<T>(listAtom, beginAtom,
		endAtom)).origin(origin).freeze(); // create()
	}
	return (new IconIndex<T>(listAtom,
		beginAtom)).origin(origin).freeze(); // create()
  }

  //=========================================================================
  // Derive the list and index values, and create the setter and getter.
  //=========================================================================

  /**
   * Derive the list and index values used in the get and set closures.
   * Adjusts the index or slice begin and end values if index origin is 1
   * and thus Icon slicing rules apply;
   * otherwise if index origin is 0 then Groovy slicing rules apply.
   * Only adjusts the index values for a Collection (i.e, List or Set),
   * or CharSequence, but not for Maps.
   * Also adjusts the index value for records, i.e. classes that are not
   * Collection, CharSequence, or Maps, if indexing into fields using integers.
   * The index can be any type of number or a string holding a number,
   * which are converted to integers for both origins 0 and 1.
   * <P>
   * Icon slice boundaries must be adjusted since they are
   * up to but not including the end index.
   * Also, Icon slices ignore reversal if the begin is after the end:
   * to accommodate this Icon slices are adjusted to have the begin before
   * the end, which must take into account the list size if endpoints are < 0.
   * An Icon slice is thus from the left to the right boundary, less the
   * rightmost element;
   * if the begin and end are equal, the result is empty.
   * <P>
   * In all cases subscripting is from the end of the list if < 0.
   * For index origin 1 and Icon slicing rules,
   * we use the Groovy ..< half open range operator
   * that does not include the last value.
   * <P>
   * A few relevant technical notes on Groovy follow.
   * <P>
   * Groovy does not support assigning into string slices,
   *	since strings are immutable.
   * <P>
   * Groovy map keys are strings: [a:1] is equivalent to ["a":1];
   *	one can map raw types by [(a):1].
   * <P>
   * A Groovy list is an ArrayList, a Groovy set is a LinkedHashSet,
   * and a Groovy map is a LinkedHashMap.
   */
  public IconIndex<T> freeze () {
	isFrozen = true;

	//====
	// Freeze list and index values by evaluting their closures.
	//====
	if (listAtom == null) { return this; }
	list = listAtom.getValue();
	if (list == null) { return this; }

	if (haveBeginAtom) {
	  if ((beginAtom == null) || (isSlice && (endAtom == null))) {
		list = null;	// Error
		return this;
	  }
	  beginObject = beginAtom.deref();
	}

	//====
	// Skip adjusting numeric endpoints if map
	// If not Map, Collection, or CharSequence, then is record/class.
	//====
	if (list.isMap()) {
	    return this;
	}

	// See if input index is number. For slice true only if both are numbers
	if (haveBeginAtom) {
	  IconValue beginNumber = beginAtom.getValue();
	  if (beginNumber.isNumber()) { 
		begin = beginNumber.getNumber().intValue();
		isNumber = true;
	  }
	  if (isSlice) {
		IconValue endNumber = endAtom.getValue();
		if (endNumber.isNumber()) { 
			end = endNumber.getNumber().intValue();
			isNumber = true;
		} else { isNumber = false; }
	  }
	}

	// For record, can have record["name"] or record[1]
	//====
	// Get size of list, string, or record if possible
	//====
	int size = -1;
	if (list.isString()) {	// if (list instanceof CharSequence)
		if (! isNumber) { list = null; return this; }
		size = list.getString().length(); 
	} else if (list.isCollection()) { // cannot slice Set | Queue
		if (! isNumber) { list = null; return this; }
		size = list.getCollection().size(); 
	} else if (list.isArray()) {
		if (! isNumber) { list = null; return this; }
		size = list.getArray().length;
	} else if (list.isOther()) {
		// Get field if indexing into Object's fields
		// If using number, will reset field to use that later
		field = new IconField<T>(list,
			(beginObject == null ? "" :
			 beginObject.toString())); // create()
		if (! isNumber) { return this; }
		size = field.getNumberFields();
	}

	//====
	// Skip adjust if origin 0, not slicing, and index >= 0
	//====
	if ((origin == 0) && (! isSlice) && (begin >= 0)) {
	    // Validate begin against size: begin < size
	    if ((size >= 0) && (begin >= size)) {
		begin = -1;
	    } else {
		end = begin + 1;
	    }
	} else {
	//====
	// Adjust begin and end for origin and slicing rules.
	// End will now be exclusive, i.e., up to but not including last element
	//====
	  if (isSlice) {
	    if (plusMinus > 0) { end += begin;
	    } else if (plusMinus < 0) { end = begin - end; };
	    eUpdate[0] = end;
	  } else { eUpdate[0] = -1; }
	  begin = adjustSlice(begin, eUpdate, size, origin, isSlice);
	  end = eUpdate[0];
	}

	if (list.isOther() && (begin >= 0)) {
		field.setFieldIndex(begin);
	}

	return this;
  }

  //=========================================================================
  // Convert Icon index positions to Java index positions.
  //=========================================================================

  /**
   * Convert slice begin and end positions from Icon to Java.
   * Adjusts begin and end for origin and slicing rules, from Icon to Java.
   * Icon has origin 1, while Java has origin 0.
   * Icon slicing mechanics are also different that Java.
   * Returned begin and end will be relative to origin 0,
   * with begin {@literal <=} end.
   * End will now be exclusive, i.e., up to but not including last element.
   * Returns: begin, and updates end if slice.
   * If adjusted begin >= size, sets to -1.
   * If adjusted end > size, sets to -1.
   */
  public static int adjustSlice (int b, int[] eUpdate, int size, int origin,
		boolean isSlice) {
	int e = ((eUpdate != null) && (eUpdate.length > 0)) ? eUpdate[0] : -1;
	if (! isSlice) {
	    if (origin != 0) {
	    	if (b > 0) { b--; }		// Shift origin to 0
	    }
	    if ((b < 0) && (size >= 0)) { b += size; }	// Index from end
	    e = b+1;
	} else {
	    if (origin == 0) {		// Java slicing rules
		e++;			    // Slice up to but not including end
	    } else {			// Icon slicing rules
	      if (size >= 0) {
		if (b == 0) { b = size;
		} else if (b > 0) { b--; }	// Shift origin to 0

		if (e == 0) { e = size;
		} else if (e > 0) { e--; }	// Shift origin to 0

		if (b < 0) { b += size; }	// Index from end
		if (e < 0) { e += size; }
	      } else {	// Best guess
		if (e > 0) { e--; }		// Shift origin to 0
		if (b > 0) { b--; }		// Shift origin to 0
	      }
	    }
	    if (b > e) {		// Swap since Icon ignores reversal
		  int swap = b;
		  b = e;
		  e = swap;
	    }	
	    // INVARIANT: b..<e  where b<=e
	}

	// Validate begin and end against size: begin < size, end <= size.
	if (size >= 0) {
	   if (b >= size) { b = -1; }
	   if (e > size) { e = -1; }
	}

	// return new int[] {b, e};
	if ((eUpdate != null) && (eUpdate.length > 0)) { eUpdate[0] = e; }
	return b;
  }

  /**
   * Converts Icon string position to Java string position within subject.
   * Offsets result by IconNumber.indexOrigin().
   * @return Java string position, or -1 if outside string boundary.
   */
  public static int convertStringPosition (String subject, int pos,
		int origin) {
	if (subject == null) { return -1; }
	return adjustSlice(pos, null, subject.length(), origin, false);
  }
 
  //=========================================================================
  // List utilities.
  //=========================================================================
  /**
   * Convert charSequence to list.
   */
  public static ArrayList<String> stringToList (CharSequence seq) {
	if (seq == null) { return null; }
	ArrayList<String> chars = new ArrayList<String>(seq.length());
	for (int i=0; i<seq.length(); i++) {
		chars.add(String.valueOf(seq.charAt(i)));
	}
	return chars;
  }

  /**
   * Convert list of characters to string.
   */
  public static String listToString (List<String> chars) {
	if (chars == null) { return null; }
	StringBuilder sb = new StringBuilder(chars.size());
	for (String c : chars) { sb.append(c); }
	return sb.toString();
  }

}

//====
// * For index origin 1 and Icon slicing rules,
// * we assume the closure uses the Groovy ..< half open range operator
// * that does not include the last value.
//====

//==== Direct IconField access
// import java.lang.reflect.Field;
// // Get declared public non-synthetic fields
// List<Field> fields = IconField.objectAsList(obj);
// if (fields != null) { size = fields.size(); }
// if (begin < size) {
// String name = fields.get(begin).getName();
//==== Like Promote.java
// List<String> fieldNames = IconField.objectAsNames(obj);
// if (fieldNames != null) { size = fieldNames.size(); }
// if (begin < size) {
//	return new IconField<T>(obj, fieldNames.get(begin)); // create()
// }
//===

//==== END OF FILE
