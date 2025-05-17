# expensapp-api

Expense tracker API

## Technologies Used

- **Java 17**
- **Spring Boot**
- **PostgreSQL**
- **Firebase Authentication**
- **Docker**

## Prerequisites

- Docker and Docker Compose installed on your system.
- [OPTIONAL] To run locally, Java 17 and a postgres DB

## Building and Running the Application

### 1. Clone the Repository

```bash
git clone https://github.com/manufabri91/expensapp-api.git
cd expensapp-api
```

### 2. Build the Docker Image using local profile

```bash
docker compose --profile local build
```

### 3. Start the Application with Docker Compose

```bash
docker-compose up -d
```

This will start the API and the PostgreSQL database in separate containers.

### 4. Stop the Application

```bash
docker-compose down
```

## Environment Variables

- `FIREBASE_API_KEY`: Firebase private api key.
- `GOOGLE_APPLICATION_CREDENTIALS`: Configuration object provided by firebase authentication for the app.
- `DECRYPT_KEY`: Decrypt key for the config store values (only needed in DEV and PROD modes, for local a dummy value is OK).

## Notes

- The application is exposed on port `8080` by default.
