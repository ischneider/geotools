package org.geotools.kml.v22.bindings;

import java.util.List;

import javax.xml.namespace.QName;

import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.Geometries;
import org.geotools.kml.KMLOptions;
import org.geotools.kml.bindings.FeatureTypeBinding;
import static org.geotools.kml.bindings.FeatureTypeBinding.FeatureTypeStyleURI;
import org.geotools.kml.v22.KML;
import org.geotools.kml.v22.SchemaRegistry;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.geotools.xs.XS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.Schema;

/**
 * Binding object for the type http://www.opengis.net/kml/2.2:SchemaType.
 * 
 * <p>
 * 
 * <pre>
 *  <code>
 *  &lt;complexType final="#all" name="SchemaType"&gt;
 *      &lt;sequence&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:SimpleField"/&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0" ref="kml:SchemaExtension"/&gt;
 *      &lt;/sequence&gt;
 *      &lt;attribute name="name" type="string"/&gt;
 *      &lt;attribute name="id" type="ID"/&gt;
 *  &lt;/complexType&gt; 
 * 	
 *   </code>
 * </pre>
 * 
 * </p>
 * 
 * @generated
 */
public class SchemaTypeBinding extends AbstractComplexBinding {

    private SchemaRegistry schemaRegistry;
    private KMLOptions config;

    public SchemaTypeBinding(SchemaRegistry schemaRegistry, KMLOptions config) {
        this.schemaRegistry = schemaRegistry;
        this.config = config;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return KML.SchemaType;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Class getType() {
        return SimpleFeatureType.class;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {

        String featureTypeName = null;
        String featureTypeId = null;

        if (node.hasAttribute("id")) {
            featureTypeId = (String) node.getAttributeValue("id");
        }

        if (node.hasAttribute("name")) {
            featureTypeName = (String) node.getAttributeValue("name");
        }
        else if (featureTypeId != null) {
            featureTypeName = featureTypeId;
        }
        else {
            featureTypeName = "feature";
        }
        
        // old style Schema's had a parent attribute
        // http://code.google.com/p/libkml/wiki/ParsingOldSchemaFiles
        SimpleFeatureType parent = null;
        // we're only dealing with Placemark as parent
        // what happens if the parent is something else???
        // this is not even documented in KML
        if ("Placemark".equals(node.getAttributeValue("parent"))) {
            parent = config.isOnlyCollectStyles() ? FeatureTypeBinding.FeatureTypeStyleURI : FeatureTypeBinding.FeatureType;
        }

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        if (parent != null) {
            tb.init(parent);
        }
        tb.setName(featureTypeName);
        //TODO: crs

        for (Node n : (List<Node>) node.getChildren("SimpleField")) {
            String name = (String) n.getAttributeValue("name");
            String typeName = (String) n.getAttributeValue("type");
            if (name != null && typeName != null) {
                tb.add(name, mapTypeName(typeName));
            }
        }
        SimpleFeatureType featureType = tb.buildFeatureType();
        schemaRegistry.add(featureTypeName, featureType);
        if (featureTypeId != null) {
            schemaRegistry.add(featureTypeId, featureType);
        }
        return featureType;
    }

    private Class mapTypeName(String typeName) {
        //try xs simple type
        Schema xsTypeMappingProfile = XS.getInstance().getTypeMappingProfile();
        NameImpl name = new NameImpl(XS.NAMESPACE, typeName);
        if (xsTypeMappingProfile.containsKey(name)) {
            AttributeType type = xsTypeMappingProfile.get(name);
            if (type.getBinding() != null) {
                return type.getBinding();
            }
        }

        //try gml geometry types
        Geometries g = Geometries.getForName(typeName);
        if (g != null) {
            return g.getBinding();
        }

        //default
        return String.class;
    }

}
