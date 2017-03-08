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
 * Interface for setting transforms for
 * default transformational interpreter.
 *
 * @author Peter Mills
 */
public interface ITransformInterpreter {

  //======================================================================
  // Transform setters.
  //======================================================================

  /**
   * Set normalize transform.
   */
  public void setNormalizeTransform (String transform);

  /**
   * Get normalize transform.
   */
  public String getNormalizeTransform ();

  /**
   * Set main code transform.
   */
  public void setCodeTransform (String transform);

  /**
   * Get main code transform.
   */
  public String getCodeTransform ();

  /**
   * Set deconstruct transform as first stage in taking code to text,
   * with output still in XML.
   */
  public void setDeconstructTransform (String transform);

  /**
   * Get deconstruct transform as first stage in taking code to text,
   * with output still in XML.
   */
  public String getDeconstructTransform ();

  /**
   * Set format transform to take code to text.
   */
  public void setFormatTransform (String transform);

  /**
   * Get format transform to take code to text.
   */
  public String getFormatTransform ();

  /**
   * Set correlate transform to format code as XML for error correlation.
   */
  public void setCorrelateFormatTransform (String transform);

  /**
   * Get correlate transform to format code as XML for error correlation.
   */
  public String getCorrelateFormatTransform ();

  /**
   * Set normalize formatting transform for tracing.
   */
  public void setNormalizeFormatTransform (String transform);

  /**
   * Get normalize formatting transform for tracing.
   */
  public String getNormalizeFormatTransform ();

  /**
   * Set export transform for artifact generation.
   */
  public void setExportTransform (String transform);

  /**
   * Get export transform for artifact generation.
   */
  public String getExportTransform ();

  //======================================================================
  // Correlate setters.
  //======================================================================

  /**
   * Gets XML concrete syntax node names used in correlating source.
   */
  public String[] getConcreteSyntaxNodes ();

  /**
   * Sets XML concrete syntax node names used in correlating source.
   * Default is: WORD, OPERATOR, DELIMITER, IDENTIFIER, QUOTE.
   */
  public void setConcreteSyntaxNodes (String[] nodes);

}

//==== END OF FILE
