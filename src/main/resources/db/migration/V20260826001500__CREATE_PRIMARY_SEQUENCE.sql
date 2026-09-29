-- ==============================================================================
-- 1. CRIAR A SEQUÊNCIA "primary_sequence" PARA GERAR IDs ÚNICOS
-- ==============================================================================

CREATE SEQUENCE IF NOT EXISTS primary_sequence
    START WITH 10000   INCREMENT BY 1
    NO MINVALUE   NO MAXVALUE   CACHE 1;