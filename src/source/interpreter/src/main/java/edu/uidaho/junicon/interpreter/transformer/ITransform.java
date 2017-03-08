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
package edu.uidaho.junicon.interpreter.transformer;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;

import javax.xml.transform.TransformerException;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

/**
 * The ITransform interface supports building and applying transform chains.
 * Transform chains will return the original node if there are
 * no transforms to apply, i.e., have idempotent behavior.
 * The transform() and deconstruct() methods are thread-safe.
 * The transform building, transformDocument(), and transformNode() methods
 * are not thread-safe.
 * The base URL to resolve relative imports and includes is set from the
 * filename, or may be explicitly set by setBaseUrl() prior to adding a
 * transform.
 *
 * @author Peter Mills
 */
public interface ITransform {

  //====================================================================
  // Setters for static defaults.
  // The setters are non-static for Spring dependency injection.
  //====================================================================

  /**
   * Gets default flag for XSLTC bytecode compiling transforms.
   */
  public boolean getDefaultCompileTransforms ();

  /**
   * Sets default flag for XSLTC bytecode compiling transforms.
   * The setters are non-static for Spring dependency injection.
   */
  public void setDefaultCompileTransforms (boolean doDefaultCompileTransforms);

  /**
   * Gets default baseURL, used to resolve includes.
   */
  public String getDefaultBaseURL ();

  /**
   * Sets default baseURL from filename, used to resolve includes.
   * The setters are non-static for Spring dependency injection.
   */
  public void setDefaultBaseURL (String filename);

  //====================================================================
  // Setters for dependency injection.
  //====================================================================

  //==========================================================================
  // setBaseURL()
  //==========================================================================

  /**
   * Set the base URL for any following added transforms,
   * used to resolve relative imports and includes.
   * If null, default for files is: getCanonicalFile().toURI().toURL().
   */
  public void setBaseURL (String baseURL);
    
  /**
   * Set the base URL from a filename,
   * used to resolve relative imports and includes.
   * Sets to File(filename).getCanonicalFile().toURI().toURL().
   */
  public void setBaseURLFromFilename (String filename);
    
  /**
   * Returns the base URL for added transforms.
   */
  public String getBaseURL ();
    
  /**
   * Returns the base URL for the most recently added transform.
   */
  public String getLastBaseURL ();
    
  //==========================================================================
  // addTransform()
  //==========================================================================

  /**
   * Add transform to chain from stylesheet in document.
   */
  public void addTransform (Document stylesheet_dom) throws TransformerException;

  /**
   * Add transform to chain from stylesheet in string.
   */
  public void addTransform (String stylesheet_str) throws TransformerException;

  /**
   * Add transform to chain from stylesheet in file.
   */
  public void addTransformFile (String stylesheet_filename)
		throws TransformerException;

  /**
   * Add transform to chain.
   */
  public void addTransform (javax.xml.transform.Transformer transformer);

  /**
   * Add transforms to chain from list of string-based stylesheets.
   */
  public void addTransforms (List<String> stylesheets) throws TransformerException;

  //==========================================================================
  // deconstruct()
  //==========================================================================

  /**
   * Apply transform chain to document with output to string.
   * Not thread-safe.
   * @return <code>transformed document string</code> or null if
   * there are no transforms to apply.
   */
  public String deconstructDocument (Document xml_dom)
		throws TransformerException;

  /**
   * Apply deconstruct chain to document with output to string.
   * @return <code>transformed document string</code> or null if
   * there are no transforms to apply.
   */
  public String deconstruct (Document xml_dom)
		throws TransformerException;
  
  /**
   * Apply deconstruct chain to document with output to string.
   * @return <code>transformed document string</code> or null if
   * there are no transforms to apply.
   */
  public String deconstructFile (String fileName)
		throws TransformerException;
  
  /**
   * Apply deconstruct chain to document with output to string.
   * @return <code>transformed document string</code> or null if
   * there are no transforms to apply.
   */
  public String deconstruct (Reader reader) 
		throws TransformerException;

  //==========================================================================
  // transform()
  //==========================================================================
	
  /**
   * Apply transform chain to document.
   * Not thread-safe.
   * @return <code>transformed document</code> or original input document if
   * there are no transforms to apply.
   */
  public Document transformDocument (Document xml_dom)
		throws TransformerException;

  /**
   * Apply transform chain to document node.
   * Not thread-safe.
   * @return <code>transformed document</code> or original input document if
   * there are no transforms to apply.
   */
  public Document transformNode (Node xml_dom)
		throws TransformerException;
   
  /**
   * Apply transform chain to document.
   * @return <code>transformed document</code> or original input document if
   * there are no transforms to apply.
   */
  public Document transform (Document xml_dom)
		throws TransformerException;
    
  /**
   * Apply transform chain to document node.
   * @return <code>transformed document</code> or original input document if
   * there are no transforms to apply.
   */
  public Document transform (Node xml_dom)
		throws TransformerException;
		
  /**
   * Apply transform chain to document string.
   * @return <code>transformed document</code> or original input document if
   * there are no transforms to apply.
   */
  public Document transform (String xml) throws TransformerException;
  
  /**
   * Apply transform chain to document in file.
   * @return <code>transformed document</code> or original input document if
   * there are no transforms to apply.
   */
  public Document transformFile (String fileName)
		throws TransformerException;
  
  /**
   * Apply transform chain to document in reader.
   * @return <code>transformed document</code> or original input document if
   * there are no transforms to apply.
   */
  public Document transform (Reader reader) 
		throws TransformerException;
    
  /**
   * Apply transform to document using stylesheet string.
   * @return <code>transformed document</code> or original input document if
   * there are no transforms to apply.
   */
  public Document transform (Document xml_dom, String stylesheet)
		throws TransformerException;
    
  /**
   * Apply transform to document using stylesheet document.
   * @return <code>transformed document</code> or original input document if
   * there are no transforms to apply.
   */
  public Document transform (Document xml_dom,
		Document stylesheet_dom) throws TransformerException;
    
  //==========================================================================
  // setTransform()
  //==========================================================================
    
  /**
   * Create singleton transform chain from stylesheet in string.
   */
  public void setTransform (String stylesheet) throws TransformerException;

  /**
   * Create singleton transform chain from stylesheet in document.
   */
  public void setTransform (Document stylesheet_dom) throws TransformerException;
  
  /**
   * Create singleton transform chain from stylesheet in file.
   */
  public void setTransformFile (String stylesheet_filename)
		throws TransformerException;
  
  /**
   * Create transform chain from string stylesheets.
   */
  public void setTransforms (List<String> stylesheets) throws TransformerException;

  /**
   * Clear transform chain.
   */
  public void clear ();
  
  //==========================================================================
  // getTransforms()
  //==========================================================================

  /**
   * Returns transform chain.
   */
  public List getTransforms ();

  /**
   * Returns transform chain element.
   */
  public javax.xml.transform.Transformer getLastTransform (int index);

  /**
   * Returns transform chain element, offset from end of chain.
   */
  public javax.xml.transform.Transformer getTransform (int index);

  //==========================================================================
  // OutputProperty and Parameters
  //==========================================================================
    
  /**
   * Get outputproperty for transform chain element at specified index.
   */
  public String getOutputProperty (int index, String name)
		throws TransformerException;
    
  /**
   * Set outputproperty for transform chain element at specified index.
   */
  public void setOutputProperty (int index, String name, String value)
		throws TransformerException;
    
  /**
   * Get parameter for transform chain element at specified index.
   */
  public Object getParameter (int index, String name)
		throws TransformerException;

  /**
   * Set parameter for transform chain element at specified index.
   */
  public void setParameter (int index, String name, Object value)
		throws TransformerException;
     
  /**
   * Set parameter for transform chain element, offset from end of chain.
   */
  public void setLastParameter (int index, String name, Object value)
		throws TransformerException;
     
  /**
   * Set parameter for first transform chain element.
   */
  public void setFirstParameter (String name, Object value)
		throws TransformerException;
     
  /**
   * Set parameter for last transform chain element.
   */
  public void setLastParameter (String name, Object value)
		throws TransformerException;
     
  //==========================================================================
  // doCompile flags
  //==========================================================================

  /**
   * Set transform XSLTC bytecode compile flag.
   */
  public void setCompileTransforms (boolean doCompileTransforms);

  /**
   * Get transform XSLTC bytecode compile flag.
   */
  public boolean getCompileTransforms ();

}

//==== END OF FILE
