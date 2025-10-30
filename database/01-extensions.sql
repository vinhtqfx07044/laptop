-- Install all required extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Verify installation
SELECT extname FROM pg_extension WHERE extname IN ('vector', 'hstore', 'uuid-ossp', 'pg_trgm', 'unaccent');