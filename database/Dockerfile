# Use official PostgreSQL 17 with all extensions
FROM postgres:17

# Install all required extensions
RUN apt-get update && apt-get install -y \
    postgresql-17-pgvector \
    postgresql-17-hstore \
    postgresql-17-contrib \
    postgresql-17-pgtap \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV POSTGRES_DB=laptop_repair
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=postgres

# Copy initialization scripts (run in order)
COPY 01-extensions.sql /docker-entrypoint-initdb.d/
COPY 02-schema.sql /docker-entrypoint-initdb.d/
COPY 03-data.sql /docker-entrypoint-initdb.d/
COPY 04-setup.sql /docker-entrypoint-initdb.d/

# Create script to copy data files if they exist
COPY init.sh /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/init.sh
