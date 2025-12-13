-- ============================================================================
-- Backup and Recovery Configuration
-- ============================================================================
-- This script sets up database-level backup strategies and recovery procedures

-- ============================================================================
-- BACKUP TABLES (Point-in-time recovery support)
-- ============================================================================

-- Create backup schema for archived data
CREATE SCHEMA IF NOT EXISTS backup;

-- Function to archive old audit logs (older than 1 year)
CREATE OR REPLACE FUNCTION archive_old_audit_logs()
RETURNS INTEGER AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    -- Move old audit logs to backup schema
    CREATE TABLE IF NOT EXISTS backup.audit_logs_archive (
        LIKE audit_logs INCLUDING ALL
    );
    
    INSERT INTO backup.audit_logs_archive
    SELECT * FROM audit_logs
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 year';
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    -- Delete archived records from main table
    DELETE FROM audit_logs
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 year';
    
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

-- Function to archive old analytics (older than 6 months)
CREATE OR REPLACE FUNCTION archive_old_analytics()
RETURNS INTEGER AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    CREATE TABLE IF NOT EXISTS backup.user_analytics_archive (
        LIKE user_analytics INCLUDING ALL
    );
    
    INSERT INTO backup.user_analytics_archive
    SELECT * FROM user_analytics
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '6 months';
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    DELETE FROM user_analytics
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '6 months';
    
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- DATA INTEGRITY FUNCTIONS
-- ============================================================================

-- Function to check database health
CREATE OR REPLACE FUNCTION check_database_health()
RETURNS TABLE (
    check_name VARCHAR,
    status VARCHAR,
    message TEXT,
    value BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'Total Users'::VARCHAR,
        CASE WHEN COUNT(*) > 0 THEN 'OK' ELSE 'WARNING' END,
        'Total user count'::TEXT,
        COUNT(*)::BIGINT
    FROM users
    
    UNION ALL
    
    SELECT 
        'Orphaned Profiles'::VARCHAR,
        CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'ERROR' END,
        'Profiles without users'::TEXT,
        COUNT(*)::BIGINT
    FROM profiles p
    LEFT JOIN users u ON p.user_id = u.id
    WHERE u.id IS NULL
    
    UNION ALL
    
    SELECT 
        'Orphaned Applications'::VARCHAR,
        CASE WHEN COUNT(*) = 0 THEN 'OK' ELSE 'ERROR' END,
        'Applications without jobs'::TEXT,
        COUNT(*)::BIGINT
    FROM applications a
    LEFT JOIN jobs j ON a.job_id = j.id
    WHERE j.id IS NULL
    
    UNION ALL
    
    SELECT 
        'Inactive Jobs'::VARCHAR,
        'INFO'::VARCHAR,
        'Jobs not active for 90+ days'::TEXT,
        COUNT(*)::BIGINT
    FROM jobs
    WHERE status != 'ACTIVE' 
    AND updated_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- PERFORMANCE MONITORING
-- ============================================================================

-- Table to track slow queries (if pg_stat_statements is enabled)
-- This would require pg_stat_statements extension
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Function to get table sizes
CREATE OR REPLACE FUNCTION get_table_sizes()
RETURNS TABLE (
    table_name VARCHAR,
    table_size TEXT,
    indexes_size TEXT,
    total_size TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        schemaname||'.'||tablename::VARCHAR,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))::TEXT,
        pg_size_pretty(pg_indexes_size(schemaname||'.'||tablename))::TEXT,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))::TEXT
    FROM pg_tables
    WHERE schemaname = 'public'
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON FUNCTION archive_old_audit_logs IS 'Archive audit logs older than 1 year to backup schema';
COMMENT ON FUNCTION archive_old_analytics IS 'Archive analytics older than 6 months to backup schema';
COMMENT ON FUNCTION check_database_health IS 'Check database integrity and health metrics';
COMMENT ON FUNCTION get_table_sizes IS 'Get size information for all tables';

