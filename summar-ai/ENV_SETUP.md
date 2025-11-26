I would# Environment Variables Setup Guide

This document describes all environment variables required to run the summar-ai application in production.

## Required Environment Variables

### Database Configuration

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | JDBC connection string for MySQL database | `jdbc:mysql://localhost:3307/summar-ai` |
| `DATABASE_USERNAME` | Database username | `root` |
| `DATABASE_PASSWORD` | Database password | `your-secure-password` |

### OAuth2 - Google Calendar Integration

| Variable | Description | How to Obtain |
|----------|-------------|---------------|
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID | Create OAuth app in [Google Cloud Console](https://console.cloud.google.com) |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret | From Google Cloud Console credentials page |
| `GOOGLE_REDIRECT_URI` | OAuth callback URL | Set to `https://yourdomain.com/login/oauth2/code/google` |

**Google Setup Steps:**
1. Go to https://console.cloud.google.com
2. Create a new project or select existing one
3. Enable **Google Calendar API** in APIs & Services
4. Go to **Credentials** → **Create Credentials** → **OAuth client ID**
5. Choose **Web application**
6. Add authorized redirect URI: `https://yourdomain.com/login/oauth2/code/google`
7. For production, submit app for verification to allow any user to authenticate
8. For testing, add specific users to "Test users" list in OAuth consent screen

### OAuth2 - Zoom Integration

| Variable | Description | How to Obtain |
|----------|-------------|---------------|
| `ZOOM_CLIENT_ID` | Zoom OAuth Client ID | Create OAuth app in [Zoom Marketplace](https://marketplace.zoom.us/) |
| `ZOOM_CLIENT_SECRET` | Zoom OAuth Client Secret | From Zoom app credentials page |
| `ZOOM_REDIRECT_URI` | OAuth callback URL | Set to `https://yourdomain.com/login/oauth2/code/zoom` |

**Zoom Setup Steps:**
1. Go to https://marketplace.zoom.us/
2. Click **Develop** → **Build App**
3. Choose **OAuth** app type
4. Fill in app information
5. Add redirect URL: `https://yourdomain.com/login/oauth2/code/zoom`
6. Add required scopes: `user:read`, `chat_message:read`
7. Activate app for production use

### OAuth2 - Jira Integration

| Variable | Description | How to Obtain |
|----------|-------------|---------------|
| `JIRA_CLIENT_ID` | Jira OAuth2 Client ID | Create OAuth2 integration in [Atlassian Developer Console](https://developer.atlassian.com/console/myapps/) |
| `JIRA_CLIENT_SECRET` | Jira OAuth2 Client Secret | From Atlassian Developer Console |
| `JIRA_REDIRECT_URI` | OAuth callback URL | Set to `https://yourdomain.com/login/oauth2/code/jira` |

**Jira Setup Steps:**
1. Go to https://developer.atlassian.com/console/myapps/
2. Click **Create** → **OAuth 2.0 integration**
3. Name your app
4. Add callback URL: `https://yourdomain.com/login/oauth2/code/jira`
5. Set permissions: `read:me`, `read:jira-user`, `read:jira-work`, `offline_access`
6. Enable distribution settings for production use

### OpenAI API

| Variable | Description | How to Obtain |
|----------|-------------|---------------|
| `OPENAI_API_KEY` | OpenAI API key for GPT integration | Get from [OpenAI Platform](https://platform.openai.com/api-keys) |

**OpenAI Setup Steps:**
1. Go to https://platform.openai.com/
2. Sign up or log in
3. Navigate to **API Keys**
4. Click **Create new secret key**
5. Copy and securely store the key

### Security Configuration

| Variable | Description | Default | Production Value |
|----------|-------------|---------|------------------|
| `COOKIE_SECURE` | Enable secure cookies (HTTPS only) | `false` | `true` |

## Local Development Setup

Create a `.env` file in the `summar-ai/` directory (this file is gitignored):

```bash
# Database
DATABASE_URL=jdbc:mysql://localhost:3307/summar-ai
DATABASE_USERNAME=root
DATABASE_PASSWORD=root

# Google Calendar
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google

# Zoom
ZOOM_CLIENT_ID=your-zoom-client-id
ZOOM_CLIENT_SECRET=your-zoom-client-secret
ZOOM_REDIRECT_URI=http://localhost:8080/login/oauth2/code/zoom

# Jira
JIRA_CLIENT_ID=your-jira-client-id
JIRA_CLIENT_SECRET=your-jira-client-secret
JIRA_REDIRECT_URI=http://localhost:8080/login/oauth2/code/jira

# OpenAI
OPENAI_API_KEY=your-openai-api-key

# Security
COOKIE_SECURE=false
```

Then load these variables before running the app:

**Windows (PowerShell):**
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
    }
}
./mvnw spring-boot:run
```

**Linux/Mac (Bash):**
```bash
export $(cat .env | xargs)
./mvnw spring-boot:run
```

## Google Cloud Platform Deployment

### Using Cloud Run or App Engine

Set environment variables in `app.yaml` or through GCP Console:

**app.yaml example:**
```yaml
env_variables:
  DATABASE_URL: "jdbc:mysql://CLOUD_SQL_CONNECTION_STRING"
  DATABASE_USERNAME: "your-db-user"
  DATABASE_PASSWORD: "your-db-password"
  GOOGLE_CLIENT_ID: "your-google-client-id"
  GOOGLE_CLIENT_SECRET: "your-google-client-secret"
  GOOGLE_REDIRECT_URI: "https://your-app.run.app/login/oauth2/code/google"
  ZOOM_CLIENT_ID: "your-zoom-client-id"
  ZOOM_CLIENT_SECRET: "your-zoom-client-secret"
  ZOOM_REDIRECT_URI: "https://your-app.run.app/login/oauth2/code/zoom"
  JIRA_CLIENT_ID: "your-jira-client-id"
  JIRA_CLIENT_SECRET: "your-jira-client-secret"
  JIRA_REDIRECT_URI: "https://your-app.run.app/login/oauth2/code/jira"
  OPENAI_API_KEY: "your-openai-api-key"
  COOKIE_SECURE: "true"
```

### Using Secret Manager (Recommended for Production)

For better security, use GCP Secret Manager:

1. Create secrets in Secret Manager:
```bash
echo -n "your-google-client-secret" | gcloud secrets create google-client-secret --data-file=-
echo -n "your-zoom-client-secret" | gcloud secrets create zoom-client-secret --data-file=-
echo -n "your-jira-client-secret" | gcloud secrets create jira-client-secret --data-file=-
echo -n "your-openai-api-key" | gcloud secrets create openai-api-key --data-file=-
echo -n "your-db-password" | gcloud secrets create database-password --data-file=-
```

2. Grant your service account access to secrets
3. Reference secrets in your deployment configuration

## Production Checklist

Before deploying to production:

- [ ] Create OAuth apps for Google, Zoom, and Jira under a shared organization account (not personal)
- [ ] Submit Google OAuth app for verification (required for public use)
- [ ] Enable production mode for Zoom and Jira OAuth apps
- [ ] Set up Cloud SQL for MySQL database
- [ ] Store all secrets in Secret Manager (not environment variables)
- [ ] Set `COOKIE_SECURE=true`
- [ ] Update all redirect URIs to production domain (HTTPS)
- [ ] Enable CSRF protection in SecurityConfig.java
- [ ] Set up SSL/TLS certificates
- [ ] Configure logging and monitoring
- [ ] Set up backup strategy for database
- [ ] Review and restrict database access
- [ ] Implement rate limiting for API endpoints

## Important Security Notes

1. **Never commit credentials to Git** - The `.gitignore` file already excludes `application.properties`
2. **Use environment-specific credentials** - Development and production should have separate OAuth apps
3. **Rotate secrets regularly** - Especially OpenAI API keys and database passwords
4. **Use organization accounts** - Create OAuth apps under a shared organization account, not personal accounts
5. **Enable 2FA** - On all cloud platform accounts
6. **Audit access logs** - Regularly review who has access to credentials

## Troubleshooting

### OAuth "App in Development" Error

This means:
- The OAuth app is in testing mode
- Only whitelisted test users can authenticate
- For Google: Add users in OAuth consent screen → Test users
- For production: Submit apps for verification/approval

### Redirect URI Mismatch

Ensure redirect URIs match exactly:
- Local: `http://localhost:8080/login/oauth2/code/{provider}`
- Production: `https://yourdomain.com/login/oauth2/code/{provider}`

### Database Connection Issues

- Verify `DATABASE_URL` format
- Check database is running and accessible
- Verify firewall rules allow connections
- For Cloud SQL: Use Cloud SQL Proxy or configure private IP
