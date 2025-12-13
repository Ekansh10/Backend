-- ============================================================================
-- Additional Indexes and Constraints for Performance and Data Integrity
-- ============================================================================

-- ============================================================================
-- FULL TEXT SEARCH INDEXES
-- ============================================================================

-- Full text search for profiles
CREATE INDEX idx_profiles_fulltext ON profiles 
    USING GIN(to_tsvector('english', 
        COALESCE(full_name, '') || ' ' || 
        COALESCE(location, '') || ' ' || 
        COALESCE(preferred_role, '')
    ));

-- Full text search for jobs
CREATE INDEX idx_jobs_fulltext ON jobs 
    USING GIN(to_tsvector('english', 
        COALESCE(job_title, '') || ' ' || 
        COALESCE(description, '') || ' ' || 
        COALESCE(requirements, '')
    )) WHERE status = 'ACTIVE';

-- ============================================================================
-- COMPOSITE INDEXES FOR COMMON QUERIES
-- ============================================================================

-- Common profile queries
CREATE INDEX idx_profiles_role_status ON profiles(profile_status, experience_level) 
    WHERE profile_status = 'COMPLETE';

-- Common job queries
CREATE INDEX idx_jobs_status_location ON jobs(status, location) 
    WHERE status = 'ACTIVE';

CREATE INDEX idx_jobs_status_posted ON jobs(status, posted_date DESC) 
    WHERE status = 'ACTIVE';

-- Application status tracking
CREATE INDEX idx_applications_candidate_status ON applications(candidate_id, status);
CREATE INDEX idx_applications_job_status ON applications(job_id, status);

-- ============================================================================
-- UNIQUE CONSTRAINTS
-- ============================================================================

-- Ensure one active profile per user
-- (Already enforced by UNIQUE constraint on profiles.user_id)

-- ============================================================================
-- CHECK CONSTRAINTS FOR DATA VALIDITY
-- ============================================================================

-- Email validation (basic)
ALTER TABLE users ADD CONSTRAINT check_email_format 
    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Salary range validation
ALTER TABLE jobs ADD CONSTRAINT check_salary_range 
    CHECK (salary_range_min IS NULL OR salary_range_max IS NULL OR salary_range_min <= salary_range_max);

-- Date validation
ALTER TABLE jobs ADD CONSTRAINT check_dates 
    CHECK (closing_date IS NULL OR posted_date IS NULL OR closing_date >= posted_date);

-- ============================================================================
-- MATERIALIZED VIEWS FOR REPORTING (Cold Data Queries)
-- ============================================================================

-- User statistics view
CREATE MATERIALIZED VIEW user_statistics AS
SELECT 
    role,
    COUNT(*) as total_users,
    COUNT(*) FILTER (WHERE is_active = true) as active_users,
    COUNT(*) FILTER (WHERE email_verified = true) as verified_users,
    COUNT(*) FILTER (WHERE last_login > CURRENT_TIMESTAMP - INTERVAL '30 days') as active_last_30_days
FROM users
GROUP BY role;

CREATE UNIQUE INDEX ON user_statistics(role);

-- Job statistics view
CREATE MATERIALIZED VIEW job_statistics AS
SELECT 
    status,
    COUNT(*) as total_jobs,
    SUM(views_count) as total_views,
    SUM(applications_count) as total_applications,
    AVG(applications_count) as avg_applications_per_job
FROM jobs
GROUP BY status;

CREATE UNIQUE INDEX ON job_statistics(status);

-- Refresh function for materialized views
CREATE OR REPLACE FUNCTION refresh_statistics()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY user_statistics;
    REFRESH MATERIALIZED VIEW CONCURRENTLY job_statistics;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTIONS FOR COMMON OPERATIONS
-- ============================================================================

-- Function to get user profile with metadata
CREATE OR REPLACE FUNCTION get_user_profile(p_user_id UUID)
RETURNS TABLE (
    profile_id UUID,
    user_id UUID,
    full_name VARCHAR,
    email VARCHAR,
    mobile VARCHAR,
    location VARCHAR,
    experience_level VARCHAR,
    preferred_role VARCHAR,
    bio TEXT,
    skills TEXT[],
    company_name VARCHAR,
    company_description TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.id,
        p.user_id,
        p.full_name,
        p.email,
        p.mobile,
        p.location,
        p.experience_level,
        p.preferred_role,
        pm.bio,
        pm.skills,
        ep.company_name,
        ep.company_description
    FROM profiles p
    LEFT JOIN profile_metadata pm ON p.id = pm.profile_id
    LEFT JOIN employer_profiles ep ON p.id = ep.profile_id
    WHERE p.user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Function to search jobs
CREATE OR REPLACE FUNCTION search_jobs(
    p_search_text TEXT DEFAULT NULL,
    p_location TEXT DEFAULT NULL,
    p_experience TEXT DEFAULT NULL,
    p_job_type TEXT DEFAULT NULL,
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE (
    job_id UUID,
    job_title VARCHAR,
    company_name VARCHAR,
    location VARCHAR,
    job_type VARCHAR,
    salary_range_min DECIMAL,
    salary_range_max DECIMAL,
    posted_date TIMESTAMP,
    match_score REAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        j.id,
        j.job_title,
        ep.company_name,
        j.location,
        j.job_type,
        j.salary_range_min,
        j.salary_range_max,
        j.posted_date,
        CASE 
            WHEN p_search_text IS NOT NULL THEN
                ts_rank(to_tsvector('english', j.job_title || ' ' || j.description), 
                       plainto_tsquery('english', p_search_text))
            ELSE 1.0
        END as match_score
    FROM jobs j
    LEFT JOIN profiles p ON j.employer_id = p.id
    LEFT JOIN employer_profiles ep ON p.id = ep.profile_id
    WHERE j.status = 'ACTIVE'
        AND (p_search_text IS NULL OR 
             to_tsvector('english', j.job_title || ' ' || j.description) @@ 
             plainto_tsquery('english', p_search_text))
        AND (p_location IS NULL OR j.location ILIKE '%' || p_location || '%')
        AND (p_experience IS NULL OR j.experience_required = p_experience)
        AND (p_job_type IS NULL OR j.job_type = p_job_type)
    ORDER BY match_score DESC, j.posted_date DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON MATERIALIZED VIEW user_statistics IS 'Cached user statistics for reporting';
COMMENT ON MATERIALIZED VIEW job_statistics IS 'Cached job statistics for reporting';
COMMENT ON FUNCTION get_user_profile IS 'Get complete user profile with all related data';
COMMENT ON FUNCTION search_jobs IS 'Full-text search for jobs with filters';

