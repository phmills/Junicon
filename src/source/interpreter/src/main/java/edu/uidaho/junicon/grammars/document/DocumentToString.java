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
package edu.uidaho.junicon.grammars.document;

import javax.xml.stream.XMLStreamException;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DOMException;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * DocumentToString responsible for convertion 
 * document to string and string to document.
 */
public class DocumentToString {
		
  /**
   * Converts document to string.
   */
  public static String documentToString (Document dom) 
			throws XMLStreamException {
      if (dom == null) { return null; };
      String str = null;
      try {
	DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
	DOMImplementation domImpl = registry.getDOMImplementation("XML 3.0");
	DOMImplementationLS domImplLS = 
		(DOMImplementationLS) domImpl.getFeature("LS", "3.0");
	LSSerializer lsSerializer = domImplLS.createLSSerializer();
	LSOutput lsOutput = domImplLS.createLSOutput();
	ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
	lsOutput.setByteStream(stringOut);
	lsSerializer.write(dom, lsOutput);
	str = stringOut.toString();
      } catch (Exception e) { throw new XMLStreamException(e); }; 
      return str;
  }

  /**
   * Converts string to document.
   */
  public static Document stringToDocument (String xml) throws XMLStreamException
  {
	if (xml == null) { return null; };
	Document dom = null;
	DocumentBuilder builder = DocumentToString.createBuilder();
	try {
		dom = builder.parse(new InputSource(new StringReader(xml)));
	} catch (SAXException e) { throw new XMLStreamException(e);  
	} catch (IOException e) { throw new XMLStreamException(e); }; 
	return dom;
  }

  /**
   * Converts string to document.
   */
  public static Document fileToDocument (String filename) 
		throws XMLStreamException {
	if (filename == null) { return null; };
	Document dom = null;
	DocumentBuilder builder = DocumentToString.createBuilder();
	try {
		dom = builder.parse(filename);
	} catch (SAXException e) { throw new XMLStreamException(e);  
	} catch (IOException e) { throw new XMLStreamException(e); }; 
	return dom;
  }

  /**
   * Converts string to document.
   */
  public static Document readerToDocument (Reader reader) 
		throws XMLStreamException {
	if (reader == null) { return null; };
	Document dom = null;
	DocumentBuilder builder = DocumentToString.createBuilder();
	try {
		dom = builder.parse(new InputSource(reader));
	} catch (SAXException e) { throw new XMLStreamException(e);  
	} catch (IOException e) { throw new XMLStreamException(e); }; 
	return dom;
  }
	
  private static DocumentBuilder createBuilder() throws XMLStreamException {
	DocumentBuilder builder = null;
	try {
		DocumentBuilderFactory factory =
			DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		builder = factory.newDocumentBuilder();
	} catch (ParserConfigurationException e) {
		throw new XMLStreamException(e);
	};
	return builder;
  }
}

//==== END OF FILE
