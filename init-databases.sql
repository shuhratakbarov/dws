-- Initialize databases for Digital Wallet System
-- This script runs when PostgreSQL container starts for the first time

CREATE DATABASE auth_db;
CREATE DATABASE wallet_db;
CREATE DATABASE customer_db;
CREATE DATABASE ledger_db;
CREATE DATABASE notification_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE wallet_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE customer_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE ledger_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;

