/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2014, Open Source Geospatial Foundation (OSGeo)
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

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Locator;
import org.picocontainer.MutablePicoContainer;

/**
 * Track any warnings emitted from the parser.
 * 
 * @author Ian Schneider <ischneider@boundlessgeo.com>
 */
public class ParseWarnings {
    final boolean enabled;
    Locator locator;
    final List<String> parseWarnings;

    ParseWarnings(boolean enabled, MutablePicoContainer container) {
        parseWarnings = new ArrayList<String>();
        this.locator = (Locator)container.getComponentInstanceOfType(Locator.class);
        this.enabled = enabled;
    }

    public boolean enabled() {
        return parseWarnings != null;
    }

    /**
     * Add a warning with an optional exception. If disabled and an exception
     * is provided, this method will throw new exception otherwise the message
     * will be logged and the exception ignored.
     *
     * @param msg the warning message
     * @param ex optional exception
     * @throws Exception if disabled
     */
    public void addWarning(String msg, Exception ex) throws Exception {
        if (!enabled && ex != null) {
            throw ex;
        }
        if (locator != null) {
            msg = "line " + locator.getLineNumber() + " : " + msg;
        }
        parseWarnings.add(msg);
    }

    public List<String> getWarnings() {
        return parseWarnings;
    }

}
