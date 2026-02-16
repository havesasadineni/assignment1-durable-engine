package com.durable.engine;

import java.sql.*;
import java.time.Instant;

public class StepStore {
    private final String jdbcUrl;

    public StepStore(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        init();
    }

    private Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(jdbcUrl);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA synchronous=NORMAL;");
            st.execute("PRAGMA busy_timeout=5000;");
        }
        return c;
    }

    private void init() {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS steps (
                  workflow_id TEXT NOT NULL,
                  step_key    TEXT NOT NULL,
                  status      TEXT NOT NULL,
                  output_json TEXT,
                  error_msg   TEXT,
                  started_at  INTEGER,
                  updated_at  INTEGER,
                  PRIMARY KEY (workflow_id, step_key)
                );
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init DB", e);
        }
    }

    public StepRecord get(String workflowId, String stepKey) throws SQLException {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT workflow_id, step_key, status, output_json, error_msg, started_at, updated_at " +
                             "FROM steps WHERE workflow_id=? AND step_key=?")) {
            ps.setString(1, workflowId);
            ps.setString(2, stepKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new StepRecord(
                        rs.getString(1),
                        rs.getString(2),
                        StepStatus.valueOf(rs.getString(3)),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getLong(6),
                        rs.getLong(7)
                );
            }
        }
    }

    /** Insert RUNNING row if not exists. Returns true if inserted, false if already existed. */
    public boolean tryInsertRunning(String workflowId, String stepKey) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT OR IGNORE INTO steps(workflow_id, step_key, status, started_at, updated_at) VALUES(?,?,?,?,?)")) {
            ps.setString(1, workflowId);
            ps.setString(2, stepKey);
            ps.setString(3, StepStatus.RUNNING.name());
            ps.setLong(4, now);
            ps.setLong(5, now);
            int changed = ps.executeUpdate();
            return changed == 1;
        }
    }

    public void markCompleted(String workflowId, String stepKey, String outputJson) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE steps SET status=?, output_json=?, error_msg=NULL, updated_at=? WHERE workflow_id=? AND step_key=?")) {
            ps.setString(1, StepStatus.COMPLETED.name());
            ps.setString(2, outputJson);
            ps.setLong(3, now);
            ps.setString(4, workflowId);
            ps.setString(5, stepKey);
            ps.executeUpdate();
        }
    }

    public void markFailed(String workflowId, String stepKey, String errorMsg) throws SQLException {
        long now = Instant.now().toEpochMilli();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE steps SET status=?, error_msg=?, updated_at=? WHERE workflow_id=? AND step_key=?")) {
            ps.setString(1, StepStatus.FAILED.name());
            ps.setString(2, errorMsg);
            ps.setLong(3, now);
            ps.setString(4, workflowId);
            ps.setString(5, stepKey);
            ps.executeUpdate();
        }
    }
}
