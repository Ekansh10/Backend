-- Update file_metadata table to support multiple resumes
-- Remove unique constraint on (profile_id, file_type) to allow multiple files of same type

-- Drop the unique constraint if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'unique_profile_file_type'
    ) THEN
        ALTER TABLE file_metadata DROP CONSTRAINT unique_profile_file_type;
    END IF;
END $$;

-- Add is_primary column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'file_metadata' 
        AND column_name = 'is_primary'
    ) THEN
        ALTER TABLE file_metadata ADD COLUMN is_primary BOOLEAN DEFAULT false;
    END IF;
END $$;

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_file_metadata_profile_type ON file_metadata(profile_id, file_type);
CREATE INDEX IF NOT EXISTS idx_file_metadata_primary ON file_metadata(profile_id, file_type, is_primary) 
    WHERE is_primary = true;

-- Add unique constraint: only one primary file per type per profile
CREATE UNIQUE INDEX IF NOT EXISTS idx_file_metadata_unique_primary 
    ON file_metadata(profile_id, file_type) 
    WHERE is_primary = true;

