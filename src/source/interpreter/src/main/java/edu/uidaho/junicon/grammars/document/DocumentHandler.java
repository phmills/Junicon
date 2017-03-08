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

import java.lang.*;
import java.util.*;
import java.io.*;

// For DOM building
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;

/**
 * Create, add nodes to, and print an XML document.
 */
public class DocumentHandler {
  
  DocumentBuilder builder = null;
  Stack stack = new Stack();
  Document document = null;

  /**
   * Constructor.
   */
  public DocumentHandler () throws XMLStreamException {
	builder = createBuilder();
  }

  /**
   * Constructor that creates a document with the root node.
   */
  public DocumentHandler (String root_tag) throws XMLStreamException {
	builder = createBuilder();
  	start_document(root_tag);
  }

  /**
   * Initialize.
   */
  private DocumentBuilder createBuilder() throws XMLStreamException {
	DocumentBuilder builder = null;
	try {
		DocumentBuilderFactory factory =
			DocumentBuilderFactory.newInstance();
		builder = factory.newDocumentBuilder();
	} catch (ParserConfigurationException pce) {
		throw new XMLStreamException(pce);
	};
	return builder;
  }

  /**
   * Create a document with a root node.
   */
  public static Document create_document(String root_tag)
		throws XMLStreamException {
	DocumentHandler dochandler = new DocumentHandler();
	return dochandler.start_document(root_tag);
  }

  /**
   * Create the document root node.
   */
  public Document start_document(String root_tag) {
	document = builder.newDocument();  
	Element root = (Element) document.createElement(root_tag); 
	document.appendChild(root);
	stack.push(root);
	return document;
  }

  /**
   * Add a child node to the currently open node in the document
   * with an optional attribute name and value,
   * and make it the parent for further added nodes,
   * i.e., push it on the open node stack.
   */
  public Element open_tag (String tag, String attr_name, String attr_value) {
	Element parent = (Element) stack.peek();
	if ((parent == null) || (tag == null) || (tag.length()==0)) {
		return null;
	};
	Element node = (Element) parent.appendChild(
		document.createElement(tag) );
	if ((attr_name != null) && (attr_value != null)) {
		node.setAttribute(attr_name, attr_value);
	};
	stack.push(node);
	return node;
  }

  /**
   * Add a child node to the currently open node in the document
   * with an optional set of attribute names and values,
   * and make it the parent for further added nodes.
   */
  public Element open_tag_attributed (String tag, String[] attr_names,
			String[] attr_values) {
	Element parent = (Element) stack.peek();
	if ((parent == null) || (tag == null) || (tag.length()==0)) {
		return null;
	};
	Element node = (Element) parent.appendChild(
		document.createElement(tag) );
	if ((attr_names != null) && (attr_values != null) &&
			(attr_names.length == attr_values.length)) {
		for (int i=0; i< attr_names.length; i++) {
			String attr_name = attr_names[i];
			String attr_value = attr_values[i];
			if ((attr_name != null) && (attr_value != null)) {
				node.setAttribute(attr_name, attr_value);
			};
		};
	};
	stack.push(node);
	return node;
  }

/**
   * Add a child node to the currently open node in the document
   * with an optional set of attribute names and values,
   * and make it the parent for further added nodes.
   */
  public Element open_tag_attributed_NS (String tag, String[] attr_namespaces_URI, String[] attr_names,
			String[] attr_values) {
	Element parent = (Element) stack.peek();
	if ((parent == null) || (tag == null) || (tag.length()==0)) {
		return null;
	};
	Element node = (Element) parent.appendChild(
		document.createElement(tag) );
	if ((attr_names != null) && (attr_values != null) &&
			(attr_names.length == attr_values.length)) {
		for (int i=0; i< attr_names.length; i++) {
			String attr_name = attr_names[i];
			String attr_value = attr_values[i];
			String attr_namespace = attr_namespaces_URI[i];
			if ((attr_name != null) && (attr_value != null)) {
				node.setAttributeNS(attr_namespace, attr_name, attr_value);
			};
		};
	};
	stack.push(node);
	return node;
  }

  /**
   * Add a text node to the currently open node in the document.
   * with an optional attribute name and value.
   */
  public Element add_text_node (String tag, String attr_name,
		String attr_value, String text) {
	Element node = open_tag (tag, attr_name, attr_value);
	if (node == null) { return null; };
	if (text != null) {
		node.appendChild (document.createTextNode(text));
	};
	close_tag ();
	return node;
  }
  
  /**
   * Add a text node to the currently open node in the document,
   * with an optional set of attribute names and values.
   */
  public Element add_text_node_attributed (String tag, String[] attr_names,
		String[] attr_values, String text) {
	Element node = open_tag_attributed (tag, attr_names, attr_values);
	if (node == null) { return null; };
	if (text != null) {
		node.appendChild (document.createTextNode(text));
	};
	close_tag ();
	return node;
  }
  
  /**
   * Close the currently open node in the document,
   * i.e., pop it from the open node stack.
   */
  public Element close_tag () {
	return (Element) stack.pop();
  }
 
  /**
   * End the document construction.
   */
  public Document end_document() {
	document.getDocumentElement().normalize();
	return document;
  }

  /**
   * Get the string representation of the document.
   */
  public String toString() {
	return document_to_string (document);
  }

  /**
   * Test if node name equal to string.
   */
  public boolean matchTag(Node node, String tag) {
	if ((node == null) || (tag == null)) { return false; };
	if (node.getNodeName().compareTo(tag) == 0) {
		return true;
	};
	return false;
  }

  /**
   * Test if node value equal to string.
   */
  public boolean matchValue(Node node, String value) {
	if ((node == null) || (value == null)) { return false; };
	if (node.getNodeValue().compareTo(value) == 0) {
		return true;
	};
	return false;
  }

  //==========================================================================
  // Print document
  //==========================================================================

  /**
   * Print an XML document.
   */
  public static void print_document (Document document)
		throws XMLStreamException {
	DocumentHandler dochandler = new DocumentHandler();
	System.out.println ( dochandler.document_to_string (document));
  }
	
  /**
   * Get the string representation of an XML document.
   */
  public String document_to_string (Document document) { 
	StringWriter theAST = new StringWriter();
	if (document == null) { return ""; };
	try {		// traverse the DOM tree and print it out
		print_nodes (document.getDocumentElement(), 0, theAST);	// root
	} catch (Exception e) {
		return "";
	};
	if (theAST.toString() == null) { return ""; };
	return (theAST.toString()).trim();
  }

  /**
   * Get the XML encoding of operator characters.
   */
  private String xml_encode (String str) {
	return str.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }

  /**
   * Print the nodes of a document in depth_first order.
   */
  private void print_nodes (Node node, int indent, StringWriter theAST) {
	if (Node.TEXT_NODE == node.getNodeType()) {
		// Added this to allow for XSLT Parsers.
		// Error: <OPERATOR><</OPERATOR>
		theAST.write ( xml_encode ( node.getNodeValue() ));
		return;
	};
	boolean is_word = true;
	String attr_name = new String();
	String attr_val = new String();

	String tabs= "    ";
	String tag = node.getNodeName();
	StringBuffer spaces = new StringBuffer();
	for (int i=0; i < indent; i++) { spaces.append(tabs); };
	String str = spaces.toString() + "<" + tag;
	
	if (node.hasAttributes()) {
		for (int i=0; i<node.getAttributes().getLength(); i++) {
			attr_name = node.getAttributes().item(i).getNodeName();
			attr_val = node.getAttributes().item(i).getNodeValue();
			str += " " + attr_name + "=\"" + xml_encode(attr_val) + "\"";
		}	
	}
	
	if (node.hasChildNodes()) {		// recurse down tree
		theAST.write(str + ">");
		indent++;
		node = node.getFirstChild();
		if ((node != null) && (Node.TEXT_NODE != node.getNodeType())) {
			is_word = false;
			theAST.write("\n"); };
		while (node != null) {
			print_nodes (node, indent, theAST);
			node = node.getNextSibling();
		};
		if (! is_word) { theAST.write(spaces.toString()); };
		theAST.write("</" + tag + ">\n");
	} else {
		theAST.write(str + "/>\n");
	};
	return;
  }
}

//==== END OF FILE
