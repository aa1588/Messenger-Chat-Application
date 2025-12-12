#!/bin/bash

echo "Starting Chat App Development Environment..."

# Start PostgreSQL with Docker
echo "Starting PostgreSQL..."
docker run -d \
  --name chat-postgres \
  -e POSTGRES_DB=chatapp \
  -e POSTGRES_USER=chatuser \
  -e POSTGRES_PASSWORD=chatpass \
  -p 5432:5432 \
  postgres:15-alpine

echo "PostgreSQL started on port 5432"

echo ""
echo "To start the backend:"
echo "cd backend && ./mvnw spring-boot:run"
echo ""
echo "To start the frontend:"
echo "cd frontend && npm install && npm start"
echo ""
echo "To stop PostgreSQL:"
echo "docker stop chat-postgres && docker rm chat-postgres"