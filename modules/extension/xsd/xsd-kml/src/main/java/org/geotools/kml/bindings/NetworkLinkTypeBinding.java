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


import javax.xml.namespace.QName;
import org.geotools.kml.KML;
import org.geotools.kml.NetworkLink;
import org.geotools.xml.*;
import org.geotools.xml.AbstractComplexBinding;



/**
 * Binding object for the type http://www.opengis.net/kml/2.2:NetworkLinkType.
 *
 * <p>
 *	<pre>
 *	 <code>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;complexType final="#all" name="NetworkLinkType" xmlns="http://www.w3.org/2001/XMLSchema"&gt;
 *      &lt;complexContent&gt;
 *        &lt;extension base="kml:AbstractFeatureType"&gt;
 *          &lt;sequence&gt;
 *            &lt;element minOccurs="0" ref="kml:refreshVisibility"/&gt;
 *            &lt;element minOccurs="0" ref="kml:flyToView"/&gt;
 *            &lt;choice&gt;
 *              &lt;annotation&gt;
 *                &lt;documentation&gt;Url deprecated in 2.2&lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *              &lt;element minOccurs="0" ref="kml:Url"/&gt;
 *              &lt;element minOccurs="0" ref="kml:Link"/&gt;
 *            &lt;/choice&gt;
 *            &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:NetworkLinkSimpleExtensionGroup"/&gt;
 *            &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:NetworkLinkObjectExtensionGroup"/&gt;
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
public class NetworkLinkTypeBinding extends AbstractComplexBinding {

	/**
	 * @generated
	 */
	public QName getTarget() {
		return KML.NetworkLinkType;
	}
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Class getType() {
		return NetworkLink.class;
	}
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Object parse(ElementInstance instance, Node node, Object value) 
		throws Exception {
        NetworkLink networkLink = null;
        Node link = node.getChild("Link");
        if (link != null) {
            Node href = link.getChild("href");
            networkLink = new NetworkLink((String) href.getValue());
        }
        return networkLink;
	}

}