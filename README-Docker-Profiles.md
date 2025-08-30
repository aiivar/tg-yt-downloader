# Docker Profiles

This project now supports separate Docker configurations for development and production environments.

## Available Profiles

### 1. Development Profile (`dev`)
- **Purpose**: For development and debugging
- **Features**: 
  - Includes JVM debug agent on port 5005
  - Allows remote debugging
  - Separate container names and volumes
- **Files**: `Dockerfile.dev`, `docker-compose.dev.yml`

### 2. Production Profile (`prod`)
- **Purpose**: For production deployment
- **Features**:
  - Optimized for production
  - No debug agent
  - Separate container names and volumes
- **Files**: `Dockerfile.prod`, `docker-compose.prod.yml`

### 3. Default Profile
- **Purpose**: Default configuration (production-like)
- **Features**: Standard configuration without debug agent
- **Files**: `Dockerfile`, `docker-compose.yml`

## Usage

### Development Environment

To run the application in development mode with debugging support:

```bash
# Build and run development profile
docker-compose -f docker-compose.dev.yml up --build

# Or build and run in detached mode
docker-compose -f docker-compose.dev.yml up --build -d
```

**Debugging**: Connect your IDE debugger to `localhost:5005`

### Production Environment

To run the application in production mode:

```bash
# Build and run production profile
docker-compose -f docker-compose.prod.yml up --build

# Or build and run in detached mode
docker-compose -f docker-compose.prod.yml up --build -d
```

### Default Environment

To run with the default configuration:

```bash
# Build and run default profile
docker-compose up --build

# Or build and run in detached mode
docker-compose up --build -d
```

## Stopping Services

```bash
# Stop development services
docker-compose -f docker-compose.dev.yml down

# Stop production services
docker-compose -f docker-compose.prod.yml down

# Stop default services
docker-compose down
```

## Container Names

- **Development**: `tg-yt-downloader-dev`, `tg-yt-downloader-db-dev`
- **Production**: `tg-yt-downloader-prod`, `tg-yt-downloader-db-prod`
- **Default**: `tg-yt-downloader`, `tg-yt-downloader-db`

## Volumes

Each profile uses separate volumes to avoid data conflicts:
- **Development**: `postgres_data_dev`
- **Production**: `postgres_data_prod`
- **Default**: `postgres_data`

## Ports

- **Application**: 8080 (all profiles)
- **Database**: 5433 (all profiles)
- **Debug**: 5005 (development profile only)

## IDE Debugging Setup

### IntelliJ IDEA
1. Go to `Run` → `Edit Configurations`
2. Click `+` → `Remote JVM Debug`
3. Set:
   - **Host**: `localhost`
   - **Port**: `5005`
   - **Use module classpath**: Select your project module
4. Click `OK` and run the debug configuration

### VS Code
1. Create `.vscode/launch.json`:
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug Docker App",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
        }
    ]
}
```

### Eclipse
1. Go to `Run` → `Debug Configurations`
2. Select `Remote Java Application`
3. Set:
   - **Host**: `localhost`
   - **Port**: `5005`
4. Click `Debug`
