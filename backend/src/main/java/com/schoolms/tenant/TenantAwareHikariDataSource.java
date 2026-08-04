package com.schoolms.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Hikari datasource that stamps every checked-out connection with the current
 * thread's tenant context (session-level custom GUCs), which PostgreSQL RLS
 * policies evaluate. Values always come from {@link TenantContext}, which is
 * populated exclusively from the signed JWT.
 */
public class TenantAwareHikariDataSource extends HikariDataSource {

    public TenantAwareHikariDataSource() {
        super();
    }

    public TenantAwareHikariDataSource(HikariConfig configuration) {
        super(configuration);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applyTenantContext(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applyTenantContext(connection);
        return connection;
    }

    private void applyTenantContext(Connection connection) throws SQLException {
        UUID schoolId = TenantContext.getSchoolId();
        boolean bypass = TenantContext.isBypassRls();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT set_config('app.school_id', ?, false), set_config('app.bypass_rls', ?, false)")) {
            ps.setString(1, schoolId == null ? "" : schoolId.toString());
            ps.setString(2, bypass ? "true" : "false");
            ps.execute();
        }
    }
}
