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

/**
  * ICatalog provides a catalog of namespaces,
  * where a namespace is set of name-value pairs.
  * In other words, a catalog provides a map from names to namespaces,
  * i.e., a named set of property maps.
  * <P>
  * Scoped annotations of the form 
  * <PRE>
  *    {@literal @}&lt;prefix:name attr="value" ... &gt;
  *    {@literal @}&lt;prefix:name attr="value" ... /&gt;
  *    {@literal @}&lt;/prefix:name&gt;
  * </PRE>
  * Commented scoped annotations that begin with #@&lt;
  * are also allowed, and are treated the same way as scoped annotations.
  * <P>
  * Annotations are represented using the keys "name.attr"
  * in a given properties map.
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
  * Internally the set of property maps could equivalently be reduced
  * to a single property
  * by dot-concatenating the namespace and property, i.e., "namespace.name.attr"
  * ICatalog would then provide a namespace view of a single property map.
  *
  * @author Peter Mills
  */
public interface ICatalog {

  /**
   * Sees if the key is in the named property map.
   */
  public boolean hasProperty (String namespace, String name);

  /**
   * Sees if the key in the named property map has the given value.
   */
  public boolean isProperty (String namespace, String name, String value);

  /**
   * Gets the value for the specified key in the named property map.
   * Returns empty string if not found or null.
   * We use empty instead of null to allow this methods use in XSLT calls.
   */
  public String getProperty (String namespace, String name);

  /**
   * Gets the value for the specified key in the named property map.
   * If not found, returns the default.
   * Returns empty string if not found and default is null.
   */
  public String getProperty (String namespace, String name, String defaultValue);

  /**
   * Sets the property in the named property map.
   * Returns the previous value of key, or empty if it did not have one.
   * Null values are not allowed.
   */
  public String setProperty (String namespace, String name, String value);

  /**
   * Removes the property from the named property map.
   */
  public void removeProperty (String namespace, String name);

  /**
   * Sets the named property map to that specified.
   */
  public void setProperties (String namespace, Properties properties);

  /**
   * Gets the named property map.
   */
  public Properties getProperties (String namespace);

  /**
   * Gets the catalog of named property maps.
   */
  public Map<String, Properties> getCatalog ();

  /**
   * Sets the catalog of named property maps.
   */
  public void setCatalog (Map<String, Properties> catalog);

}

//==== END OF FILE
