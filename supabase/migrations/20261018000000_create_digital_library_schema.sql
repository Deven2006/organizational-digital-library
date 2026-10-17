-- ==============================================================================
-- Schema Migration: Organizational Digital Library
-- Target: Supabase PostgreSQL (Auth, Profiles, Books, Storage & RLS Policies)
-- ==============================================================================

-- 1. PROFILES TABLE
-- Stores profile metadata linked to auth.users.
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT,
    role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index on profiles role for rapid role checks
CREATE INDEX IF NOT EXISTS idx_profiles_role ON public.profiles(role);

-- Enable RLS on profiles
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- 2. SECURITY DEFINER HELPER: is_admin
-- Checks if the authenticated caller has the 'admin' role in profiles table.
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid() AND role = 'admin'
  );
$$;

-- Revoke default public execution, grant strictly to authenticated
REVOKE EXECUTE ON FUNCTION public.is_admin() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.is_admin() FROM anon;
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;

-- 3. PROFILES RLS POLICIES
-- Anyone authenticated can view their own profile, or admins can view all profiles.
DROP POLICY IF EXISTS "Users can view own profile or admin can view all" ON public.profiles;
CREATE POLICY "Users can view own profile or admin can view all"
ON public.profiles
FOR SELECT
TO authenticated
USING (
  auth.uid() = id OR public.is_admin()
);

-- Users can update only their own profile details.
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
ON public.profiles
FOR UPDATE
TO authenticated
USING (
  auth.uid() = id OR public.is_admin()
)
WITH CHECK (
  auth.uid() = id OR public.is_admin()
);

-- Only admins may manually insert profiles (normal users are created via signup trigger).
DROP POLICY IF EXISTS "Admins can insert profiles" ON public.profiles;
CREATE POLICY "Admins can insert profiles"
ON public.profiles
FOR INSERT
TO authenticated
WITH CHECK (
  public.is_admin()
);

-- Only admins may delete profiles.
DROP POLICY IF EXISTS "Admins can delete profiles" ON public.profiles;
CREATE POLICY "Admins can delete profiles"
ON public.profiles
FOR DELETE
TO authenticated
USING (
  public.is_admin()
);

-- STRICT ROLE INTEGRITY TRIGGER:
-- Prevent users from self-promoting or altering their own role unless they are an admin.
CREATE OR REPLACE FUNCTION public.enforce_profile_role_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NEW.role IS DISTINCT FROM OLD.role THEN
    IF NOT public.is_admin() THEN
      RAISE EXCEPTION 'Unauthorized: Only administrators can modify user roles';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.enforce_profile_role_guard() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.enforce_profile_role_guard() FROM anon;
GRANT EXECUTE ON FUNCTION public.enforce_profile_role_guard() TO authenticated;

DROP TRIGGER IF EXISTS trg_profile_role_guard ON public.profiles;
CREATE TRIGGER trg_profile_role_guard
BEFORE UPDATE ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.enforce_profile_role_guard();

-- AUTOMATIC PROFILE CREATION TRIGGER:
-- Automatically insert a 'member' profile when a user signs up.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name, role)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(NEW.email, '@', 1)),
    'member'
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM anon;
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM authenticated;
GRANT EXECUTE ON FUNCTION public.handle_new_user() TO postgres, supabase_auth_admin;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW
EXECUTE FUNCTION public.handle_new_user();

-- 4. BOOKS TABLE
CREATE TABLE IF NOT EXISTS public.books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    author TEXT,
    description TEXT,
    file_type TEXT NOT NULL CHECK (lower(file_type) IN ('pdf', 'docx', 'txt')),
    storage_path TEXT NOT NULL,
    parsed_content TEXT,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Useful indexes for querying and searching
CREATE INDEX IF NOT EXISTS idx_books_title ON public.books(title);
CREATE INDEX IF NOT EXISTS idx_books_is_published ON public.books(is_published);
CREATE INDEX IF NOT EXISTS idx_books_created_at ON public.books(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_books_storage_path ON public.books(storage_path);

-- Enable RLS on books
ALTER TABLE public.books ENABLE ROW LEVEL SECURITY;

-- 5. BOOKS RLS POLICIES
-- Members can only select published books. Admins can select all books (draft & published).
DROP POLICY IF EXISTS "Allow select books based on role and publish state" ON public.books;
CREATE POLICY "Allow select books based on role and publish state"
ON public.books
FOR SELECT
TO authenticated
USING (
  public.is_admin() OR is_published = TRUE
);

-- Only admins can insert books.
DROP POLICY IF EXISTS "Admins can insert books" ON public.books;
CREATE POLICY "Admins can insert books"
ON public.books
FOR INSERT
TO authenticated
WITH CHECK (
  public.is_admin()
);

-- Only admins can update books.
DROP POLICY IF EXISTS "Admins can update books" ON public.books;
CREATE POLICY "Admins can update books"
ON public.books
FOR UPDATE
TO authenticated
USING (
  public.is_admin()
)
WITH CHECK (
  public.is_admin()
);

-- Only admins can delete books.
DROP POLICY IF EXISTS "Admins can delete books" ON public.books;
CREATE POLICY "Admins can delete books"
ON public.books
FOR DELETE
TO authenticated
USING (
  public.is_admin()
);

-- Automatic updated_at timestamp trigger for books
CREATE OR REPLACE FUNCTION public.set_updated_at_timestamp()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.set_updated_at_timestamp() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.set_updated_at_timestamp() FROM anon;
GRANT EXECUTE ON FUNCTION public.set_updated_at_timestamp() TO authenticated;

DROP TRIGGER IF EXISTS trg_books_updated_at ON public.books;
CREATE TRIGGER trg_books_updated_at
BEFORE UPDATE ON public.books
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at_timestamp();

-- 6. STORAGE BUCKET: book-files
-- Create private storage bucket if not already present
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'book-files',
  'book-files',
  false, -- private bucket!
  52428800, -- 50 MB limit
  ARRAY[
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/msword',
    'text/plain'
  ]
)
ON CONFLICT (id) DO UPDATE SET public = false;

-- 7. STORAGE POLICIES (on storage.objects)
-- Storage upload: Only administrators can upload to book-files
DROP POLICY IF EXISTS "Admins can upload book files" ON storage.objects;
CREATE POLICY "Admins can upload book files"
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'book-files' AND public.is_admin()
);

-- Storage update: Only administrators can update files in book-files
DROP POLICY IF EXISTS "Admins can update book files" ON storage.objects;
CREATE POLICY "Admins can update book files"
ON storage.objects
FOR UPDATE
TO authenticated
USING (
  bucket_id = 'book-files' AND public.is_admin()
);

-- Storage delete: Only administrators can delete files in book-files
DROP POLICY IF EXISTS "Admins can delete book files" ON storage.objects;
CREATE POLICY "Admins can delete book files"
ON storage.objects
FOR DELETE
TO authenticated
USING (
  bucket_id = 'book-files' AND public.is_admin()
);

-- Storage select / read:
-- Admins can read any file in book-files.
-- Members can read ONLY files that correspond to an active published book record.
DROP POLICY IF EXISTS "Read access for published books or admins" ON storage.objects;
CREATE POLICY "Read access for published books or admins"
ON storage.objects
FOR SELECT
TO authenticated
USING (
  bucket_id = 'book-files' AND (
    public.is_admin()
    OR EXISTS (
      SELECT 1 FROM public.books
      WHERE storage_path = storage.objects.name
      AND is_published = TRUE
    )
  )
);

-- 8. SECURE INITIAL ADMINISTRATOR PROVISIONING PROCEDURE
-- To prevent abuse via client-side RPC, EXECUTE is revoked from public, anon, and authenticated.
-- It can only be called from Supabase SQL Editor (postgres superuser) or service_role.
CREATE OR REPLACE FUNCTION public.promote_user_to_admin(target_email TEXT)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  target_user_id UUID;
BEGIN
  -- Strict caller authorization: Must be superuser or an existing admin
  IF auth.role() = 'authenticated' AND NOT public.is_admin() THEN
    RAISE EXCEPTION 'Access Denied: Only administrators or database superusers can execute this function.';
  END IF;

  SELECT id INTO target_user_id FROM auth.users WHERE lower(email) = lower(trim(target_email));
  IF target_user_id IS NULL THEN
    RAISE EXCEPTION 'User with email % not found in auth.users. Please ensure they have signed up first.', target_email;
  END IF;

  INSERT INTO public.profiles (id, full_name, role)
  VALUES (target_user_id, split_part(target_email, '@', 1), 'admin')
  ON CONFLICT (id) DO UPDATE
  SET role = 'admin';

  RETURN 'Successfully granted admin role to ' || target_email;
END;
$$;

-- Lock down execution privileges strictly:
REVOKE EXECUTE ON FUNCTION public.promote_user_to_admin(TEXT) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.promote_user_to_admin(TEXT) FROM anon;
REVOKE EXECUTE ON FUNCTION public.promote_user_to_admin(TEXT) FROM authenticated;
GRANT EXECUTE ON FUNCTION public.promote_user_to_admin(TEXT) TO postgres;
GRANT EXECUTE ON FUNCTION public.promote_user_to_admin(TEXT) TO service_role;
