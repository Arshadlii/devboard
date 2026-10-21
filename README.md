# DevBoard — Project Management & Issue Tracking Platform

DevBoard is a web-based project management and issue tracking application that helps teams organize projects, manage tasks, collaborate through comments, share attachments, and track activity.

## Features

- User registration and login with password hashing.
- Role-based access control for administrators, project managers, and developers.
- Project creation and project membership management.
- Search registered developers by name or email.
- Task creation, assignment, and status management.
- Task status workflow with validated transitions.
- Task comments and activity history.
- File attachments with upload and download functionality.
- PostgreSQL persistence.
- Responsive web interface built with HTML, CSS, and JavaScript.

## Technology Stack

| Component | Technology |
|---|---|
| Backend | Java 21, Spring Boot |
| Security | Spring Security, BCrypt |
| Database | PostgreSQL |
| Persistence | Spring Data JPA, Hibernate |
| Frontend | HTML, CSS, JavaScript |
| Build tool | Maven |
| Version control | Git and GitHub |

## Prerequisites

Install the following before running the application:

- Java Development Kit (JDK) 21
- PostgreSQL
- Git
- A web browser

## Setup and Installation

### 1. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd devboard
```

Replace the placeholder with the actual GitHub repository URL.

### 2. Create the database

Open PostgreSQL or pgAdmin and execute:

```sql
CREATE DATABASE devboard_db;
```

If the database already exists, do not create it again.

### 3. Configure database credentials

Configure the datasource in `src/main/resources/application.properties`.

Prefer environment variables for credentials:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/devboard_db}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
```

Keep the project's existing settings where needed. The example above illustrates environment-based configuration; verify that the property names match your application before replacing anything.

Set `DB_USERNAME` and `DB_PASSWORD` in your local environment. Never commit real passwords, API keys, or other secrets to GitHub.

### 4. Run the application

On Windows:

```powershell
.\mvnw.cmd clean spring-boot:run
```

On macOS or Linux:

```bash
./mvnw clean spring-boot:run
```

Wait until Spring Boot reports that the application has started.

### 5. Open DevBoard

Visit:

http://localhost:8080

Register an account or sign in with an existing account.

## User Roles

| Role | Responsibilities |
|---|---|
| Administrator | Administrative access, subject to application authorization rules |
| Project Manager | Create projects and manage project tasks and members where authorized |
| Developer | Access assigned or joined projects and collaborate on tasks |

Authorization is enforced by the backend. Hiding a button in the frontend is not a substitute for backend permission checks.

## API Endpoints

All routes below use the `/api` prefix. Routes listed with path parameters require the corresponding IDs.

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a user |
| POST | `/api/auth/login` | Sign in |
| GET | `/api/auth/me` | Get the current authenticated user |
| POST | `/api/auth/logout` | Sign out |

### Projects and Members

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/projects` | Create a project |
| GET | `/api/projects` | List accessible projects |
| GET | `/api/projects/{projectId}` | Get project details |
| GET | `/api/projects/{projectId}/members` | List project members |
| POST | `/api/projects/{projectId}/members` | Add a project member |
| GET | `/api/projects/{projectId}/developer-options?q={term}` | Search eligible developers |
| DELETE | `/api/projects/{projectId}/members/{userId}` | Remove a project member |

### Tasks and Collaboration

Task routes use the project and task IDs. The implemented features include task creation, status updates, comments, activity history, and attachments.

Verify the exact controller mappings in the source code before adding the individual task routes to this table.

## Attachments

- Maximum supported file size: 5 MB.
- Supported extensions include PDF, PNG, JPG, JPEG, TXT, CSV, DOC, and DOCX.
- Attachments are stored on the server, with metadata stored in PostgreSQL.

The default upload directory is local. For production deployment, configure persistent file storage and protect uploaded files appropriately.

## Testing

The following scenarios should be verified before submission:

- Registration and login.
- Logout and session persistence.
- Unauthorized project access is rejected.
- Valid task status transitions succeed.
- Invalid status transitions are rejected.
- Developers can be searched and added to projects.
- Authorized users can remove eligible project members.
- Comments and activity records are displayed.
- Attachments can be uploaded and downloaded.
- Invalid file types and files exceeding the size limit are rejected.

## Test Accounts

Use the accounts created in your local test environment. Do not publish real passwords or private user information.

For assessment demonstrations, create dedicated test accounts for each role and provide credentials to the evaluator through the submission channel if required.

| Role | Test email |
|---|---|
| Project Manager | Use your dedicated test account |
| Developer | Use your dedicated developer test account |
| Administrator | Use a dedicated administrator account if configured |

## Security Notes

- Passwords are stored using password hashing.
- API access is protected by authentication and authorization rules.
- Database credentials should be configured through environment variables.
- Uploaded files should be validated and stored securely.
- Production deployments should use HTTPS and persistent storage.
- Do not commit secrets, private credentials, or user-uploaded files to GitHub.

## Future Improvements

- Production deployment with persistent attachment storage.
- Automated integration and authorization tests.
- Improved task filtering and reporting.
- Email notifications and team activity alerts.

## Author

Developed as a project management and issue tracking application for a software development assessment.

**Repository:** [Add your GitHub repository URL here](https://github.com/Arshadlii/devboard).

**Demo recording:** Add your recording link here after uploading it.
