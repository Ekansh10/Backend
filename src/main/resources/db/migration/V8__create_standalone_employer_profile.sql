-- ============================================================================
-- Create Standalone Employer Profile Table
-- ============================================================================
-- This table stores employer/company profiles separately from candidate profiles
-- for better data separation and schema clarity.

-- Drop the old employer_profiles table if it exists (it references profiles)
DROP TABLE IF EXISTS employer_profiles CASCADE;

-- Create standalone employer_profile table
CREATE TABLE IF NOT EXISTS employer_profile (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    
    -- Contact person information
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile VARCHAR(50),
    location VARCHAR(255),
    linkedin VARCHAR(500),
    
    -- Company information
    company_name VARCHAR(255) NOT NULL,
    company_description TEXT,
    website VARCHAR(500) NOT NULL,
    
    -- Profile picture (stores file ID from file_metadata)
    profile_picture VARCHAR(255),
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1
);

-- Create indexes for employer_profile
CREATE INDEX idx_employer_profile_user_id ON employer_profile(user_id);
CREATE INDEX idx_employer_profile_email ON employer_profile(email);
CREATE INDEX idx_employer_profile_company_name ON employer_profile(company_name);
CREATE INDEX idx_employer_profile_website ON employer_profile(website);

-- Add comment to table
COMMENT ON TABLE employer_profile IS 'Stores employer/company profile information separately from candidate profiles';

-- Update file_metadata table to support both candidate and employer profiles
-- Make profile_id nullable and add employer_profile_id
ALTER TABLE file_metadata 
    ALTER COLUMN profile_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS employer_profile_id UUID REFERENCES employer_profile(id) ON DELETE CASCADE;

-- Add check constraint to ensure at least one profile reference exists
ALTER TABLE file_metadata 
    ADD CONSTRAINT chk_file_metadata_profile_reference 
    CHECK (
        (profile_id IS NOT NULL AND employer_profile_id IS NULL) OR
        (profile_id IS NULL AND employer_profile_id IS NOT NULL)
    );

-- Create index for employer_profile_id
CREATE INDEX IF NOT EXISTS idx_file_metadata_employer_profile ON file_metadata(employer_profile_id);

