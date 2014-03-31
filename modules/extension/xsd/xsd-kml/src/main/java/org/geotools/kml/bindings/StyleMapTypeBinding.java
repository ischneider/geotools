/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.kml.bindings;


import java.net.URI;
import java.util.List;
import org.geotools.xml.*;
import org.geotools.xml.AbstractComplexBinding;


import javax.xml.namespace.QName;
import org.geotools.kml.StyleMap;
import org.geotools.kml.v22.KML;

/**
 * Binding object for the type http://www.opengis.net/kml/2.2:StyleMapType.
 *
 * <p>
 *	<pre>
 *	 <code>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;complexType final="#all" name="StyleMapType" xmlns="http://www.w3.org/2001/XMLSchema"&gt;
 *      &lt;complexContent&gt;
 *        &lt;extension base="kml:AbstractStyleSelectorType"&gt;
 *          &lt;sequence&gt;
 *            &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:Pair"/&gt;
 *            &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:StyleMapSimpleExtensionGroup"/&gt;
 *            &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:StyleMapObjectExtensionGroup"/&gt;
 *          &lt;/sequence&gt;
 *        &lt;/extension&gt;
 *      &lt;/complexContent&gt;
 *    &lt;/complexType&gt; 
 *		
 *	  </code>
 *	 </pre>
 * </p>
 *
 * @generated
 */
public class StyleMapTypeBinding extends AbstractComplexBinding {
    private final StyleMap styleMap;

    public StyleMapTypeBinding(StyleMap styleMap) {
        this.styleMap = styleMap;
    }


	/**
	 * @generated
	 */
	public QName getTarget() {
		return KML.StyleMapType;
	}
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Class getType() {
		return null;
	}
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Object parse(ElementInstance instance, Node node, Object value) 
		throws Exception {

        List pairs = node.getChildren("Pair");

        // for now, resolve the normal style id and alias the map id to this
        for (int i = 0; i < pairs.size(); i++) {
            Node pair = (Node) pairs.get(i);
            if ("normal".equals(pair.getChildValue("key"))) {
                URI styleMapID = new URI("#" + (String)node.getAttributeValue("id"));
                URI normalID = (URI) pair.getChildValue("styleUrl");
                // @todo what about inline Style elements
                styleMap.alias(styleMapID, normalID);
            }
        }

		return super.parse(instance,node,value);
	}

}