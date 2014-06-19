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

import java.util.List;
import org.geotools.xml.Configuration;
import org.geotools.xml.XSD;
import org.picocontainer.MutablePicoContainer;

/**
 * Parsing options for reuse in both parsers.
 */
public abstract class KMLOptions extends Configuration {
    private boolean lenientGeometryParsing = false;

    private boolean onlyCollectStyles = false;

    ParseWarnings warnings;

    protected KMLOptions(XSD xsd) {
        super(xsd);
    }

    /**
     * If true, styles will be collected and each styled feature will receive
     * a URI instead of the style object itself. The URI can be used to resolve
     * the style. The default is false;
     */
    public void setOnlyCollectStyles(boolean onlyCollectStyles) {
        this.onlyCollectStyles = onlyCollectStyles;
    }

    public boolean isOnlyCollectStyles() {
        return onlyCollectStyles;
    }

    /**
     * Get a list of any warnings related to lenient parsing. The returned
     * list may be modified as the parse continues.
     * @return non-null list of warnings
     */
    public List<String> getParseWarnings() {
        return warnings.getWarnings();
    }

    public boolean isLenientGeometryParsing() {
        return lenientGeometryParsing;
    }

    public void setLenientGeometryParsing(boolean lenientGeometryParsing) {
        this.lenientGeometryParsing = lenientGeometryParsing;
    }

    @Override
    protected void configureContext(MutablePicoContainer container) {
        container.registerComponentInstance(warnings = new ParseWarnings(lenientGeometryParsing, container));
    }
}
