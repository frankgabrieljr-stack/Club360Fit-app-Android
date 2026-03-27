-- Run in Supabase SQL Editor when sign-up returns "Database error saving new user".
-- That message comes from Auth when something fails in Postgres during user creation
-- (often an AFTER INSERT trigger on auth.users, or a signup hook).

-- 1) Triggers on auth.users
SELECT
  t.tgname AS trigger_name,
  pg_get_triggerdef(t.oid) AS definition
FROM pg_trigger t
JOIN pg_class c ON c.oid = t.tgrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'auth'
  AND c.relname = 'users'
  AND NOT t.tgisinternal;

-- 2) Functions named like common Supabase templates (inspect definitions in Dashboard)
SELECT
  n.nspname AS schema,
  p.proname AS function_name
FROM pg_proc p
JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE p.proname ILIKE '%user%'
  AND n.nspname IN ('public', 'auth')
ORDER BY n.nspname, p.proname;
