-- Signup trigger `handle_new_client_intake` creates a public.clients row before a coach exists.
-- coach_id must be nullable until an admin assigns the member to a coach.
-- Safe to run if already applied (no-op when column is already nullable).

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'clients'
      AND column_name = 'coach_id'
      AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE public.clients ALTER COLUMN coach_id DROP NOT NULL;
  END IF;
END $$;
