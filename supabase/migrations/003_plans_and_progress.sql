-- Club360Fit: workout_plans, meal_plans, progress_check_ins
-- Run in Supabase SQL Editor. Safe to run if tables already exist (IF NOT EXISTS).

-- ── workout_plans ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.workout_plans (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id  uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  title      text NOT NULL,
  week_start date NOT NULL,
  plan_text  text NOT NULL DEFAULT '',
  created_at timestamptz DEFAULT now()
);

ALTER TABLE public.workout_plans ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "coach_rw_workout_plans" ON public.workout_plans;
CREATE POLICY "coach_rw_workout_plans" ON public.workout_plans
  FOR ALL USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );
DROP POLICY IF EXISTS "client_r_workout_plans" ON public.workout_plans;
CREATE POLICY "client_r_workout_plans" ON public.workout_plans
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

-- ── meal_plans ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.meal_plans (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id  uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  title      text NOT NULL,
  week_start date NOT NULL,
  plan_text  text NOT NULL DEFAULT '',
  created_at timestamptz DEFAULT now()
);

ALTER TABLE public.meal_plans ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "coach_rw_meal_plans" ON public.meal_plans;
CREATE POLICY "coach_rw_meal_plans" ON public.meal_plans
  FOR ALL USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );
DROP POLICY IF EXISTS "client_r_meal_plans" ON public.meal_plans;
CREATE POLICY "client_r_meal_plans" ON public.meal_plans
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );

-- ── progress_check_ins (progress tracker) ─────────────────────────
CREATE TABLE IF NOT EXISTS public.progress_check_ins (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id     uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
  check_in_date date NOT NULL,
  weight_kg     numeric(5,2),
  notes         text DEFAULT '',
  workout_done  boolean DEFAULT false,
  meals_followed boolean DEFAULT false,
  created_at    timestamptz DEFAULT now()
);

ALTER TABLE public.progress_check_ins ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "coach_rw_progress" ON public.progress_check_ins;
CREATE POLICY "coach_rw_progress" ON public.progress_check_ins
  FOR ALL USING (
    client_id IN (SELECT id FROM public.clients WHERE coach_id = auth.uid())
  );
DROP POLICY IF EXISTS "client_r_progress" ON public.progress_check_ins;
CREATE POLICY "client_r_progress" ON public.progress_check_ins
  FOR SELECT USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );
DROP POLICY IF EXISTS "client_insert_progress" ON public.progress_check_ins;
CREATE POLICY "client_insert_progress" ON public.progress_check_ins
  FOR INSERT WITH CHECK (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );
DROP POLICY IF EXISTS "client_update_progress" ON public.progress_check_ins;
CREATE POLICY "client_update_progress" ON public.progress_check_ins
  FOR UPDATE USING (
    client_id IN (SELECT id FROM public.clients WHERE user_id = auth.uid())
  );
