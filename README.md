# Distributed Task Scheduler

A distributed task scheduling system built with Java and Spring Boot, featuring multi-threaded job execution, Redis caching, and PostgreSQL persistence.

## Features

- **Multi-threaded Execution**: Thread pool-based task execution with configurable concurrency
- **Priority Scheduling**: Tasks prioritized by urgency (LOW, MEDIUM, HIGH, CRITICAL)
- **Retry Logic**: Automatic retry with configurable max attempts
- **Redis Caching**: Distributed caching for improved performance
- **REST API**: Complete RESTful interface for task management
- **Transaction Management**: ACID-compliant database operations
- **Scheduled Processing**: Automatic task pickup every 5 seconds

## Tech Stack

- Java 17
- Spring Boot 3.2.0
- PostgreSQL
- Redis
- Maven
- Lombok
- JUnit

## Architecture

### Models
- **Task**: Main entity with status tracking, priority, retry logic, and timestamps

### Services
- **TaskService**: Business logic for task creation, execution, scheduling, and cancellation
- Multi-threaded execution using `ExecutorService` with 10 worker threads
- Automatic retry mechanism for failed tasks
- Scheduled task pickup and processing

### API Endpoints

```
POST   /api/tasks              - Create new task
GET    /api/tasks              - Get all tasks
GET    /api/tasks/{id}         - Get specific task
PATCH  /api/tasks/{id}/status  - Update task status
DELETE /api/tasks/{id}         - Cancel task
POST   /api/tasks/{id}/execute - Manually trigger task execution
```

## Setup

1. **Prerequisites**
   - Java 17+
   - PostgreSQL
   - Redis
   - Maven

2. **Database Setup**
```sql
CREATE DATABASE taskscheduler;
```

3. **Configuration**
Update `application.yml` with your database credentials

4. **Build & Run**
```bash
mvn clean install
mvn spring-boot:run
```

## Usage Example

```bash
# Create a task
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Data Processing Job",
    "description": "Process customer data",
    "priority": "HIGH",
    "scheduledTime": "2024-01-10T10:00:00",
    "maxRetries": 3
  }'

# Get task status
curl http://localhost:8080/api/tasks/1
```

## Key Implementation Details

### Concurrent Execution
- Uses `ConcurrentHashMap` to track running tasks
- `ExecutorService` with fixed thread pool (10 threads)
- Automatic cleanup of completed futures

### Error Handling
- Comprehensive try-catch blocks around task execution
- Automatic retry for failed tasks (configurable max retries)
- Detailed error messages stored in database

### Caching Strategy
- Redis caching with 10-minute TTL
- Cache invalidation on create/update/delete operations
- Improves read performance for frequently accessed tasks