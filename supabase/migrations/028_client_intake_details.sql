-- Migration 028: Persist richer client intake details for coach review.
-- Adds birthday and signup questionnaire fields to public.clients, then backfills
-- from auth.users metadata for existing accounts.

ALTER TABLE public.clients
  ADD COLUMN IF NOT EXISTS birth_date date,
  ADD COLUMN IF NOT EXISTS meals_per_day text,
  ADD COLUMN IF NOT EXISTS workout_frequency text;

CREATE OR REPLACE FUNCTION public.safe_auth_meta_date(meta jsonb)
RETURNS date
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
  raw text;
BEGIN
  raw := COALESCE(
    NULLIF(TRIM(meta->>'birth_date'), ''),
    NULLIF(TRIM(meta->>'birthday'), ''),
    NULLIF(TRIM(meta->>'date_of_birth'), ''),
    NULLIF(TRIM(meta->>'dob'), '')
  );

  IF raw IS NULL OR raw !~ '^\d{4}-\d{2}-\d{2}$' THEN
    RETURN NULL;
  END IF;

  RETURN raw::date;
EXCEPTION WHEN OTHERS THEN
  RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION public.sync_client_intake_details_from_auth()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  UPDATE public.clients
  SET
    phone = COALESCE(NULLIF(phone, ''), NULLIF(TRIM(NEW.raw_user_meta_data->>'phone'), '')),
    medical_conditions = COALESCE(
      NULLIF(medical_conditions, ''),
      NULLIF(TRIM(NEW.raw_user_meta_data->>'medical_conditions'), '')
    ),
    food_restrictions = COALESCE(
      NULLIF(food_restrictions, ''),
      NULLIF(TRIM(NEW.raw_user_meta_data->>'food_restrictions'), '')
    ),
    meals_per_day = COALESCE(
      NULLIF(meals_per_day, ''),
      NULLIF(TRIM(NEW.raw_user_meta_data->>'meals_per_day'), '')
    ),
    workout_frequency = COALESCE(
      NULLIF(workout_frequency, ''),
      NULLIF(TRIM(NEW.raw_user_meta_data->>'workout_frequency'), '')
    ),
    birth_date = COALESCE(birth_date, public.safe_auth_meta_date(NEW.raw_user_meta_data)),
    updated_at = now()
  WHERE user_id = NEW.id;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS zz_sync_client_intake_details_from_auth ON auth.users;
CREATE TRIGGER zz_sync_client_intake_details_from_auth
  AFTER INSERT OR UPDATE OF raw_user_meta_data ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.sync_client_intake_details_from_auth();

UPDATE public.clients c
SET
  phone = COALESCE(NULLIF(c.phone, ''), NULLIF(TRIM(u.raw_user_meta_data->>'phone'), '')),
  medical_conditions = COALESCE(
    NULLIF(c.medical_conditions, ''),
    NULLIF(TRIM(u.raw_user_meta_data->>'medical_conditions'), '')
  ),
  food_restrictions = COALESCE(
    NULLIF(c.food_restrictions, ''),
    NULLIF(TRIM(u.raw_user_meta_data->>'food_restrictions'), '')
  ),
  meals_per_day = COALESCE(
    NULLIF(c.meals_per_day, ''),
    NULLIF(TRIM(u.raw_user_meta_data->>'meals_per_day'), '')
  ),
  workout_frequency = COALESCE(
    NULLIF(c.workout_frequency, ''),
    NULLIF(TRIM(u.raw_user_meta_data->>'workout_frequency'), '')
  ),
  birth_date = COALESCE(c.birth_date, public.safe_auth_meta_date(u.raw_user_meta_data)),
  updated_at = now()
FROM auth.users u
WHERE c.user_id = u.id
  AND (
    NULLIF(c.phone, '') IS NULL
    OR NULLIF(c.medical_conditions, '') IS NULL
    OR NULLIF(c.food_restrictions, '') IS NULL
    OR NULLIF(c.meals_per_day, '') IS NULL
    OR NULLIF(c.workout_frequency, '') IS NULL
    OR c.birth_date IS NULL
  );
