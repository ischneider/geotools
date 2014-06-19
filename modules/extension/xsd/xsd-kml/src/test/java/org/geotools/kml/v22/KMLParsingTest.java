package org.geotools.kml.v22;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static junit.framework.TestCase.assertEquals;
import org.geotools.kml.NetworkLink;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Symbolizer;
import org.geotools.xml.Parser;
import org.geotools.xml.PullParser;
import org.geotools.xml.StreamingParser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.net.URI;

public class KMLParsingTest extends KMLTestSupport {

    // lazily parse this and cache - makes debugging annoying if statically parsed
    static SimpleFeature doc;
    static SimpleFeature doc() {
        if (doc == null) {
            try {
                doc = new KMLParsingTest().parseSamples();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return doc;
    }

    public void testParseDocument() throws Exception {
        assertNotNull(doc());
        assertEquals("document", doc.getType().getTypeName());
        assertEquals("KML Samples", doc.getAttribute("name"));
        assertEquals(6, ((List)doc.getAttribute("Feature")).size());
    }

    public void testParseFolder() throws Exception {
        SimpleFeature folder = (SimpleFeature) ((List)doc().getAttribute("Feature")).get(0);

        assertEquals("Placemarks", folder.getAttribute("name"));
        assertTrue(folder.getAttribute("description").toString().startsWith("These are just some"));
        assertEquals(3, ((List)folder.getAttribute("Feature")).size());
    }

    public void testParsePlacemark() throws Exception {
        SimpleFeature folder = (SimpleFeature) ((List)doc().getAttribute("Feature")).get(0);
        SimpleFeature placemark = (SimpleFeature) ((List)folder.getAttribute("Feature")).get(0);
        
        assertEquals("Simple placemark", placemark.getAttribute("name"));
        assertTrue(placemark.getAttribute("description").toString().startsWith("Attached to the ground"));
        Point p = (Point) placemark.getDefaultGeometry();
        assertEquals(-122.08220, p.getX(), 0.0001);
        assertEquals(37.42229, p.getY(), 0.0001);
    }

    public void testParseWithSchema() throws Exception {
        
    }

    public void testStreamParse() throws Exception {
        StreamingParser p = new StreamingParser(createConfiguration(),
            getClass().getResourceAsStream("KML_Samples.kml"), KML.Placemark);
        int count = 0;
        while(p.parse() != null) {
            count++;
        }
        assertEquals(20, count);
    }

    public void testPullParse() throws Exception {
        KMLConfiguration config = new KMLConfiguration();
        PullParser p = new PullParser(config,
            getClass().getResourceAsStream("KML_Samples.kml"), KML.Placemark);
     
        int count = 0;
        SimpleFeature parsed;
        // previously only counting features missed some errors like LookAt
        // and region mistakenly being taken as the geometry
        Object[] checks = new Object[]{
            "Simple placemark", Point.class, 1,
            "Floating placemark", Point.class, 1,
            "Extruded placemark", Point.class, 1,
            "Roll over this icon", Point.class, 1,
            "Descriptive HTML", null, 0,
            "Tessellated", LineString.class, 2,
            "Untessellated", LineString.class, 2,
            "Absolute", LineString.class, 11,
            "Absolute Extruded", LineString.class, 11,
            "Relative", LineString.class, 11,
            "Relative Extruded", LineString.class, 11,
            "Building 40", Polygon.class, 22,
            "Building 41", Polygon.class, 19,
            "Building 42", Polygon.class, 24,
            "Building 43", Polygon.class, 25,
            "The Pentagon", Polygon.class, 12,
            "Absolute", Polygon.class, 5,
            "Absolute Extruded", Polygon.class, 5,
            "Relative", Polygon.class, 9,
            "Relative Extruded", Polygon.class, 9,};
        while( (parsed = (SimpleFeature) p.parse()) != null) {
            int idx = count * 3;
            Object name = parsed.getAttribute("name");
            Object geom = parsed.getAttribute("Geometry");
            assertEquals(checks[idx], name);
            assertEquals(checks[idx + 1], geom == null ? null : geom.getClass());
            assertEquals(checks[idx + 2], geom == null ? 0 : ((Geometry) geom).getCoordinates().length);
            count++;
        }
        assertEquals(20, count);

        // verify the stylemap alias works
        FeatureTypeStyle style = config.getStyleMap().get(new URI("#exampleStyleMap"));
        assertNotNull(style);
        assertTrue(style == config.getStyleMap().get(new URI("#normalPlacemark")));
    }

    public void testPullParseOrHandler() throws Exception {
        PullParser p = new PullParser(createConfiguration(), getClass().getResourceAsStream(
                "KML_Samples.kml"), KML.Placemark, KML.GroundOverlay, KML.ScreenOverlay);
        int count = 0;
        while (p.parse() != null) {
            count++;
        }
        assertEquals(28, count);
    }

    public void testParseExtendedData() throws Exception {
        String xml = 
            " <Placemark> " + 
            "    <name>Club house</name> " + 
            "    <ExtendedData> " + 
            "      <Data name='holeNumber'> " + 
            "        <value>1</value> " + 
            "      </Data> " + 
            "      <Data name='holeYardage'> " + 
            "        <value>234</value> " + 
            "      </Data> " + 
            "      <Data name='holePar'> " + 
            "        <value>4</value> " + 
            "      </Data> " + 
            "    </ExtendedData> " + 
            "    <Point> " + 
            "      <coordinates>-111.956,33.5043</coordinates> " + 
            "    </Point> " + 
            "  </Placemark> ";
        buildDocument(xml);

        SimpleFeature f = (SimpleFeature) parse();
        Map<Object, Object> userData = f.getUserData();
        assertNotNull(userData);

        @SuppressWarnings("unchecked")
        Map<String, Object> untypedData = (Map<String, Object>) userData.get("UntypedExtendedData");
        assertEquals("1", untypedData.get("holeNumber"));
        assertEquals("234", untypedData.get("holeYardage"));
        assertEquals("4", untypedData.get("holePar"));
    }

    public void testExtendedDataTyped() throws Exception {
        String xml = 
            "<kml xmlns='http://www.opengis.net/kml/2.2'> "+
                    "<Document>   "+
                    "  <name>ExtendedData+SchemaData</name>   "+
                    "  <open>1</open>"+
                    ""+
                    "  <!-- Declare the type 'TrailHeadType' with 3 fields -->"+
                    "  <Schema name='TrailHeadType' id='TrailHeadTypeId'>     "+
                    "    <SimpleField type='string' name='TrailHeadName'>       "+
                    "      <displayName><![CDATA[<b>Trail Head Name</b>]]></displayName>     "+
                    "    </SimpleField>     "+
                    "    <SimpleField type='double' name='TrailLength'>       "+
                    "      <displayName><![CDATA[<i>Length in miles</i>]]></displayName>     "+
                    "    </SimpleField>     "+
                    "    <SimpleField type='int' name='ElevationGain'>       "+
                    "      <displayName><![CDATA[<i>Change in altitude</i>]]></displayName>     "+
                    "    </SimpleField>   "+
                    "  </Schema> "+
                    ""+
                    "<!-- This is analogous to adding three fields to a new element of type TrailHead:"+
                    ""+
                    "  <TrailHeadType>        "+
                    "    <TrailHeadName>...</TrailHeadName>        "+
                    "    <TrailLength>...</TrailLength>        "+
                    "    <ElevationGain>...</ElevationGain>    "+
                    " </TrailHeadType>"+
                    "-->      "+
                    ""+
                    "  <!-- Instantiate some Placemarks extended with TrailHeadType fields -->    "+
                    "  <Placemark>     "+
                    "    <name>Easy trail</name>"+
                    "    <ExtendedData>       "+
                    "      <SchemaData schemaUrl='#TrailHeadTypeId'>         "+
                    "        <SimpleData name='TrailHeadName'>Pi in the sky</SimpleData>         "+
                    "        <SimpleData name='TrailLength'>3.14159</SimpleData>         "+
                    "        <SimpleData name='ElevationGain'>10</SimpleData>       "+
                    "      </SchemaData>     "+
                    "    </ExtendedData>     "+
                    "    <Point>       "+
                    "      <coordinates>-122.000,37.002</coordinates>     "+
                    "    </Point>   "+
                    "  </Placemark>    "+
                    "  <Placemark>     "+
                    "    <name>Difficult trail</name>"+
                    "    <ExtendedData>"+
                    "      <SchemaData schemaUrl='#TrailHeadTypeId'>         "+
                    "        <SimpleData name='TrailHeadName'>Mount Everest</SimpleData>        "+
                    "        <SimpleData name='TrailLength'>347.45</SimpleData>         "+
                    "        <SimpleData name='ElevationGain'>10000</SimpleData>       "+
                    "      </SchemaData>     "+
                    "    </ExtendedData>    "+
                    "    <Point>       "+
                    "      <coordinates>-121.998,37.0078</coordinates>     "+
                    "    </Point>   "+
                    "  </Placemark>   "+
                    "  <!-- old style schema -->"+
                    "  <TrailHeadType>"+
                    "    <TrailHeadName>Flatty Flats</TrailHeadName>"+
                    "    <TrailLength>1</TrailLength>         "+
                    "    <ElevationGain>0</ElevationGain>       "+
                    "    <Point>       "+
                    "      <coordinates>1,1</coordinates>     "+
                    "    </Point>   "+
                    "  </TrailHeadType>"+
                    "</Document> "+
                "</kml>";
        buildDocument(xml);

        SimpleFeature doc = (SimpleFeature)parse();
        List<SimpleFeature> features = (List<SimpleFeature>) doc.getAttribute("Feature");
        assertEquals(3, features.size());

        SimpleFeature f = features.get(0);
        assertEquals("Pi in the sky", f.getAttribute("TrailHeadName"));
        assertEquals(3.14159, f.getAttribute("TrailLength"));
        assertEquals(10, f.getAttribute("ElevationGain"));

        SimpleFeatureType t = f.getType();
        assertEquals(String.class, t.getDescriptor("TrailHeadName").getType().getBinding());
        assertEquals(Double.class, t.getDescriptor("TrailLength").getType().getBinding());
        assertEquals(Integer.class, t.getDescriptor("ElevationGain").getType().getBinding());

        f = features.get(2);
        assertEquals("Flatty Flats", f.getAttribute("TrailHeadName"));
        assertEquals(1.0, f.getAttribute("TrailLength"));
        assertEquals(0, f.getAttribute("ElevationGain"));
        // verify it inherits Style
        assertTrue(f.getFeatureType().getDescriptor("Style") != null);
    }

    public void testIconStyle() throws Exception {
        String xml = "<Style>" + "<IconStyle>" + "<Icon>" + "<href>uri</href>" + "</Icon>" + "</IconStyle>" + "</Style>";

        buildDocument(xml);

        FeatureTypeStyle style = (FeatureTypeStyle) parse();
        assertNull(style.getName());
        Symbolizer[] syms = style.rules().get(0).getSymbolizers();

        assertEquals(1, syms.length);
        PointSymbolizer p = (PointSymbolizer) syms[0];
        String uri = p.getGraphic().getExternalGraphics()[0].getOnlineResource().getLinkage().toString();
        assertEquals("uri", uri);
    }
    
    public void testEmbeddedIconStyle() throws Exception {
        String xml = "<Placemark>" + "<Style>" + "<IconStyle>" + "<Icon>" + "<href>uri</href>" + "</Icon>" + "</IconStyle>" + "</Style>" + "</Placemark>";
        
        buildDocument(xml);
        
        SimpleFeature sf = (SimpleFeature) parse();

        FeatureTypeStyle style = (FeatureTypeStyle) sf.getAttribute("Style");

        // it shouldn't have a name
        assertNull(style.getName());
        // @todo verify registered in StyleMap from configuration
    }

    public void testNetworkLink() throws Exception {
        String xml = "<NetworkLink>" + "<Link>" + "<href>uri</href>" + "</Link>" + "</NetworkLink>";

        buildDocument(xml);

        NetworkLink link = (NetworkLink) parse();

        assertEquals(link.getHref(), "uri");
    }

    public void testLenientGeometryParsing() throws Exception {
        KMLConfiguration config = new KMLConfiguration();
        config.setLenientGeometryParsing(true);
        PullParser p = new PullParser(config,
            getClass().getResourceAsStream("badGeometries.kml"), KML.Placemark);

        Object parsed = null;
        while ((parsed = p.parse()) != null) {
            assertTrue(((SimpleFeature) parsed).getDefaultGeometry() == null);
        }
        assertEquals(Arrays.asList(
                "line 20 : Invalid number of coordinates",
                "line 26 : Invalid number of coordinates",
                "line 34 : Invalid number of coordinates",
                "line 48 : Invalid number of points in LinearRing (found 3 - must be 0 or >= 4)",
                "line 60 : Points of LinearRing do not form a closed linestring",
                "line 74 : Points of LinearRing do not form a closed linestring",
                "line 94 : Points of LinearRing do not form a closed linestring"
        ), config.getParseWarnings());
    }

    SimpleFeature parseSamples() throws Exception {
        Parser p = new Parser(createConfiguration());
        SimpleFeature doc =
            (SimpleFeature) p.parse(getClass().getResourceAsStream("KML_Samples.kml"));
        return doc;
    }
}
