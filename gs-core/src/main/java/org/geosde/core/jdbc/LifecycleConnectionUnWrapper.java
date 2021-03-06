package org.geosde.core.jdbc;

import java.sql.Connection;
import java.sql.Statement;

import org.geosde.core.jdbc.data.datasource.UnWrapper;

/**
 * Un-wraps the {@link LifecycleConnection} instances
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class LifecycleConnectionUnWrapper implements UnWrapper {

    public boolean canUnwrap(Connection conn) {
        return conn instanceof LifecycleConnection;
    }

    public boolean canUnwrap(Statement st) {
        return false;
    }

    public Connection unwrap(Connection conn) {
        return ((LifecycleConnection) conn).delegate;
    }

    public Statement unwrap(Statement statement) {
        throw new UnsupportedOperationException("This un-wrapper cannot operate on statements");
    }

}