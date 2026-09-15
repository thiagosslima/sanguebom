-- ==============================================================================
-- CF-323 - Carga de itens de exame (valores de referencia) e geracao de exames
--          historicos para os cidadaos ja cadastrados.
--
-- Baseado no arquivo de referencia "carga-valores-referencia.sql":
--   1. Itens de exame do perfil GLICEMICO e LIPIDICO (9 itens).
--   2. Faixas de referencia (reference_range) para cada item.
--   3. Regras clinicas (rule) para cada faixa de referencia.
--
-- Alem disso, esta migration inclui:
--   4. 2 novos itens de exame do perfil RENAL (CREATININE e UREA), com suas
--      respectivas faixas de referencia e regras.
--   5. Para cada cidadao (public.app_user) ja existente, a criacao de 3 exames
--      em datas distintas (180, 90 e 0 dias atras), cada um com um resultado
--      "NORMAL" para todos os 11 itens de exame acima.
--
-- Todos os IDs novos sao gerados a partir da sequence "primary_sequence"
-- (a mesma utilizada pelas entidades JPA), evitando colisao com os IDs fixos
-- (1, 2, ...) inseridos pela migration V20260821195000__INSERT_INICIAL_DADOS.
-- ==============================================================================


-- ==============================================================================
-- 1. ITENS DE EXAME - PERFIL GLICEMICO E LIPIDICO
-- ==============================================================================

INSERT INTO public.exam_item (
    id,
    code,
    name,
    unit,
    category,
    description,
    active,
    created_at
)
VALUES
(
    nextval('primary_sequence'),
    'GLUCOSE',
    'Glicose',
    'mg/dL',
    'GLYCEMIC',
    'Concentração de glicose no sangue.',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    'HBA1C',
    'Hemoglobina glicada',
    '%',
    'GLYCEMIC',
    'Percentual de hemoglobina glicada utilizado para avaliação do controle glicêmico.',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    'INSULIN',
    'Insulina',
    'µUI/mL',
    'GLYCEMIC',
    'Concentração de insulina no sangue.',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    'ESTIMATED_AVERAGE_GLUCOSE',
    'Glicose média estimada',
    'mg/dL',
    'GLYCEMIC',
    'Estimativa da concentração média de glicose derivada principalmente da hemoglobina glicada.',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    'TOTAL_CHOLESTEROL',
    'Colesterol total',
    'mg/dL',
    'LIPID',
    'Concentração de colesterol total no sangue.',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    'HDL',
    'HDL colesterol',
    'mg/dL',
    'LIPID',
    'Colesterol associado às lipoproteínas de alta densidade.',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    'LDL',
    'LDL colesterol',
    'mg/dL',
    'LIPID',
    'Colesterol associado às lipoproteínas de baixa densidade.',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    'TRIGLYCERIDES',
    'Triglicerídeos',
    'mg/dL',
    'LIPID',
    'Concentração de triglicerídeos no sangue.',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    'VLDL',
    'VLDL colesterol',
    'mg/dL',
    'LIPID',
    'Colesterol associado às lipoproteínas de muito baixa densidade.',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ==============================================================================
-- 2. NOVOS ITENS DE EXAME - PERFIL RENAL
-- ==============================================================================

INSERT INTO public.exam_item (
    id,
    code,
    name,
    unit,
    category,
    description,
    active,
    created_at
)
VALUES
(
    nextval('primary_sequence'),
    'CREATININE',
    'Creatinina',
    'mg/dL',
    'RENAL',
    'Concentração de creatinina no sangue, utilizada para avaliação da função renal.',
    TRUE,
    CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    'UREA',
    'Ureia',
    'mg/dL',
    'RENAL',
    'Concentração de ureia no sangue, utilizada para avaliação da função renal.',
    TRUE,
    CURRENT_TIMESTAMP
);


-- ==============================================================================
-- 3. FAIXAS DE REFERENCIA (reference_range)
-- Adultos: 21 a 59 anos - MVP - sem diferenciação por sexo
-- ==============================================================================

INSERT INTO public.reference_range (
    id,
    exam_item_id,
    sex,
    age_min_years,
    age_max_years,
    version,
    source,
    valid_from,
    valid_until,
    created_at
)
VALUES
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'GLUCOSE'),
    NULL, 21, 59, 'GLYCEMIC_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'HBA1C'),
    NULL, 21, 59, 'GLYCEMIC_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'INSULIN'),
    NULL, 21, 59, 'GLYCEMIC_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'ESTIMATED_AVERAGE_GLUCOSE'),
    NULL, 21, 59, 'GLYCEMIC_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'TOTAL_CHOLESTEROL'),
    NULL, 21, 59, 'LIPID_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'HDL'),
    NULL, 21, 59, 'LIPID_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'LDL'),
    NULL, 21, 59, 'LIPID_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'TRIGLYCERIDES'),
    NULL, 21, 59, 'LIPID_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'VLDL'),
    NULL, 21, 59, 'LIPID_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'CREATININE'),
    NULL, 21, 59, 'RENAL_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
),
(
    nextval('primary_sequence'),
    (SELECT id FROM public.exam_item WHERE code = 'UREA'),
    NULL, 21, 59, 'RENAL_V1', 'Referência convencional para MVP', CURRENT_DATE, NULL, CURRENT_TIMESTAMP
);


-- ==============================================================================
-- 4. REGRAS CLINICAS (rule)
-- ==============================================================================

-- GLICOSE:  <70 LOW | 70-99 NORMAL | 100-125 ATTENTION | >=126 HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'GLUCOSE' AND rr.version = 'GLYCEMIC_V1'),
 NULL, 70, FALSE, FALSE, 'LOW', 4, 'Resultado abaixo da faixa esperada. Recomenda-se avaliação profissional.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'GLUCOSE' AND rr.version = 'GLYCEMIC_V1'),
 70, 100, TRUE, FALSE, 'NORMAL', 1, 'Resultado dentro da faixa esperada.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'GLUCOSE' AND rr.version = 'GLYCEMIC_V1'),
 100, 126, TRUE, FALSE, 'ATTENTION', 6, 'Resultado acima da faixa esperada. Recomenda-se acompanhamento profissional.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'GLUCOSE' AND rr.version = 'GLYCEMIC_V1'),
 126, NULL, TRUE, FALSE, 'HIGH', 10, 'Resultado elevado. Recomenda-se avaliação por profissional de saúde.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP);

-- HBA1C: <5.7 NORMAL | 5.7-6.4 ATTENTION | >=6.5 HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'HBA1C' AND rr.version = 'GLYCEMIC_V1'),
 NULL, 5.7, FALSE, FALSE, 'NORMAL', 1, 'Hemoglobina glicada dentro da faixa esperada.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'HBA1C' AND rr.version = 'GLYCEMIC_V1'),
 5.7, 6.5, TRUE, FALSE, 'ATTENTION', 6, 'Hemoglobina glicada acima da faixa esperada.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'HBA1C' AND rr.version = 'GLYCEMIC_V1'),
 6.5, NULL, TRUE, FALSE, 'HIGH', 10, 'Resultado elevado. Recomenda-se avaliação por profissional de saúde.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP);

-- INSULINA: <2 LOW | 2-25 NORMAL | 25-40 ATTENTION | >=40 HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'INSULIN' AND rr.version = 'GLYCEMIC_V1'),
 NULL, 2, FALSE, FALSE, 'LOW', 5, 'Resultado abaixo da faixa operacional definida para o MVP.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'INSULIN' AND rr.version = 'GLYCEMIC_V1'),
 2, 25, TRUE, FALSE, 'NORMAL', 1, 'Resultado dentro da faixa operacional definida para o MVP.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'INSULIN' AND rr.version = 'GLYCEMIC_V1'),
 25, 40, TRUE, FALSE, 'ATTENTION', 6, 'Resultado acima da faixa operacional. Recomenda-se acompanhamento profissional.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'INSULIN' AND rr.version = 'GLYCEMIC_V1'),
 40, NULL, TRUE, FALSE, 'HIGH', 10, 'Resultado elevado segundo a referência operacional do MVP.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP);

-- GLICOSE MEDIA ESTIMADA: <117 NORMAL | 117-137 ATTENTION | >=137 HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'ESTIMATED_AVERAGE_GLUCOSE' AND rr.version = 'GLYCEMIC_V1'),
 NULL, 117, FALSE, FALSE, 'NORMAL', 1, 'Glicose média estimada dentro da faixa esperada.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'ESTIMATED_AVERAGE_GLUCOSE' AND rr.version = 'GLYCEMIC_V1'),
 117, 137, TRUE, FALSE, 'ATTENTION', 6, 'Glicose média estimada acima da faixa esperada.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'ESTIMATED_AVERAGE_GLUCOSE' AND rr.version = 'GLYCEMIC_V1'),
 137, NULL, TRUE, FALSE, 'HIGH', 10, 'Glicose média estimada elevada.', 'GLYCEMIC_V1', TRUE, CURRENT_TIMESTAMP);

-- COLESTEROL TOTAL: <190 NORMAL | 190-239 ATTENTION | >=240 HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'TOTAL_CHOLESTEROL' AND rr.version = 'LIPID_V1'),
 NULL, 190, FALSE, FALSE, 'NORMAL', 1, 'Colesterol total dentro da faixa desejável.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'TOTAL_CHOLESTEROL' AND rr.version = 'LIPID_V1'),
 190, 240, TRUE, FALSE, 'ATTENTION', 6, 'Colesterol total acima da faixa desejável.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'TOTAL_CHOLESTEROL' AND rr.version = 'LIPID_V1'),
 240, NULL, TRUE, FALSE, 'HIGH', 10, 'Colesterol total elevado. Recomenda-se avaliação por profissional de saúde.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP);

-- HDL: <40 LOW | 40-59 NORMAL | >=60 ALTO (protetor)
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'HDL' AND rr.version = 'LIPID_V1'),
 NULL, 40, FALSE, FALSE, 'LOW', 6, 'HDL abaixo da faixa desejável. Recomenda-se acompanhamento profissional.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'HDL' AND rr.version = 'LIPID_V1'),
 40, 60, TRUE, FALSE, 'NORMAL', 1, 'HDL dentro da faixa desejável.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'HDL' AND rr.version = 'LIPID_V1'),
 60, NULL, TRUE, FALSE, 'NORMAL', 0, 'HDL em faixa protetora.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP);

-- LDL: <100 NORMAL | 100-159 ATTENTION | >=160 HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'LDL' AND rr.version = 'LIPID_V1'),
 NULL, 100, FALSE, FALSE, 'NORMAL', 1, 'LDL dentro da faixa desejável.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'LDL' AND rr.version = 'LIPID_V1'),
 100, 160, TRUE, FALSE, 'ATTENTION', 6, 'LDL acima da faixa desejável.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'LDL' AND rr.version = 'LIPID_V1'),
 160, NULL, TRUE, FALSE, 'HIGH', 10, 'LDL elevado. Recomenda-se avaliação por profissional de saúde.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP);

-- TRIGLICERIDEOS: <150 NORMAL | 150-199 ATTENTION | 200-499 HIGH | >=500 VERY_HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'TRIGLYCERIDES' AND rr.version = 'LIPID_V1'),
 NULL, 150, FALSE, FALSE, 'NORMAL', 1, 'Triglicerídeos dentro da faixa desejável.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'TRIGLYCERIDES' AND rr.version = 'LIPID_V1'),
 150, 200, TRUE, FALSE, 'ATTENTION', 6, 'Triglicerídeos limítrofes.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'TRIGLYCERIDES' AND rr.version = 'LIPID_V1'),
 200, 500, TRUE, FALSE, 'HIGH', 8, 'Triglicerídeos elevados. Recomenda-se acompanhamento profissional.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'TRIGLYCERIDES' AND rr.version = 'LIPID_V1'),
 500, NULL, TRUE, FALSE, 'VERY_HIGH', 10, 'Triglicerídeos muito elevados. Recomenda-se avaliação por profissional de saúde.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP);

-- VLDL: <30 NORMAL | 30-40 ATTENTION | >=40 HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'VLDL' AND rr.version = 'LIPID_V1'),
 NULL, 30, FALSE, FALSE, 'NORMAL', 1, 'VLDL dentro da faixa esperada.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'VLDL' AND rr.version = 'LIPID_V1'),
 30, 40, TRUE, FALSE, 'ATTENTION', 6, 'VLDL acima da faixa esperada.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'VLDL' AND rr.version = 'LIPID_V1'),
 40, NULL, TRUE, FALSE, 'HIGH', 10, 'VLDL elevado.', 'LIPID_V1', TRUE, CURRENT_TIMESTAMP);

-- CREATININA: <0.6 LOW | 0.6-1.19 NORMAL | 1.2-1.99 ATTENTION | >=2.0 HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'CREATININE' AND rr.version = 'RENAL_V1'),
 NULL, 0.6, FALSE, FALSE, 'LOW', 4, 'Creatinina abaixo da faixa esperada.', 'RENAL_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'CREATININE' AND rr.version = 'RENAL_V1'),
 0.6, 1.2, TRUE, FALSE, 'NORMAL', 1, 'Creatinina dentro da faixa esperada.', 'RENAL_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'CREATININE' AND rr.version = 'RENAL_V1'),
 1.2, 2.0, TRUE, FALSE, 'ATTENTION', 6, 'Creatinina acima da faixa esperada. Recomenda-se acompanhamento profissional.', 'RENAL_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'CREATININE' AND rr.version = 'RENAL_V1'),
 2.0, NULL, TRUE, FALSE, 'HIGH', 10, 'Creatinina elevada. Recomenda-se avaliação por profissional de saúde.', 'RENAL_V1', TRUE, CURRENT_TIMESTAMP);

-- UREIA: <10 LOW | 10-49 NORMAL | 50-99 ATTENTION | >=100 HIGH
INSERT INTO public.rule (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, level, score, description, version, active, created_at)
VALUES
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'UREA' AND rr.version = 'RENAL_V1'),
 NULL, 10, FALSE, FALSE, 'LOW', 4, 'Ureia abaixo da faixa esperada.', 'RENAL_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'UREA' AND rr.version = 'RENAL_V1'),
 10, 50, TRUE, FALSE, 'NORMAL', 1, 'Ureia dentro da faixa esperada.', 'RENAL_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'UREA' AND rr.version = 'RENAL_V1'),
 50, 100, TRUE, FALSE, 'ATTENTION', 6, 'Ureia acima da faixa esperada. Recomenda-se acompanhamento profissional.', 'RENAL_V1', TRUE, CURRENT_TIMESTAMP),
(nextval('primary_sequence'), (SELECT rr.id FROM public.reference_range rr JOIN public.exam_item ei ON ei.id = rr.exam_item_id WHERE ei.code = 'UREA' AND rr.version = 'RENAL_V1'),
 100, NULL, TRUE, FALSE, 'HIGH', 10, 'Ureia elevada. Recomenda-se avaliação por profissional de saúde.', 'RENAL_V1', TRUE, CURRENT_TIMESTAMP);


-- ==============================================================================
-- 5. GERACAO DE 3 EXAMES (EM DATAS DISTINTAS) PARA CADA CIDADAO EXISTENTE
--    Cada exame recebe um resultado "NORMAL" para os 11 itens de exame acima.
-- ==============================================================================

DO $$
DECLARE
    v_health_unit_id BIGINT;
    v_user           RECORD;
    v_item           RECORD;
    v_days_ago        INT;
    v_offsets         INT[] := ARRAY[180, 90, 0]; -- exame mais antigo -> mais recente
    v_collected_at    TIMESTAMP;
    v_exam_id         BIGINT;
    v_value           NUMERIC;
BEGIN
    -- Unidade de saúde utilizada para os exames gerados automaticamente
    SELECT id INTO v_health_unit_id FROM public.health_unit ORDER BY id LIMIT 1;

    IF v_health_unit_id IS NULL THEN
        RAISE NOTICE 'Nenhuma health_unit encontrada. Exames não foram gerados.';
        RETURN;
    END IF;

    FOR v_user IN SELECT id FROM public.app_user ORDER BY id LOOP
        FOREACH v_days_ago IN ARRAY v_offsets LOOP
            v_collected_at := date_trunc('minute', CURRENT_TIMESTAMP) - (v_days_ago || ' days')::interval;
            v_exam_id := nextval('primary_sequence');

            INSERT INTO public.exam (
                id, user_id, health_unit_id, collected_at, released_at, status, external_reference, created_at
            )
            VALUES (
                v_exam_id,
                v_user.id,
                v_health_unit_id,
                v_collected_at,
                v_collected_at + interval '1 day',
                'COMPLETED',
                'AUTO_GEN_' || v_exam_id,
                CURRENT_TIMESTAMP
            );

            FOR v_item IN
                SELECT id, code, unit
                FROM public.exam_item
                WHERE code IN (
                    'GLUCOSE', 'HBA1C', 'INSULIN', 'ESTIMATED_AVERAGE_GLUCOSE',
                    'TOTAL_CHOLESTEROL', 'HDL', 'LDL', 'TRIGLYCERIDES', 'VLDL',
                    'CREATININE', 'UREA'
                )
            LOOP
                v_value := CASE v_item.code
                    WHEN 'GLUCOSE' THEN 88
                    WHEN 'HBA1C' THEN 5.2
                    WHEN 'INSULIN' THEN 10
                    WHEN 'ESTIMATED_AVERAGE_GLUCOSE' THEN 100
                    WHEN 'TOTAL_CHOLESTEROL' THEN 170
                    WHEN 'HDL' THEN 55
                    WHEN 'LDL' THEN 95
                    WHEN 'TRIGLYCERIDES' THEN 120
                    WHEN 'VLDL' THEN 20
                    WHEN 'CREATININE' THEN 0.9
                    WHEN 'UREA' THEN 30
                END;

                INSERT INTO public.exam_result (
                    id, exam_id, exam_item_id, value_numeric, unit, flag, created_at
                )
                VALUES (
                    nextval('primary_sequence'),
                    v_exam_id,
                    v_item.id,
                    v_value,
                    v_item.unit,
                    'NORMAL',
                    CURRENT_TIMESTAMP
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
