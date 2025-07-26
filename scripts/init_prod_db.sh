#!/bin/bash

# Load environment variables from .env file if it exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

DB_HOST="${POSTGRES_PROD_HOST}"
DB_PORT="${POSTGRES_PROD_PORT}"
DB_NAME="${POSTGRES_PROD_DB}"
DB_USER="${POSTGRES_PROD_USER}"
DB_PASSWORD="${POSTGRES_PROD_PASSWORD}"

echo "Connecting to PostgreSQL database: $DB_NAME on $DB_HOST:$DB_PORT as user $DB_USER"

# Export password để psql không hỏi mật khẩu
export PGPASSWORD=$DB_PASSWORD

# Initialize PostgreSQL database with schema and sample data
echo "Creating PostgreSQL schema..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f ./scripts/schema_postgresql.sql

echo "Inserting sample data..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f ./scripts/init_postgresql.sql

if [ $? -eq 0 ]; then
  echo "Successfully initialized PostgreSQL database for prod environment."
else
  echo "Error initializing PostgreSQL database for prod environment. Please check logs."
  exit 1
fi
