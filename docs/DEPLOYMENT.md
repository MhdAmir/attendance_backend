# Eros Attendance System - Production Deployment

## Deployment to VPS

### Prerequisites
- Docker & Docker Compose installed on VPS
- Google credentials JSON file
- Domain/IP address for the backend

### Setup Steps

1. **Clone repository to VPS:**
```bash
git clone <your-repo-url>
cd eros-attendence/backend
```

2. **Create `.env` file:**
```bash
cp .env.example .env
nano .env  # Edit with your actual values
```

3. **Copy Google credentials:**
```bash
# Copy your google-credentials.json to backend directory
scp google-credentials.json user@your-vps:/path/to/backend/
```

4. **Update configuration:**
- Edit `.env` file with your actual values
- Update `GOOGLE_SHEETS_SPREADSHEET_ID`
- Update `PASETO_SECRET_KEY` (generate new: `openssl rand -base64 32`)
- Update `OTP_SECRET_HEX` (must match robot's secret)

5. **Build and run:**
```bash
# Build and start services
docker-compose up -d --build

# Check logs
docker-compose logs -f backend

# Check status
docker-compose ps
```

6. **Access application:**
- Backend API: `http://your-vps-ip:8080`
- Swagger UI: `http://your-vps-ip:8080/swagger-ui.html`
- API Docs: `http://your-vps-ip:8080/v3/api-docs`

### Management Commands

```bash
# Stop services
docker-compose down

# Restart services
docker-compose restart

# View logs
docker-compose logs -f backend
docker-compose logs -f db

# Database backup
docker exec eros_attendance_db pg_dump -U eros eros_attendance_db > backup.sql

# Database restore
docker exec -i eros_attendance_db psql -U eros eros_attendance_db < backup.sql

# Clean up (removes volumes - WARNING: deletes data)
docker-compose down -v
```

### Nginx Reverse Proxy (Optional)

If using Nginx as reverse proxy:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### SSL/HTTPS with Let's Encrypt

```bash
# Install certbot
sudo apt install certbot python3-certbot-nginx

# Get certificate
sudo certbot --nginx -d your-domain.com

# Auto-renewal
sudo certbot renew --dry-run
```

### Monitoring

Check application health:
```bash
curl http://localhost:8080/actuator/health
```

### Troubleshooting

1. **Container won't start:**
```bash
docker-compose logs backend
```

2. **Database connection error:**
```bash
docker-compose logs db
docker exec -it eros_attendance_db psql -U eros -d eros_attendance_db
```

3. **Google Sheets sync error:**
- Verify google-credentials.json is mounted correctly
- Check credentials have proper permissions
- Verify spreadsheet ID is correct

4. **OTP validation error:**
- Ensure OTP_SECRET_HEX matches robot configuration
- Check time synchronization: `ntpdate -q pool.ntp.org`

### Security Recommendations

1. Change default passwords in `.env`
2. Generate new PASETO secret key
3. Use firewall to restrict database port (5432)
4. Keep google-credentials.json secure
5. Enable HTTPS/SSL
6. Regular backups
7. Monitor logs for suspicious activity

### Updates

```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
docker-compose up -d --build

# Check if update successful
docker-compose logs -f backend
```
