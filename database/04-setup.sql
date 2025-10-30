-- Create read-only user for application
CREATE USER app_user WITH ENCRYPTED PASSWORD 'app_password';

-- Grant necessary permissions to app_user
GRANT CONNECT ON DATABASE laptop_repair TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- Set default permissions for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_user;

-- Create admin user for management
CREATE USER admin_user WITH ENCRYPTED PASSWORD 'admin_password';
GRANT ALL PRIVILEGES ON DATABASE laptop_repair TO admin_user;

-- Verify setup
SELECT current_database(), current_user;
\dt
