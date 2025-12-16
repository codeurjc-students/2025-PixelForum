# 📘 Development Guide
## 📑 Index
- [Introduction](#1️⃣-Introduction)
- [Execution Technologies](#2️⃣-Execution-Technologies)
- [Tools](#3️⃣-Tools)
- [Architecture](#4️⃣-Architecture)
- [Quality Control](#5️⃣-Quality-Control)
- [Development Process](#6️⃣-Development-Process)
- [Execution & Release](#7️⃣-Execution--Release)
  
## 1️⃣ Introduction

**PixelForum** is a web application designed as an interactive forum for video game enthusiasts 🎮.  
Its architecture follows a **Single Page Application (SPA)** model , where the client-side interface is dynamically rendered by the browser while the backend provides data through API REST.  

The system is divided into three main layers:  
- 🖥️ **Client (Frontend):** Developed with **Angular**, responsible for the graphical interface and routing without page reloads.  
- 🧠 **Server (Backend):** Implemented with **Spring Boot**, which exposes a REST API that handles business logic, user authentication (JWT), and communication with the database. 
- 🗄️ **Database:** A **MySQL** relational database that stores users, forum posts, comments, and related data.  

Deployment is containerized with **Docker 🐳** and orchestrated using **Docker Compose**, allowing the system’s components to run as isolated services.  
Continuous Integration and Deployment (CI/CD) pipelines are configured through **GitHub Actions**, automating build, testing, and deployment processes.

---

### 📋 Summary Table

| Category | Description |
|----------|-------------|
| 🧩 **Type** | Web SPA + REST API (client–server architecture) |
| 💻 **Technologies** | Java 21, Spring Boot, JPA, JWT, MapStruct, Angular, MySQL |
| 🛠️ **Tools** | Visual Studio Code, Git, GitHub Actions, Docker, Docker Compose |
| ✅ **Quality Control** | Unit, integration & system tests (JUnit 5, Selenium), static analysis (Sonar), test coverage (JaCoCo) |
| 🚀 **Deployment** | Dockerized services with Docker Compose |
| 🔁 **Development Process** | Iterative & incremental workflow; Git for version control; CI/CD pipelines using GitHub Actions |

---

## 2️⃣ Execution Technologies

- **Angular** — Framework for building the SPA interface and managing client-side routing. It consumes REST endpoints to render data dynamically. 🌐 [Angular](https://angular.dev)
  
- **Java 21 (Oracle JDK)** — Runtime used to execute the backend Spring Boot application. Provides the Java Virtual Machine (JVM) and standard libraries. 🌐 [Java](https://www.oracle.com/java/)

- **Spring Boot** — Runtime framework for the backend, managing application lifecycle, dependency injection, and REST API execution. 🌐 [Spring Boot](https://spring.io)

- **MySQL Server** — Database engine used to persist application data during runtime. 🌐 [MySQL](https://www.mysql.com)

- **Node.js** — Runtime environment for executing the Angular build (serving the frontend in production). 🌐 [Node.js](https://nodejs.org)

- **npm** — Package manager and execution tool for Angular scripts and frontend build tasks in production. 🌐 [npm](https://www.npmjs.com)
  
- **Docker** — Container runtime that allows the application (backend, frontend, database) to run consistently across environments. 🌐 [Docker](https://www.docker.com)

---

## 3️⃣ Tools
  
- **Visual Studio Code (VS Code):** Main IDE used for developing both frontend (Angular) and backend (Spring Boot) modules, supporting extensions for Java, Docker, and Git integration. 🌐 [Visual Studio Code](https://code.visualstudio.com)

- **Git** — Version control system used to manage source code, track changes, and collaborate with team members. 🌐 [Git](https://git-scm.com)

- **GitHub** — Platform for hosting Git repositories, managing issues (Kanban board), pull requests, and CI/CD pipelines. 🌐 [GitHub](https://github.com)

- **GitHub Actions:** Enables automated CI/CD workflows for testing and deployment. 🌐 [Github actions](https://github.com/features/actions)

- **Docker & Docker Compose:** Used for containerizing and orchestrating the services (frontend, backend, database) into a reproducible deployment environment. 🌐 [Docker](https://www.docker.com)

---

## 4️⃣ Architecture

### Deployment Architecture

The application follows a **microservices-inspired deployment architecture**, with independent processes for the backend, frontend, and database:
- 🖥️ **Frontend (Angular)** — Runs as a separate process, served via **Node.js**, consuming the backend REST API.
- 🧠 **Backend (Spring Boot)** — Runs as an independent process exposing REST endpoints over **HTTPS**.
- 🗄️ **Database (MySQL)** — Runs as an independent process, accessed by the backend via the **JDBC protocol**.

All services communicate using standard protocols: the frontend and backend interact via **REST over HTTPS** (backend on port **8443**, frontend on port **4200**), while the backend connects to the database using **TCP/IP** (MySQL on port **3306**).

### API REST

The REST API documentation is automatically generated using **springdoc-openapi** and made available via Swagger UI.  
An HTML version is hosted using [raw.githack.com](https://raw.githack.com), providing a live view of the API specification, here:
 🌐 [Api-docs.html](https://rawcdn.githack.com/codeurjc-students/2025-PixelForum/65f151c814cd4a7f095f490b6a65a9393d05e418/docs/api/api-docs.html)

---

## 5️⃣ Quality Control

This section describes the quality assurance strategy applied to **PixelForum**, including automated tests on both backend and frontend, system-level tests, API validation, and static code analysis using **SonarQube**.  
The goal of these quality controls is to ensure reliability, security, maintainability, and functional correctness across all components of the SPA + REST API architecture.

### Server tests (Backend)
The backend (Spring Boot) includes automated tests that validate business logic, data access, and API behavior.

#### ✔️ Types of backend tests
- **Unit tests (JUnit 5 + Mockito):** Validate isolated service logic and utility classes without loading the Spring context. Dependencies such as repositories are mocked to ensure that only the business logic is tested in complete isolation.
- **Integration tests (Spring Boot Test):** Validate the interaction between services and repositories within a full Spring application context. These tests run against the real MySQL database used in the development environment, ensuring that persistence operations and service behavior function correctly under realistic conditions.


#### 📊 Backend testing statistics
```bash
mvn test
```
###### Client tests (Frontend)e frontend (Angular) includes unit and integration tests that validate UI logic and service communication.

#### ✔️ Types of client tests
- **Unit tests (Karma + Jasmine):** Validate Angular components and services using Angular’s TestBed with mocked dependencies. These tests verify component logic, service behavior, and simplified DOM interactions in isolation, without relying on external modules or real network calls.
- **Integration tests (Real API integration):** Validate the behavior of frontend services by performing real HTTP requests against the backend API. These tests ensure that the Angular application communicates correctly with the Spring Boot backend, verifying data flow and real network integration.

#### 📊 Frontend testing statistics
```bash
npm run test:coverage
```

## System Tests
System tests validate the behavior of the application as a whole, combining the frontend, backend, and data layers under real execution conditions. These tests do not isolate components; instead, they verify the end-to-end functionality from the user perspective or through full backend interactions. Two technologies are used in this project for system testing: Selenium WebDriver for UI-level tests and RestAssured for backend API-level system verification.

### Static Code Analysis (SonarQube)
Static code analysis was performed using SonarQube, a tool that inspects the source code without executing it. SonarQube helps to identify potential bugs, code smells, security vulnerabilities, and maintainability issues, providing an overall quality assessment of the project.

## 6️⃣ Development Process
The development process followed an iterative and incremental approach, based on the principles of the Agile manifesto. Some best practices from Extreme Programming (XP) and Kanban were applied to improve code quality, collaboration, and task management. Although Scrum was not formally implemented, the process emphasizes continuous improvement, frequent deliveries, and incremental progress.

### 🧠 Task Management
- Tasks tracked through **GitHub Issues**.  
- Visual workflow managed via **GitHub Projects** (Kanban board). *Captura*

### 🌿 Version Control
- Repository managed with **Git**.  
- Branching strategy:  
  - `main` → stable release branch  
  - `feature` → branch for developing new features; merged into main after review and testing
  - `fix` → branch for bug fixes; merged into main once the issue is resolved and tested
- **Metrics:** ~300 commits and 20+ branches over development lifecycle.  

### ⚙️ Continuous Integration / Deployment
- **CI pipeline:** Runs automated tests, builds Docker images, and performs static analysis.  
- **CD pipeline:** Deploys the latest stable version using Docker Compose.  

---

## 7️⃣ Execution & Release

### 7.1 **Clone the repository:**
To start working with the project, clone the Git repository using the following command:
```bash
git clone https://github.com/username/pixelforum.git
```

### 7.2 **Execution**
#### Backend (Spring Boot)
1. Ensure that the MySQL database is running locally. For example, start the MySQL server and make sure the database dbpixelforum exists, or create it using:
```sql
CREATE DATABASE dbpixelforum;
```
2. Configure your application.properties file with the correct database credentials.
3. Run the Spring Boot backend using Maven:
```bash
cd backend
mvn spring-boot:run
```
The backend server will start at:
[http://localhost:8080](http://localhost:8080)

#### Frontend (Angular)
1. Navigate to the frontend directory:
```bash
cd frontend
```
2. Run the Angular application:
```bash
npm start
```
The frontend will be available at:
[http://localhost:4200](http://localhost:4200)
It will communicate with the backend API at [http://localhost:8080](http://localhost:8080).

### 7.3 Using Development Tools
- IDE / Editor: The project can be edited using IntelliJ IDEA or Visual Studio Code. These environments provide syntax highlighting, debugging tools, and integration with Git.

- Postman / REST Client: Use Postman or similar tools to interact with the backend REST API. The provided Postman collection includes examples for all API endpoints with sample data. Import the file Postman_collection.json to send GET, POST, PUT, and DELETE requests easily.

### 7.4 Running Tests
#### Backend
Unit Tests:
```bash
mvn test -Dgroups=unit
```
Integration Tests: 
```bash
mvn test -Dgroups=integration
```
System / E2E Tests:
```bash
mvn test -Dgroups=e2e
```
To execute all backend and system tests at once use:
```bash
mvn test
```

#### Frontend
Unit Tests:
```bash
npm run test:unit
```
Integration Tests: *the application most be running in order to execute the tests*
```bash
npm run test:integartion
```
System / E2E Tests: *the application most be running in order to execute the tests*
```bash
cd backend
mvn -Dtest=UISystemTest test
```
To execute all frontend tests at once use:
```bash
ng test
```