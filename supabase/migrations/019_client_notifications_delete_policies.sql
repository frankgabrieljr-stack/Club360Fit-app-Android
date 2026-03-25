-- Allow members and coaches to delete notification rows (swipe-to-delete in apps).

DROP POLICY IF EXISTS "client_delete_own_notifications" ON public.client_notifications;
CREATE POLICY "client_delete_own_notifications" ON public.client_notifications
  FOR DELETE TO authenticated
  USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
    AND visible_to_client = true
  );

DROP POLICY IF EXISTS "coach_delete_client_notifications" ON public.client_notifications;
CREATE POLICY "coach_delete_client_notifications" ON public.client_notifications
  FOR DELETE TO authenticated
  USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );
