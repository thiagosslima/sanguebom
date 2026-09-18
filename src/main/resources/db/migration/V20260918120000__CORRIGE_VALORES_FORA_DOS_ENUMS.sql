-- ==============================================================================
-- Corrige valores de seed que nao existem nos enums Java correspondentes.
--
-- A V20260821195000 gravou dois valores que nunca existiram no codigo. Como as
-- colunas sao mapeadas com @Enumerated(EnumType.STRING), qualquer consulta que
-- carregue essas linhas levanta "No enum constant" e devolve 500:
--
--   reference_range.sex = 'ALL'      -> enum Sex tem M, F, MALE, FEMALE
--   rule.level          = 'CRITICO'  -> enum ExamResultFlag nao tem CRITICO
--
-- Endpoints afetados:
--   GET /api/referenceRanges
--   GET /api/rules
--   GET /api/v1/exam-items/{GLI_JEJUM|COL_TOTAL}/reference-ranges
--
-- A correcao e feita aqui, e nao na migration original, porque ela ja foi
-- aplicada em outros ambientes e alterar o arquivo quebraria o checksum do
-- Flyway.
-- ==============================================================================

-- 1. sex: NULL ja e a convencao usada pela V20260902100000 para "vale para
--    ambos os sexos", e e o que a query findApplicableRangeForUserByExamItem
--    espera encontrar.
UPDATE public.reference_range
SET sex = NULL
WHERE sex = 'ALL';

-- 2. level: VERY_HIGH e o valor que as regras equivalentes da V20260902100000
--    usam para a faixa mais grave.
UPDATE public.rule
SET level = 'VERY_HIGH'
WHERE level = 'CRITICO';
