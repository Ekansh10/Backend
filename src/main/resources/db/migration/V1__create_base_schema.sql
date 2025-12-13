-- ============================================================================
-- Gradia Database Schema - Base Tables (Hot Data - Frequently Accessed)
-- ============================================================================
-- These tables store frequently accessed data and are optimized for read/write
-- performance with proper indexing and partitioning strategies.

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- For text search

-- ============================================================================
-- CORE USER TABLES (Hot Data)
-- ============================================================================

-- Users table - Core authentication and user management
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('CANDIDATE', 'EMPLOYER', 'ADMIN', 'SPONSOR')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    last_login TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1 -- For optimistic locking
);

-- Indexes for users (hot queries)
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role) WHERE is_active = true;
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = true;
CREATE INDEX idx_users_last_login ON users(last_login DESC) WHERE is_active = true;

-- ============================================================================
-- PROFILE TABLES (Hot Data - Split for Performance)
-- ============================================================================

-- Profiles - Core profile information (frequently accessed)
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile VARCHAR(50),
    location VARCHAR(255),
    linkedin VARCHAR(500),
    experience_level VARCHAR(100),
    preferred_role VARCHAR(255),
    profile_status VARCHAR(50) DEFAULT 'INCOMPLETE' CHECK (profile_status IN ('INCOMPLETE', 'COMPLETE', 'VERIFIED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1
);

-- Profile metadata - Extended information (less frequently accessed)
CREATE TABLE profile_metadata (
    profile_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
    bio TEXT,
    skills TEXT[],
    certifications TEXT[],
    languages TEXT[],
    availability_status VARCHAR(50),
    salary_expectation_min DECIMAL(10, 2),
    salary_expectation_max DECIMAL(10, 2),
    work_preference TEXT[], -- remote, hybrid, on-site
    notice_period INTEGER, -- days
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Employer specific profile data
CREATE TABLE employer_profiles (
    profile_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    company_description TEXT,
    company_size VARCHAR(50), -- startup, small, medium, large, enterprise
    industry VARCHAR(255),
    website VARCHAR(500),
    founded_year INTEGER,
    headquarters_location VARCHAR(255),
    company_type VARCHAR(100), -- private, public, nonprofit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for profiles
CREATE INDEX idx_profiles_user_id ON profiles(user_id);
CREATE INDEX idx_profiles_email ON profiles(email);
CREATE INDEX idx_profiles_status ON profiles(profile_status);
CREATE INDEX idx_profiles_location ON profiles(location) WHERE location IS NOT NULL;
CREATE INDEX idx_profiles_experience ON profiles(experience_level) WHERE experience_level IS NOT NULL;
CREATE INDEX idx_employer_profiles_company ON employer_profiles(company_name);

-- ============================================================================
-- FILE STORAGE METADATA (Hot Data - File References)
-- ============================================================================

-- File metadata table - Stores references to uploaded files
CREATE TABLE file_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    file_type VARCHAR(50) NOT NULL CHECK (file_type IN ('PROFILE_PICTURE', 'RESUME', 'COVER_LETTER', 'CERTIFICATE', 'PORTFOLIO', 'OTHER')),
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL, -- Storage path (S3, local, etc.)
    file_size BIGINT NOT NULL, -- bytes
    mime_type VARCHAR(100),
    storage_provider VARCHAR(50) DEFAULT 'LOCAL', -- LOCAL, S3, GCS, etc.
    storage_bucket VARCHAR(255),
    is_public BOOLEAN DEFAULT false,
    upload_status VARCHAR(50) DEFAULT 'COMPLETED' CHECK (upload_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1,
    CONSTRAINT unique_profile_file_type UNIQUE (profile_id, file_type) -- One file per type per profile
);

-- Indexes for file metadata
CREATE INDEX idx_file_metadata_profile ON file_metadata(profile_id);
CREATE INDEX idx_file_metadata_type ON file_metadata(file_type);
CREATE INDEX idx_file_metadata_status ON file_metadata(upload_status);
CREATE INDEX idx_file_metadata_storage ON file_metadata(storage_provider, storage_bucket);

-- ============================================================================
-- JOB TABLES (Hot Data)
-- ============================================================================

-- Jobs - Active job postings (frequently accessed)
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employer_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    job_title VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    description TEXT NOT NULL,
    requirements TEXT,
    experience_required VARCHAR(100),
    skills TEXT[],
    job_type VARCHAR(50), -- full-time, part-time, contract, internship
    location VARCHAR(255),
    salary_range_min DECIMAL(10, 2),
    salary_range_max DECIMAL(10, 2),
    currency VARCHAR(10) DEFAULT 'USD',
    status VARCHAR(50) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED', 'PAUSED')),
    posted_date TIMESTAMP,
    closing_date TIMESTAMP,
    views_count INTEGER DEFAULT 0,
    applications_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1
);

-- Job metadata - Extended job information
CREATE TABLE job_metadata (
    job_id UUID PRIMARY KEY REFERENCES jobs(id) ON DELETE CASCADE,
    benefits TEXT[],
    work_culture TEXT,
    growth_opportunities TEXT,
    team_size INTEGER,
    reporting_to VARCHAR(255),
    travel_required BOOLEAN DEFAULT false,
    visa_sponsorship BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for jobs
CREATE INDEX idx_jobs_employer ON jobs(employer_id);
CREATE INDEX idx_jobs_status ON jobs(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_jobs_location ON jobs(location) WHERE status = 'ACTIVE';
CREATE INDEX idx_jobs_posted_date ON jobs(posted_date DESC) WHERE status = 'ACTIVE';
CREATE INDEX idx_jobs_skills ON jobs USING GIN(skills) WHERE status = 'ACTIVE';
CREATE INDEX idx_jobs_title_search ON jobs USING GIN(to_tsvector('english', job_title)) WHERE status = 'ACTIVE';

-- ============================================================================
-- APPLICATION TABLES (Hot Data)
-- ============================================================================

-- Applications - Job applications (frequently accessed)
CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REVIEWED', 'SHORTLISTED', 'INTERVIEW', 'OFFERED', 'REJECTED', 'WITHDRAWN')),
    cover_letter TEXT,
    applied_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by UUID REFERENCES users(id),
    version INTEGER DEFAULT 1,
    CONSTRAINT unique_job_candidate UNIQUE (job_id, candidate_id)
);

-- Application history - Track status changes
CREATE TABLE application_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by UUID REFERENCES users(id),
    notes TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for applications
CREATE INDEX idx_applications_job ON applications(job_id);
CREATE INDEX idx_applications_candidate ON applications(candidate_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_date ON applications(applied_date DESC);
CREATE INDEX idx_application_history_app ON application_history(application_id);

-- ============================================================================
-- TRIGGERS FOR UPDATED_AT
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_profile_metadata_updated_at BEFORE UPDATE ON profile_metadata
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_employer_profiles_updated_at BEFORE UPDATE ON employer_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_jobs_updated_at BEFORE UPDATE ON jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_job_metadata_updated_at BEFORE UPDATE ON job_metadata
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_applications_updated_at BEFORE UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================================================

COMMENT ON TABLE users IS 'Core user authentication and management - Hot data table';
COMMENT ON TABLE profiles IS 'User profile information - Hot data table, frequently accessed';
COMMENT ON TABLE profile_metadata IS 'Extended profile metadata - Less frequently accessed';
COMMENT ON TABLE employer_profiles IS 'Employer-specific profile data';
COMMENT ON TABLE file_metadata IS 'File storage metadata - References to uploaded files';
COMMENT ON TABLE jobs IS 'Job postings - Hot data table, active jobs frequently accessed';
COMMENT ON TABLE job_metadata IS 'Extended job information - Less frequently accessed';
COMMENT ON TABLE applications IS 'Job applications - Hot data table';
COMMENT ON TABLE application_history IS 'Application status change history';

