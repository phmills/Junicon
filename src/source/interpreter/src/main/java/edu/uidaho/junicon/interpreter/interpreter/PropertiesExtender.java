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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * PropertiesExtender extends Properties
 * with setters for changeagble delegation of defaultProperties,
 * as well as load() and store() methods that operate on strings.
 * Like Properties, the constructor allows a default Properties
 * to be specified.
 *
 * @author Peter Mills
 */
public class PropertiesExtender implements IPropertiesExtender {
				// extends Properties

  private PropertiesDefaulter properties = new PropertiesDefaulter();
  private static Properties defaultDefaultProperties = null;
  private IPropertiesExtender parent = null; // used to set default properties
  private IPropertiesExtender extender = null;	// delegate to extender
  private Properties delegate = null;	    // delegate to another Properties

  /**
   * Constructor.  Sets default properties to static default if defined.
   */
  public PropertiesExtender () {
	if (defaultDefaultProperties != null) {
		setDefaultProperties(defaultDefaultProperties);
	}
  }

  /**
   * Constructor with given default properties.
   */
  public PropertiesExtender (Properties defaultProperties) {
	setDefaultProperties(defaultProperties);
  }

  /**
   * Inner class that extends Properties with ability to set defaults.
   */
  public class PropertiesDefaulter extends Properties {
    public PropertiesDefaulter () {
	super();
    }
    public PropertiesDefaulter (Properties defaultProperties) {
	super(defaultProperties);
    }
    public void setDefaultProperties (Properties defaultProperties) {
	defaults = defaultProperties;
    }
    public Properties getDefaultProperties () {
	return defaults;
    }
  }

  //====================================================================
  // Setters for static defaults.
  //	The setters are non-static for Spring dependency injection.
  //====================================================================
 
  public Properties getDefaultDefaultProperties () {
	return defaultDefaultProperties;
  }

  public void setDefaultDefaultProperties (Properties defaultProperties) 
  {
	defaultDefaultProperties = defaultProperties;
  }

  //====================================================================
  // Extensions to Properties.
  //====================================================================

  public Properties getProperties () {
	if (extender != null) { return extender.getProperties(); }
	if (delegate != null) { return delegate; }
	return (Properties) properties;
  }

  public void setProperties (Properties properties) {
	if (properties == null) { return; };
	this.delegate = properties;
  }

  public void setPropertiesDelegate (IPropertiesExtender extender) {
	if (extender == null) { return; };
	this.extender = extender;
  }

  public IPropertiesExtender getPropertiesDelegate () {
	return extender;
  }

  public Properties getDefaultProperties () {
	if (extender != null) { return extender.getDefaultProperties(); }
	if (delegate != null) { return null; }	// Not allowed
	return properties.getDefaultProperties();
  }

  public void setDefaultProperties (Properties defaultProperties) {
	if (extender != null) { 
		extender.setDefaultProperties(defaultProperties);
		return;
	}
	if (delegate != null) { return; }	// Not allowed
	if (defaultProperties == properties) { return; };   // Prevent cycles
	// this.defaultProperties = defaultProperties;
	properties.setDefaultProperties(defaultProperties);
  }

  public void setDefaultProperties (IPropertiesExtender parent) {
	if (parent == null) { return; }
	this.parent = parent;
	setDefaultProperties(parent.getProperties());
  }

  public void setDefaultPropertiesToSystem (boolean useSystemProperties) {
	setDefaultProperties(System.getProperties());
  }

  public void setAddProperties (Properties additions) { 
	if (additions == null) { return; };
	Properties properties = getProperties();
	properties.putAll(additions);
  }

  public void clearProperties () {
	Properties properties = getProperties();
	properties.clear();
  }

  public Collection<String> getPropertyNames () {
	Properties properties = getProperties();
	return properties.stringPropertyNames();
  }

  //====================================================================
  // Properties methods.
  //====================================================================

  public String getProperty (String key) {
	Properties properties = getProperties();
	return properties.getProperty(key);
  }

  public String getProperty (String key, String defaultValue) {
	Properties properties = getProperties();
  	return properties.getProperty(key, defaultValue);
  }

  public Object setProperty (String key, String value) {
	Properties properties = getProperties();
	return properties.setProperty(key, value);
  }

  //====================================================================
  // Load() and store() properties as string.
  //====================================================================

  public String storeProperties () throws IOException {
	Properties properties = getProperties();
	return storePropertiesAsString(properties);
  }

  public void loadProperties ( String propertiesText,
		boolean isWindowsIni) throws IOException {
	Properties properties = getProperties();
	loadPropertiesFromString(properties,
		propertiesText, isWindowsIni);
  }

  //====================================================================
  // Static versions of load() and store() methods.
  //====================================================================

  /**
   * Returns the given property list as a string
   * in a format suitable for loading using load().
   */
  public static String storePropertiesAsString (Properties properties)
		throws IOException {
	if (properties == null) { return null; };
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
		properties.store(out, null);
	} catch (IOException e) {
		throw e;
		// throw new IOException(e);
	};
	return out.toString();
  }

  /**
   * Load the properties from a string representing a properties file.
   * @param properties
   *		The properties to load into.
   *		If null will create a new Properties.
   * @param propertiesText
   *		Properties as string from store().
   * @param isWindowsIni
   *		Is Windows and not Java properties file, so
   *		backslashes need to be escaped.
   * @return <code>loaded properties</code>
   */
  public static Properties loadPropertiesFromString (Properties properties,
		String propertiesText, boolean isWindowsIni)
		throws IOException {
	if (properties == null) {
		properties = new Properties();
	};
	if (propertiesText == null) { return properties; };
	if (isWindowsIni) {	// Escape backslash with an extra '\'
		propertiesText = propertiesText.replaceAll("\\\\", "\\\\\\\\");
	};
	try {
		properties.load(new ByteArrayInputStream(
			propertiesText.getBytes()));
	} catch (IOException e) {
		throw e;
		// throw new IOException(e);
	};
	return properties;
  }

}

//==== END OF FILE
