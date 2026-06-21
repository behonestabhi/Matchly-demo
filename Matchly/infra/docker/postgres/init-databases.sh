#!/bin/bash
# Creates one database per service in the single PostgreSQL container.
# Runs automatically on first container start (mounted into
# /docker-entrypoint-initdb.d). Honors database-per-service for local dev.
set -e

DBS="auth_db candidate_db job_db application_db matching_db interview_db analytics_db ai_db"

for db in $DBS; do
  echo "Creating database: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done

# pgvector extension for the AI service's database.
echo "Enabling pgvector on ai_db"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "ai_db" <<-EOSQL
  CREATE EXTENSION IF NOT EXISTS vector;
EOSQL
