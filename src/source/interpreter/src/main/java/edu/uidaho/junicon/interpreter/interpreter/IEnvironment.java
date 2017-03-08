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

/**
  * Interface for accessing environment variables.
  * An implementation should make these methods thread-safe.
  *
  * @author Peter Mills
  */
public interface IEnvironment { 	// extends IPropertiesLoader

  /**
   * Get the value of an environment variable.
   */
  public Object getEnv (String name); 

  /**
   * Get the value of an environment variable.
   * If not found the default value is returned.
   */
  public Object getEnv (String name, Object defaultValue);

  /**
   * Set the value of an environment variable.
   */
  public void setEnv (String name, Object value);

  /**
   * Gets the set of environment variable names.
   */
  public String[] getEnvNames ();

  /**
   * Get the environment.
   */
  public IEnvironment getEnvironment ();

  /**
   * Set the delegated environment.
   */
  public void setDelegateEnvironment (IEnvironment delegate);

  /**
   * Get the delegated environment.
   * <br>
   * If delegation occurs, the getEnv, setEnv, and getEnvNames methods 
   * use only the delegated environment and not this environment.
   * This is unlike Properties, which searches defaultProperties only if 
   * local search fails.
   */
  public IEnvironment getDelegateEnvironment ();

}

//==== END OF FILE
