-- ==============================================================================
-- Migration: 20261018000001_fix_admin_provisioning.sql
-- Narrow Repair Script: Fix Administrator Provisioning & Harden Role Guards
-- Safe & Rerunnable: Preserves all existing data (profiles, books, storage)
-- ==============================================================================

-- 1. Hardened is_admin() Helper Function
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid() AND role = 'admin'
  );
$$;

ALTER FUNCTION public.is_admin() OWNER TO postgres;
REVOKE ALL ON FUNCTION public.is_admin() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.is_admin() FROM anon;
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated, postgres, service_role;

-- 2. Corrected Role Guard Trigger Function
-- Removed SECURITY DEFINER so that current_user and session_user represent the real caller.
-- Explicitly permits role modifications from:
--   a) Direct database operators/DBAs in SQL Editor (session_user IN ('postgres', 'supabase_admin'))
--   b) Trusted backend service role contexts (auth.role() = 'service_role')
--   c) Authenticated administrators (public.is_admin())
-- Blocks self-promotion and role-tampering by standard members and anonymous users.
CREATE OR REPLACE FUNCTION public.enforce_profile_role_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
  -- Intercept role modifications on UPDATE or admin assignment on INSERT
  IF (TG_OP = 'UPDATE' AND NEW.role IS DISTINCT FROM OLD.role)
     OR (TG_OP = 'INSERT' AND NEW.role = 'admin') THEN

    IF session_user IN ('postgres', 'supabase_admin')
       OR auth.role() = 'service_role'
       OR public.is_admin() THEN
      RETURN NEW;
    END IF;

    RAISE EXCEPTION 'Unauthorized: Only administrators can modify user roles';
  END IF;

  RETURN NEW;
END;
$$;

ALTER FUNCTION public.enforce_profile_role_guard() OWNER TO postgres;
REVOKE ALL ON FUNCTION public.enforce_profile_role_guard() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.enforce_profile_role_guard() FROM anon;
GRANT EXECUTE ON FUNCTION public.enforce_profile_role_guard() TO authenticated, postgres, service_role;

-- Re-create the trigger to monitor both INSERT and UPDATE on profiles
DROP TRIGGER IF EXISTS trg_profile_role_guard ON public.profiles;
CREATE TRIGGER trg_profile_role_guard
BEFORE INSERT OR UPDATE ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.enforce_profile_role_guard();

-- 3. Hardened Administrator Provisioning Function
-- Restricted strictly to trusted database operators (postgres / service_role).
-- Completely revoked from PUBLIC, anon, and authenticated so it cannot be invoked via PostgREST / RPC.
CREATE OR REPLACE FUNCTION public.promote_user_to_admin(target_email TEXT)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
  target_user_id UUID;
  cleaned_email TEXT;
BEGIN
  -- Normalize email input
  cleaned_email := lower(trim(target_email));
  IF cleaned_email IS NULL OR cleaned_email = '' THEN
    RAISE EXCEPTION 'Invalid email: email address cannot be empty';
  END IF;

  -- Layered authorization check: Reject unprivileged callers even if privileges are misconfigured
  IF session_user = 'authenticator' THEN
    IF auth.role() = 'service_role' THEN
      NULL; -- Allowed for service_role
    ELSIF auth.role() = 'authenticated' AND public.is_admin() THEN
      NULL; -- Allowed for existing authenticated admins
    ELSE
      RAISE EXCEPTION 'Access Denied: Only administrators or database superusers can execute this function.';
    END IF;
  ELSIF session_user NOT IN ('postgres', 'supabase_admin') THEN
    RAISE EXCEPTION 'Access Denied: Unauthorized session user %.', session_user;
  END IF;

  -- Look up target user in auth.users
  SELECT id INTO target_user_id
  FROM auth.users
  WHERE lower(email) = cleaned_email;

  IF target_user_id IS NULL THEN
    RAISE EXCEPTION 'User with email "%" not found in auth.users. Please ensure they have signed up first.', target_email;
  END IF;

  -- Upsert into public.profiles with role = 'admin'
  -- Preserves existing full_name if profile already exists
  INSERT INTO public.profiles (id, full_name, role)
  VALUES (
    target_user_id,
    COALESCE(
      (SELECT raw_user_meta_data->>'full_name' FROM auth.users WHERE id = target_user_id),
      split_part(cleaned_email, '@', 1)
    ),
    'admin'
  )
  ON CONFLICT (id) DO UPDATE
  SET role = 'admin';

  RETURN 'Successfully granted admin role to ' || target_email;
END;
$$;

ALTER FUNCTION public.promote_user_to_admin(TEXT) OWNER TO postgres;

-- Lock down execution privileges strictly:
REVOKE ALL ON FUNCTION public.promote_user_to_admin(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.promote_user_to_admin(TEXT) FROM anon;
REVOKE ALL ON FUNCTION public.promote_user_to_admin(TEXT) FROM authenticated;
GRANT EXECUTE ON FUNCTION public.promote_user_to_admin(TEXT) TO postgres;
GRANT EXECUTE ON FUNCTION public.promote_user_to_admin(TEXT) TO service_role;
