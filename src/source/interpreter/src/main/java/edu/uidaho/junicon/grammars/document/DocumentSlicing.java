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

import java.util.*;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;
  
/**
 * DocumentSlicing consists of methods 
 * to access and modify a document's elements based on location given by
 * path and slice, where:<br> 
 * path is represented by array of integers, and slice is an interval, 
 * represented by integer <code>from</code> and <code>to</code> parameters.
 */

public class DocumentSlicing implements IDocumentSlicing {

	private DocumentBuilder builder = null;

	/**
	 * Constructor.
	 */
	public DocumentSlicing() {
	}

	/**
	 * Initialize document builder.
	 * Lazily invoked when creating a document for the first time.
	 * @throws XMLStreamException on DocumentBuilderFactory exception.
	 */
	private DocumentBuilder createBuilder() throws XMLStreamException {
		if (builder != null) { return builder; };
		try {
			DocumentBuilderFactory factory =
				DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException pce) {
			throw new XMLStreamException(pce);
		};
		return builder;
	  }

	//=================================================================
	// Document and element creation.
	//=================================================================

	public Document createDocument () throws XMLStreamException {
		builder = createBuilder();
		if (builder == null) { return null; };
		return builder.newDocument();  
	}

	public Document createDocument (String root_tag) 
			throws XMLStreamException {
		Document document = createDocument();
		if (document == null) { return null; };
		if ((root_tag == null) || (root_tag.length() == 0)) {
			return document;
		};
		Element root = (Element) document.createElement(root_tag); 
		document.appendChild(root);
		return document;
	}

	public Element setChild (Document document, Element child) {
		if ((document == null) || (child == null)) { return child; };	
		Node elt = document.getFirstChild();
		if (elt != null) {
			document.removeChild(elt);
		};
		Element insertion = (Element) getImportNode(document, child);
		document.appendChild(insertion);
		return insertion;
	}

	public Element createElement (Document document, String tag) {
		if ((document == null) || (tag == null)) { return null; };
		return document.createElement(tag);
	}

	//=================================================================
	// Traverse.
	//=================================================================

	public Element find(Document document, int[] path) {
		if (document == null) { return null; };
		return find(document.getDocumentElement(), path);
	}

	public Element find(Element parent, int[] path) {
		if (parent == null) { return null; };
		if (path == null) { return parent; };
		Element node = parent;
		for (int i=0; i < path.length; i++) {
			node = getChild(node, path[i]);	
			if (node == null) { return null; };
		}
		return node;
	}

	//=================================================================
	// Get Children.
	//=================================================================

	public int getNumChildren(Element parent) {
		if (parent == null) { return 0; };
		NodeList nodes = parent.getChildNodes(); 
		if (nodes == null) { return 0; };
		Element[] elts = filterElements(nodes);
		if (elts == null) { return 0; };
		return elts.length;
	}
	
	public int getChildIndex(Element element) {
		if (element == null || element.getParentNode() == null) { return -1; };
		
		NodeList nodeList = element.getParentNode().getChildNodes();
		Element[] elts = filterElements(nodeList);
		if (elts == null) { return -1; };
		
		int index = 0;
		while ((index < elts.length) && (elts[index] != element)) { 
			index++;
		}
		return index;
	}

	public Element[] getChildren(Element parent) {
		if (parent == null) { return null; };
		NodeList nodeList = parent.getChildNodes();
		if (nodeList == null) { return null; };
		Element[] elts = filterElements(nodeList);
		return elts;
	}

	public Element[] getChildren(Element parent, int from, int to) {
		Element[] elts = getChildren(parent);
		if (elts == null) { return null; };
		if (from < 0) { from = 0; };
		if ((to < 0) || (to >= elts.length)) { to = elts.length-1; };
		if (from > to) { return new Element[0]; };
		int sourceLength = (to - from) + 1; 
		Element[] retval = new Element[sourceLength];
		for (int i=0; i < sourceLength; i++) {
			retval[i] = elts[from + i];
		}
		return retval;
	}

	public Element getChild(Element parent, int index) {
		Element[] elts = getChildren(parent);
		if (elts == null) { return null; };
		if (index < 0) { index = elts.length - 1; };
		if (index >= elts.length) { return null; };
		return elts[index];
	}

	public Element getChild(Element parent, String nodeName) {
		if (nodeName == null) { return parent; };
		Element[] elts = getChildren(parent);
		if (elts == null) { return null; };
		for (int i=0; i< elts.length; i++) {
			if (elts[i].getNodeName().equals(nodeName)) {
				return elts[i];
			};
		};
		return null;
	}

	public Element getChild(Document document) {
		if (document == null) { return null; };	
		return document.getDocumentElement();
	}

	//=================================================================
	// Get/Set Attributes.
	//=================================================================

	public String getTagName (Element element) {
		if (element == null) { return null; };
		return element.getNodeName();
	}

	public Map getAttributes (Element element) {
		if (element == null) { return null; };
		NamedNodeMap map = element.getAttributes();
		if (map == null) { return null; };
		int len = map.getLength();
		Map hash = new HashMap(len);
		for (int i=0; i<len; i++) {
			hash.put(map.item(i).getNodeName(), map.item(i).getNodeValue());
		}
		return hash;
	}

	public String[] getAttributeNames (Element element) {
		if (element == null) { return null; };
		NamedNodeMap map = element.getAttributes();
		if (map == null) { return null; };
		int len = map.getLength();
		String[] names = new String[len];
		for (int i=0; i<len; i++) {
			names[i] = map.item(i).getNodeName();
		}
		return names;
	}
	
	public boolean hasAttribute (Element element, String name) {
		if ((element == null) || (name == null)) { return false; };
		return element.hasAttribute(name);
	}

	public String getAttribute(Element element, String attrName) {
		if (element == null) { return null; };
		NamedNodeMap map = element.getAttributes();
		if (map == null) { return null; };
		Node attrNode = map.getNamedItem(attrName);
		if (attrNode == null) { return null; };
		return attrNode.getNodeValue();
	}

	public Element setAttribute(Element element, String name, String value) {
		if (element == null || name == null || value == null) {
			return element;
		};
		element.setAttribute(name, value);
		return element;
	}
	
	public Element setAttributes(Element element, String[] names, 
			String[] values) {
		if (element == null || names == null || values == null) {
			return element;
		};
		if (names.length != values.length) { return element; };
		
		for (int i=0; i < names.length; i++) {
			element.setAttribute(names[i], values[i]);
		}
		return element;
	}
	
	public Element setAttributeNS(Element element, String namespaceUri,
			String qualified_name, String value) {
		if (element == null || namespaceUri == null ||
				qualified_name == null || value == null) {
			return element;
		};
		element.setAttributeNS(namespaceUri, qualified_name, value);
		return element;
	}
	
	//=================================================================
	// Get/Set Text node values.
	//=================================================================

	public boolean isTextNode (Element element) {
		return (null != getTextValue(element));
	}

	public String getTextValue (Element element) {
		if ((element == null) || (! element.hasChildNodes())) { return null; };
		Node node = element.getFirstChild();
		if ((Node.TEXT_NODE != node.getNodeType())) { return null; };
		return element.getFirstChild().getNodeValue();
	}
	
	public Element setTextValue(Element element, String text) {
		if (element == null || text == null) { return element; };
		
		if (element.hasChildNodes() && 
				element.getFirstChild().getNodeType() == Node.TEXT_NODE) {
			element.getFirstChild().setNodeValue(text);
		} else {
			Document dom = element.getOwnerDocument();
			element.appendChild(dom.createTextNode(text));
		}
		return element;
	}

	//=================================================================
	// Insertion and replacement.
	//=================================================================

	public Element insertAfter(Element parent, int index, 
			Element insertion) {
		if (insertion == null) { return parent; };
		Element[] repl = { insertion };
		return insertAfter(parent, index, repl);
	}

	public Element insertAfter(Element parent, int index, 
			Document insertion) {
		if (insertion == null) { return parent; };
		Document[] repl = { insertion };
		return insertAfter(parent, index, repl);
	}

	public Element insertAfter(Element parent, int index, 
			Document[] insertion) {
		return insertAfter(parent, index,
			documentToElement(insertion));
	}
	
	public Element insertAfter(Element parent, int index, 
			Element[] insertion) {
		if ((parent == null) || (insertion == null) ||
				(insertion.length == 0)) {
			return parent;
		};
		Node child = null;
		if (index < 0) {	// prepend
			child = parent.getFirstChild();
		} else {		// get child to insert before
			child = getChild(parent,index+1);
		};
		for (int i=0; i<insertion.length; i++) {
			if (insertion[i] != null) {
				parent.insertBefore(
					getImportNode(parent, insertion[i]),
					child);
			};
		}
		return parent;
	}
	
	public Element append (Element parent, Element[] insertion) {
		if ((parent == null) || (insertion == null) ||
				(insertion.length == 0)) {
			return parent;
		};
		for (int i=0; i<insertion.length; i++) {
			if (insertion[i] != null) {
				parent.appendChild(
					getImportNode(parent, insertion[i]));
			};
		};
		return parent;
	}
	
	public Element append (Element parent, Document[] insertion) {
		return append(parent, documentToElement(insertion));
	}
	
	public Element append (Element parent, Element insertion) {
		Element[] repl = { insertion };
		return append(parent, ((insertion == null) ? null : repl));
	}

	public Element append (Element parent, Document insertion) {
		Document[] repl = { insertion };
		return append(parent, ((insertion == null) ? null : repl));
	}
	
	public Element replace(Element parent, int from, int to, 
			Element replacement) {
		Element[] repl = { replacement };
		return replace(parent, from, to,
			((replacement == null) ? null : repl));
	}

	public Element replace(Element parent, int from, int to, 
			Document replacement) {
		Document[] repl = { replacement };
		return replace(parent, from, to,
			((replacement == null) ?  null : repl));
	}

	public Element replace(Element parent, int from, int to, 
			Document[] replacement) {
		return replace(parent, from, to, documentToElement(replacement));
	}

	public Element replace(Element parent, int from, int to, 
			Element[] replacement) {
		if (parent == null) { return null; };
		Element[] elts = getChildren(parent);
		if ((elts == null) || (elts.length == 0)) { return parent; };
		if (from < 0) { from = 0; };
		if ((to < 0) || (to >= elts.length)) { to = elts.length-1; };
		if (from > to) { return parent; };
		int sourceLength = (to - from) + 1; 
		int targetLength = 0;
		if (replacement != null) {
			targetLength = replacement.length;
		};
		int replaceLength = targetLength;
		if (sourceLength < targetLength) { 
			replaceLength = sourceLength;
		}
		for (int i=0; i < replaceLength; i++) {
			if (replacement[i] != null) {
				parent.replaceChild(
					getImportNode(parent, replacement[i]),
					elts[from + i]);
			};
		}
		for (int i=replaceLength; i < sourceLength; i++) {
				parent.removeChild(elts[from + i]);
		}
		Element child = null;
		if ((from + replaceLength) < elts.length) {	
			child = elts[from + replaceLength];
		};
		for (int i=replaceLength; i<targetLength; i++) {
			if (replacement[i] != null) { 
				parent.insertBefore(
					getImportNode(parent, replacement[i]),
					child);
			};
		}
		return parent;
	}

	public Element remove (Element parent, int from, int to) {
		return replace(parent, from, to, (Element[]) null);
	}

	public Element remove (Element parent, int index) {
		return replace(parent, index, index, (Element[]) null);
	}

	public Element[] documentToElement (Document[] documents) {
		if (documents == null) { return null; };
		Element[] elements = new Element[documents.length];
		for (int i=0; i < documents.length; i++) {
			if (documents[i] == null) {
				elements[i] = null;
			} else {
				elements[i] = documents[i].getDocumentElement();
			};
		}
		return elements;
	}

	public Document[] elementToDocument (Element[] elements, String
			root_tag) throws XMLStreamException {
		if (elements == null) { return null; };
		Document[] documents = new Document[elements.length];
		for (int i=0; i < elements.length; i++) {
			documents[i] = elementToDocument(elements[i], root_tag);
		}
		return documents;
	}

	public Document elementToDocument (Element element, String
			root_tag) throws XMLStreamException {
		if (element == null) { return null; };
		Document document = createDocument(root_tag);
		if (document == null) { return null; };
		if ((root_tag != null) && (root_tag.length() > 0)) {
			append(getChild(document), element);
		} else {
			setChild(document, element);
		};
		return document;
	}

	//=================================================================
	// private methods
	//=================================================================

	/**
	 * Clone node for insertion into foreign document.
	 * @return cloned node, even if
	 * original node in same document as target parent.
	 */
	private Node getImportNode (Element parent, Element node) {
		if ((parent == null) || (node == null)) { return node; };
		Document target = parent.getOwnerDocument();
		// if (target == node.getOwnerDocument()) { return node; };
		return target.importNode(node, true);
	}

	/**
	 * Clone node for insertion into foreign document.
	 * @return cloned node, even if
	 * original node in same document as target parent.
	 */
	private Node getImportNode (Document target, Element node) {
		if ((target == null) || (node == null)) { return node; };
		// if (target == node.getOwnerDocument()) { return node; };
		return target.importNode(node, true);
	}

	/**
	 * Filters list of nodes to return only nodes of element type.
	 */
	private Element[] filterElements(NodeList nodeList) {
		if (nodeList == null) { return null; };
		
		Node[] nodes = new Node[nodeList.getLength()];
		for (int i=0; i < nodeList.getLength(); i++) {
			nodes[i] = nodeList.item(i);
		}
		return filterElements(nodes);
	}

	/**
	 * Filters array of nodes to return only nodes of element type.
	 */
	private Element[] filterElements(Node[] nodes) {
		if (nodes == null) { return null; };
		
		ArrayList eltArray = new ArrayList();
		for (int i=0; i < nodes.length; i++) {
			if (nodes[i] instanceof Element) { 
				eltArray.add(nodes[i]); 
			}
		}
		return (Element[]) eltArray.toArray(new Element[0]);
	}
}

//==== END OF FILE
