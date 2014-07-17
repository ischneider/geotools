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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.metadata.iso.citation.OnLineResourceImpl;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.ExternalGraphicImpl;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Font;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Facility to 'override' styles. A feature may refer to a style via a styleUrl
 * and have an inline Style provide overrides.
 */
public class StyleOverride {

    final StyleMap styles;
    final StyleBuilder sb = new StyleBuilder();
    final Map<URI, Integer> inlineCount = new HashMap<URI, Integer>();

    // used to mark style options with whether the value is a default
    public static final String DEFAULT_COLOR = "_kml_default_color";
    public static final String DEFAULT_WIDTH = "_kml_default_width";

    public StyleOverride(StyleMap styles) {
        this.styles = styles;
    }

    private URI generateURI(URI baseURI) {
        Integer count = inlineCount.get(baseURI);
        if (count == null) {
            count = 1;
        }
        URI generated;
        try {
            generated = new URI(baseURI + "-inline-" + count);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
        inlineCount.put(baseURI, count + 1);
        return generated;
    }

    public void process(SimpleFeature feature) {
        FeatureTypeStyle inline = (FeatureTypeStyle) feature.getUserData().get(FeatureTypeStyle.class);
        if (inline == null) return;

        URI baseURI = (URI) feature.getAttribute("Style");
        URI generatedUri = generateURI(baseURI);
        FeatureTypeStyle base = styles.get(baseURI);
        feature.setAttribute("Style", generatedUri);
        if (base == null) {
            // cannot handle forward references styles
            return;
        }
        process(base, inline, generatedUri);
    }

    private void process(FeatureTypeStyle base, FeatureTypeStyle inline, URI generatedUri) {
        styles.put(generatedUri, overrideStyle(base, inline));
    }

    private FeatureTypeStyle overrideStyle(FeatureTypeStyle base, FeatureTypeStyle inline) {
        List<Symbolizer> updates = inline.rules().get(0).symbolizers();
        List<Symbolizer> output = new ArrayList<Symbolizer>();
        List<Symbolizer> additions = new ArrayList<Symbolizer>(3);
        List<Symbolizer> bases = base.rules().get(0).symbolizers();
        for (Symbolizer b: bases) {
            try {
                // we can't copy/clone these since the various utilities attempted
                // all seem to have various issues
                // the various inconsistencies used for building are due to
                // the difficulty/flaws of the style API
                if (b instanceof PointSymbolizer) {
                    PointSymbolizer orig = (PointSymbolizer) b;
                    ExternalGraphicImpl origEG = (ExternalGraphicImpl) orig.getGraphic().graphicalSymbols().get(0);
                    // have to use implementations here as the factory methods cause problems
                    ExternalGraphicImpl copyEG = new ExternalGraphicImpl();
                    copyEG.setFormat(origEG.getFormat());
                    if (origEG.getCustomProperties() != null) {
                        copyEG.setCustomProperties(new HashMap<String,Object>(origEG.getCustomProperties()));
                    }
                    OnLineResourceImpl resource = new OnLineResourceImpl();
                    if (origEG.getOnlineResource() != null) {
                        resource.setLinkage(origEG.getOnlineResource().getLinkage());
                    }
                    copyEG.setOnlineResource(resource);
                    output.add(sb.createPointSymbolizer(
                            sb.createGraphic(copyEG, null, null)
                    ));
                } else if (b instanceof LineSymbolizer) {
                    // these will have been created with color, number literals
                    LineSymbolizer orig = (LineSymbolizer) b;
                    LineSymbolizer neu = sb.createLineSymbolizer();
                    neu.setStroke(sb.createStroke(orig.getStroke().getColor(), orig.getStroke().getWidth()));
                    output.add(neu);
                } else if (b instanceof PolygonSymbolizer) {
                    // these will have been created with color, number literals
                    PolygonSymbolizer orig = (PolygonSymbolizer) b;
                    PolygonSymbolizer neu = sb.createPolygonSymbolizer();
                    neu.setStroke(sb.createStroke(orig.getStroke().getColor(), orig.getStroke().getWidth()));
                    output.add(neu);
                } else if (b instanceof TextSymbolizer) {
                    TextSymbolizer orig = (TextSymbolizer) b;
                    Font font = sb.createFont(
                            (String) orig.getFont().getFamily().get(0).evaluate(null),
                            (Double) orig.getFont().getSize().evaluate(null)
                    );
                    TextSymbolizer text = sb.createTextSymbolizer();
                    text.setFont(font);
                    text.setFill(sb.createFill(
                            sb.literalExpression(orig.getFill().getColor().evaluate(null))
                    ));
                    text.setLabel(orig.getLabel());
                    output.add(text);
                } else {
                    throw new RuntimeException("unexpected style " + b);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        for (int i = 0; i < updates.size(); i++) {
            Symbolizer update = updates.get(i);
            Symbolizer orig = null;
            for (int j = 0; j < output.size(); j++) {
                Symbolizer s = output.get(j);
                if (s.getClass() == update.getClass()) {
                    orig = s;
                    break;
                }
            }
            if (orig != null) {
                if (orig instanceof PointSymbolizer) {
                    // only graphic/externalgraphic
                    PointSymbolizer p1 = (PointSymbolizer) orig;
                    PointSymbolizer p2 = (PointSymbolizer) update;
                    if (p1.getGraphic().graphicalSymbols().size() > 0
                            && p2.getGraphic().graphicalSymbols().size() > 0) {
                        ExternalGraphic eg1 = (ExternalGraphic) p1.getGraphic().graphicalSymbols().get(0);
                        ExternalGraphic eg2 = (ExternalGraphic) p2.getGraphic().graphicalSymbols().get(0);
                        eg1.setCustomProperties(eg2.getCustomProperties());
                        // usually the override just colors/scales but its possible
                        // a different URI is used
                        if (eg2.getOnlineResource() != null) {
                            ((OnLineResourceImpl)eg1.getOnlineResource()).setLinkage(eg2.getOnlineResource().getLinkage());
                            if (eg2.getFormat() != null) {
                                eg1.setFormat(eg2.getFormat());
                            }
                        }
                    }
                } else if (orig instanceof LineSymbolizer) {
                    LineSymbolizer l1 = (LineSymbolizer) orig;
                    LineSymbolizer l2 = (LineSymbolizer) update;
                    // we only support width and color
                    if (l2.getOptions().get(StyleOverride.DEFAULT_COLOR) == null) {
                        l1.getStroke().setColor(l2.getStroke().getColor());
                    }
                    if (l2.getOptions().get(StyleOverride.DEFAULT_WIDTH) == null) {
                        l1.getStroke().setWidth(l2.getStroke().getWidth());
                    }
                } else if (orig instanceof PolygonSymbolizer) {
                    PolygonSymbolizer l1 = (PolygonSymbolizer) orig;
                    PolygonSymbolizer l2 = (PolygonSymbolizer) update;
                    // we only support color of fill, line color is provided
                    // by a LineSymbolizer, default is blank so no need to check
                    if (l2.getFill() != null) {
                        l1.setFill(sb.createFill(l2.getFill().getColor()));
                    }
                } else if (orig instanceof TextSymbolizer) {
                    TextSymbolizer l1 = (TextSymbolizer) orig;
                    TextSymbolizer l2 = (TextSymbolizer) update;
                    Number fontSize = (Number) l2.getFont().getSize().evaluate(null);
                    if ( fontSize.intValue() != 16) {
                        l1.setFont(sb.createFont((String) l2.getFont().getFamily().get(0).evaluate(null), fontSize.doubleValue()));
                    }
                    if (l2.getOptions().get(StyleOverride.DEFAULT_COLOR) == null) {
                        l1.setFill(sb.createFill(l2.getFill().getColor()));
                    }
                } else {
                    throw new RuntimeException();
                }
            } else {
                additions.add(update);
            }
        }
        output.addAll(additions);
        return sb.createFeatureTypeStyle(null, output.toArray(new Symbolizer[0]));
    }
}
