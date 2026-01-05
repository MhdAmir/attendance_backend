# Google Service Account Credentials Setup

## Steps to configure Google Sheets API:

### 1. Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Google Sheets API:
   - Go to "APIs & Services" > "Library"
   - Search for "Google Sheets API"
   - Click "Enable"

### 2. Create Service Account
1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "Service Account"
3. Fill in:
   - Service account name: `eros-attendance-service`
   - Service account ID: `eros-attendance-service`
   - Click "Create and Continue"
4. Grant role: "Editor" or "Google Sheets API User"
5. Click "Done"

### 3. Create Service Account Key
1. Click on the service account you just created
2. Go to "Keys" tab
3. Click "Add Key" > "Create new key"
4. Select "JSON" format
5. Click "Create"
6. Download the JSON file

### 4. Setup Credentials File
1. Rename downloaded file to `google-credentials.json`
2. Move it to: `/home/eros/eros-attendence/backend/src/main/resources/`
3. The file should look like:
```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "eros-attendance-service@your-project.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "..."
}
```

### 5. Share Google Sheet with Service Account
1. Open your Google Sheet: https://docs.google.com/spreadsheets/d/1ofkN92tAbTUioPKwZ-0RhPVe-n26G2kWAwXmOvR5Qjs/edit
2. Click "Share" button (top right)
3. Add the service account email from JSON file (e.g., `eros-attendance-service@your-project.iam.gserviceaccount.com`)
4. Grant "Editor" permission
5. Click "Send"

### 6. Verify Configuration
- OTP Secret in `application.properties`: `otp.secret=EROS1NASIONAL2024SECRET`
- **IMPORTANT**: Use the SAME secret in your offline robot's OTP generator!
- Google Sheet ID: `1ofkN92tAbTUioPKwZ-0RhPVe-n26G2kWAwXmOvR5Qjs`
- Credentials path: `/home/eros/eros-attendence/backend/src/main/resources/google-credentials.json`

## Testing OTP
Use endpoint: `GET /api/attendance/otp/current` to get current valid OTP for testing.
The robot should generate the same OTP using the same secret.

## API Endpoints

### Check-in
```bash
POST /api/attendance/check-in
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "otpCode": "123456"
}
```

### Check-out
```bash
POST /api/attendance/check-out
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "otpCode": "123456"
}
```

### Get Current Attendance
```bash
GET /api/attendance/current
Authorization: Bearer <access_token>
```

### Get History
```bash
GET /api/attendance/history?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59
Authorization: Bearer <access_token>
```
