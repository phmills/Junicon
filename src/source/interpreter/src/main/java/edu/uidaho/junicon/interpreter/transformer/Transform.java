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
 * The Transform class supports building and applying transform chains.
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
public class Transform implements ITransform {

    private TransformerFactory tfactory = null;
    private DocumentBuilderFactory dFactory = null;
    private DocumentBuilder dBuilder = null;
    private ArrayList list = new ArrayList();

    // static defaults
    static boolean defaultCompileTransforms = false;
    static String defaultBaseURL = null;

    private boolean doCompile = defaultCompileTransforms;
    private String baseURL = defaultBaseURL;
    private String lastBaseURL = defaultBaseURL;

  //==========================================================================
  // Constructors
  //==========================================================================

  /**
   * Create empty transform.
   */
  public Transform () throws TransformerException {
	initialize();
  }
        
  /**
   * Create empty transform.
   */
  public Transform (boolean compileTransforms) throws TransformerException {
	initialize();
	setCompileTransforms(compileTransforms);
  }
        
  /**
   * Create singleton transform chain from stylesheet in document.
   */
  public Transform (Document stylesheet_dom) throws TransformerException {
	initialize();
        addTransform(stylesheet_dom);
  }

  /**
   * Create singleton transform chain from string-based stylesheet.
   */
  public Transform (String stylesheet) throws TransformerException {
	initialize();
        addTransform(stylesheet);
  }

  /**
   * Create singleton transform chain from string-based stylesheet.
   */
  public Transform (String stylesheet, boolean compileTransforms)
		throws TransformerException {
	initialize();
	setCompileTransforms(compileTransforms);
        addTransform(stylesheet);
  }

  /**
   * Create transform chain from string-based stylesheets.
   */
  public Transform (List<String> stylesheets) throws TransformerException {
	initialize();
        addTransforms(stylesheets);
  }

  /**
   * Create transform chain from string-based stylesheets.
   */
  public Transform (List<String> stylesheets, boolean compileTransforms)
		throws TransformerException {
	initialize();
	setCompileTransforms(compileTransforms);
        addTransforms(stylesheets);
  }

  /**
   * Initializer.
   */
  private void initialize () throws TransformerException {
	try {
		tfactory = TransformerFactory.newInstance();
        } catch (TransformerFactoryConfigurationError e) {
		throw new TransformerException(e);
	};
	try {
		dFactory = DocumentBuilderFactory.newInstance();
		dBuilder = dFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
		// Parser with specified options can't be built
		throw new TransformerException(e);
	};
  }

  //==========================================================================
  // Setters for dependency injection.
  //==========================================================================

  public void setBaseURL (String baseURL) {
	this.baseURL = baseURL;
  }

  public void setBaseURLFromFilename (String filename) {
	this.baseURL = filenameToURI(filename);
  }
    
  public String getBaseURL () {
	return baseURL;
  }
    
  public String getLastBaseURL () {
	return lastBaseURL;
  }

  public void setCompileTransforms (boolean doCompileTransforms) {
	doCompile = doCompileTransforms;
  }

  public boolean getCompileTransforms () {
	return doCompile;
  }

  //====================================================================
  // Setters for static defaults.
  //	The setters are non-static for Spring dependency injection.
  //====================================================================
 
  public boolean getDefaultCompileTransforms () {
    	return getStaticDefaultCompileTransforms();
  }

  public void setDefaultCompileTransforms (boolean defaultCompileTransforms) {
	setStaticDefaultCompileTransforms(defaultCompileTransforms);
  }

  public String getDefaultBaseURL () {
    	return getStaticDefaultBaseURL();
  }

  public void setDefaultBaseURL (String filename) {
	setStaticDefaultBaseURL(filename);
  }

  //==========================================================================
  // addTransform()
  //==========================================================================

  public void addTransforms (List<String> stylesheets) 
		throws TransformerException {
	if (stylesheets == null) { return; };
	for (String i : stylesheets) {
		addTransform(i);
	}
  }

  public void setTransforms (List<String> stylesheets)
		throws TransformerException {
	clear();
	addTransforms(stylesheets);
  }

  public void addTransform (Document stylesheet_dom) throws TransformerException {
	lastBaseURL = baseURL;
	if (stylesheet_dom == null) { return; };
	DOMSource source = new DOMSource(stylesheet_dom);
	if (lastBaseURL != null) { source.setSystemId(lastBaseURL); };
	addTransform(source);
  }

  public void addTransform (String stylesheet_str) throws TransformerException {
	lastBaseURL = baseURL;
	if (stylesheet_str == null) { return; };
	StreamSource source = 
		new StreamSource(new StringReader(stylesheet_str));
	if (lastBaseURL != null) { source.setSystemId(lastBaseURL); };
	addTransform(source);
  }

  public void addTransformFile (String stylesheet_filename)
		throws TransformerException {
	lastBaseURL = baseURL;
	if (stylesheet_filename == null) { return; };
	try {
        	File file = new File(stylesheet_filename);
		StreamSource source = new StreamSource(new FileReader(file));
		if (lastBaseURL == null) {
			lastBaseURL =
			    file.getCanonicalFile().toURI().toURL().toString();
		};
		if (lastBaseURL != null) { source.setSystemId(lastBaseURL); };
		addTransform(source);
	} catch (FileNotFoundException e) {
		throw new TransformerException(e);
	} catch (IOException e) {
		throw new TransformerException(e);
	};
  }

  /**
   * Add transform to chain from XML transform Source.
   */
  private void addTransform (Source source) throws TransformerException {
      if (source == null) { return; };
      try {
	Transformer transformer;
	if (doCompile) {
		transformer = tfactory.newTemplates(source).newTransformer();
	} else {
		transformer = tfactory.newTransformer(source);
	};
	list.add(transformer);
      } catch (TransformerConfigurationException e) {
		throw new TransformerException(e);
      };
  }
  //====
  // NOTE: xalan will not throw TranformerConfigurationException on error,
  //	just leaves transformer as non-null with internal null.
  //	So must catch Exception's below.
  //====

  public void addTransform (javax.xml.transform.Transformer transformer) {
        list.add(transformer);
  }

  //==========================================================================
  // deconstruct()
  //==========================================================================

  public String deconstructDocument (Document xml_dom)
		throws TransformerException {
	String str_result = null;

	if ((xml_dom == null) || (list.size() <= 0)) { return null; };
	Node dom = transformNode ((Node) xml_dom, list.size()-1, false);
	if (dom == null) { return null; };
	try {
		DOMSource dom_src = new DOMSource(dom);
		StringWriter sWriter = new StringWriter();
		StreamResult dom_result_str = new StreamResult(sWriter);
	
		Transformer newTransformer =
		    (Transformer) list.get(list.size()-1);
		if (newTransformer == null) { return null; };
        	newTransformer.transform(dom_src, dom_result_str);
		str_result = sWriter.toString();
      	} catch (TransformerException e) {
		throw new TransformerException(e);
	} catch (Exception e) {
		throw new TransformerException(e);
	}
	return str_result;
  }

  public synchronized String deconstruct (Document xml_dom)
		throws TransformerException {
	return deconstructDocument(xml_dom);
  }
  
  public synchronized String deconstructFile (String fileName)
		throws TransformerException {
	String deconstructed = null;
	if (fileName == null) { return null; };
	try {
		Document dom = dBuilder.parse(fileName);
		deconstructed = deconstructDocument(dom);
	} catch (SAXException e) { throw new TransformerException(e);
	} catch (IOException e) { throw new TransformerException(e); };
	return deconstructed;
  }
  
  public synchronized String deconstruct (Reader reader) 
		throws TransformerException {
	String deconstructed = null;
	if (reader == null) { return null; };
	try {
		Document dom = dBuilder.parse(new InputSource(reader));
		deconstructed = deconstructDocument(dom);
	} catch (SAXException e) { throw new TransformerException(e);
	} catch (IOException e) { throw new TransformerException(e); };
	return deconstructed;
  }

  //==========================================================================
  // transform()
  //==========================================================================
	
  /**
   * Apply transform chain to document node.
   * If no transforms to apply, returns original node (idempotent),
   * or null if returnDocument is true and the input node was not a document.
   */
  private Node transformNode (Node xml_dom, int listSize,
		boolean returnDocument) throws TransformerException {
        DOMSource dom_src;
        DOMResult dom_result;
        Node dom = xml_dom;

	if ((xml_dom == null) || (listSize > list.size())) { return null; };
	if (listSize <= 0) { 			// no transforms applied
		if (returnDocument && 
				(!(xml_dom instanceof org.w3c.dom.Document))) {
			return null; };
		return xml_dom;
	};
	try {
		for (int i=0; i<listSize; i++) {
			dom_src = new DOMSource(dom);
			dom_result = new DOMResult();
			Transformer newTransformer = (Transformer) list.get(i);
			if (newTransformer == null) { return null; };
        		newTransformer.transform(dom_src, dom_result);
			dom = (Node) dom_result.getNode();
		};
	} catch (TransformerException e) {
		throw new TransformerException(e);
	} catch (Exception e) {
		throw new TransformerException(e);
	}
	return dom;
  }

  public Document transformDocument (Document xml_dom)
		throws TransformerException {
	return (Document) transformNode ((Node) xml_dom, list.size(), true);
  }

  public Document transformNode (Node xml_dom)
		throws TransformerException {
	return (Document) transformNode ((Node) xml_dom, list.size(), true);
  }

   
  public synchronized Document transform(Document xml_dom)
		throws TransformerException {
	return transformDocument (xml_dom);
  }
    
  public synchronized Document transform (Node xml_dom)
		throws TransformerException {
	return (Document) transformNode (xml_dom);
  }
		
  public synchronized Document transform (String xml) throws TransformerException {
	Document transformed = null;
	if (xml == null) { return null; };
	try {
		Document dom = dBuilder.parse(
			new InputSource (new StringReader(xml)));
		transformed = transformDocument(dom);
	} catch (SAXException e) {
		throw new TransformerException(e);
	} catch (IOException e) {
		throw new TransformerException(e);
	};
	return transformed;
  }
  
  public synchronized Document transformFile (String fileName)
		throws TransformerException {
	Document transformed = null;
	if (fileName == null) { return null; };
	try {
		Document dom = dBuilder.parse(fileName);
		transformed = transformDocument(dom);
	} catch (SAXException e) {
		throw new TransformerException(e);
	} catch (IOException e) {
		throw new TransformerException(e);
	};
	return transformed;
  }
  
  public synchronized Document transform (Reader reader) 
		throws TransformerException {
	Document transformed = null;
	if (reader == null) { return null; };
	try {
		Document dom = dBuilder.parse(new InputSource(reader));
		transformed = transformDocument(dom);
	} catch (SAXException e) {
		throw new TransformerException(e);
	} catch (IOException e) {
		throw new TransformerException(e);
	};
	return transformed;
  }
    
  public synchronized Document transform (Document xml_dom, String stylesheet)
		throws TransformerException {
	setTransform(stylesheet);
	return transformDocument(xml_dom);
  }
    
  public synchronized Document transform (Document xml_dom,
		Document stylesheet_dom) throws TransformerException {
	setTransform(stylesheet_dom);
	return transformDocument(xml_dom);
  }
    
  //==========================================================================
  // setTransform()
  //==========================================================================
    
  public void setTransform (String stylesheet) throws TransformerException {
	clear();
	addTransform (stylesheet);
  }
     

  public void setTransform (Document stylesheet_dom) throws TransformerException {
	clear();
	addTransform (stylesheet_dom);
  }
  
  public void setTransformFile (String stylesheet_filename)
		throws TransformerException {
	clear();
	addTransformFile (stylesheet_filename);
  }
  
  public void clear () {
	list.clear();
  }
  
  //==========================================================================
  // getTransforms()
  //==========================================================================

  public List getTransforms () {
	return list;
  }	

  public javax.xml.transform.Transformer getLastTransform (int index) {
	return getTransform(list.size()-(1+index));
  }

  public javax.xml.transform.Transformer getTransform (int index) {
	if ((index < 0) || (index >= list.size())) { return null; };
	return (Transformer) list.get(index);
  }

  //==========================================================================
  // OutputProperty and Parameters
  //==========================================================================
    
  public String getOutputProperty (int index, String name)
		throws TransformerException {
	String retprop = null;
	Transformer transformer = getTransform(index);
	if ((transformer == null) || (name == null)) { return null; };
	try {
		retprop = transformer.getOutputProperty(name);
	} catch (IllegalArgumentException e) {
		throw new TransformerException(e);
	} catch (Exception e) {
		throw new TransformerException(e);
	}
	return retprop;
  }
    
  public void setOutputProperty (int index, String name, String value)
		throws TransformerException {
	Transformer transformer = getTransform(index);
	if ((transformer == null) || (name == null)
		|| (value == null)) { return; };
	try {
		transformer.setOutputProperty(name, value); 
	} catch (IllegalArgumentException e) {
		throw new TransformerException(e);
	} catch (Exception e) {
		throw new TransformerException(e);
	}
  }
    
  public Object getParameter (int index, String name)
		throws TransformerException {
	Object retparam = null;
	Transformer transformer = getTransform(index);
	if ((transformer == null) || (name == null)) { return null; };
	try {
		retparam = transformer.getParameter(name);
	} catch (IllegalArgumentException e) {
		throw new TransformerException(e);
	} catch (Exception e) {
		throw new TransformerException(e);
	}
	return retparam;
  }

  public void setParameter (int index, String name, Object value)
		throws TransformerException {
	Transformer transformer = getTransform(index);
	if ((transformer == null) || (name == null)
		|| (value == null)) { return; };
	try {
		transformer.setParameter(name, value); 
	} catch (IllegalArgumentException e) {
		throw new TransformerException(e);
	} catch (Exception e) {
		throw new TransformerException(e);
	}
  }
     
  public void setLastParameter (int index, String name, Object value)
		throws TransformerException {
	setParameter(list.size()-(1+index), name, value);
  }
     
  public void setFirstParameter (String name, Object value)
		throws TransformerException {
	setParameter(0, name, value);
  }
     
  public void setLastParameter (String name, Object value)
		throws TransformerException {
	setParameter((list.size()-1), name, value);
  }
     
  //====================================================================
  // Setters for static defaults.
  //====================================================================
 
  /**
   * Gets default flag for XSLTC bytecode compiling transforms.
   */
  public static boolean getStaticDefaultCompileTransforms () {
    	return defaultCompileTransforms;
  }

  /**
   * Sets default flag for XSLTC bytecode compiling transforms.
   */
  public static void setStaticDefaultCompileTransforms (boolean doDefaultCompileTransforms) {
	defaultCompileTransforms = doDefaultCompileTransforms;
  }

  /**
   * Gets default baseURL, used to resolve includes.
   */
  public static String getStaticDefaultBaseURL () {
	return defaultBaseURL;
  }

  /**
   * Sets default baseURL from filename, used to resolve includes.
   */
  public static void setStaticDefaultBaseURL (String filename) {
	defaultBaseURL = filenameToURI(filename);
  }

  private static String filenameToURI (String filename) {
	if (filename == null) { return null; };
	String baseURL = null;
	try {
		baseURL = new File(filename).getCanonicalFile().toURI().toURL().toString();
	} catch (Exception e) {
	};
	return baseURL;
  }

}

//==== END OF FILE
