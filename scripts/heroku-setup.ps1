# Heroku Deployment Setup Script
# Run this script to set up all Heroku apps

param(
    [Parameter(Mandatory=$true)]
    [string]$SupabaseHost,

    [Parameter(Mandatory=$true)]
    [string]$SupabasePassword,

    [Parameter(Mandatory=$false)]
    [string]$FrontendUrl = "https://your-app.azurestaticapps.net"
)

Write-Host "=== Digital Wallet System - Heroku Setup ===" -ForegroundColor Cyan

# Generate JWT secret
$JWT_SECRET = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes((New-Guid).ToString() + (New-Guid).ToString()))
Write-Host "Generated JWT Secret (save this): $JWT_SECRET" -ForegroundColor Yellow

# Supabase connection URL (pooled)
$DATABASE_URL = "jdbc:postgresql://$SupabaseHost:6543/postgres?pgbouncer=true"

Write-Host "`nCreating Heroku apps..." -ForegroundColor Cyan

$apps = @("dws-api-gateway", "dws-auth", "dws-wallet", "dws-customer", "dws-ledger", "dws-notification")

foreach ($app in $apps) {
    heroku create $app --region us 2>$null
    heroku stack:set container -a $app
}

Write-Host "`nConfiguring environment variables..." -ForegroundColor Cyan

# API Gateway
heroku config:set `
    SPRING_PROFILES_ACTIVE=prod `
    JWT_SECRET=$JWT_SECRET `
    AUTH_SERVICE_URL=https://dws-auth.herokuapp.com `
    WALLET_SERVICE_URL=https://dws-wallet.herokuapp.com `
    CUSTOMER_SERVICE_URL=https://dws-customer.herokuapp.com `
    LEDGER_SERVICE_URL=https://dws-ledger.herokuapp.com `
    NOTIFICATION_SERVICE_URL=https://dws-notification.herokuapp.com `
    FRONTEND_URL=$FrontendUrl `
    -a dws-api-gateway

# Auth Service
heroku config:set `
    SPRING_PROFILES_ACTIVE=prod `
    DATABASE_URL=$DATABASE_URL `
    DB_USERNAME=postgres `
    DB_PASSWORD=$SupabasePassword `
    JWT_SECRET=$JWT_SECRET `
    -a dws-auth

# Wallet Service
heroku config:set `
    SPRING_PROFILES_ACTIVE=prod `
    DATABASE_URL=$DATABASE_URL `
    DB_USERNAME=postgres `
    DB_PASSWORD=$SupabasePassword `
    LEDGER_SERVICE_URL=https://dws-ledger.herokuapp.com `
    NOTIFICATION_SERVICE_URL=https://dws-notification.herokuapp.com `
    -a dws-wallet

# Customer Service
heroku config:set `
    SPRING_PROFILES_ACTIVE=prod `
    DATABASE_URL=$DATABASE_URL `
    DB_USERNAME=postgres `
    DB_PASSWORD=$SupabasePassword `
    -a dws-customer

# Ledger Service
heroku config:set `
    SPRING_PROFILES_ACTIVE=prod `
    DATABASE_URL=$DATABASE_URL `
    DB_USERNAME=postgres `
    DB_PASSWORD=$SupabasePassword `
    -a dws-ledger

# Notification Service
heroku config:set `
    SPRING_PROFILES_ACTIVE=prod `
    DATABASE_URL=$DATABASE_URL `
    DB_USERNAME=postgres `
    DB_PASSWORD=$SupabasePassword `
    -a dws-notification

Write-Host "`n=== Setup Complete ===" -ForegroundColor Green
Write-Host "Next steps:"
Write-Host "1. Update FRONTEND_URL with your Azure Static Web Apps URL"
Write-Host "2. Configure MAIL_* variables for notification service"
Write-Host "3. Add GitHub secrets for CI/CD"
Write-Host "4. Push to main branch to trigger deployment"

