//========================================================================
// Copyright (c) 2013 Orielle, LLC.  
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

// To parse XML in X-annotation
import java.io.StringReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
  * Catalog provides an named set of property maps.
  * In other words, provides a map from names to environments.
  *
  * @author Peter Mills
  */
public class Catalog {

  private Map<String, Properties> catalog = new HashMap<String, Properties>();

  // To parse XML in X-annotation
  private static DocumentBuilderFactory factory;
  private static DocumentBuilder builder;
  static {
	try {
		factory = DocumentBuilderFactory.newInstance();
		builder = factory.newDocumentBuilder();
	} catch (Exception e) { builder = null; }
  }

  //==========================================================================
  // Constructors
  //==========================================================================

  public Catalog () { ; }

  //==========================================================================
  // Catalog of namespaces.
  //==========================================================================

  /**
   * Gets property for given key.  Returns null if not found.
   */
  private String getRawProperty (String namespace, String name) {
	if ((name == null) || name.isEmpty()) { return null; }
	Properties props = getProperties(namespace); 
	if (props == null) { return null; }
	return props.getProperty(name);
  }

  private String getRawProperty (String namespace, String name, String defaultValue) {
	if ((name == null) || name.isEmpty()) { return null; }
	Properties props = getProperties(namespace); 
	if (props == null) { return null; }
	return props.getProperty(name, defaultValue);
  }

  public boolean hasProperty (String namespace, String name) {
	String value = getRawProperty(namespace, name);
	if (value == null) { return false; }
	return true;
  }

  public boolean isProperty (String namespace, String name, String value) {
	if (value == null) { return false; }
	String propValue = getRawProperty(namespace, name);
	if (propValue == null) { return false; }
	return propValue.equals(value);
  }

  public String getProperty (String namespace, String name) {
	String value = getRawProperty(namespace, name);
	if (value == null) { return ""; }
	return value;
  }

  public String getProperty (String namespace, String name, String defaultValue) {
	String value = getRawProperty(namespace, name, defaultValue);
	if (value == null) { return ""; }
	return value;
  }

  public Object setProperty (String namespace, String name, String value) {
	if ((name == null) || name.isEmpty() || (value == null)) {
		return null;
	}
	Properties props = getProperties(namespace); 
	if (props == null) { return null; }
	return props.setProperty(name, value);
  }

  public void removeProperty (String namespace, String name) {
	if ((name == null) || name.isEmpty()) { return; }
	Properties props = getProperties(namespace); 
	if (props == null) { return; }
	props.remove(name);
  }

  public void setProperties (String namespace, Properties properties) {
	if ((catalog == null) || (namespace == null) || namespace.isEmpty()) { return; }
	catalog.put(namespace, properties);
  }

  public Properties getProperties (String namespace) {
	if ((catalog == null) || (namespace == null) || namespace.isEmpty()) {
		return null;
	}
	return catalog.get(namespace);
  }

  public Map<String, Properties> getCatalog () {
	return catalog;
  }

  public void setCatalog (Map<String, Properties> catalog) {
	if (catalog == null) { return; }
	this.catalog = catalog;
  }

  //==========================================================================
  // Scoped annotations
  //==========================================================================

  /**
   * Parse a scoped annotation and add its properties to the given map.
   * A scoped annotation, or X-annotation, is of the form
   * <PRE>
   *	{@literal @}&lt;prefix:name attr="value" ... &gt;
   *	{@literal @}&lt;prefix:name attr="value" ... /&gt;
   *    {@literal @}&lt;/prefix:name&gt;
   * </PRE>
   * Commented scoped annotations that begin with #@&lt;
   * are also allowed, and are treated the same way as scoped annotations.
   * <P>
   * Annotations are represented using the keys "name.attr"
   * in a given properites map.
   * Annotation tags may also have a namespace prefix of 
   * the form "prefix:name".  In this case the prefix is used
   * to look up the properties map from a catalog of namespaces.
   * If no catalog is specified or the prefix is not found,
   * the namespace prefix is ignored,
   * and the given default namespace is used.
   * <P>
   * On set, a scoped annotation for a given "name" will save and then
   * clear all other attributes of the form "name.*". 
   * The saved attributes are restored on an ending tag.
   * <P>
   * We first save "name" and "name.*" into savedProperties from the given
   * properties, after clearing savedProperties of similary named entries.
   * We then clear "name" and "name.*" from the given properties.
   * For each attribute in the annotation,
   * the property "name.attr" is set using its value,
   * as well as the property "name" with empty value.
   * Name can begin with an XML namespace of the form "space:",
   * which is included in the property.
   *
   * @param line annotation
   * @param properties namespace in which to set annotations,
   *	if catalog is not specified or prefix is not found.
   * @param savedProperties namespace in which to save annotations,
   *	if savedCatalog is not specified or prefix is not found.
   *	Ignored if prefix is found in the catalog.
   * @param catalog catalog in which to look up properties map using prefix
   * @param savedCatalog catalog in which to look up savedProperties
   *	Used only if prefix is found in the catalog.
   * @return tag name of the scoped annotation, or null
   * if the line is not a scoped annotation.
   */
  public static String addAnnotation (String line, Properties properties,
		Properties savedProperties,
		Map<String, Properties> catalog,
		Map<String, Properties> savedCatalog) {
	if ((line == null) || ((properties == null) && (catalog == null))) {
		return null;
	}

	// Change X-annotation into XML element
	Element element = parseAnnotation(line, false);
	if (element == null) { return null; }

	// Handle namespace prefix.
	//	TagName = prefix:localName, i.e., qualified name.
	String prefix = element.getPrefix();
	String name = element.getLocalName();
	String qualifiedName = element.getTagName();	// getNodeName()
	if ((name == null) || name.isEmpty()) { return null; }

	Properties namespace = getNamespaceFromPrefix(prefix, catalog);
	Properties savedNamespace = getNamespaceFromPrefix(prefix,savedCatalog);
	if (namespace != null) {
		properties = namespace;
		savedProperties = savedNamespace;
	}

	// Save name and name.* into savedProperties ; first clear it.
	if (savedProperties != null) { 
		clearAnnotations(name, savedProperties);
		copyAnnotations(name, properties, savedProperties);
	}

	// Clear name and name.* in properties
	clearAnnotations(name, properties);

	// Set name and name=attr in properties
	properties.setProperty(name, "");
	try {
	  NamedNodeMap map = element.getAttributes();
	  if (map != null) {
		int len = map.getLength();
		for (int i=0; i<len; i++) {
		  String attr = map.item(i).getNodeName();
		  String value = map.item(i).getNodeValue();
		  if ((attr != null) && (! attr.isEmpty()) &&
				(value != null)) {
			String prop = name + "." + attr;
			properties.setProperty(prop, value);
		  }
		}
	  }
	} catch (Exception e) { return null; }

	return name;
  }

  /**
   * Add annotation to the given properties map.
   * @return tag name of the scoped annotation, or null
   * if the line is not a scoped annotation.
   */
  public static String addAnnotation (String line, Properties properties,
		Properties savedProperties) {
	return addAnnotation(line, properties, savedProperties, null, null);
  }

  /**
   * Parse an ending scoped annotation and
   * remove its properties from the given map,
   * restoring values to those saved.
   * An ending scoped annotation is of the form
   * <PRE>
   *	{@literal @}&lt;/name&gt;
   * </PRE>
   * Commented scoped annotations that begin with #@&lt;
   * are also allowed, and are treated the same way as scoped annotations.
   * <P>
   * Any properties for "name" as well as "name.*" are first removed
   * from the given properties.
   * Then if savedProperties is not null,
   * its properties for "name" as well as "name.*" are restored into
   * the given properties.
   * @return tag name of the scoped annotation, or null
   * if the line is not a scoped annotation.
   */
  public static String resetAnnotation (String line, Properties properties,
		Properties savedProperties,
		Map<String, Properties> catalog,
		Map<String, Properties> savedCatalog) {
	if ((line == null) || ((properties == null) && (catalog == null))) {
		return null;
	}

	// Change X-annotation into XML element
	Element element = parseAnnotation(line, true);
	if (element == null) { return null; }

	// Handle namespace prefix.
	//	TagName = prefix:localName, i.e., qualified name.
	String prefix = element.getPrefix();
	String name = element.getLocalName();
	String qualifiedName = element.getTagName();	// getNodeName()
	if ((name == null) || name.isEmpty()) { return null; }

	Properties namespace = getNamespaceFromPrefix(prefix, catalog);
	Properties savedNamespace = getNamespaceFromPrefix(prefix,savedCatalog);
	if (namespace != null) {
		properties = namespace;
		savedProperties = savedNamespace;
	}

	// Remove name and name.* from properties
	clearAnnotations(name, properties);

	// Restore name and name.* from savedProperties
	copyAnnotations(name, savedProperties, properties);

	return name;
  }

  /**
   * Reset annotation to the saved value for the given properties map.
   * @return tag name of the scoped annotation, or null
   * if the line is not a scoped annotation.
   */
  public static String resetAnnotation (String line, Properties properties,
		Properties savedProperties) {
	return resetAnnotation(line, properties, savedProperties, null, null);
  }

  /**
   * Find properties in catalog for given prefix.
   * If not found, return given default properties.
   */
  private static Properties getNamespaceFromPrefix (String prefix,
		Map<String, Properties> catalog) {
	if (((prefix == null) || prefix.isEmpty()) || (catalog == null)) {
		return null;
	}
	return catalog.get(prefix);
  }
	
  /**
   * Removes any keys of the form "name" or "name.*" from the given properties.
   */
  private static void clearAnnotations (String name, Properties properties) {
	if ((name == null) || name.isEmpty() || (properties == null)) {
		return;
	}
	String prefix = name + ".";
	for (String key : properties.stringPropertyNames()) {	// keySet()
		if ((key != null) && (key.startsWith(prefix)
			|| key.equals(name))) {
		    properties.remove(key);
		}
	}
  }

  /**
   * Copies "name" or "name.*" between the given properties.
   */
  private static void copyAnnotations (String name, Properties from,
		Properties to) {
	if ((name == null) || name.isEmpty() || (from == null)
			|| (to == null)) {
		return;
	}
	String prefix = name + ".";
	for (String key : from.stringPropertyNames()) {		// keySet()
		if ((key != null) && (key.startsWith(prefix)
			|| key.equals(name))) {
		    to.setProperty(key, from.getProperty(key));	// get()
		}
	}
  }

  /**
   * Parse a scoped annotation into an XML element.
   * Parses both beginning and ending annotations of the form:
   * <PRE>
   *	@&lt;name attr="value" ... &gt;
   *	@&lt;name attr="value" ... /&gt;
   *	@&lt;/name&gt;
   * </PRE>
   * Commented scoped annotations that begin with #@&lt;
   * are also allowed, and are treated the same way as scoped annotations.
   * <P>
   * Ignores extraneous characters after the last >, e.g., ;.
   * @param line	scoped annotation to parse
   * @param isClosing	true if is closing tag
   * @return an Element node, or null if parse error.
   */
  private static Element parseAnnotation (String line, boolean isClosing) {
	if (line == null) { return null; }
	line = line.trim();

	// Strip comment character if commented scoped annotation
	if (line.startsWith("#@<")) {
		line = line.substring(1);
	}

	// Ignore extraneous characters after the last >, e.g., ;
	if (line.startsWith("@<") && (! line.endsWith(">"))) {
		int ending = line.lastIndexOf('>');
		if (ending >= 0) { line = line.substring(0, ending+1); }
	}

	// Check if XML compliant
	if (! (line.startsWith("@<") && line.endsWith(">"))) { return null; }
	if (isClosing) {	// weak test
	  if (! (line.startsWith("@</"))) { return null; }
	}

	// Parse X-annotation @<name ...> into XML element <name .../>
	StringBuilder str = new StringBuilder(line);
	str = str.deleteCharAt(0);		// skip over @
	if (! line.endsWith("/>")) {		// replace last > with />
		str.insert(str.length()-1, "/");
	}
	if (line.startsWith("</")) {		// replace first </ with <
		str.deleteCharAt(1);
	}

	// Parse: @<name attr="value" ... >
	if (builder == null) { return null; }
	String name = null;
	Element element;
	try {
	  Document doc = builder.parse(new InputSource(
		new StringReader(str.toString())));
	  NodeList els = doc.getElementsByTagName("*");
	  if (els.getLength() <= 0) { return null; }
	  element = (Element) els.item(0);		// first element
	} catch (Exception e) { return null; };

	return element;
  }

  /**
   * Get text value for element, if it exists.
   */
  private String getTextValue (Element element) {
	if ((element == null) || (! element.hasChildNodes())) { return null; };
	Node node = element.getFirstChild();
	if ((Node.TEXT_NODE != node.getNodeType())) { return null; };
	return element.getFirstChild().getNodeValue();
  }
	
}

//==== END OF FILE
