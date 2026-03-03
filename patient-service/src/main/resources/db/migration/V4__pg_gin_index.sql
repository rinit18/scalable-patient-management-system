CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_patient_name_trgm
    ON patient
    USING gin (name gin_trgm_ops);