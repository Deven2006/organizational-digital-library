-- ==============================================================================
-- Migration: 20261019000000_add_book_reconciliation_rpc.sql
-- Feature: Authoritative reconciliation for deleted books across member devices
-- Safe & Rerunnable: Checks book existence without weakening RLS or privacy
-- ==============================================================================

CREATE OR REPLACE FUNCTION public.check_existing_books(book_ids UUID[])
RETURNS TABLE(id UUID, is_published BOOLEAN)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT b.id, b.is_published
  FROM public.books b
  WHERE b.id = ANY(book_ids);
$$;

-- Security hardening
ALTER FUNCTION public.check_existing_books(UUID[]) OWNER TO postgres;
REVOKE ALL ON FUNCTION public.check_existing_books(UUID[]) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.check_existing_books(UUID[]) FROM anon;
GRANT EXECUTE ON FUNCTION public.check_existing_books(UUID[]) TO authenticated, service_role;
