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
import java.net.URLConnection;
import java.util.HashMap;
import javax.xml.namespace.QName;
import org.geotools.kml.KML;
import org.geotools.kml.KMLOptions;
import org.geotools.metadata.iso.citation.OnLineResourceImpl;
import org.geotools.styling.ExternalGraphicImpl;
import org.geotools.styling.Graphic;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.StyleBuilder;
import org.geotools.xml.*;
import org.geotools.xml.AbstractComplexBinding;



/**
 * Binding object for the type http://www.opengis.net/kml/2.2:IconStyleType.
 *
 * <p>
 *	<pre>
 *	 <code>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;complexType final="#all" name="IconStyleType" xmlns="http://www.w3.org/2001/XMLSchema"&gt;
 *      &lt;complexContent&gt;
 *        &lt;extension base="kml:AbstractColorStyleType"&gt;
 *          &lt;sequence&gt;
 *            &lt;element minOccurs="0" ref="kml:scale"/&gt;
 *            &lt;element minOccurs="0" ref="kml:heading"/&gt;
 *            &lt;element minOccurs="0" name="Icon" type="kml:BasicLinkType"/&gt;
 *            &lt;element minOccurs="0" ref="kml:hotSpot"/&gt;
 *            &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:IconStyleSimpleExtensionGroup"/&gt;
 *            &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:IconStyleObjectExtensionGroup"/&gt;
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
public class IconStyleTypeBinding extends AbstractComplexBinding {

    StyleBuilder sb;
    KMLOptions options;

    public IconStyleTypeBinding(StyleBuilder sb, KMLOptions options) {
        this.sb = sb;
        this.options = options;
    }

	/**
	 * @generated
	 */
	public QName getTarget() {
		return KML.IconStyleType;
	}
	
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Class getType() {
		return PointSymbolizer.class;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *	
	 * @generated modifiable
	 */	
	public Object parse(ElementInstance instance, Node node, Object value) 
		throws Exception {
        Node icon = node.getChild("Icon");
        ExternalGraphicImpl eg = new ExternalGraphicImpl();
        Graphic g = sb.createGraphic(eg, null, null);
        if (icon != null) {
            Node href = icon.getChild("href");
            URI uri = new URI((String) href.getValue());
            // have to use implementation classes here
            OnLineResourceImpl resource = new OnLineResourceImpl();
            resource.setLinkage(uri);
            eg.setURI(uri.toString());
            // attempt to resolve the format/mimetype
            // most likely is jpg/png/gif which all resolve using this approach
            // svg doesn't seem supported by google earth (or URLConnection)
            String mimeType = URLConnection.guessContentTypeFromName(uri.getPath());
            mimeType = mimeType == null ? "unknown" : mimeType;
            eg.setFormat(mimeType);
            eg.setOnlineResource(resource);
        }
        Object color = node.getChildValue("color");
        Object scale = node.getChildValue("scale");
        // place color and scale values onto the properties to be dealt with elsewhere
        if (color != null || scale != null) {
            eg.setCustomProperties(new HashMap<String,Object>());
            if (color != null) {
                eg.getCustomProperties().put("color", color);
            }
            if (scale != null) {
                // we could potentially resolve the image here and compute the
                // size (in pixels) value of the Graphic
                eg.getCustomProperties().put("scale", scale);
            }
        }
        return sb.createPointSymbolizer(g);
	}

}