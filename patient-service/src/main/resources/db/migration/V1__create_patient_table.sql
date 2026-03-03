CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE patient (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         name VARCHAR(255) NOT NULL,
                         email VARCHAR(255) UNIQUE NOT NULL,
                         address VARCHAR(255) NOT NULL,
                         date_of_birth DATE NOT NULL,
                         registered_date DATE NOT NULL
);