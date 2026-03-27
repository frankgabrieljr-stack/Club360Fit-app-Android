-- Store member workout notes and coach replies directly on workout_session_logs.
ALTER TABLE public.workout_session_logs
  ADD COLUMN IF NOT EXISTS note_to_coach text,
  ADD COLUMN IF NOT EXISTS coach_reply text,
  ADD COLUMN IF NOT EXISTS coach_replied_at timestamptz;

COMMENT ON COLUMN public.workout_session_logs.note_to_coach IS
  'Optional member note attached when logging a workout session.';
COMMENT ON COLUMN public.workout_session_logs.coach_reply IS
  'Optional coach reply to the member workout note.';
COMMENT ON COLUMN public.workout_session_logs.coach_replied_at IS
  'Timestamp when coach_reply was last updated.';

-- Coach needs update rights on coached clients' workout session logs for replies.
DROP POLICY IF EXISTS "coach_update_session_logs" ON public.workout_session_logs;
CREATE POLICY "coach_update_session_logs" ON public.workout_session_logs
  FOR UPDATE USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  )
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );
