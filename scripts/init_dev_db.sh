#!/bin/bash

# Load environment variables from .env file if it exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

DB_HOST="${POSTGRES_DEV_HOST}"
DB_PORT="${POSTGRES_DEV_PORT}"
DB_NAME="${POSTGRES_DEV_DB}"
DB_USER="${POSTGRES_DEV_USER}"
DB_PASSWORD="${POSTGRES_DEV_PASSWORD}"

echo "Connecting to PostgreSQL database: $DB_NAME on $DB_HOST:$DB_PORT as user $DB_USER"

# Export password để psql không hỏi mật khẩu
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f ./src/main/resources/data.sql

if [ $? -eq 0 ]; then
  echo "Successfully executed data.sql for dev environment."
else
  echo "Error executing data.sql for dev environment. Please check logs."
  exit 1
fi