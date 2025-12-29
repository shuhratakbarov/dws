-- Supabase Schema Setup for Digital Wallet System
-- Run this in Supabase SQL Editor or via psql

-- Create schemas for each microservice
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS wallet;
CREATE SCHEMA IF NOT EXISTS customer;
CREATE SCHEMA IF NOT EXISTS ledger;
CREATE SCHEMA IF NOT EXISTS notification;

-- Verify schemas
SELECT schema_name FROM information_schema.schemata
WHERE schema_name IN ('auth', 'wallet', 'customer', 'ledger', 'notification');

