FROM postgres:17

# Install pgvector và các extensions khác
RUN apt-get update && apt-get install -y \
    postgresql-17-pgvector \
    postgresql-17-hstore \
    postgresql-17-contrib \
    && rm -rf /var/lib/apt/lists/*

# Copy database init scripts
COPY 01-extensions.sql /docker-entrypoint-initdb.d/
COPY 02-schema.sql /docker-entrypoint-initdb.d/
COPY 03-data.sql /docker-entrypoint-initdb.d/
