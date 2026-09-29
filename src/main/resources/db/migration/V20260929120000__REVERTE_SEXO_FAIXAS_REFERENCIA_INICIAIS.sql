-- Reverte a alteracao de sex feita por V20260928120000 nas duas faixas
-- originalmente cadastradas com ALL em V20260821195000.
-- As demais faixas ja usavam NULL e devem permanecer assim.
-- Mantem as correcoes de CRITICO para HIGH em rule e exam_result.
--
-- ATENCAO: ALL nao e suportado pelo enum Sex nem pela busca atual do motor.
-- Esta reversao restaura os dados anteriores, mas tambem essa incompatibilidade.

UPDATE public.reference_range AS rr
SET sex = 'ALL'
FROM public.exam_item AS ei
WHERE rr.exam_item_id = ei.id
  AND rr.sex IS NULL
  AND (
      (rr.id = 1 AND ei.code = 'GLI_JEJUM' AND rr.version = 'SBD_2023')
      OR
      (rr.id = 2 AND ei.code = 'COL_TOTAL' AND rr.version = 'SBC_2020')
  );
