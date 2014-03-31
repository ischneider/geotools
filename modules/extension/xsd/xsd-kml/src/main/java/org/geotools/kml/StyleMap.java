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
package org.geotools.kml;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.geotools.styling.FeatureTypeStyle;


/**
 * Simple container for holding styles by uri.
 * <p>
 * This is lame as it is just a hash map in memory. It should really be an
 * embedded db that serializes / deserializes out to disk.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project
 *
 * @source $URL$
 */
public class StyleMap {
    protected Map map = Collections.synchronizedMap(new HashMap());

    private Map<URI,URI> aliases = new HashMap<URI,URI>();
    /**
     * Add a style. According to KML, duplicate id's may be added but last in,
     * last out.
     * @param uri may be null, one will be generated
     * @param style non-null style
     * @return the URI used. if generated, this is useful
     */
    public URI put(URI uri, FeatureTypeStyle style) {
        if (uri == null) {
            int id = map.size() + 1;
            try {
                uri = new URI("#unamed-" + id);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
        map.put(uri, style);
        return uri;
    }

    public FeatureTypeStyle get(URI uri) {
        URI aliased = aliases.get(uri);
        if (aliased != null) {
            uri = aliased;
        }
        return (FeatureTypeStyle) map.get(uri);
    }

    public Collection<URI> keys() {
        HashSet<URI> keys = new HashSet<URI>(map.keySet());
        keys.addAll(aliases.keySet());
        return keys;
    }

    /**
     * Alias a style uri to another (original). This allows mapping a StyleMap
     * id to it's 'normal' style since we cannot really support rollover.
     * @param string
     * @param uri
     */
    public void alias(URI alias, URI original) {
        aliases.put(alias, original);
    }

}
