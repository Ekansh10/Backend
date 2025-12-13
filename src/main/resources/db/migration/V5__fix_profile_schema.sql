-- Fix Profile table to match entity
-- Add missing columns that are in the Profile entity

-- Add company_description to profiles table if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'profiles' 
        AND column_name = 'company_description'
    ) THEN
        ALTER TABLE profiles ADD COLUMN company_description TEXT;
    END IF;
END $$;

-- Add website column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'profiles' 
        AND column_name = 'website'
    ) THEN
        ALTER TABLE profiles ADD COLUMN website VARCHAR(255);
    END IF;
END $$;

-- Verify the columns exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
        AND table_name = 'profiles' 
        AND column_name = 'company_description'
    ) THEN
        RAISE EXCEPTION 'Column company_description was not added';
    END IF;
END $$;

