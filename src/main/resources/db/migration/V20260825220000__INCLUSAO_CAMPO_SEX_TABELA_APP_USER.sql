-- ==============================================================================
-- 1. INCLUSÃO DO CAMPO SEX NA TABELA APP_USER
-- ==============================================================================

ALTER TABLE public.app_user
    ADD COLUMN sex VARCHAR(1);

-- ==============================================================================
-- 2. INCLUSÃO DA CONSTRAINT PARA VALIDAR O CAMPO SEX
-- ==============================================================================

ALTER TABLE public.app_user
    ADD CONSTRAINT app_user_sex_check
        CHECK (sex IS NULL OR sex IN ('M', 'F'));

-- ==============================================================================
-- 3. INCLUSÃO DE DADOS INICIAIS PARA TESTES
-- ==============================================================================

UPDATE public.app_user
SET sex = 'M'
WHERE id = 1
  AND sex IS NULL;

UPDATE public.app_user
SET sex = 'F'
WHERE id = 2
  AND sex IS NULL;