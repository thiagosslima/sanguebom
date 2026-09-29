-- ==============================================================================
-- CF-347 / CF-348 - NOTIFICACOES DO CIDADAO
-- ==============================================================================

-- Chave do "ciclo" do aviso: identifica o fato que originou a notificacao
-- (ex.: EXAM:10001 para resultado disponivel, 2027-02-15 para a meta de exame).
ALTER TABLE public.notification ADD COLUMN IF NOT EXISTS reference_key varchar(120);

-- CF-397: o mesmo aviso, para o mesmo cidadao, no mesmo ciclo, so pode existir uma vez.
-- reference_key NULL nao entra no indice (NULLs sao distintos no Postgres),
-- entao o CRUD generico de notificacao continua livre.
CREATE UNIQUE INDEX IF NOT EXISTS notification_user_type_reference_uidx
    ON public.notification (user_id, "type", reference_key);

-- Historico do cidadao: listagem paginada da mais recente para a mais antiga.
CREATE INDEX IF NOT EXISTS notification_user_created_at_idx
    ON public.notification (user_id, created_at DESC);
