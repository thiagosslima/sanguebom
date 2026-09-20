-- Carga ADITIVA de demonstracao: 34 cidadaos ficticios, identificados por
-- nome "(Demo)" e e-mail @demo.sanguebom.example. Nao altera dados anteriores,
-- faixas de referencia nem regras clinicas. IDs usam a sequence do JPA.
--
-- As datas sao relativas ao DIA DA EXECUCAO. Metas envelhecem normalmente;
-- esta migration versionada nao e um processo de renovacao diaria de fixtures.
-- As classificacoes sao calculadas das regras ja existentes, nao constituem
-- recomendacao medica. Casos importados/legados estao explicitamente separados.
-- Inventario e limites da cobertura: docs/CENARIOS_DEMONSTRACAO.md.

CREATE TEMP TABLE demo_citizen_scenarios (
    scenario text PRIMARY KEY,
    name text NOT NULL,
    age_years integer NOT NULL,
    sex varchar(1) NOT NULL,
    periodicity varchar(20) NOT NULL,
    mode text NOT NULL,
    exam_count integer NOT NULL,
    due_days integer,
    user_id bigint
) ON COMMIT DROP;

INSERT INTO demo_citizen_scenarios
    (scenario, name, age_years, sex, periodicity, mode, exam_count, due_days)
VALUES
('sem_historico', 'Alice Ferreira', 28, 'F', 'YEARLY', 'none', 0, NULL),
('baixo', 'Bruno Almeida', 35, 'M', 'SEMESTERLY', 'normal', 1, NULL),
('moderado', 'Camila Santos', 42, 'F', 'QUARTERLY', 'moderate', 1, NULL),
('alto', 'Diego Oliveira', 48, 'M', 'QUARTERLY', 'high', 1, NULL),
('muito_alto', 'Elisa Costa', 54, 'F', 'QUARTERLY', 'very_high', 1, NULL),
('vence_15', 'Fabio Rodrigues', 31, 'M', 'SEMESTERLY', 'normal', 1, 15),
('vence_hoje', 'Gabriela Lima', 39, 'F', 'YEARLY', 'normal', 1, 0),
('vence_30', 'Henrique Barbosa', 33, 'M', 'QUARTERLY', 'normal', 1, 30),
('vence_31', 'Isabela Rocha', 37, 'F', 'QUARTERLY', 'normal', 1, 31),
('venceu_ontem', 'Joao Pereira', 46, 'M', 'SEMESTERLY', 'high', 1, -1),
('atrasado', 'Karina Martins', 45, 'F', 'QUARTERLY', 'high', 1, -60),
('coletado', 'Lucas Ribeiro', 26, 'M', 'YEARLY', 'collected', 1, NULL),
('em_analise', 'Mariana Araujo', 29, 'F', 'YEARLY', 'analysis', 1, NULL),
('melhora', 'Nicolas Fernandes', 40, 'M', 'QUARTERLY', 'improving', 3, NULL),
('piora', 'Olivia Gomes', 50, 'F', 'QUARTERLY', 'worsening', 3, NULL),
('estavel', 'Pedro Carvalho', 38, 'M', 'YEARLY', 'normal', 3, NULL),
('historico_extenso', 'Rafaela Nascimento', 44, 'F', 'QUARTERLY', 'long', 25, NULL),
('itens_diferentes', 'Samuel Teixeira', 47, 'M', 'YEARLY', 'mixed', 2, NULL),
('valor_zero', 'Tatiana Moreira', 32, 'F', 'YEARLY', 'zero', 2, NULL),
('limites_marcadores', 'Vinicius Cardoso', 43, 'M', 'QUARTERLY', 'boundaries', 12, NULL),
('score_zero', 'Yasmin Freitas', 30, 'F', 'YEARLY', 'score_zero', 1, NULL),
('score_tres', 'Andre Batista', 36, 'M', 'SEMESTERLY', 'score_three', 1, NULL),
('score_oito', 'Beatriz Duarte', 41, 'F', 'QUARTERLY', 'score_eight', 1, NULL),
('sem_perfil', 'Caio Mendes', 34, 'M', 'YEARLY', 'no_profile', 0, NULL),
('sem_sexo', 'Daniela Pinto', 33, 'F', 'YEARLY', 'no_sex', 0, NULL),
('sem_nascimento', 'Eduardo Vieira', 35, 'M', 'YEARLY', 'no_birth', 0, NULL),
('menor_faixa', 'Fernanda Campos', 18, 'F', 'YEARLY', 'none', 0, NULL),
('maior_faixa', 'Gustavo Lopes', 70, 'M', 'SEMESTERLY', 'none', 0, NULL),
('inativo', 'Helena Machado', 36, 'F', 'YEARLY', 'inactive', 1, -45),
('resultado_textual', 'Igor Azevedo', 29, 'M', 'YEARLY', 'text', 2, NULL),
('sem_avaliacao', 'Julia Monteiro', 32, 'F', 'YEARLY', 'no_assessment', 1, NULL),
('legado', 'Leandro Correia', 45, 'M', 'YEARLY', 'legacy', 2, NULL),
('otimo_importado', 'Luana Dias', 39, 'F', 'YEARLY', 'imported_optimal', 1, NULL),
('pontual_alterado', 'Marcelo Reis', 46, 'M', 'QUARTERLY', 'high', 2, NULL);

DO $$
DECLARE
    s record;
    item record;
    matched record;
    unit_id bigint;
    citizen_id bigint;
    exam_id_value bigint;
    item_id_value bigint;
    item_unit text;
    item_count integer;
    candidate_count integer;
    months integer;
    periodicity_value text;
    last_collection date;
    collected timestamp;
    released timestamp;
    exam_status text;
    scenario_mode text;
    values_json jsonb;
    result_flag text;
    total_score numeric;
    final_score numeric;
    risk_level text;
    explanation_value text;
    j integer;
    stamp timestamp := CURRENT_TIMESTAMP;
    base_date date := CURRENT_DATE;
    birth date;
    boundary_values numeric[] := ARRAY[69.99999,70,99.99999,100,125.99999,126,
                                       149.99999,150,199.99999,200,499.99999,500];
BEGIN
    -- Nao reutilizar um cadastro anterior com e-mail coincidente.
    IF EXISTS (SELECT 1 FROM public.app_user u JOIN demo_citizen_scenarios d
               ON lower(u.email) = d.scenario || '@demo.sanguebom.example') THEN
        RAISE EXCEPTION 'E-mail reservado da carga demo ja existe; nenhum cadastro foi substituido';
    END IF;
    IF (SELECT count(*) FROM public.achievement
        WHERE code IN ('SANGUE_BOM','HEALTH_CHAMPION','PUNCTUAL') AND active) <> 3 THEN
        RAISE EXCEPTION 'A carga demo requer as tres conquistas ativas das migrations anteriores';
    END IF;

    FOR s IN SELECT * FROM demo_citizen_scenarios ORDER BY scenario LOOP
        citizen_id := nextval('public.primary_sequence');
        UPDATE demo_citizen_scenarios SET user_id = citizen_id WHERE scenario = s.scenario;
        months := CASE s.periodicity WHEN 'QUARTERLY' THEN 3 WHEN 'SEMESTERLY' THEN 6 ELSE 12 END;
        periodicity_value := s.periodicity;
        last_collection := base_date - 2;
        IF s.due_days IS NOT NULL THEN
            last_collection := (base_date + s.due_days - make_interval(months => months))::date;
            -- plusMonths do Java e o PostgreSQL ajustam o ultimo dia do mes.
            -- Escolher uma periodicidade reversivel garante os limites 0/30/31/-1
            -- mesmo em fevereiro/anos bissextos, sem produzir coleta futura.
            IF (last_collection + make_interval(months => months))::date <> base_date + s.due_days THEN
                SELECT m INTO STRICT months FROM unnest(ARRAY[3,6,12]) AS candidates(m)
                WHERE ((base_date + s.due_days - make_interval(months => m))::date
                       + make_interval(months => m))::date = base_date + s.due_days
                ORDER BY m LIMIT 1;
                periodicity_value := CASE months WHEN 3 THEN 'QUARTERLY' WHEN 6 THEN 'SEMESTERLY' ELSE 'YEARLY' END;
                last_collection := (base_date + s.due_days - make_interval(months => months))::date;
            END IF;
        END IF;
        birth := CASE WHEN s.mode = 'no_birth' THEN NULL
                      ELSE (base_date - make_interval(years => s.age_years, days => 60))::date END;
        INSERT INTO public.app_user
            (id,cpf_hash,name,birth_date,email,status,sex,created_at,updated_at)
        VALUES (citizen_id,encode(sha256(convert_to('SANGUEBOM_DEMO_20260919:' || s.scenario,'UTF8')),'hex'),
                s.name || ' (Demo)',birth,s.scenario || '@demo.sanguebom.example',
                CASE WHEN s.mode = 'inactive' THEN 'INACTIVE' ELSE 'ACTIVE' END,s.sex,
                stamp - interval '3 years',stamp);
        IF s.mode <> 'no_profile' THEN
            INSERT INTO public.health_profile
                (id,user_id,sex,height_cm,weight_kg,exam_periodicity,risk_factors,created_at,updated_at)
            VALUES (nextval('public.primary_sequence'),citizen_id,
                    CASE WHEN s.mode = 'no_sex' THEN NULL ELSE s.sex END,
                    CASE WHEN s.sex = 'F' THEN 165 ELSE 178 END,
                    CASE WHEN s.mode IN ('high','very_high','worsening') THEN 92 ELSE 68 END,
                    periodicity_value,
                    CASE WHEN s.mode IN ('high','very_high','worsening')
                         THEN '["SEDENTARY","FAMILY_HISTORY"]'::jsonb ELSE '[]'::jsonb END,
                    stamp - interval '3 years',stamp);
        END IF;

        FOR j IN 1..s.exam_count LOOP
            -- Alternar unidades existentes, sem depender de IDs fixos.
            SELECT id INTO unit_id FROM public.health_unit ORDER BY id
            OFFSET ((j - 1) % GREATEST((SELECT count(*)::integer FROM public.health_unit),1)) LIMIT 1;
            IF unit_id IS NULL THEN RAISE EXCEPTION 'Unidade de saude obrigatoria para a carga demo'; END IF;
            collected := last_collection::timestamp + interval '8 hours'
                - make_interval(days => (s.exam_count - j) * CASE
                    WHEN s.mode = 'long' THEN 28 WHEN s.mode = 'boundaries' THEN 7 ELSE 70 END);
            exam_status := CASE s.mode WHEN 'collected' THEN 'COLLECTED'
                WHEN 'analysis' THEN 'IN_ANALYSIS' WHEN 'text' THEN 'IN_ANALYSIS'
                WHEN 'legacy' THEN 'COMPLETED' ELSE 'RELEASED' END;
            released := CASE WHEN exam_status IN ('RELEASED','COMPLETED')
                             THEN collected + interval '4 hours' ELSE NULL END;
            exam_id_value := nextval('public.primary_sequence');
            INSERT INTO public.exam
                (id,user_id,health_unit_id,collected_at,released_at,status,external_reference,created_at)
            VALUES (exam_id_value,citizen_id,unit_id,collected,released,exam_status,
                    'DEMO_20260919:' || s.scenario || ':' || j,collected);
            IF s.mode = 'collected' THEN CONTINUE; END IF;

            scenario_mode := CASE
                WHEN s.mode = 'improving' THEN (ARRAY['very_high','high','normal'])[j]
                WHEN s.mode = 'worsening' THEN (ARRAY['normal','high','very_high'])[j]
                WHEN s.mode = 'long' THEN (ARRAY['normal','high','moderate'])[(j-1)%3+1]
                ELSE s.mode END;
            values_json := CASE scenario_mode
                WHEN 'moderate' THEN '{"GLUCOSE":60}'::jsonb
                WHEN 'high' THEN '{"GLUCOSE":110}'::jsonb
                WHEN 'very_high' THEN '{"GLUCOSE":180,"TRIGLYCERIDES":600}'::jsonb
                WHEN 'score_zero' THEN '{"HDL":65}'::jsonb
                WHEN 'score_three' THEN '{"GLUCOSE":90,"INSULIN":1}'::jsonb
                WHEN 'score_eight' THEN '{"TRIGLYCERIDES":200}'::jsonb
                WHEN 'analysis' THEN '{"GLUCOSE":110}'::jsonb
                WHEN 'text' THEN jsonb_build_object('GLUCOSE',CASE WHEN j=1 THEN NULL ELSE 95 END)
                WHEN 'imported_optimal' THEN '{"HDL":65}'::jsonb
                WHEN 'mixed' THEN CASE WHEN j=1 THEN '{"GLUCOSE":90,"HBA1C":5.2}'::jsonb
                    ELSE '{"GLUCOSE":110,"HDL":65,"CREATININE":1.5}'::jsonb END
                WHEN 'zero' THEN jsonb_build_object('GLUCOSE',CASE WHEN j=1 THEN 0 ELSE 70 END)
                WHEN 'boundaries' THEN jsonb_build_object(CASE WHEN j<=6 THEN 'GLUCOSE' ELSE 'TRIGLYCERIDES' END,boundary_values[j])
                WHEN 'legacy' THEN jsonb_build_object('GLI_JEJUM',CASE WHEN j=1 THEN 115 ELSE 88 END)
                ELSE '{"GLUCOSE":88,"HBA1C":5.2,"INSULIN":10,"ESTIMATED_AVERAGE_GLUCOSE":100,
                        "TOTAL_CHOLESTEROL":170,"HDL":55,"LDL":95,"TRIGLYCERIDES":120,
                        "VLDL":20,"CREATININE":0.9,"UREA":30}'::jsonb END;
            total_score := 0;
            item_count := 0;
            FOR item IN SELECT key AS code,value::numeric AS measured FROM jsonb_each_text(values_json) LOOP
                SELECT id,unit INTO STRICT item_id_value,item_unit FROM public.exam_item WHERE code=item.code AND active;
                result_flag := 'IN_ANALYSIS';
                IF exam_status IN ('RELEASED','COMPLETED') THEN
                    -- Reproduz os limites inclusivos/exclusivos e a faixa por sexo/idade
                    -- usados pelo motor. Ambiguidade ou falta de regra aborta a transacao.
                    SELECT count(*) INTO candidate_count FROM public.reference_range rr
                    JOIN public.rule r ON r.reference_range_id=rr.id
                    WHERE rr.exam_item_id=item_id_value
                      AND (rr.sex IS NULL OR rr.sex=s.sex)
                      AND (rr.age_min_years IS NULL OR rr.age_min_years<=s.age_years)
                      AND (rr.age_max_years IS NULL OR rr.age_max_years>=s.age_years)
                      AND r.active
                      AND (r.min_value IS NULL OR CASE WHEN r.min_inclusive THEN item.measured>=r.min_value ELSE item.measured>r.min_value END)
                      AND (r.max_value IS NULL OR CASE WHEN r.max_inclusive THEN item.measured<=r.max_value ELSE item.measured<r.max_value END);
                    IF candidate_count <> 1 THEN
                        RAISE EXCEPTION 'Cenario %, item %, valor %: esperada uma regra, encontradas %',s.scenario,item.code,item.measured,candidate_count;
                    END IF;
                    SELECT r.level,r.score INTO STRICT matched FROM public.reference_range rr
                    JOIN public.rule r ON r.reference_range_id=rr.id
                    WHERE rr.exam_item_id=item_id_value
                      AND (rr.sex IS NULL OR rr.sex=s.sex)
                      AND (rr.age_min_years IS NULL OR rr.age_min_years<=s.age_years)
                      AND (rr.age_max_years IS NULL OR rr.age_max_years>=s.age_years)
                      AND r.active
                      AND (r.min_value IS NULL OR CASE WHEN r.min_inclusive THEN item.measured>=r.min_value ELSE item.measured>r.min_value END)
                      AND (r.max_value IS NULL OR CASE WHEN r.max_inclusive THEN item.measured<=r.max_value ELSE item.measured<r.max_value END);
                    result_flag := matched.level;
                    total_score := total_score + matched.score;
                END IF;
                IF s.mode='imported_optimal' THEN result_flag := 'OPTIMAL'; END IF;
                INSERT INTO public.exam_result
                    (id,exam_id,exam_item_id,value_numeric,value_text,unit,flag,created_at)
                VALUES (nextval('public.primary_sequence'),exam_id_value,item_id_value,item.measured,
                    CASE WHEN s.mode='text' AND item.measured IS NULL THEN 'Amostra insuficiente. Aguardando nova coleta.' ELSE NULL END,
                    item_unit,result_flag,COALESCE(released,collected));
                item_count := item_count + 1;
            END LOOP;

            IF exam_status IN ('RELEASED','COMPLETED') AND s.mode NOT IN ('no_assessment','imported_optimal') THEN
                final_score := round(total_score/item_count,2);
                risk_level := CASE WHEN final_score>=8 THEN 'VERY_HIGH' WHEN final_score>=6 THEN 'HIGH'
                                   WHEN final_score>=3 THEN 'MODERATE' ELSE 'LOW' END;
                explanation_value := CASE risk_level
                    WHEN 'LOW' THEN 'Os resultados avaliados apresentam, em conjunto, baixo nível de alteração.'
                    WHEN 'MODERATE' THEN 'Foram identificadas alterações que indicam necessidade de acompanhamento profissional.'
                    WHEN 'HIGH' THEN 'Foram identificadas alterações relevantes. Recomenda-se acompanhamento profissional.'
                    ELSE 'Foram identificadas alterações importantes. Recomenda-se avaliação profissional.' END;
                IF s.mode='legacy' THEN
                    risk_level := CASE WHEN j=1 THEN 'ALERTA' ELSE 'NORMAL' END;
                    explanation_value := 'Exemplo de avaliação importada de sistema legado; classificação original preservada.';
                END IF;
                INSERT INTO public.risk_assessment
                    (id,user_id,exam_id,score,level,rules_version,explanation,created_at)
                VALUES (nextval('public.primary_sequence'),citizen_id,exam_id_value,final_score,risk_level,
                    CASE WHEN s.mode='legacy' THEN 'DEMO_LEGACY' ELSE 'DEMO_20260919' END,
                    explanation_value || ' Dados fictícios de demonstração.',released);
            END IF;

            IF exam_status IN ('RELEASED','COMPLETED') THEN
                INSERT INTO public.notification
                    (id,user_id,type,title,message,scheduled_at,sent_at,status,created_at,reference_key)
                VALUES (nextval('public.primary_sequence'),citizen_id,'EXAM_RESULT_AVAILABLE',
                    'Resultado de exame disponível',
                    'O resultado do exame coletado em ' || to_char(collected,'DD/MM/YYYY') || ' está disponível. Dados fictícios de demonstração.',
                    released,CASE WHEN j%2=0 THEN released + interval '5 minutes' ELSE NULL END,
                    CASE WHEN j%2=0 THEN 'SENT' ELSE 'PENDING' END,released,'EXAM:' || exam_id_value);
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- Conquistas derivadas do historico: exatamente as regras atuais do motor.
INSERT INTO public.user_achievement (id,user_id,achievement_id,earned_at)
SELECT nextval('public.primary_sequence'),d.user_id,a.id,min(e.released_at)
FROM demo_citizen_scenarios d JOIN public.exam e ON e.user_id=d.user_id AND e.status='RELEASED'
CROSS JOIN public.achievement a WHERE a.code='SANGUE_BOM'
GROUP BY d.user_id,a.id;

INSERT INTO public.user_achievement (id,user_id,achievement_id,earned_at)
SELECT nextval('public.primary_sequence'),d.user_id,a.id,min(r.created_at)
FROM demo_citizen_scenarios d JOIN public.risk_assessment r ON r.user_id=d.user_id AND r.score<=3
JOIN public.exam e ON e.id=r.exam_id AND e.status='RELEASED'
CROSS JOIN public.achievement a WHERE a.code='HEALTH_CHAMPION'
GROUP BY d.user_id,a.id;

-- A conquista e permanente: pode ter sido obtida em qualquer par consecutivo.
WITH pairs AS (
    SELECT e.user_id,e.collected_at,e.released_at,
           lag(e.collected_at) OVER (PARTITION BY e.user_id ORDER BY e.collected_at) AS previous_at
    FROM public.exam e JOIN demo_citizen_scenarios d ON d.user_id=e.user_id WHERE e.status='RELEASED'
), eligible AS (
    SELECT p.user_id,min(p.released_at) AS earned_at FROM pairs p
    JOIN public.health_profile hp ON hp.user_id=p.user_id
    WHERE p.collected_at::date < (p.previous_at::date + make_interval(months => CASE hp.exam_periodicity
        WHEN 'QUARTERLY' THEN 3 WHEN 'SEMESTERLY' THEN 6 ELSE 12 END) + interval '30 days')::date
    GROUP BY p.user_id
)
INSERT INTO public.user_achievement (id,user_id,achievement_id,earned_at)
SELECT nextval('public.primary_sequence'),e.user_id,a.id,e.earned_at
FROM eligible e CROSS JOIN public.achievement a WHERE a.code='PUNCTUAL';

-- Mesma chave de ciclo da rotina diaria: evita duplicar alertas no primeiro scan.
WITH goals AS (
    SELECT d.user_id,(max(e.collected_at)::date + make_interval(months => CASE hp.exam_periodicity
        WHEN 'QUARTERLY' THEN 3 WHEN 'SEMESTERLY' THEN 6 ELSE 12 END))::date AS due_date
    FROM demo_citizen_scenarios d JOIN public.exam e ON e.user_id=d.user_id
    JOIN public.health_profile hp ON hp.user_id=d.user_id
    JOIN public.app_user u ON u.id=d.user_id AND u.status='ACTIVE'
    GROUP BY d.user_id,hp.exam_periodicity
)
INSERT INTO public.notification
    (id,user_id,type,title,message,scheduled_at,sent_at,status,created_at,reference_key)
SELECT nextval('public.primary_sequence'),g.user_id,
    CASE WHEN g.due_date<CURRENT_DATE THEN 'EXAM_GOAL_OVERDUE' ELSE 'EXAM_GOAL_DUE_SOON' END,
    CASE WHEN g.due_date<CURRENT_DATE THEN 'Sua meta de exames está vencida' ELSE 'Seu próximo exame está próximo do prazo' END,
    'Prazo da meta: ' || to_char(g.due_date,'DD/MM/YYYY') || '. Cenário fictício de demonstração.',
    CURRENT_TIMESTAMP,NULL,'PENDING',CURRENT_TIMESTAMP,g.due_date::text
FROM goals g WHERE g.due_date<=CURRENT_DATE+30;

-- Estados aceitos pelo CRUD legado, que nao sao produzidos pelo stream atual.
INSERT INTO public.notification
    (id,user_id,type,title,message,scheduled_at,sent_at,status,created_at,reference_key)
SELECT nextval('public.primary_sequence'),d.user_id,'REMINDER','Lembrete importado de demonstração',
    'Exemplo fictício de notificação legada ' || n.status || '.',
    CURRENT_TIMESTAMP-interval '1 day',
    CASE WHEN n.status='READ' THEN CURRENT_TIMESTAMP-interval '12 hours' ELSE NULL END,
    n.status,CURRENT_TIMESTAMP-interval '1 day','DEMO_LEGACY:' || n.status
FROM demo_citizen_scenarios d CROSS JOIN (VALUES ('READ'),('UNREAD')) AS n(status)
WHERE d.scenario='legado';
