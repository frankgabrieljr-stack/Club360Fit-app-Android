-- Allow coaches to create in-app notifications for clients they coach (e.g. meal photo feedback).

DROP POLICY IF EXISTS "coach_insert_client_notifications" ON public.client_notifications;
CREATE POLICY "coach_insert_client_notifications" ON public.client_notifications
  FOR INSERT TO authenticated
  WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );
