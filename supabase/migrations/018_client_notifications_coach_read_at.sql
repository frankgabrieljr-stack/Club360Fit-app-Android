-- Separate "cleared" state for coach vs member on the same notification row.

ALTER TABLE public.client_notifications
  ADD COLUMN IF NOT EXISTS coach_read_at timestamptz;

COMMENT ON COLUMN public.client_notifications.read_at IS
  'When the member marked this row read (Updates inbox).';
COMMENT ON COLUMN public.client_notifications.coach_read_at IS
  'When the coach marked this row read (coach Hub / member hub bell).';

CREATE INDEX IF NOT EXISTS idx_client_notifications_coach_unread
  ON public.client_notifications (client_id, coach_read_at)
  WHERE coach_read_at IS NULL;
