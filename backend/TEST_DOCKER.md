# Testing the Backend Dockerfile

## Option 1: Build and Run the Docker Image Directly

### Step 1: Build the Docker image
```bash
cd backend
docker build -t ai-nl-search-backend .
```

### Step 2: Run the container
```bash
docker run -p 8080:8080 ai-nl-search-backend
```

### Step 3: Test the health endpoint
In another terminal or browser:
```bash
curl http://localhost:8080/api/health
```
Expected response: `ok`

Or open in browser: http://localhost:8080/api/health

## Option 2: Use Docker Compose (Recommended)

### Step 1: Build and start all services
```bash
# From project root
docker-compose up --build
```

### Step 2: Test the health endpoint
```bash
curl http://localhost:8080/api/health
```
Expected response: `ok`

### Step 3: Check logs
```bash
docker-compose logs backend
```

### Step 4: Stop services
```bash
docker-compose down
```

## Option 3: Build Only (Without Running)

```bash
cd backend
docker build -t ai-nl-search-backend .
```

Check if build succeeded (no errors in output).

## Troubleshooting

### If build fails:
1. Check that `mvnw` file exists and is executable
2. Verify all dependencies are available
3. Check Docker logs: `docker build -t ai-nl-search-backend . 2>&1 | tee build.log`

### If container exits immediately:
1. Check logs: `docker logs <container-id>`
2. Verify the JAR file exists: `docker run --entrypoint ls ai-nl-search-backend target/`
3. Check if port 8080 is already in use

### If health endpoint doesn't respond:
1. Verify container is running: `docker ps`
2. Check container logs: `docker logs <container-id>`
3. Test from inside container: `docker exec -it <container-id> curl http://localhost:8080/api/health`

