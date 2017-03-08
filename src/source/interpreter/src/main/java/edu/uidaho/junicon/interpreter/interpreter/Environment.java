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

import java.util.*;

/**
 * Provides thread-safe methods to set and get environment variables.
 * Delegation of environment to another IEnvironment is supported.
 *
 * @author Peter Mills
 */
public class Environment extends PropertiesExtender
	implements IEnvironment, IPropertiesExtender
{

  // Local environment
  private Object envLock = new Object();
  private Map<String, Object> localEnv = new HashMap<String, Object>();
  private IEnvironment envDelegate = null;

  /**
   * Constructor.
   */
  public Environment () {
	init(null);
  }

  /**
   * Constructor with delegate environment.
   */
  public Environment (IEnvironment delegateEnvironment) {
	super();
	init(delegateEnvironment);
  }

  /**
   * Constructor with delegate environment and default properties.
   */
  public Environment (IEnvironment delegateEnvironment,
 		Properties defaultProperties) {
 	super(defaultProperties);
 	init(delegateEnvironment);
   }

  /**
   * Initializer.
   */
  private void init(IEnvironment delegateEnvironment) {
	envDelegate = delegateEnvironment;
  }

  //======================================================================
  // Environment access with delegation
  //======================================================================

  public Object getEnv (String name) {
	Object retval = null;
	synchronized (envLock) {
		if (envDelegate == null) { retval = localEnv.get(name);
		} else { retval = envDelegate.getEnv(name); };
	};
	return retval;
  }

  public Object getEnv (String name, Object defaultValue) {
	Object retval = getEnv(name);
	if (retval == null) { retval = defaultValue; };
	return retval;
  }

  public void setEnv (String name, Object value) {
	synchronized (envLock) {
		if (envDelegate == null) { localEnv.put(name, value); 
		} else { envDelegate.setEnv(name, value); };
	};
  }

  public String[] getEnvNames () {
	String[] retval = null;
	synchronized (envLock) {
		if (envDelegate == null) {
		   retval = (String[]) localEnv.keySet().toArray(new String[0]);
		} else { retval = envDelegate.getEnvNames(); };
	};
	return retval;
  }

  public void setDelegateEnvironment (IEnvironment delegate) {
	init(delegate);
  }

  public IEnvironment getDelegateEnvironment () {
	return envDelegate;
  }

  public IEnvironment getEnvironment () {
	return this;
  }

}

//==== END OF FILE
