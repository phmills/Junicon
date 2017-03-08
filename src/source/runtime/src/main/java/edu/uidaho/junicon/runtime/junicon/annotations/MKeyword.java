//========================================================================
// Copyright (c) 2014 Orielle, LLC.  
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
package edu.uidaho.junicon.runtime.junicon.annotations;

import edu.uidaho.junicon.runtime.junicon.annotations.Over.OverTypes;

import java.lang.annotation.*;

/**
 * Annotation interface for &amp;-keywords.
 * Bound to the field holding the keyword.
 *
 * @author Peter Mills
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MKeyword {
  /**
   * Name of keyword, without ampersand.
   */
  String name();

  /** 
   * Type of keyword.
   */
  OverTypes over() default OverTypes.VALUE;
}

//====
// * Name of keyword.
// String name();
//====

//==== END OF FILE
