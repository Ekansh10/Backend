-- ============================================================================
-- Gradia Database Schema - Cold Tables (Less Frequently Accessed)
-- ============================================================================
-- These tables store historical data, analytics, and audit information.
-- Optimized for write performance and long-term storage.

-- ============================================================================
-- AUDIT AND LOGGING TABLES (Cold Data)
-- ============================================================================

-- Audit logs - Track all important changes
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    entity_type VARCHAR(100) NOT NULL, -- users, profiles, jobs, applications
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, LOGIN, LOGOUT
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- Create partitions for audit logs (monthly partitions)
CREATE TABLE audit_logs_2025_12 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');
CREATE TABLE audit_logs_2026_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE audit_logs_2026_02 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

-- Indexes for audit logs
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_ip ON audit_logs(ip_address);

-- ============================================================================
-- ANALYTICS TABLES (Cold Data)
-- ============================================================================

-- User activity analytics
CREATE TABLE user_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL, -- page_view, search, application, profile_update
    event_data JSONB,
    session_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- Create partitions for analytics (monthly)
CREATE TABLE user_analytics_2025_12 PARTITION OF user_analytics
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');
CREATE TABLE user_analytics_2026_01 PARTITION OF user_analytics
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

-- Indexes for analytics
CREATE INDEX idx_user_analytics_user ON user_analytics(user_id);
CREATE INDEX idx_user_analytics_event ON user_analytics(event_type);
CREATE INDEX idx_user_analytics_created ON user_analytics(created_at DESC);
CREATE INDEX idx_user_analytics_session ON user_analytics(session_id);

-- Job analytics - Track job performance
CREATE TABLE job_analytics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    views INTEGER DEFAULT 0,
    applications INTEGER DEFAULT 0,
    shares INTEGER DEFAULT 0,
    saves INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(job_id, date)
) PARTITION BY RANGE (date);

-- Create partitions for job analytics (monthly)
CREATE TABLE job_analytics_2025_12 PARTITION OF job_analytics
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');
CREATE TABLE job_analytics_2026_01 PARTITION OF job_analytics
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

-- Indexes for job analytics
CREATE INDEX idx_job_analytics_job ON job_analytics(job_id);
CREATE INDEX idx_job_analytics_date ON job_analytics(date DESC);

-- ============================================================================
-- NOTIFICATION TABLES (Warm Data)
-- ============================================================================

-- Notifications - User notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(100) NOT NULL, -- application_status, job_match, message, system
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(500),
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for notifications
CREATE INDEX idx_notifications_user ON notifications(user_id) WHERE is_read = false;
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
CREATE INDEX idx_notifications_type ON notifications(type);

-- ============================================================================
-- SEARCH AND MATCHING TABLES (Warm Data)
-- ============================================================================

-- Saved searches - User saved job searches
CREATE TABLE saved_searches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    search_name VARCHAR(255),
    search_criteria JSONB NOT NULL, -- Store search filters as JSON
    is_active BOOLEAN DEFAULT true,
    last_run_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Job matches - AI/algorithm based job matches
CREATE TABLE job_matches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    candidate_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    match_score DECIMAL(5, 2) NOT NULL, -- 0-100
    match_reasons TEXT[],
    is_viewed BOOLEAN DEFAULT false,
    is_applied BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(candidate_id, job_id)
);

-- Indexes for matching
CREATE INDEX idx_saved_searches_user ON saved_searches(user_id) WHERE is_active = true;
CREATE INDEX idx_job_matches_candidate ON job_matches(candidate_id) WHERE is_applied = false;
CREATE INDEX idx_job_matches_score ON job_matches(match_score DESC);

-- ============================================================================
-- ADMIN TABLES (Warm Data)
-- ============================================================================

-- Admin actions - Track admin operations
CREATE TABLE admin_actions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type VARCHAR(100) NOT NULL,
    target_entity_type VARCHAR(100),
    target_entity_id UUID,
    action_details JSONB,
    ip_address INET,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- System settings - Application configuration
CREATE TABLE system_settings (
    key VARCHAR(255) PRIMARY KEY,
    value JSONB NOT NULL,
    description TEXT,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for admin
CREATE INDEX idx_admin_actions_admin ON admin_actions(admin_id);
CREATE INDEX idx_admin_actions_created ON admin_actions(created_at DESC);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE audit_logs IS 'Audit trail for all important changes - Cold data, partitioned by month';
COMMENT ON TABLE user_analytics IS 'User activity analytics - Cold data, partitioned by month';
COMMENT ON TABLE job_analytics IS 'Job performance analytics - Cold data, partitioned by month';
COMMENT ON TABLE notifications IS 'User notifications - Warm data';
COMMENT ON TABLE saved_searches IS 'User saved job searches - Warm data';
COMMENT ON TABLE job_matches IS 'AI-based job matches for candidates - Warm data';
COMMENT ON TABLE admin_actions IS 'Admin operation tracking - Warm data';
COMMENT ON TABLE system_settings IS 'System configuration settings';

