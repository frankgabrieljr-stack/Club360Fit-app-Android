# Profiles Table and Auth Sync - Deployment Guide

## Overview

This deployment ensures that `public.profiles` stays in sync with `auth.users` metadata for the Club360Fit application. The JWT uses `user_metadata.role` for routing, while the profiles table provides a PostgREST-queryable interface for user data.

## What Was Changed

### 1. Database Migration (023_profiles_table_and_auth_sync.sql)

**Location:** `supabase/migrations/023_profiles_table_and_auth_sync.sql`

**Changes:**
- Creates `public.profiles` table if not exists with columns: id, email, full_name, avatar_url, role, created_at, updated_at
- Enables RLS with proper policies:
  - Users can SELECT/UPDATE their own row
  - Users can INSERT their own row (for signup flows)
  - Admins (via JWT metadata check) can read/update all profiles
- Grants SELECT, INSERT, UPDATE to authenticated; ALL to service_role
- Creates `sync_profile_from_auth_user()` function (SECURITY DEFINER) that upserts profiles from auth.users metadata
- Creates trigger `sync_profile_from_auth_user` on auth.users AFTER INSERT OR UPDATE OF email, raw_user_meta_data
- Backfills existing auth.users rows into profiles table

**Status:** ✅ Applied to Supabase (mjkrokpctcieahxtxvxq) on 2026-03-27

### 2. Edge Function Update (set-user-role)

**Location:** `supabase/functions/set-user-role/index.ts`

**Changes:**
- After successfully updating auth.users user_metadata with new role, the function now also:
  - Derives full_name from metadata (full_name, name, or first_name + last_name)
  - Upserts into public.profiles table with updated role, email, full_name, avatar_url
  - Uses onConflict: 'id' for safe upserts
  - Logs warnings (but doesn't fail) if profiles table is not deployed yet

**Status:** ✅ Code committed to repository (commit 1758ac9)
**Deployment:** ⏳ PENDING - Function must be redeployed to Supabase

## Deployment Commands

### Step 1: Apply Database Migration

**Option A: Via Supabase SQL Editor** (COMPLETED ✅)
```sql
-- Already applied via Supabase Dashboard SQL Editor
-- Migration: 023_profiles_table_and_auth_sync.sql
```

**Option B: Via Supabase CLI** (Alternative for future deployments)
```bash
# From project root
cd supabase
supabase db push --project-ref mjkrokpctcieahxtxvxq
```

### Step 2: Deploy Edge Function (REQUIRED)

```bash
# Deploy set-user-role function with updated profiles sync logic
supabase functions deploy set-user-role --project-ref mjkrokpctcieahxtxvxq
```

**Important:** The Edge Function MUST be redeployed after code changes to ensure role updates sync to the profiles table.

## Verification

### 1. Verify Profiles Table
```sql
-- Check table exists and has data
SELECT * FROM public.profiles ORDER BY created_at DESC;

-- Verify trigger exists
SELECT * FROM pg_trigger WHERE tgname = 'sync_profile_from_auth_user';

-- Check RLS policies
SELECT * FROM pg_policies WHERE tablename = 'profiles';
```

### 2. Test Role Change Sync
```bash
# Use set-user-role function to change a user's role
# Then verify both auth.users AND public.profiles are updated

# Check auth.users metadata
SELECT id, email, raw_user_meta_data->>'role' as role 
FROM auth.users 
WHERE id = '<user_id>';

# Check public.profiles
SELECT id, email, role, updated_at 
FROM public.profiles 
WHERE id = '<user_id>';
```

### 3. Test New User Signup
Create a new user and verify:
- Profile row is automatically created via trigger
- Role defaults to 'client' if not specified
- All metadata fields sync correctly

## Important Notes

### JWT Remains Source of Truth
- Mobile apps continue to use `user_metadata.role` from JWT for client vs admin routing
- The profiles table is for PostgREST queries and data display
- **After role changes, users should sign out and sign in again** to refresh their JWT

### Trigger Behavior
- Trigger only fires on INSERT or UPDATE of `email` or `raw_user_meta_data` columns (not on every auth.users update)
- This prevents unnecessary profile updates on unrelated auth changes

### Backwards Compatibility
- Migration uses IF NOT EXISTS so it's safe to run even if profiles table exists from earlier manual creation
- Old trigger `on_auth_user_created_profile` is dropped and replaced with new `sync_profile_from_auth_user` trigger

## Cleanup

No cleanup required. The old trigger has been automatically replaced by the migration.

## Rollback (if needed)

If you need to rollback this migration:

```sql
-- Remove trigger
DROP TRIGGER IF EXISTS sync_profile_from_auth_user ON auth.users;

-- Remove function
DROP FUNCTION IF EXISTS public.sync_profile_from_auth_user();

-- Optionally drop table (WARNING: loses profile data)
DROP TABLE IF EXISTS public.profiles CASCADE;
```

**Note:** Rollback is NOT recommended as the profiles table is now integrated into the app architecture.

## Next Steps

1. ✅ Migration applied to database
2. ⏳ **Deploy set-user-role Edge Function** (see Step 2 above)
3. Test role changes with admin account
4. Verify profile sync is working for new signups
5. Monitor Edge Function logs for any profiles table sync warnings

## Contact

For issues or questions, check:
- Supabase Dashboard: https://supabase.com/dashboard/project/mjkrokpctcieahxtxvxq
- Edge Functions Logs: https://supabase.com/dashboard/project/mjkrokpctcieahxtxvxq/functions
- Database Logs: https://supabase.com/dashboard/project/mjkrokpctcieahxtxvxq/logs/postgres-logs
