-- ==============================================================================
-- Migration: 20261018000002_add_series_and_offline_reading.sql
-- Features: Book Series Management & Offline Reading Progress Tracking
-- Safe & Rerunnable: Preserves all existing books, profiles, and storage policies
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. SERIES TABLE
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.series (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT,
    cover_image_url TEXT,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for rapid series browsing and publication filtering
CREATE INDEX IF NOT EXISTS idx_series_is_published ON public.series(is_published);
CREATE INDEX IF NOT EXISTS idx_series_created_at ON public.series(created_at DESC);

-- Enable Row Level Security on series
ALTER TABLE public.series ENABLE ROW LEVEL SECURITY;

-- SERIES RLS POLICIES
-- Members can view published series; Admins can view all (draft & published).
DROP POLICY IF EXISTS "Allow select series based on role and publish state" ON public.series;
CREATE POLICY "Allow select series based on role and publish state"
ON public.series
FOR SELECT
TO authenticated
USING (
  public.is_admin() OR is_published = TRUE
);

-- Only admins can insert series
DROP POLICY IF EXISTS "Admins can insert series" ON public.series;
CREATE POLICY "Admins can insert series"
ON public.series
FOR INSERT
TO authenticated
WITH CHECK (
  public.is_admin()
);

-- Only admins can update series
DROP POLICY IF EXISTS "Admins can update series" ON public.series;
CREATE POLICY "Admins can update series"
ON public.series
FOR UPDATE
TO authenticated
USING (
  public.is_admin()
)
WITH CHECK (
  public.is_admin()
);

-- Only admins can delete series
DROP POLICY IF EXISTS "Admins can delete series" ON public.series;
CREATE POLICY "Admins can delete series"
ON public.series
FOR DELETE
TO authenticated
USING (
  public.is_admin()
);

-- Updated_at trigger for series
DROP TRIGGER IF EXISTS trg_series_updated_at ON public.series;
CREATE TRIGGER trg_series_updated_at
BEFORE UPDATE ON public.series
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at_timestamp();


-- ------------------------------------------------------------------------------
-- 2. EXTEND BOOKS TABLE (Series relationship, ordering, and content versioning)
-- ------------------------------------------------------------------------------
-- Add nullable series_id with ON DELETE SET NULL so deleting a series
-- detaches its books rather than deleting them.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'books' AND column_name = 'series_id'
    ) THEN
        ALTER TABLE public.books 
        ADD COLUMN series_id UUID REFERENCES public.series(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'books' AND column_name = 'series_order'
    ) THEN
        ALTER TABLE public.books 
        ADD COLUMN series_order INT NOT NULL DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'books' AND column_name = 'version'
    ) THEN
        ALTER TABLE public.books 
        ADD COLUMN version INT NOT NULL DEFAULT 1;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'books' AND column_name = 'content_hash'
    ) THEN
        ALTER TABLE public.books 
        ADD COLUMN content_hash TEXT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_books_series_id ON public.books(series_id);
CREATE INDEX IF NOT EXISTS idx_books_series_order ON public.books(series_id, series_order ASC);


-- ------------------------------------------------------------------------------
-- 3. READING PROGRESS TABLE (Cloud sync with offline-first client resolution)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.reading_progress (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES public.books(id) ON DELETE CASCADE,
    progress_percent INT NOT NULL DEFAULT 0 CHECK (progress_percent BETWEEN 0 AND 100),
    last_page INT NOT NULL DEFAULT 0 CHECK (last_page >= 0),
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    client_updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_reading_progress_user ON public.reading_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_reading_progress_book ON public.reading_progress(book_id);

-- Enable Row Level Security on reading_progress
ALTER TABLE public.reading_progress ENABLE ROW LEVEL SECURITY;

-- Users can select strictly their own reading progress
DROP POLICY IF EXISTS "Users can read own reading progress" ON public.reading_progress;
CREATE POLICY "Users can read own reading progress"
ON public.reading_progress
FOR SELECT
TO authenticated
USING (
  auth.uid() = user_id
);

-- Users can insert strictly their own reading progress
DROP POLICY IF EXISTS "Users can insert own reading progress" ON public.reading_progress;
CREATE POLICY "Users can insert own reading progress"
ON public.reading_progress
FOR INSERT
TO authenticated
WITH CHECK (
  auth.uid() = user_id
);

-- Users can update strictly their own reading progress
DROP POLICY IF EXISTS "Users can update own reading progress" ON public.reading_progress;
CREATE POLICY "Users can update own reading progress"
ON public.reading_progress
FOR UPDATE
TO authenticated
USING (
  auth.uid() = user_id
)
WITH CHECK (
  auth.uid() = user_id
);

-- Users can delete strictly their own reading progress
DROP POLICY IF EXISTS "Users can delete own reading progress" ON public.reading_progress;
CREATE POLICY "Users can delete own reading progress"
ON public.reading_progress
FOR DELETE
TO authenticated
USING (
  auth.uid() = user_id
);

-- Updated_at trigger for reading_progress
DROP TRIGGER IF EXISTS trg_reading_progress_updated_at ON public.reading_progress;
CREATE TRIGGER trg_reading_progress_updated_at
BEFORE UPDATE ON public.reading_progress
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at_timestamp();

-- ------------------------------------------------------------------------------
-- ROLLBACK GUIDANCE (for operator reference):
-- To revert this migration safely if needed:
--   DROP TABLE IF EXISTS public.reading_progress;
--   ALTER TABLE public.books DROP COLUMN IF EXISTS series_order;
--   ALTER TABLE public.books DROP COLUMN IF EXISTS series_id;
--   ALTER TABLE public.books DROP COLUMN IF EXISTS version;
--   ALTER TABLE public.books DROP COLUMN IF EXISTS content_hash;
--   DROP TABLE IF EXISTS public.series;
-- ------------------------------------------------------------------------------
