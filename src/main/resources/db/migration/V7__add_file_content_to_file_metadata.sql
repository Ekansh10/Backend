-- Add file_content column to file_metadata table to store files in PostgreSQL
-- This migration adds BYTEA column to store binary file data

-- Add file_content column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'file_metadata' 
        AND column_name = 'file_content'
    ) THEN
        ALTER TABLE file_metadata ADD COLUMN file_content BYTEA;
    END IF;
END $$;

-- Update storage_provider default to 'DATABASE' for new records
ALTER TABLE file_metadata ALTER COLUMN storage_provider SET DEFAULT 'DATABASE';

-- Add comment
COMMENT ON COLUMN file_metadata.file_content IS 'Binary file content stored in PostgreSQL (BYTEA). Used for resumes and profile pictures.';

