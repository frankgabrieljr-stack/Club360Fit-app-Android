-- Migration 027: Device tokens for native push notifications.
-- Apps register one row per physical device token. Edge Functions use service_role
-- to fan out `client_notifications` rows to the correct member or coach devices.

CREATE TABLE IF NOT EXISTS public.push_device_tokens (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  platform text NOT NULL CHECK (platform IN ('android_fcm', 'ios_apns')),
  token text NOT NULL,
  environment text NOT NULL DEFAULT 'production' CHECK (environment IN ('sandbox', 'production')),
  app_version text,
  device_id text,
  enabled boolean NOT NULL DEFAULT true,
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (platform, token)
);

CREATE INDEX IF NOT EXISTS idx_push_device_tokens_user_enabled
  ON public.push_device_tokens (user_id, enabled, last_seen_at DESC);

ALTER TABLE public.push_device_tokens ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "push_tokens_select_own" ON public.push_device_tokens;
CREATE POLICY "push_tokens_select_own" ON public.push_device_tokens
  FOR SELECT USING (user_id = auth.uid());

DROP POLICY IF EXISTS "push_tokens_insert_own" ON public.push_device_tokens;
CREATE POLICY "push_tokens_insert_own" ON public.push_device_tokens
  FOR INSERT WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "push_tokens_update_own" ON public.push_device_tokens;
CREATE POLICY "push_tokens_update_own" ON public.push_device_tokens
  FOR UPDATE USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS "push_tokens_delete_own" ON public.push_device_tokens;
CREATE POLICY "push_tokens_delete_own" ON public.push_device_tokens
  FOR DELETE USING (user_id = auth.uid());

CREATE TABLE IF NOT EXISTS public.push_notification_dedupe (
  dedupe_key text PRIMARY KEY,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.push_notification_dedupe ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "push_dedupe_no_client_access" ON public.push_notification_dedupe;
CREATE POLICY "push_dedupe_no_client_access" ON public.push_notification_dedupe
  FOR ALL USING (false)
  WITH CHECK (false);
