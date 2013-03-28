package org.geotools.data.teradata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCTestSetup;
import org.geotools.jdbc.JDBCTestSupport;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test our special handling of SRID.
 *
 * Teradata has a mapping table from EPSG codes to internal/native codes. For
 * example, EPSG:4326 maps to TD:1619
 *
 * A problem arises when geometries in the table are encoded with a native SRID
 * different than what is declared in the geometry columns. This causes an issue
 * when a spatial query is issued that does not match the geometry SRID. The
 * approach we will take is: 1) get the geometry column native SRID 2) look at
 * the first geometry in the table and see if it has an SRID set
 *
 * If 2 succeeds, verify that this is the same as the geometry column. If not,
 * warn about the discrepancy but use the geometry SRID. Otherwise, we fall back
 * to 1 for externally identifying the SRID but internally we will use an SRID
 * of 0 (or just not specifying it) for querying.
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class TeradataSRIDTest extends JDBCTestSupport {

    static {
        Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
        Logger.getLogger("org.geotools.jdbc").setLevel(Level.FINE);
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new TeradataSRIDTestSetup();
    }

    public void testBboxFilter() throws Exception {
        assertBboxEntries("srid_in_geom");
        assertBboxEntries("srid_in_geom_no_match");
        assertBboxEntries("srid_in_meta_only");
    }

    private void assertBboxEntries(String typeName) throws Exception {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        // should match only "r2"
        BBOX bbox = ff.bbox(aname("geometry"), 0, 0, 2, 2, "EPSG:4326");
        FeatureCollection features = dataStore.getFeatureSource(tname(typeName)).getFeatures(bbox);
        FeatureIterator it = features.features();
        int cnt = 0;
        try {
            while (it.hasNext()) {
                it.next();
                cnt++;
            }
        } finally {
            it.close();
        }
        assertEquals(2, cnt);
    }

    private void assertAttributeSRID(String typename, Integer expectedSRID, Integer expectedCrs) throws Exception {
        SimpleFeatureType schema = dataStore.getSchema(typename);
        CoordinateReferenceSystem crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
        Integer nativeSRID = (Integer) schema.getGeometryDescriptor().getUserData().get(JDBCDataStore.JDBC_NATIVE_SRID);
        assertEquals(expectedSRID, nativeSRID);

        Connection connection = dataStore.getConnection(Transaction.AUTO_COMMIT);
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("select SRTEXT from sysspatial.spatial_ref_sys where AUTH_SRID=" + expectedCrs);
        rs.next();
        CoordinateReferenceSystem expectedCRS = CRS.parseWKT(rs.getString(1));
        assertEquals(expectedCRS, crs);
        rs.close();
        st.close();
        connection.close();
    }

    public void testSRIDInGeom() throws Exception {
        assertAttributeSRID("srid_in_geom", 1619, 4326);
    }

    public void testSRIDInGeomNoMatch() throws Exception {
        assertAttributeSRID("srid_in_geom_no_match", 2191, 26754);
    }

    public void testSRIDInMetaOnly() throws Exception {
        assertAttributeSRID("srid_in_meta_only", null, 4326);
    }

    static class TeradataSRIDTestSetup extends TeradataTestSetup {

        private void createTable(String name, Integer internalSRID, Integer geomSRID) throws Exception {
            runSafe("drop table " + name);
            runSafe(String.format("DELETE FROM SYSSPATIAL.GEOMETRY_COLUMNS WHERE F_TABLE_NAME = '%s'", name));
            run(String.format("CREATE TABLE \"%s\"(" //
                    + "\"id\" PRIMARY KEY not null integer, " //
                    + "\"geometry\" ST_GEOMETRY)", name));
            run(String.format("INSERT INTO SYSSPATIAL.GEOMETRY_COLUMNS (F_TABLE_CATALOG, F_TABLE_SCHEMA, F_TABLE_NAME,"
                    + " F_GEOMETRY_COLUMN, COORD_DIMENSION, SRID, GEOM_TYPE) VALUES ('"
                    + fixture.getProperty("database") + "', '" + fixture.getProperty("schema")
                    + "', '%s', 'geometry', 2, " + internalSRID + ", 'POINT')", name));
            run(String.format("INSERT INTO \"%s\" VALUES(0, 'POINT(0 0)')", name));
            run(String.format("INSERT INTO \"%s\" VALUES(1, 'POINT(1 1)')", name));
            if (geomSRID != null) {
                run(String.format("UPDATE \"%s\" set geometry.st_srid = %s", name, geomSRID));
            }
        }

        @Override
        protected void setUpData() throws Exception {
            createTable("srid_in_geom", srid4326, srid4326);
            createTable("srid_in_geom_no_match", srid4326, 2191);
            createTable("srid_in_meta_only", srid4326, null);
        }
    }
}
