package org.geotools.data.teradata;

import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCTestSetup;
import org.geotools.jdbc.JDBCTestSupport;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
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

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new TeradataSRIDTestSetup();
    }

    public void testComputeActiveSRID() {
        Integer declared = 1913;
        Integer geom = null;

        assertEquals(declared, TeradataDialect.computeActiveSRID("foo", declared, geom)[0]);
        geom = 2700;
        assertEquals(geom, TeradataDialect.computeActiveSRID("foo", declared, geom)[0]);
    }

    private void assertAttributeSRID(String typename, Integer expected) throws Exception {
        SimpleFeatureType schema = dataStore.getSchema(typename);
        CoordinateReferenceSystem crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
        Integer nativeSRID = (Integer) schema.getGeometryDescriptor().getUserData().get(JDBCDataStore.JDBC_NATIVE_SRID);
        assertEquals(expected, nativeSRID);
        CoordinateReferenceSystem expectedCRS = CRS.decode("EPSG:" + expected);
        assertEquals(expectedCRS, crs);
    }

    public void testSRIDInGeom() throws Exception {
        assertAttributeSRID("srid_in_geom", 4326);
    }

    public void testSRIDInGeomNoMatch() throws Exception {
        assertAttributeSRID("srid_in_geom_no_match", 26754);
    }

    static class TeradataSRIDTestSetup extends TeradataTestSetup {

        @Override
        protected void setUpData() throws Exception {
            runSafe("drop table srid_in_geom");
            runSafe("DELETE FROM SYSSPATIAL.GEOMETRY_COLUMNS WHERE F_TABLE_NAME = 'srid_in_geom'");
            run("CREATE TABLE \"srid_in_geom\"(" //
                    + "\"id\" PRIMARY KEY not null integer, " //
                    + "\"geometry\" ST_GEOMETRY)");
            run("INSERT INTO SYSSPATIAL.GEOMETRY_COLUMNS (F_TABLE_CATALOG, F_TABLE_SCHEMA, F_TABLE_NAME,"
                    + " F_GEOMETRY_COLUMN, COORD_DIMENSION, SRID, GEOM_TYPE) VALUES ('"
                    + fixture.getProperty("database") + "', '" + fixture.getProperty("schema")
                    + "', 'srid_in_geom', 'geometry', 2, " + srid4326 + ", 'POINT')");
            run("INSERT INTO \"srid_in_geom\" VALUES(0, 'POINT(0 0)')");
            run("INSERT INTO \"srid_in_geom\" VALUES(1, 'POINT(1 1)')");
            run("UPDATE srid_in_geom set geometry.st_srid = 1619");


            runSafe("drop table srid_in_geom_no_match");
            runSafe("DELETE FROM SYSSPATIAL.GEOMETRY_COLUMNS WHERE F_TABLE_NAME = 'srid_in_geom_no_match'");

            run("CREATE TABLE \"srid_in_geom_no_match\"(" //
                    + "\"id\" PRIMARY KEY not null integer, " //
                    + "\"geometry\" ST_GEOMETRY)");
            run("INSERT INTO SYSSPATIAL.GEOMETRY_COLUMNS (F_TABLE_CATALOG, F_TABLE_SCHEMA, F_TABLE_NAME,"
                    + " F_GEOMETRY_COLUMN, COORD_DIMENSION, SRID, GEOM_TYPE) VALUES ('"
                    + fixture.getProperty("database") + "', '" + fixture.getProperty("schema")
                    + "', 'srid_in_geom_no_match', 'geometry', 2, " + srid4326 + ", 'POINT')");
            run("INSERT INTO \"srid_in_geom_no_match\" VALUES(0, 'POINT(0 0)')");
            run("INSERT INTO \"srid_in_geom_no_match\" VALUES(1, 'POINT(1 1)')");
            run("UPDATE srid_in_geom_no_match set geometry.st_srid = 2191");
        }
    }
}
