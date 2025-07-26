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
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f ./src/main/resources/data.sql

if [ $? -eq 0 ]; then
  echo "Successfully executed data.sql for prod environment."
else
  echo "Error executing data.sql for prod environment. Please check logs."
  exit 1
}
