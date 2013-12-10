package org.geotools.jdbc;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class JDBCBulkFeatureWriter implements Closeable {
    private final SimpleFeatureType featureType;
    private final Set<String> pkColumnNames;
    private final PrimaryKey key;
    private final Transaction tx;
    private final Connection cx;
    private final JDBCDataStore store;
    private PreparedStatement preparedStatement;
    private final PreparedStatementSQLDialect dialect;
    private final List<Object> keyValues;
    private int batchCount = 0;

    public JDBCBulkFeatureWriter(SimpleFeatureType featureType, PrimaryKey key, PreparedStatementSQLDialect dialect, JDBCDataStore store) throws SQLException, IOException {
        this.featureType = featureType;
        this.key = key;
        this.dialect = dialect;
        tx = new DefaultTransaction();
        cx = store.getConnection(tx);
        pkColumnNames = store.getColumnNames(key);
        this.store = store;
        this.keyValues = store.getNextValues(key, cx);
    }

    public void write(SimpleFeature feature) throws IOException, SQLException {
        if (preparedStatement == null) {
            preparedStatement = store.createInsertPreparedStatement(dialect, featureType, feature, key, pkColumnNames, false, cx);
            dialect.onInsert(preparedStatement, cx, featureType);
        }
        store.setPreparedStatementAttributes(dialect, preparedStatement, featureType, feature, pkColumnNames, key, false, keyValues);
        preparedStatement.addBatch();
        if (batchCount++ < 1000) {
            preparedStatement.executeBatch();
            batchCount = 0;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (batchCount > 0) {
                try {
                    preparedStatement.executeBatch();
                } catch (SQLException ex) {
                    tx.rollback();
                    throw new IOException(ex);
                }
            }
            tx.commit();
        } finally {
            tx.close();
            store.closeSafe(cx);
            store.closeSafe(preparedStatement);
        }
    }

}
