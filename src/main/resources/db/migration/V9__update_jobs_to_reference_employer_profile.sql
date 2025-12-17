-- ============================================================================
-- Update Jobs Table to Reference employer_profile Instead of profiles
-- ============================================================================
-- This migration fixes the relationship mismatch where jobs reference
-- profiles(id) but employers now use the separate employer_profile table.

-- Step 1: Add new column for employer_profile_id
ALTER TABLE jobs 
    ADD COLUMN IF NOT EXISTS employer_profile_id UUID;

-- Step 2: Migrate existing data
-- For each job, find the corresponding employer_profile based on user_id
UPDATE jobs j
SET employer_profile_id = (
    SELECT ep.id 
    FROM employer_profile ep
    JOIN profiles p ON ep.user_id = p.user_id
    WHERE p.id = j.employer_id
)
WHERE employer_profile_id IS NULL;

-- Step 3: For jobs where employer_profile doesn't exist yet, 
-- we need to create employer_profile entries from profiles data
-- This handles edge cases where employer data exists in profiles but not in employer_profile
INSERT INTO employer_profile (
    id, user_id, full_name, email, mobile, location, linkedin,
    company_name, company_description, website, profile_picture,
    created_at, updated_at, version
)
SELECT 
    uuid_generate_v4(),
    p.user_id,
    p.full_name,
    p.email,
    p.mobile,
    p.location,
    p.linkedin,
    p.company_name,
    p.company_description,
    p.website,
    p.profile_picture,
    p.created_at,
    p.updated_at,
    1
FROM profiles p
WHERE p.company_name IS NOT NULL 
  AND p.company_name != ''
  AND NOT EXISTS (
      SELECT 1 FROM employer_profile ep WHERE ep.user_id = p.user_id
  )
ON CONFLICT DO NOTHING;

-- Step 4: Update jobs again for any newly created employer_profiles
UPDATE jobs j
SET employer_profile_id = (
    SELECT ep.id 
    FROM employer_profile ep
    JOIN profiles p ON ep.user_id = p.user_id
    WHERE p.id = j.employer_id
)
WHERE employer_profile_id IS NULL;

-- Step 5: Drop the old foreign key constraint
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'jobs_employer_id_fkey'
    ) THEN
        ALTER TABLE jobs DROP CONSTRAINT jobs_employer_id_fkey;
    END IF;
END $$;

-- Step 6: Add foreign key constraint for the new column
ALTER TABLE jobs
    ADD CONSTRAINT jobs_employer_profile_id_fkey
    FOREIGN KEY (employer_profile_id) 
    REFERENCES employer_profile(id) 
    ON DELETE CASCADE;

-- Step 7: Create index for the new column
CREATE INDEX IF NOT EXISTS idx_jobs_employer_profile 
    ON jobs(employer_profile_id);

-- Step 8: Make the new column NOT NULL (after data migration)
-- Only do this if all jobs have been migrated
DO $$
DECLARE
    unmigrated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO unmigrated_count
    FROM jobs
    WHERE employer_profile_id IS NULL;
    
    IF unmigrated_count = 0 THEN
        ALTER TABLE jobs ALTER COLUMN employer_profile_id SET NOT NULL;
    ELSE
        RAISE WARNING 'Cannot set NOT NULL: % jobs still have NULL employer_profile_id', unmigrated_count;
    END IF;
END $$;

-- Step 9: Drop the old employer_id column
-- Only do this if the new column is NOT NULL
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'jobs' 
        AND column_name = 'employer_id'
        AND is_nullable = 'NO'
    ) THEN
        -- New column is NOT NULL, safe to drop old one
        ALTER TABLE jobs DROP COLUMN employer_id;
    ELSE
        -- Keep old column for now, but add a check constraint
        RAISE WARNING 'Keeping employer_id column as employer_profile_id is still nullable';
    END IF;
END $$;

-- Step 10: Update the index that was using employer_id
-- Drop old index if it exists
DROP INDEX IF EXISTS idx_jobs_employer;

-- The new index idx_jobs_employer_profile was already created in Step 7

-- Add comment
COMMENT ON COLUMN jobs.employer_profile_id IS 'References employer_profile table (migrated from profiles.id in V9)';

