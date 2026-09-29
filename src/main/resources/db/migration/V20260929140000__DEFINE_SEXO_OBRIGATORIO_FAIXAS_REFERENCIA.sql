UPDATE public.reference_range
SET sex = 'ALL'
WHERE sex IS NULL;

ALTER TABLE public.reference_range
    ALTER COLUMN sex SET NOT NULL;
