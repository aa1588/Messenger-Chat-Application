# Chat Application

A real-time chat application built with Spring Boot and React.

## Features
- User authentication (JWT)
- Real-time messaging with WebSockets (STOMP)
- Group chats and direct messages
- PostgreSQL database
- Docker environment

## Tech Stack
- Backend: Spring Boot, Spring Security, Spring WebSocket, JWT
- Frontend: React, TypeScript, SockJS, STOMP
- Database: PostgreSQL
- Containerization: Docker

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 17+
- Node.js 18+

### Running the Application

1. Start the database:
```bash
docker-compose up -d postgres
```

2. Run the backend:
```bash
cd backend
./mvnw spring-boot:run
```

3. Run the frontend:
```bash
cd frontend
npm install
npm start
```

The application will be available at:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080