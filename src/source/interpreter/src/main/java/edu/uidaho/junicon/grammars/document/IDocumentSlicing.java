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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The <code>IDocumentSlicing</code> interface consists of a
 * collection of methods to inspect and modify a sequence of nodes
 * based on its location given by path and slice, where:
 * <br> 
 * <b>path</b> is an array of integers that represents the sequence of 
 * indexing steps to traverse from the root document element to the
 * parent node, and
 * <br>
 * <b>slice</b> is the range of child nodes (relative to the
 * parent) on which to operate, specified by one of the following forms:
 * <ul>
 * <li>interval, represented by <code>Integer</code> 
 * <code>from</code> and <code>to</code> parameters, 
 * where from &lt;= 0 means the head of the child sequence and
 * to &lt; 0 means the end of the child sequence,
 * or
 * <li>name of the node, represented by <code>String</code> 
 * <code>tagName</code> parameter, or
 * <li>position of the node, represented by <code>Integer</code> 
 * <code>from</code> parameter, or
 * <li>a further path extension to a child node, 
 * specified by an array of integers.
 * </ul>
 * Path and slice indices have origin 0, and are offsets
 * into the sequence of child <i>element nodes</i> 
 * thus excluding text and comment nodes.
 * <br>
 * Slice indices are inclusive in that the "to" index is
 * included in the range, unlike String substring() where the
 * "to" is "end+1".
 */

public interface IDocumentSlicing {

	//=================================================================
	// Document and element creation.
	//=================================================================

	/**
	 * Creates document.
	 * @return created document.
	 * @throws XMLStreamException on DocumentBuilderFactory exception.
	 */
	public Document createDocument () throws XMLStreamException;

	/**
	 * Creates document with given document element.
	 * @param root_tag Name of document element: if null or empty
	 * string will not create document element.
	 * @return created document.
	 * @throws XMLStreamException on DocumentBuilderFactory exception.
	 */
	public Document createDocument (String root_tag)
		throws XMLStreamException;

	/**
	 * Sets document element to given node.
	 * Imports node into document if it is foreign node.
	 * @return inserted document element.
	 */
	public Element setChild (Document document, Element child);

	/**
	 * Creates node with given tag.
	 * @return created node.
	 */
	public Element createElement (Document document, String tag);

	//=================================================================
	// Traverse.
	//=================================================================

	/**
	 * Find element specified by <code>path</code>. 
	 * @return result of performing child indexing using path,
	 * or original element if path is null.
	 */
	public Element find (Element parent, int[] path);

	/**
	 * Find element specified by <code>path</code>. 
	 * Minimally drops to first document element, i.e. getDocumentElement().
	 */
	public Element find (Document document, int[] path);

	//=================================================================
	// Get Children.
	//=================================================================

	/**
	 * Gets number of children of the element.
	 * @return number of children, or 0 if element is null.
	 */
	public int getNumChildren (Element parent);
	
	/**
	 * Gets index of child in parent relative to sequence of its elements.
	 * @return element index, or -1 if no parent.
	 */
	public int getChildIndex (Element element);

	/**
	 * Gets child at given index within parent's elements.
	 * @param index of child, or < 0 for last element. 
	 * @return child, or null if index otherwise out of bounds.
	 */
	public Element getChild (Element parent, int index);

	/**
	 * Gets first child element with given nodeName.
	 * @return child with name, or null if no such child.
	 */
	public Element getChild (Element parent, String nodeName);

	/**
	 * Gets children elements.
	 * @return array of children which are elements,
	 * or null if no children elements.
	 */
	public Element[] getChildren (Element parent);

	/**
	 * Gets children specified by the interval.
	 * @param from &lt;= 0 is start.
	 * @param to &lt; 0 is end.
	 * @return array of children within the interval of child elements,
	 * or null if no children elements.
	 */
	public Element[] getChildren (Element parent, int from, int to);

	/**
	 * Gets document element.
	 */
	public Element getChild (Document document);

	//=================================================================
	// Get/Set Attributes.
	//=================================================================

	/**
	 * Gets name of the element.
	 * @return tag name, or null if element is null.
	 */
	public String getTagName (Element element);

	/**
	 * Gets all the attributes of the element.
	 * @return Map of attributes, or null if element is null.
	 */
	public Map getAttributes (Element element);

	/**
	 * Gets names of all the attributes of the element.
	 * @return array of attribute names, or null if element is null.
	 */
	public String[] getAttributeNames (Element element);

	/**
	 * Returns if element has given attribute.
	 */
	public boolean hasAttribute (Element element, String name);
	
	/**
	 * Gets attribute of the element.
	 * @return attribute value, or null if no such attribute name.
	 */
	public String getAttribute (Element element, String attrName);

	/**
	 * Sets attribute of the element.
	 * @return original element, to permit cascading.
	 */
	public Element setAttribute (Element element, String name, String value);
	
	/**
	 * Sets attributes of the element.
	 * @return original element, to permit cascading.
	 */
	public Element setAttributes (Element element, String[] names, 
			String[] values);

	/**
	 * Sets qualified attribute of the element using given namespace URI.
	 * @return original element, to permit cascading.
	 */
	public Element setAttributeNS(Element element, String namespaceUri,
			String qualified_name, String value);
	
	//=================================================================
	// Get/Set Text node values.
	//=================================================================

	/**
	 * Returns if element is text node.
	 */
	public boolean isTextNode (Element element);
	
	/**
	 * Gets value of the text field of the element.
	 * @return text value of the first child,
	 * or null if element does not have a text node as the first child.
	 */
	public String getTextValue (Element element);
	
	/**
	 * Sets the text value of the first child of the element,
	 * or adds the text field to the element
	 * if the element does not have a text node as the first child.
	 * @return original element, to permit cascading.
	 */
	public Element setTextValue (Element element, String text);

	//=================================================================
	// Insertion and replacement.
	//=================================================================

	/**
	 * Inserts elements after given index of parent element.
	 * @param index &lt; 0 means prepend.
	 * If index is otherwise out of bounds, appends after last element.
	 * @param insertion Elements to insert.
	 * Null insertion elements are ingored.
	 * @return parent element, to permit cascading.
	 */
	public Element insertAfter (Element parent, int index, 
			Element[] insertion);

	/**
	 * Inserts documents after given index of parent element.
	 * @return parent element, to permit cascading.
	 */
	public Element insertAfter (Element parent, int index, 
			Document[] insertion);
	
	/**
	 * Inserts element after given index of parent element.
	 * @return parent element, to permit cascading.
	 */
	public Element insertAfter (Element parent, int index, 
			Element insertion);

	/**
	 * Inserts document after given index of parent element.
	 * @return parent element, to permit cascading.
	 */
	public Element insertAfter (Element parent, int index, 
			Document insertion);
	
	/**
	 * Appends elements to children of parent.
	 * @return parent element, to permit cascading.
	 */
	public Element append (Element parent, Element[] insertion);
	
	/**
	 * Appends documents to children of parent.
	 * @return parent element, to permit cascading.
	 */
	public Element append (Element parent, Document[] insertion);
	
	/**
	 * Appends element to children of parent.
	 * @return parent element, to permit cascading.
	 */
	public Element append (Element parent, Element insertion);
	
	/**
	 * Appends document to children of parent.
	 * @return parent element, to permit cascading.
	 */
	public Element append (Element parent, Document insertion);
	
	/**
	 * Replaces elements in given range of parent element.
	 * @param from &lt;= 0 is start.
	 * @param to &lt; 0 means end.
	 * If to is otherwise out of bounds, replaces to last element.
	 * @param replacement if null, effects deletion.
	 * Null replacement elements leave corresponding source elements
	 * untouched.
	 * @return parent element, to permit cascading.
	 */
	public Element replace (Element parent, int from, int to, 
			Element[] replacement);

	/**
	 * Replaces elements in given range of parent element.
	 * @return parent element, to permit cascading.
	 */
	public Element replace (Element parent, int from, int to, 
			Document[] replacement);

	/**
	 * Replaces elements in given range of parent element.
	 * @return parent element, to permit cascading.
	 */
	public Element replace (Element parent, int from, int to, 
			Element replacement);

	/**
	 * Replaces elements in given range of parent element.
	 * @return parent element, to permit cascading.
	 */
	public Element replace (Element parent, int from, int to, 
			Document replacement);

	/**
	 * Removes elements in given range of parent.
	 * @return parent element, to permit cascading.
	 */
	public Element remove (Element parent, int from, int to);

	/**
	 * Removes element at given index.
	 * @return parent element, to permit cascading.
	 */
	public Element remove (Element parent, int index);

	//=================================================================
	// Conversion.
	//=================================================================

	/**
	 * Convert array of documents to their root elements.
	 */
	public Element[] documentToElement (Document[] documents);

	/**
	 * Convert array of elements to documents.
	 * @param root_tag_name If non-null, will surround element with
	 * a root element with this tag name.
	 * Otherwise, the element becomes the root document element.
	 * @return created documents.
	 * @throws XMLStreamException on DocumentBuilderFactory exception.
	 */
	public Document[] elementToDocument (Element[] elements, String
			root_tag_name) throws XMLStreamException;

	/**
	 * Convert element to document.
	 * @param root_tag_name If non-null, will surround element with
	 * a root element with this tag name.
	 * Otherwise, the element becomes the root document element.
	 * @return created documents.
	 * @throws XMLStreamException on DocumentBuilderFactory exception.
	 */
	public Document elementToDocument (Element element, String
			root_tag_name) throws XMLStreamException;

}

//==== END OF FILE
