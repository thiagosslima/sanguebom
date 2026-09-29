-- ==============================================================================
-- CF-350 / CF-352 - Motor de regras de gamificação
--
-- O código da conquista passou a ser mapeado como enum (AchivementCode) na
-- entidade Achievement. Os códigos semeados originalmente (FIRST_EXAM e
-- HEALTH_HERO) não existem no enum e quebrariam a leitura da tabela, então são
-- renomeados no lugar -- e não recriados -- para preservar as linhas de
-- public.user_achievement que já apontam para eles.
-- ==============================================================================

UPDATE public.achievement
SET code        = 'SANGUE_BOM',
    name        = 'Selo Sangue Bom',
    description = 'Conquistado ao ter o primeiro exame processado pelo Sangue Bom'
WHERE code = 'FIRST_EXAM';

UPDATE public.achievement
SET code        = 'HEALTH_CHAMPION',
    name        = 'Selo Campeão da Saúde',
    description = 'Conquistado ao obter uma média final igual ou menor do que 3'
WHERE code = 'HEALTH_HERO';

INSERT INTO public.achievement (id, code, name, description, active, created_at)
SELECT 3,
       'PUNCTUAL',
       'Selo Pontual',
       'Conquistado ao fazer dois ou mais exames dentro dos prazos estabelecidos',
       true,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM public.achievement WHERE code = 'PUNCTUAL');

-- Conquistas legadas fora do enum ficariam inacessíveis para a aplicação.
UPDATE public.achievement
SET active = false
WHERE code NOT IN ('SANGUE_BOM', 'HEALTH_CHAMPION', 'PUNCTUAL');
