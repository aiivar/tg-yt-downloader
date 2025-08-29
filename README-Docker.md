# Docker Setup for TG YT Downloader

This document explains how to run the TG YT Downloader application using Docker and Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- `yt-dlp-java-2.0.3.jar` file in the project root directory

## Quick Start

1. **Build and run the application:**
   ```bash
   docker-compose up --build
   ```

2. **Run in detached mode:**
   ```bash
   docker-compose up -d --build
   ```

3. **Stop the application:**
   ```bash
   docker-compose down
   ```

4. **Stop and remove volumes:**
   ```bash
   docker-compose down -v
   ```

## Services

The Docker Compose setup includes:

- **app**: Spring Boot application (port 8080) with yt-dlp binary installed
- **db**: PostgreSQL database (port 5433)

## Environment Variables

You can customize the application by setting environment variables:

- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `SPRING_JPA_HIBERNATE_DDL_AUTO`: Hibernate DDL mode
- `SPRING_JPA_SHOW_SQL`: Enable SQL logging

## Database

- **Database**: `tg_yt_downloader`
- **Username**: `postgres`
- **Password**: `password`
- **Port**: `5432`

## Health Checks

Both services include health checks:
- Application: `http://localhost:8080/actuator/health`
- Database: PostgreSQL readiness check

## Volumes

- `postgres_data`: Persistent PostgreSQL data storage

## Development

For development, you can:

1. **View logs:**
   ```bash
   docker-compose logs -f app
   ```

2. **Access database:**
   ```bash
   docker-compose exec db psql -U postgres -d tg_yt_downloader
   ```

3. **Restart services:**
   ```bash
   docker-compose restart app
   ```

## Troubleshooting

1. **Port conflicts**: Ensure ports 8080 and 5432 are available
2. **Database connection**: Wait for the database to be ready before the app starts
3. **Build issues**: Clean Docker cache with `docker system prune -a`
4. **Missing yt-dlp-java JAR**: Ensure `yt-dlp-java-2.0.3.jar` is present in the project root directory
5. **yt-dlp binary**: The Docker image automatically installs the latest yt-dlp binary from GitHub

## Production Considerations

For production deployment:

1. Change default passwords
2. Use external database if needed
3. Configure proper logging
4. Set up monitoring and alerting
5. Use secrets management for sensitive data
