-- ==============================================================================
-- CORRECAO DAS FAIXAS DE REFERENCIA COM SEX = 'ALL'
--
-- A carga inicial (V20260821195000) gravou as faixas da glicemia de jejum
-- (GLI_JEJUM) e do colesterol total (COL_TOTAL) com sex = 'ALL', valor que
-- nao existe no enum Sex. O motor de analise busca a faixa com
-- "rr.sex IS NULL OR rr.sex = :sex", entao essas faixas nunca eram
-- encontradas e a publicacao de exames com esses itens falhava com 404.
--
-- A convencao adotada nas cargas seguintes (V20260902100000) e sex NULL
-- para faixas que valem para ambos os sexos.
-- ==============================================================================

UPDATE public.reference_range
SET sex = NULL
WHERE sex = 'ALL';

-- ==============================================================================
-- CORRECAO DO NIVEL 'CRITICO' NAS REGRAS DA GLICEMIA DE JEJUM
--
-- A mesma carga gravou a regra de glicemia >= 126 mg/dL (diabetes) com
-- level = 'CRITICO', que tambem nao existe no enum ExamResultFlag. Com a faixa
-- passando a ser encontrada, o motor carrega todas as regras dela e a
-- conversao do enum falharia (500). O nivel equivalente na carga
-- GLYCEMIC_V1 para a mesma faixa de valores e HIGH.
-- ==============================================================================

UPDATE public."rule"
SET level = 'HIGH'
WHERE level = 'CRITICO';

UPDATE public.exam_result
SET flag = 'HIGH'
WHERE flag = 'CRITICO';
