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
package edu.uidaho.junicon.support.transforms;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

/**
 * Supporting methods used by XSLT transforms.
 * This class provides supporting Java methods invoked from XSLT
 * to transform Unicon goal-directed expressions into Groovy.
 * <P>
 * The static methods used by XSLT
 * delegate to the current thread's unique instance.
 * Delegation uses an inheritable thread local,
 * or a resolver if specified.
 * <P>
 * This package is standalone and has no dependencies on other packages.
 * This allows the transforms and their supporting Java static methods
 * to be used independently from the transformational interpreter.
 *
 * @author Peter Mills
 */
public class TransformSupport extends Catalog
		implements IThreadResolver, IThreadResource {
	
  // Setters for dependency injection

  // List of declared and referenced variables.
  // Used to get unique temporary local variable names.
  private Map<String,String> uniqueList = new HashMap<String,String>();
  private Map<String,String> nameToUnique = new HashMap<String,String>();
  private int uniqueCount = 0;
  private String uniqueSeparator = "_";

  // Used to record top-level global variables
  private Map<String,String> globalList = new HashMap<String,String>();

  // Used to record top-level class names
  private Map<String,String> classList = new HashMap<String,String>();

  //======================================================================
  // Constructors
  //======================================================================

  /**
   * Constructor.
   */
  public TransformSupport () {
  }

  //==========================================================================
  // Get unique temporary local variable name
  //==========================================================================

  /**
   * Reset list of declared and referenced variables.
   * @return empty string.
   */
  public String resetUniqueList (String dummy) {
	uniqueList.clear();
	nameToUnique.clear();
	uniqueCount = 0;
	return "";
  }

  /**
   * Add name to list of declared and referenced variables.
   * @return empty string.
   */
  public String addUniqueName (String name) {
	if ((name == null) || name.isEmpty()) { return null; };
	uniqueList.put(name, "");
	return "";
  }

  /**
   * Add name and its uniqueified value to the memorized history.
   * Only retains most recent uniqueified value.
   * @return uniqueified name
   */
  private String addNameToUnique (String name, String unique) {
	if ((name == null) || name.isEmpty() || (unique == null)) {
		return null;
	}
	nameToUnique.put(name, unique);
	return unique;
  }

  /**
   * Derive unique temporary variable name.
   * Uniqueify variable name by appending and incrementing count until
   * it is unique within the namespace.
   * @param name string to prepend to uniqueified variable name.
   *	Default is "x".
   * @return unique variable name of the form "prefix_count", e.g., x_1.
   */
  public String getNewUnique (String name) {
	return getNewUnique(name, null, false, true);
  }

  /**
   * Derive minimally unique temporary variable name.
   * Omits suffix of "_n" if already unique.
   * @param name string to prepend to uniqueified variable name.
   *	Default is "x".
   * @return unique variable name of the form "prefix_count", e.g., x_1.
   */
  public String getMinimalUnique (String name) {
	return getNewUnique(name, null, true, true);
  }

  /**
   * Derive unique temporary variable name using name+append.
   * Omits suffix of "_n" if already unique.
   * @param name string to prepend to uniqueified variable name.
   * @return unique variable name
   */
  public String getMinimalUnique (String name, String append) {
	if ((name != null) && (append != null)) {
		name = name + append;
	}
	return getMinimalUnique(name);
  }

  /**
   * Derive same unique name for given name, as that last derived
   * using getSameUnique.
   * Uses the concatenation of name+append for the name.
   * If not previously made unique, generates minimal unique name.
   * Omits suffix of "_n" if already unique.
   */
  public String getSameUnique (String name, String append) {
	return getSameUnique(name, append, null, null);
  }

  /**
   * Derive same unique name for given name, as that last derived
   * using getSameUnique.
   * Uses the concatenation of name+append for the name used in uniqueification,
   * and uses a map key of name+"append.mapClass.mapMethod"
   * to record the unique entry for the given name.
   * If found in the map, uses the mapped value for the same unique name;
   * otherwise, if not previously made unique, generates a minimal unique name,
   * and records it in the map using the above key.
   * Omits suffix of "_n" if already unique.
   */
  public String getSameUnique (String name, String append, String mapClass,
		String mapMethod) {
	if (name == null) { name = ""; }
	if (append != null) { name = name + append; }
	if ((mapClass == null) || mapClass.isEmpty()) { mapClass = "";
	} else { mapClass = "." + mapClass; }
	if ((mapMethod == null) || mapMethod.isEmpty()) { mapMethod = "";
	} else { mapMethod = "." + mapMethod; }
	String mapSuffix = mapClass + mapMethod;
	String unique = nameToUnique.get(name + mapSuffix);
	if (unique != null) { return unique;  }
	//====
	// WATCH OUT: getSameUnique may not return same value, if intervening
	// unique reset the map.  So only getSameUnique gets to reset the map.
	//====
	unique = getNewUnique(name, null, true, true);
	addNameToUnique(name + mapSuffix, unique);
	return unique;
  }

  /**
   * Derive minimally unique temporary variable name, that is reusable.
   * Will not enter derived name into the list of names not to collide with.
   * Intended to be used within a known fixed scope, e.g., lambda expression.
   */
  public String getReusableUnique (String name) {
	return getNewUnique(name, null, true, false);
  }

  /**
   * Derive unique temporary variable name.
   * Uniqueify variable name in purely formal way by
   * appending "_count" to name until it is unique within the namespace.
   * @param name string to prefix to uniqueified variable name. Default is "".
   * @param separator string to append to name.  Default is "_".
   * @param omitSuffixIfPossible omit _count suffix if name is already unique.
   * @param doNotReuse if adds derived name to list of names not to reuse.
   * @return unique variable name of the form "prefix_count", e.g., x_1.
   */
  public String getNewUnique (String name, String separator,
		boolean omitSuffixIfPossible, boolean doNotReuse) {
		// boolean addToNameMap
	// Uniqueify variable name in purely formal way.
	//	Variants: (count++) || (countStr + "1") || (current + "_")
	if (name == null) { name = ""; }
	if (separator == null) { separator = uniqueSeparator; }
	String unique = name + separator + Integer.toString(uniqueCount);
	if (omitSuffixIfPossible) { unique = name; }
	while (uniqueList.containsKey(unique)) {
		uniqueCount++;
		unique = name + separator + Integer.toString(uniqueCount);
	}
	if (doNotReuse) { uniqueList.put(unique, ""); }
	// if (addToNameMap) { addNameToUnique(name, unique); }
	return unique;
  }

  //==========================================================================
  // Record top-level global variable names.
  //==========================================================================
  /**
   * Reset list of global variables.
   * @return empty string.
   */
  public String resetGlobals (String dummy) {
	globalList.clear();
	return "";
  }

  /**
   * Add name to list of global variables.
   * @return empty string.
   */
  public String addGlobal (String name) {
	if ((name == null) || name.isEmpty()) { return null; };
	globalList.put(name, "");
	return "";
  }

  /**
   * See if global is declared.
   */
  public boolean hasGlobal (String name) {
	if ((name == null) || name.isEmpty()) { return false; };
	return globalList.containsKey(name);
  }

  //==========================================================================
  // Record top-level class names.
  //==========================================================================
  /**
   * Reset list of classes.
   * @return empty string.
   */
  public String resetClasses (String dummy) {
	classList.clear();
	return "";
  }

  /**
   * Add name to list of classes.
   * @return empty string.
   */
  public String addClass (String name) {
	if ((name == null) || name.isEmpty()) { return null; };
	classList.put(name, "");
	return "";
  }

  /**
   * See if class is declared.
   */
  public boolean hasClass (String name) {
	if ((name == null) || name.isEmpty()) { return false; };
	return classList.containsKey(name);
  }

  //==========================================================================
  // Replace in string.
  //==========================================================================

  /**
   * Replace each matched substring with given substitution,
   * for a sequence of match and replace pairs.
   * <P>
   * Example: replace("aac", "a", "b", "c", "d") yields "bbd".
   */
  public String replace (String str, String... replacements) {
	if ((str == null) || (replacements == null)) { return null; }
	for (int i=0; i< replacements.length; i++) {
		String match = replacements[i++];
		String sub = replacements[i];
		if ((match != null) && (sub != null) && (! match.isEmpty())) {
			str = str.replace(match,sub);
		}
	}
	return str;
  }

  //====================================================================
  //====================================================================
  // Static cut-throughs for XSLT transforms.
  //====================================================================
  //====================================================================

  //==========================================================================
  // Get unique temporary name
  //==========================================================================

  /**
   * Reset list of declared and referenced variables.
   */
  public static String TresetUniqueList (String dummy) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.resetUniqueList(dummy);
  }

  /**
   * Add name to list of declared and referenced variables.
   */
  public static String TaddUniqueName (String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.addUniqueName(name);
  }

  /**
   * Derive unique temporary variable name,
   * by adding an increasing suffix until it is unique within the namespace.
   */
  public static String TgetNewUnique (String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.getNewUnique(name);
  }

  /**
   * Derive unique temporary variable name,
   * omitting the suffix if already unique.
   */
  public static String TgetMinimalUnique (String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.getMinimalUnique(name);
  }

  /**
   * Derive unique temporary variable for name+append,
   * omitting the suffix if already unique.
   */
  public static String TgetMinimalUnique (String name, String append) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.getMinimalUnique(name, append);
  }

  /**
   * Derive same unique name for name+append, as that last derived.
   */
  public static String TgetSameUnique (String name, String append) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.getSameUnique(name, append);
  }

  /**
   * Derive unique temporary variable name,
   * that is reusable and omits the suffix if possible.
   */
  public static String TgetReusableUnique (String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.getReusableUnique(name);
  }

  //==========================================================================
  // Record top-level global variable names.
  //==========================================================================
  /**
   * Reset list of global variables.
   * @return empty string.
   */
  public static String TresetGlobals (String dummy) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.resetGlobals(dummy);
  }

  /**
   * Add name to list of global variables.
   * @return empty string.
   */
  public static String TaddGlobal (String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.addGlobal(name);
  }

  /**
   * See if global is declared.
   */
  public static boolean ThasGlobal (String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return false; };
	return interp.hasGlobal(name);
  }

  //==========================================================================
  // Record top-level class names.
  //==========================================================================
  /**
   * Reset list of classes.
   * @return empty string.
   */
  public static String TresetClasses (String dummy) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.resetClasses(dummy);
  }

  /**
   * Add name to list of class names.
   * @return empty string.
   */
  public static String TaddClass (String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.addClass(name);
  }

  /**
   * See if class is declared.
   */
  public static boolean ThasClass (String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return false; };
	return interp.hasClass(name);
  }

  //==========================================================================
  // Replace in string.
  //==========================================================================

  /**
   * Replace each matched substring with given substitution,
   * for a sequence of match and replace pairs.
   * <P>
   * Example: replace("aac", "a", "b", "c", "d") yields "bbd".
   */
  public static String TreplaceVarargs (String str, String... replacements) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.replace(str, replacements);
  }

  /**
   * Replace using fixed argument list, for XSLT.
   */
  public static String Treplace (String str, String from1, String to1) {
	return TreplaceVarargs(str, from1, to1);
  }

  public static String Treplace (String str, String from1, String to1,
		String from2, String to2) {
	return TreplaceVarargs(str, from1, to1, from2, to2);
  }

  public static String Treplace (String str, String from1, String to1,
		String from2, String to2, String from3, String to3) {
	return TreplaceVarargs(str, from1, to1, from2, to2, from3, to3);
  }

  public static String Treplace (String str, String from1, String to1,
		String from2, String to2, String from3, String to3,
		String from4, String to4) {
	return TreplaceVarargs(str, from1, to1, from2, to2, from3, to3,
		from4, to4);
  }

  public static String Treplace (String str, String from1, String to1,
		String from2, String to2, String from3, String to3,
		String from4, String to4, String from5, String to5) {
	return TreplaceVarargs(str, from1, to1, from2, to2, from3, to3,
		from4, to4, from5, to5);
  }

  public static String Treplace (String str, String from1, String to1,
		String from2, String to2, String from3, String to3,
		String from4, String to4, String from5, String to5,
		String from6, String to6) {
	return TreplaceVarargs(str, from1, to1, from2, to2, from3, to3,
		from4, to4, from5, to5, from6, to6);
  }

  public static String Treplace (String str, String from1, String to1,
		String from2, String to2, String from3, String to3,
		String from4, String to4, String from5, String to5,
		String from6, String to6, String from7, String to7) {
	return TreplaceVarargs(str, from1, to1, from2, to2, from3, to3,
		from4, to4, from5, to5, from6, to6, from7, to7);
  }

  public static String Treplace (String str, String from1, String to1,
		String from2, String to2, String from3, String to3,
		String from4, String to4, String from5, String to5,
		String from6, String to6, String from7, String to7,
		String from8, String to8) {
	return TreplaceVarargs(str, from1, to1, from2, to2, from3, to3,
		from4, to4, from5, to5, from6, to6, from7, to7,
		from8, to8);
  }

  public static String Treplace (String str, String from1, String to1,
		String from2, String to2, String from3, String to3,
		String from4, String to4, String from5, String to5,
		String from6, String to6, String from7, String to7,
		String from8, String to8, String from9, String to9) {
	return TreplaceVarargs(str, from1, to1, from2, to2, from3, to3,
		from4, to4, from5, to5, from6, to6, from7, to7,
		from8, to8, from9, to9);
  }

  public static String Treplace (String str, String from1, String to1,
		String from2, String to2, String from3, String to3,
		String from4, String to4, String from5, String to5,
		String from6, String to6, String from7, String to7,
		String from8, String to8, String from9, String to9,
		String from10, String to10) {
	return TreplaceVarargs(str, from1, to1, from2, to2, from3, to3,
		from4, to4, from5, to5, from6, to6, from7, to7,
		from8, to8, from9, to9, from10, to10);
  }

  //==========================================================================
  // Catalog.
  //==========================================================================

  /**
   * Sees if the key is in the named property map.
   */
  public boolean ThasProperty (String props, String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return false; };
	return interp.hasProperty(props, name);
  }

  /**
   * Sees if the key in the named property map has the given value.
   */
  public boolean TisProperty (String props, String name, String value) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return false; };
	return interp.isProperty(props, name, value);
  }

  /**
   * Gets the value for the specified key in the named property map.
   * Returns empty string if not found or null.
   */
  public String TgetProperty (String props, String name) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.getProperty(props, name);
  }

  /**
   * Gets the value for the specified key in the named property map.
   * If not found, returns the default.
   * Returns empty string if not found and default is null.
   */
  public String TgetProperty (String props, String name, String defaultValue) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return null; };
	return interp.getProperty(props, name, defaultValue);
  }

  /**
   * Sets the property in the named property map.
   * Returns the previous value of key, or empty if it did not have one.
   */
  public void TsetProperty (String props, String name, String value) {
	TransformSupport interp = (TransformSupport) getCurrentSupport();
	if (interp == null) { return; };
	interp.setProperty(props, name, value);
  }

  //==========================================================================
  // Print.
  //==========================================================================

  /**
   * Print.
   */
  public static String println (String line) {
	if (line == null) { return null; }
	System.out.println(line);
	return null;
  }

  //====================================================================
  // Big literals.
  //====================================================================

  /**
   * Escape quotes in big literal, so it can be placed in a quote.
   */
  public static String escapeLiteralString (String literalString)
  {
	StringBuffer stringbuffer = new StringBuffer();
	int escape = 0;

	for (int i = 0; i < literalString.length(); i++) {
	    char current = literalString.charAt(i);
	    if (current == '\\') {
		escape++;
	    } else if (current == '"' && escape % 2 == 0) {
		stringbuffer.append('\\');
	    } else {
		escape = 0;
	    }

	    stringbuffer.append(current);
	}
	return stringbuffer.toString();
  }

  //==========================================================================
  // Current thread's cut-through
  //==========================================================================

  private static IThreadResolver resolver = null;

  private static final InheritableThreadLocal<TransformSupport> uniqueSupport =
	new InheritableThreadLocal <TransformSupport> () {
		@Override protected TransformSupport initialValue () {
			return new TransformSupport();	
		}
		// @Override protected T childValue (T parent) { };
	};
 
  public IThreadResource getThreadResource () {
	return getCurrentSupport();
  }

  public void setThreadResolver (IThreadResolver threadResolver) {
	resolver = threadResolver;
  }

  public IThreadResolver getThreadResolver () {
	return resolver;
  }

  /**
   * Get the current thread's cut-through.
   * Returns a unique transform cut-through for each thread.
   * Uses an inheritable thread local, or delegates to a resolver if non-null.
   */
  public static TransformSupport getCurrentSupport () {
	if (resolver != null) {
		IThreadResource resource = resolver.getThreadResource();
		if (resource != null) { return (TransformSupport) resource; };
	}
	return uniqueSupport.get();
  }

}

//==== END OF FILE
