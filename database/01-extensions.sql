-- Install all required extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Verify installation
SELECT extname FROM pg_extension WHERE extname IN ('vector', 'hstore', 'uuid-ossp', 'pg_trgm', 'unaccent');

-- Create immutable_unaccent function for fuzzy search
-- This function creates an immutable version of unaccent for use in indexes
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
RETURNS NULL ON NULL INPUT
AS $$
    SELECT unaccent($1);
$$;