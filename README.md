# AI-Powered Natural Language Search

Natural language search application for orders using AWS Bedrock.

## Prerequisites

- Docker and Docker Compose
- AWS Account with Bedrock access
- AWS Credentials (Access Key ID and Secret Access Key)

## Setup

### 1. Configure AWS Credentials

The application requires AWS credentials to access AWS Bedrock. You can provide them in one of the following ways:

**Option 1: Environment variables (recommended for Docker)**
Create a `.env` file in the root directory:
```bash
AWS_ACCESS_KEY_ID=your-access-key-id
AWS_SECRET_ACCESS_KEY=your-secret-access-key
AWS_REGION=eu-central-1
```

**Option 2: Pass directly to docker-compose**
```bash
AWS_ACCESS_KEY_ID=your-key AWS_SECRET_ACCESS_KEY=your-secret docker-compose up
```

**Option 3: AWS credentials file (for local development)**
Configure `~/.aws/credentials` on your host machine.

### 2. Run with Docker Compose

```bash
docker-compose up --build
```

This will start:
- **Frontend**: http://localhost:80
- **Backend**: http://localhost:8080
- **Database**: MySQL on port 3306

## Troubleshooting

### "Unable to understand your query" Error

This error typically means:
1. **AWS credentials are not configured** - Check that AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY are set
2. **AWS Bedrock access denied** - Ensure your AWS account has access to Bedrock in the eu-central-1 region
3. **Network issues** - Check that the backend container can reach AWS Bedrock API

Check the backend logs for detailed error messages:
```bash
docker-compose logs backend
```

## Development

### Building Individual Services

- **Backend**: `docker build ./backend`
- **Frontend**: `docker build ./frontend`
- **Database**: `docker build ./db`