-- As faixas V1 foram criadas com valid_from igual à data de execução da
-- migration. Isso impedia a associação de regras aos resultados de demonstração
-- coletados antes dessa data. A vigência é explicitamente limitada ao ano de
-- 2026 para preservar futuras versões das faixas.
UPDATE public.reference_range
SET valid_from = DATE '2026-01-01',
    valid_until = DATE '2026-12-31'
WHERE version IN ('GLYCEMIC_V1', 'LIPID_V1', 'RENAL_V1')
  AND valid_from = DATE '2026-09-18'
  AND valid_until IS NULL;
