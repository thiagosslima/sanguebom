-- ==============================================================================
-- 1. TABELAS DE DOMÍNIO E CONFIGURAÇÃO (Independente)
-- ==============================================================================

-- 1.1 Itens de Exame
INSERT INTO public.exam_item (id, code, name, unit, category, description, active, created_at) VALUES
                                                                                                   (1, 'GLI_JEJUM', 'Glicemia em Jejum', 'mg/dL', 'BIOQUIMICA', 'Avaliação dos níveis de glicose no sangue.', true, CURRENT_TIMESTAMP),
                                                                                                   (2, 'COL_TOTAL', 'Colesterol Total', 'mg/dL', 'LIPIDOGRAMA', 'Medição da quantidade total de colesterol.', true, CURRENT_TIMESTAMP);

-- 1.2 Unidades de Saúde
INSERT INTO public.health_unit (id, name, cnes, type, status, created_at) VALUES
                                                                              (1, 'Laboratório Central SP', '1234567', 'LABORATORY', 'ACTIVE', CURRENT_TIMESTAMP),
                                                                              (2, 'Clínica Vida Saudável', '7654321', 'CLINIC', 'ACTIVE', CURRENT_TIMESTAMP);

-- 1.3 Conquistas (Gamificação)
INSERT INTO public.achievement (id, code, name, description, active, created_at) VALUES
                                                                                     (1, 'FIRST_EXAM', 'Primeiro Passo', 'Realizou o seu primeiro exame na plataforma.', true, CURRENT_TIMESTAMP),
                                                                                     (2, 'HEALTH_HERO', 'Saúde de Ferro', 'Apresentou resultados 100% dentro da normalidade.', true, CURRENT_TIMESTAMP);


-- ==============================================================================
-- 2. REGRAS CLÍNICAS E FAIXAS DE REFERÊNCIA (Diretrizes SBD/SBC)
-- ==============================================================================

-- 2.1 Faixas base
INSERT INTO public.reference_range (id, exam_item_id, sex, age_min_years, age_max_years, "version", "source", valid_from, created_at) VALUES
                                                                                                                                          (1, 1, 'ALL', 18, 120, 'SBD_2023', 'Sociedade Brasileira de Diabetes (SBD)', '2023-01-01', CURRENT_TIMESTAMP),
                                                                                                                                          (2, 2, 'ALL', 20, 120, 'SBC_2020', 'Diretriz Brasileira de Dislipidemias (SBC)', '2020-01-01', CURRENT_TIMESTAMP);

-- 2.2 Regras (Glicemia)
INSERT INTO public."rule" (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, "level", score, description, "version", active, created_at) VALUES
                                                                                                                                                                       (1, 1, 0.0, 99.0, true, true, 'NORMAL', 0, 'Normoglicemia (Desejável)', '1.0', true, CURRENT_TIMESTAMP),
                                                                                                                                                                       (2, 1, 100.0, 125.0, true, true, 'ALERTA', 1.5, 'Glicemia de jejum alterada (Pré-diabetes)', '1.0', true, CURRENT_TIMESTAMP),
                                                                                                                                                                       (3, 1, 126.0, 999.0, true, false, 'CRITICO', 3.0, 'Diabetes Mellitus', '1.0', true, CURRENT_TIMESTAMP);

-- 2.3 Regras (Colesterol)
INSERT INTO public."rule" (id, reference_range_id, min_value, max_value, min_inclusive, max_inclusive, "level", score, description, "version", active, created_at) VALUES
                                                                                                                                                                       (4, 2, 0.0, 190.0, true, false, 'NORMAL', 0, 'Desejável (Para adultos > 20 anos)', '1.0', true, CURRENT_TIMESTAMP),
                                                                                                                                                                       (5, 2, 190.0, 999.0, true, false, 'ALERTA', 2.0, 'Elevado (Requer avaliação de frações)', '1.0', true, CURRENT_TIMESTAMP);


-- ==============================================================================
-- 3. PACIENTES E PERFIS DE SAÚDE
-- ==============================================================================

-- 3.1 Usuários do App
INSERT INTO public.app_user (id, cpf_hash, name, birth_date, email, status, created_at, updated_at) VALUES
                                                                                                        (1, 'hash_cpf_carlos123', 'Carlos Eduardo Mendes', '1980-04-10', 'carlos@email.com', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                        (2, 'hash_cpf_ana456', 'Ana Beatriz Silva', '1992-08-22', 'ana@email.com', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.2 Perfis de Saúde
INSERT INTO public.health_profile (id, user_id, sex, height_cm, weight_kg, exam_periodicity, risk_factors, created_at, updated_at) VALUES
                                                                                                                                       (1, 1, 'MALE', 178.0, 88.5, 'SEMESTERLY', '["SEDENTARY", "SMOKER"]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                       (2, 2, 'FEMALE', 165.0, 62.0, 'YEARLY', '[]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- ==============================================================================
-- 4. JORNADA DE EXAMES, RESULTADOS E AVALIAÇÕES
-- ==============================================================================

-- 4.1 Exames Realizados
INSERT INTO public.exam (id, user_id, health_unit_id, collected_at, released_at, status, external_reference, created_at) VALUES
                                                                                                                             (1, 1, 1, '2026-08-15 08:30:00', '2026-08-16 10:00:00', 'COMPLETED', 'LAB_C_9981', CURRENT_TIMESTAMP),
                                                                                                                             (2, 2, 2, '2026-08-18 07:45:00', '2026-08-19 09:15:00', 'COMPLETED', 'CLI_V_4422', CURRENT_TIMESTAMP);

-- 4.2 Resultados (2 itens por exame/usuário)
-- Resultados do Carlos (Exame 1 - Perfil Alterado)
INSERT INTO public.exam_result (id, exam_id, exam_item_id, value_numeric, unit, flag, created_at) VALUES
                                                                                                      (1, 1, 1, 115.0, 'mg/dL', 'ALERTA', CURRENT_TIMESTAMP), -- Pré-diabetes
                                                                                                      (2, 1, 2, 220.0, 'mg/dL', 'ALERTA', CURRENT_TIMESTAMP); -- Colesterol alto

-- Resultados da Ana (Exame 2 - Perfil Saudável)
INSERT INTO public.exam_result (id, exam_id, exam_item_id, value_numeric, unit, flag, created_at) VALUES
                                                                                                      (3, 2, 1, 88.0, 'mg/dL', 'NORMAL', CURRENT_TIMESTAMP), -- Normal
                                                                                                      (4, 2, 2, 160.0, 'mg/dL', 'NORMAL', CURRENT_TIMESTAMP); -- Normal

-- 4.3 Avaliações de Risco Geradas (Score calculado)
INSERT INTO public.risk_assessment (id, user_id, exam_id, score, "level", rules_version, explanation, created_at)
VALUES                                                                                                                      (1, 1, 1, 3.5, 'ALERTA', '1.0', 'Glicemia (1.5) e Colesterol (2.0) apresentam níveis de alerta.', CURRENT_TIMESTAMP),
                                                                                                                      (2, 2, 2, 0.0, 'NORMAL', '1.0', 'Todos os indicadores avaliados estão dentro da faixa desejável.', CURRENT_TIMESTAMP);

-- 4.4 Conquistas Desbloqueadas
INSERT INTO public.user_achievement (user_id, achievement_id, earned_at) VALUES
                                                                             (1, 1, CURRENT_TIMESTAMP), -- Carlos fez o 1º exame
                                                                             (2, 1, CURRENT_TIMESTAMP), -- Ana fez o 1º exame
                                                                             (2, 2, CURRENT_TIMESTAMP); -- Ana ganhou bônus de "Saúde de Ferro"


-- ==============================================================================
-- 5. SINCRONIZAÇÃO DAS SEQUÊNCIAS DE AUTO-INCREMENTO (CRÍTICO)
-- ==============================================================================
-- SELECT setval('public.exam_item_id_seq', (SELECT MAX(id) FROM public.exam_item));
-- SELECT setval('public.health_unit_id_seq', (SELECT MAX(id) FROM public.health_unit));
-- SELECT setval('public.achievement_id_seq', (SELECT MAX(id) FROM public.achievement));
-- SELECT setval('public.reference_range_id_seq', (SELECT MAX(id) FROM public.reference_range));
-- SELECT setval('public.rule_id_seq', (SELECT MAX(id) FROM public."rule"));
-- SELECT setval('public.app_user_id_seq', (SELECT MAX(id) FROM public.app_user));
-- SELECT setval('public.health_profile_id_seq', (SELECT MAX(id) FROM public.health_profile));
-- SELECT setval('public.exam_id_seq', (SELECT MAX(id) FROM public.exam));
-- SELECT setval('public.exam_result_id_seq', (SELECT MAX(id) FROM public.exam_result));
-- SELECT setval('public.risk_assessment_id_seq', (SELECT MAX(id) FROM public.risk_assessment));