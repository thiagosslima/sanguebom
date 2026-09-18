-- ==============================================================================
-- Unicidade de identificadores de negocio.
--
-- 1. app_user.cpf_hash e app_user.email
--    AppUserService.create salvava sem nenhuma checagem e as colunas nao tinham
--    constraint, entao o mesmo cidadao podia ser cadastrado indefinidamente.
--    Como o cpf_hash e a unica identificacao do cidadao, duplicatas fragmentam
--    o historico clinico entre registros diferentes.
--
-- 2. achievement.code
--    O code representa uma conquista unica (o enum AchivementCode tem 3
--    valores), mas nao havia constraint. Com duas linhas do mesmo code,
--    AchievementRepository.findByCode() passava a levantar
--    "Query did not return a unique result" e o POST /api/exams respondia 500
--    ao avaliar conquistas.
--
-- Antes de criar os indices, as duplicatas ja existentes sao neutralizadas com
-- um sufixo no registro mais novo. Nenhuma linha e apagada: exames, perfis e
-- avaliacoes de risco continuam apontando para os mesmos ids.
-- ==============================================================================

-- ------------------------------------------------------------------ app_user

UPDATE public.app_user a
SET cpf_hash = a.cpf_hash || '_dup_' || a.id
WHERE a.cpf_hash IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM public.app_user b
      WHERE b.cpf_hash = a.cpf_hash AND b.id < a.id
  );

UPDATE public.app_user a
SET email = a.email || '.dup' || a.id
WHERE a.email IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM public.app_user b
      WHERE lower(b.email) = lower(a.email) AND b.id < a.id
  );

-- NULL nao conflita com NULL em indice unico do PostgreSQL, entao cidadaos sem
-- e-mail ou sem cpf_hash continuam permitidos.
CREATE UNIQUE INDEX IF NOT EXISTS app_user_cpf_hash_uidx
    ON public.app_user (cpf_hash);

CREATE UNIQUE INDEX IF NOT EXISTS app_user_email_uidx
    ON public.app_user (lower(email));

-- --------------------------------------------------------------- achievement

-- Aqui nao da para sufixar o valor como foi feito no app_user: achievement.code
-- e mapeado com @Enumerated(EnumType.STRING) sobre AchivementCode, e gravar um
-- valor fora do enum recriaria o mesmo defeito que a V20260918120000 corrigiu.
--
-- As duplicatas so existem porque o POST aceitava code repetido, entao sao
-- descartadas, preservando o registro original de cada code (o de menor id, que
-- vem das cargas iniciais). As concessoes que apontavam para uma duplicata sao
-- removidas junto, por causa da FK; o motor de regras volta a conceder a
-- conquista na proxima avaliacao de exame.
DELETE FROM public.user_achievement ua
WHERE ua.achievement_id IN (
    SELECT a.id FROM public.achievement a
    WHERE EXISTS (
        SELECT 1 FROM public.achievement b
        WHERE b.code = a.code AND b.id < a.id
    )
);

DELETE FROM public.achievement a
WHERE EXISTS (
    SELECT 1 FROM public.achievement b
    WHERE b.code = a.code AND b.id < a.id
);

CREATE UNIQUE INDEX IF NOT EXISTS achievement_code_uidx
    ON public.achievement (code);
