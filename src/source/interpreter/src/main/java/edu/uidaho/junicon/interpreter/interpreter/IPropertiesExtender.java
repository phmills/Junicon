//========================================================================
// Copyright (c) 2011 Orielle, LLC.  
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
package edu.uidaho.junicon.interpreter.interpreter;

import java.util.Properties;
import java.util.Map;
import java.util.Collection;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
  * IPropertiesExtender extends Properties
  * with setters for changeable delegation of its defaultProperties,
  * as well as load() and store() methods that operate on strings.
  *
  * @author Peter Mills
  */
public interface IPropertiesExtender {		// extends Map

  //====================================================================
  // Setters for static defaults.
  //	The setters are non-static for Spring dependency injection.
  //====================================================================

  /**
   * Get default defaultProperties to be used unless overriden.
   */
  public Properties getDefaultDefaultProperties ();

  /**
   * Set default defaultProperties to be used unless overriden.
   */
  public void setDefaultDefaultProperties (Properties defaultProperties);

  //====================================================================
  // Interface for Properties
  //====================================================================

  /**
   * Gets the value for the specified key in the property list.
   */
  public String getProperty (String key);

  /**
   * Gets the value for the specified key in the property list.
   * If not found the default value is returned.
   */
  public String getProperty (String key, String defaultValue);

  /**
   * Sets the value for the specified key in the property list.
   */
  public Object setProperty (String key, String value);

  //====================================================================
  // Extensions to Properties
  //====================================================================

  /**
   * Get underlying properties, from this owner or the delegate if set.
   * If delegating to another extender, gets its current Properties.
   * Always returns non-null.
   */
  public Properties getProperties ();

  /**
   * Delegate properties to another Properties.
   * Ignored if properties is null.
   */
  public void setProperties (Properties properties);

  /**
   * Delegate properties to the given IPropertiesExtender.
   * Methods will then use the properties for the given extender.
   * Takes priority over a delegate Properties.
   * Ignored if extender is null.
   */
  public void setPropertiesDelegate (IPropertiesExtender extender);

  /**
   * Get properties delegate.
   */
  public IPropertiesExtender getPropertiesDelegate ();

  /**
   * Get defaultProperties, for this or the delegate if set.
   * If a delegate is set to a Properties instead of an Extender, returns null,
   * as this is not allowed.
   */
  public Properties getDefaultProperties ();

  /**
   * Set defaultProperties, for this or the delegate if set.
   * If a delegate is set to a Properties instead of an Extender,
   * this is not allowed.
   */
  public void setDefaultProperties (Properties defaultProperties);

  /**
   * Set defaultProperties using parent IPropertiesExtender's properties.
   * Ignored if parent is null.
   */
  public void setDefaultProperties (IPropertiesExtender parent);

  /**
   * Set defaultProperties to System.Properties.
   */
  public void setDefaultPropertiesToSystem (boolean useSystemProperties);

  /**
   * Add entries to properties map.
   */
  public void setAddProperties (Properties additions);

  /**
   * Clear properties map.  Does not alter defaultProperties.
   */
  public void clearProperties ();

  /**
   * Gets set of property names (domain for map), including default properties.
   */
  public Collection<String> getPropertyNames ();

  /**
   * Returns this property list as a string
   * in a format suitable for loading using load().
   */
  public String storeProperties () throws IOException;

  /**
   * Load the properties from a string representing a properties file.
   * @param isWindowsIni
   *		Is Windows and not Java properties file, so
   *		backslashes need to be escaped.
   */
  public void loadProperties ( String propertiesText,
		boolean isWindowsIni) throws IOException;

}

//==== END OF FILE
