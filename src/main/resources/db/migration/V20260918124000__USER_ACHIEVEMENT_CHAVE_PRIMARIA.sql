-- ==============================================================================
-- Da a user_achievement uma chave primaria propria.
--
-- A tabela nascia com PK composta (user_id, achievement_id) e a entidade usava
-- @EmbeddedId. O UserAchievementResource, porem, expoe /api/userAchievements/{id}
-- com um Long, e o UserAchievementRepository declara JpaRepository<..., Long>.
-- O resultado eram tres dos quatro handlers quebrados:
--
--   POST /api/userAchievements       500 - Identifier must be manually assigned
--                                          before calling 'persist()'
--   GET  /api/userAchievements/{id}  500 - Supplied id had wrong type
--   PUT  /api/userAchievements/{id}  500 - idem
--
-- E o quarto, a listagem, devolvia o achievement_id no campo id do DTO, entao
-- registros de cidadaos diferentes chegavam com o mesmo id.
--
-- A chave surrogate alinha a tabela com todas as demais do projeto, que usam
-- primary_sequence. O par (user_id, achievement_id) continua unico, agora como
-- constraint, preservando a garantia de que cada cidadao ganha cada conquista
-- uma unica vez.
-- ==============================================================================

ALTER TABLE public.user_achievement
    ADD COLUMN IF NOT EXISTS id BIGINT;

UPDATE public.user_achievement
SET id = nextval('primary_sequence')
WHERE id IS NULL;

ALTER TABLE public.user_achievement
    ALTER COLUMN id SET NOT NULL;

ALTER TABLE public.user_achievement
    DROP CONSTRAINT IF EXISTS user_achievement_pkey;

ALTER TABLE public.user_achievement
    ADD CONSTRAINT user_achievement_pkey PRIMARY KEY (id);

CREATE UNIQUE INDEX IF NOT EXISTS user_achievement_user_achievement_uidx
    ON public.user_achievement (user_id, achievement_id);
