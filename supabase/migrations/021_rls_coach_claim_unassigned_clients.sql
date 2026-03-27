-- New signups may have public.clients.coach_id NULL until a coach claims them.
-- Existing RLS only allowed plans when coach_id = auth.uid(), so meal/workout inserts failed.
-- This adds: JWT role check (admin = coach in this app), SELECT/UPDATE on clients, and widened plan policies.

CREATE OR REPLACE FUNCTION public.club360_is_coach_or_admin()
RETURNS boolean
LANGUAGE sql
STABLE
SET search_path = public
AS $$
  SELECT COALESCE(
    (auth.jwt() -> 'user_metadata' ->> 'role') = 'admin',
    false
  );
$$;

GRANT EXECUTE ON FUNCTION public.club360_is_coach_or_admin() TO authenticated;

-- Coaches (JWT role admin) can see unassigned client rows so they appear in the client list.
DROP POLICY IF EXISTS "coaches_select_unassigned_clients" ON public.clients;
CREATE POLICY "coaches_select_unassigned_clients"
  ON public.clients FOR SELECT
  USING (coach_id IS NULL AND public.club360_is_coach_or_admin());

-- Claim: set coach_id to the signed-in coach when still NULL.
DROP POLICY IF EXISTS "coaches_claim_unassigned_clients" ON public.clients;
CREATE POLICY "coaches_claim_unassigned_clients"
  ON public.clients FOR UPDATE
  USING (coach_id IS NULL AND public.club360_is_coach_or_admin())
  WITH CHECK (auth.uid() = coach_id);

-- meal_plans: coach owns row OR client is unclaimed and viewer is coach (admin JWT).
DROP POLICY IF EXISTS "coach_rw_meal_plans" ON public.meal_plans;
CREATE POLICY "coach_rw_meal_plans" ON public.meal_plans
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM public.clients c
      WHERE c.id = client_id
        AND (
          c.coach_id = auth.uid()
          OR (c.coach_id IS NULL AND public.club360_is_coach_or_admin())
        )
    )
  );

DROP POLICY IF EXISTS "coach_rw_workout_plans" ON public.workout_plans;
CREATE POLICY "coach_rw_workout_plans" ON public.workout_plans
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM public.clients c
      WHERE c.id = client_id
        AND (
          c.coach_id = auth.uid()
          OR (c.coach_id IS NULL AND public.club360_is_coach_or_admin())
        )
    )
  );

DROP POLICY IF EXISTS "coach_rw_progress" ON public.progress_check_ins;
CREATE POLICY "coach_rw_progress" ON public.progress_check_ins
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM public.clients c
      WHERE c.id = client_id
        AND (
          c.coach_id = auth.uid()
          OR (c.coach_id IS NULL AND public.club360_is_coach_or_admin())
        )
    )
  );
