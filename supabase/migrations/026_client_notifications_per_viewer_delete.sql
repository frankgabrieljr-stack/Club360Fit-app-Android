-- Migration 026: Per-viewer notification delete/hide.
-- `client_notifications` is shared between member and coach. A hard DELETE removes
-- it for both sides, so store independent delete timestamps instead.

ALTER TABLE public.client_notifications
  ADD COLUMN IF NOT EXISTS client_deleted_at timestamptz,
  ADD COLUMN IF NOT EXISTS coach_deleted_at timestamptz;

COMMENT ON COLUMN public.client_notifications.client_deleted_at IS
  'When set, hide this notification from the member inbox only.';
COMMENT ON COLUMN public.client_notifications.coach_deleted_at IS
  'When set, hide this notification from coach inboxes only.';

CREATE INDEX IF NOT EXISTS idx_client_notifications_client_visible
  ON public.client_notifications (client_id, created_at DESC)
  WHERE client_deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_client_notifications_coach_visible
  ON public.client_notifications (client_id, created_at DESC)
  WHERE coach_deleted_at IS NULL;
