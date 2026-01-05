# Eros Attendance System - Backend

Backend API untuk sistem absensi Eros menggunakan Spring Boot dengan autentikasi PASETO dan verifikasi OTP.

## Features

- 🔐 Autentikasi menggunakan PASETO (Platform-Agnostic Security Tokens)
- 📱 Verifikasi OTP dari robot offline (TOTP algorithm)
- ✅ Check-in/Check-out sekali per hari
- 📊 Sync otomatis ke Google Sheets dengan 2 format:
  - Detail: timestamp lengkap check-in/out per baris
  - Summary: format centang per tanggal dalam bulan
- 🔄 Refresh token support
- 📝 API Documentation dengan Swagger/OpenAPI
- 🐳 Docker support untuk deployment

## Tech Stack

- Java 17
- Spring Boot 4.0.1
- PostgreSQL 16
- PASETO v2.local
- Google Sheets API
- Docker & Docker Compose

## Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL 16+
- Docker & Docker Compose (untuk deployment)

## Quick Start (Development)

1. **Clone repository:**
```bash
git clone <repo-url>
cd backend
```

2. **Setup PostgreSQL:**
```bash
# Menggunakan Docker
docker-compose up -d db

# Atau install manual dan buat database
createdb eros_attendance_db
```

3. **Configure application:**
```bash
# Edit src/main/resources/application.properties
# Update database credentials, Google Sheets ID, dll
```

4. **Add Google Credentials:**
```bash
# Copy google-credentials.json ke src/main/resources/
cp /path/to/google-credentials.json src/main/resources/
```

5. **Run application:**
```bash
./mvnw spring-boot:run
```

6. **Access Swagger UI:**
```
http://localhost:8080/swagger-ui.html
```

## Production Deployment

Lihat [DEPLOYMENT.md](DEPLOYMENT.md) untuk panduan lengkap deployment ke VPS.

**Quick deploy dengan Docker:**
```bash
# 1. Copy .env file
cp .env.example .env

# 2. Edit .env dengan nilai yang sesuai
nano .env

# 3. Copy google credentials
cp /path/to/google-credentials.json ./

# 4. Build & run
docker-compose up -d --build

# 5. Check logs
docker-compose logs -f backend
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register user baru
- `POST /api/auth/login` - Login dan dapatkan access token
- `POST /api/auth/refresh` - Refresh access token

### Attendance
- `GET /api/attendance/otp/current` - Dapatkan OTP code saat ini
- `POST /api/attendance/check-in` - Check-in dengan OTP
- `POST /api/attendance/check-out` - Check-out dengan OTP
- `GET /api/attendance/current` - Dapatkan attendance aktif
- `GET /api/attendance/history` - Dapatkan history attendance

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:postgresql://localhost:5432/eros_attendance_db` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `eros` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `EROS1NASIONAL` |
| `PASETO_SECRET_KEY` | PASETO encryption key | - |
| `OTP_SECRET_HEX` | OTP secret (hex) | - |
| `GOOGLE_SHEETS_SPREADSHEET_ID` | Spreadsheet ID | - |
| `SERVER_PORT` | Application port | `8080` |

## Development

### Build
```bash
./mvnw clean package
```

### Run tests
```bash
./mvnw test
```

### Generate OTP Secret
```bash
# Generate hex secret
openssl rand -hex 32

# Or from string
echo -n "YourSecretString" | xxd -p
```

### Generate PASETO Key
```bash
openssl rand -base64 32
```

## Google Sheets Setup

1. Buat Google Cloud Project
2. Enable Google Sheets API
3. Buat Service Account
4. Download credentials JSON
5. Share spreadsheet dengan service account email

Lihat [GOOGLE_SHEETS_SETUP.md](GOOGLE_SHEETS_SETUP.md) untuk detail lengkap.

## OTP Robot Setup

Robot C++ harus menggunakan OTP_SECRET_HEX yang sama. Lihat [robot/OTP_SETUP.md](../robot/OTP_SETUP.md).

## Project Structure

```
backend/
├── src/main/java/com/backend/
│   ├── config/           # Configuration classes
│   ├── controller/       # REST controllers
│   ├── dto/             # Data Transfer Objects
│   ├── entity/          # JPA entities
│   ├── exception/       # Exception handlers
│   ├── filter/          # Security filters
│   ├── repository/      # JPA repositories
│   └── service/         # Business logic
├── src/main/resources/
│   ├── application.properties
│   └── google-credentials.json
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## Troubleshooting

### OTP tidak valid
- Pastikan OTP_SECRET_HEX sama dengan robot
- Cek sinkronisasi waktu: `ntpdate -q pool.ntp.org`
- OTP berlaku 5 menit, strict mode (tidak ada tolerance)

### Google Sheets sync gagal
- Cek google-credentials.json path
- Pastikan service account punya akses ke spreadsheet
- Cek spreadsheet ID benar

### Database connection error
- Pastikan PostgreSQL running
- Cek username/password
- Cek port 5432 tidak diblokir firewall

## License

Private project - All rights reserved

## Support

Untuk bantuan, hubungi tim Eros.
