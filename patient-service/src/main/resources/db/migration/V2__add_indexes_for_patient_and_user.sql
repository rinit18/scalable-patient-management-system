CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_patient_name
    ON patient(name);

CREATE INDEX idx_patient_name_trgm
    ON patient
    USING gin (name gin_trgm_ops);

CREATE INDEX idx_patient_registered_date
    ON patient(registered_date);