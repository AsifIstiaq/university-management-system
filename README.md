# University Management System

A Spring Boot (Maven) application with three role based panels - **Admin**, **Teacher** and **Student** -
a **REST API**, a basic **Thymeleaf UI**, **Spring Security** authentication/authorization, and **Docker** support.

| | |
|---|---|
| Java / Spring Boot | 17 / 3.4.2 |
| Database | PostgreSQL 16 (H2 in-memory for tests) |
| Architecture | MVC + layered: `controller -> service -> repository -> entity`, with `dto` objects at the edges |
| Security | Spring Security 6: HTTP Basic for `/api/**`, form login + session + CSRF for the UI, BCrypt passwords |

## Quick start

### Option 1 - Docker (recommended)

```bash
docker compose up --build
```

Open <http://localhost:8080>. Settings come from `.env` (DB credentials, port, bootstrap admin).
Data is kept in the `pgdata` volume; `docker compose down -v` wipes it.

### Option 2 - Local

1. Start PostgreSQL with a database `universitydb` and user/password `admin` / `admin`
   (or `docker compose up postgres` and publish port 5432, or edit `src/main/resources/application.yml`).
2. Run:
   ```bash
   ./mvnw spring-boot:run
   ```

### Default accounts

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Teacher (demo) | `teacher1` | `teacher123` |
| Student (demo) | `student1` | `student123` |

The admin is created on first start if no admin exists (`ADMIN_USERNAME` / `ADMIN_PASSWORD`).
The demo teacher/student are created when `SEED_DEMO_DATA=true`. **Change these / disable the demo data for any real deployment.**

## Who can do what

| Action | Admin | Teacher | Student |
|---|:---:|:---:|:---:|
| Create / read / update / delete **teachers** | yes | **yes** | no |
| Create / read / update / delete **students** | yes | no | **yes** |
| Create / read / update / delete **admins** | yes (can't delete the last admin) | no | no |
| Read / update / delete **own** account | yes | yes | yes |

In short: an **admin** manages all three account types, a **teacher** manages teacher accounts only, and a
**student** manages student accounts only. A teacher can never see or change students or admins, and a student can
never see or change teachers or admins - not in the UI and not through the API.

Details:

* **Create** - teachers create other teachers, students create other students, admins create any account.
  There is no public self-registration.
* **Update** - when managing accounts (the teacher / student lists, or `PUT /api/{type}/{id}`) the full record can be
  edited, including academic fields (teacher department/designation, student roll/department/semester).
  Editing *your own* profile (the "My Profile" page, `/me` endpoints) only covers personal details
  (name, email, phone, password).
* **Delete** - teachers may delete any teacher, students any student. Deleting yourself is done from "My Profile"
  (the list hides the delete button on your own row). The last remaining admin can never be deleted.
* The `/me` operations always use the id of the logged-in user from the session/credentials - never an id supplied in
  the request.
* An account id is only ever looked up through its own type's repository, so `DELETE /api/teachers/{id}` with a
  student's or admin's id returns `404` and changes nothing.
* Usernames are immutable. The password is optional on update (blank = unchanged).
* Because accounts are fully managed by peers, a teacher can edit (and reset the password of) another teacher, and a
  student can do the same to another student. This follows the requested "same CRUD as admin, limited to your own type".

The rules live in one place: `config/SecurityConfig.java`.

## Basic UI

Log in at <http://localhost:8080/login>; you are redirected to the panel for your role.

| Panel | URLs |
|---|---|
| Admin | `/admin` (dashboard), `/admin/teachers`, `/admin/students`, `/admin/admins`, `/admin/profile` |
| Teacher | `/teacher/teachers` (list, `/new`, `/{id}/edit`), `/teacher/profile` (+ `/edit`) |
| Student | `/student/students` (list, `/new`, `/{id}/edit`), `/student/profile` (+ `/edit`) |

The teacher and student management pages are the same controllers and templates the admin panel uses
(`TeacherManagementController`, `StudentManagementController`, `templates/manage/*`); only the URL prefix differs.

## REST API

Authenticate every request with HTTP Basic. Errors are JSON (`401`, `403`, `404`, `409`, `400` with `fieldErrors`).

| Method & path | Who | Description |
|---|---|---|
| `GET /api/teachers`, `GET /api/teachers/{id}` | Admin, Teacher | List / read teachers |
| `POST /api/teachers` | Admin, Teacher | Create teacher |
| `PUT /api/teachers/{id}` | Admin, Teacher | Update teacher (personal + academic fields) |
| `DELETE /api/teachers/{id}` | Admin, Teacher | Delete teacher |
| `GET / PUT / DELETE /api/teachers/me` | Teacher | Own record (PUT body: `fullName`, `email`, `phone`, `password`) |
| `GET /api/students`, `GET /api/students/{id}` | Admin, Student | List / read students |
| `POST /api/students` | Admin, Student | Create student |
| `PUT /api/students/{id}` | Admin, Student | Update student |
| `DELETE /api/students/{id}` | Admin, Student | Delete student |
| `GET / PUT / DELETE /api/students/me` | Student | Own record |
| `GET /api/admins`, `GET /api/admins/{id}` | Admin | List / read admins |
| `POST /api/admins` | Admin | Create admin |
| `PUT /api/admins/{id}` | Admin | Update admin |
| `DELETE /api/admins/{id}` | Admin | Delete admin (not the last one) |
| `GET / PUT / DELETE /api/admins/me` | Admin | Own record |

Status codes: `200` OK, `201` Created (with `Location`), `204` No Content (delete), `400` validation error,
`401` not authenticated, `403` role not allowed, `404` not found, `409` duplicate username/email/roll or business rule.

## Project structure

```
.
├── Dockerfile                 multi-stage build (Maven -> slim JRE, non-root)
├── compose.yaml               app + PostgreSQL (with health check)
├── .env                       compose variables (DB, port, bootstrap admin)
├── pom.xml
├── .github/workflows          ci.yml (tests + Docker build) and demo-failing-tests.yml
└── src
    ├── main
    │   ├── java/com/example/university
    │   │   ├── UniversityApplication.java
    │   │   ├── config        AppConfig (ModelMapper, PasswordEncoder), SecurityConfig, DataInitializer, WebBindingAdvice
    │   │   ├── controller    MVC controllers for the UI (Admin*, Teacher/StudentManagementController, Profile, Home)
│   │   │                 + *RestController classes for the API
    │   │   ├── dto           request / response objects and validation rules
    │   │   ├── entity        User (abstract) -> Admin, Teacher, Student; Role
    │   │   ├── exception     custom exceptions + JSON error handler for the API
    │   │   ├── repository    Spring Data JPA repositories
    │   │   ├── security      UserPrincipal, CustomUserDetailsService
    │   │   └── service       AccountService, AdminService, TeacherService, StudentService
    │   └── resources
    │       ├── application.yml
    │       ├── static/css/style.css
    │       └── templates     login, error, admin/* (dashboard, admins), manage/* (teacher/student lists + forms,
    │       │                   shared by the admin, teacher and student panels), profile/*, fragments/layout
    └── test                  unit, repository and integration tests (+ demo/ with tests that fail on purpose)
```

Data model: one `users` table with the shared columns (username, BCrypt password, name, email, phone, role) plus
`admins`, `teachers` and `students` tables for role specific columns (JPA `JOINED` inheritance).

## Tests and continuous integration

```bash
./mvnw test                   # normal run: every test except the demo failures
./mvnw test -Pdemo-failure    # also runs the 2 tests that fail on purpose (demo only)
```

Tests run on an in-memory H2 database (profile `test`), so no PostgreSQL is needed.

| Kind | Test class | What it checks |
|---|---|---|
| Unit | `StudentServiceTest` (2) | duplicate roll is rejected before saving; update trims and applies academic fields |
| Unit | `CustomUserDetailsServiceTest` (2) | login lookup returns the stored hash; unknown user -> `UsernameNotFoundException` |
| Unit | `UserPrincipalTest` (1) | the logged-in user carries the id and a `ROLE_` authority |
| Unit | `TeacherRequestValidationTest` (1) | username/password required on create only, department always |
| Integration | `ApiWorkflowIntegrationTest` (3) | password stored as BCrypt hash and the new teacher can log in; password change invalidates the old one; delete removes rows from both `users` and `teachers` |
| Existing | `TeacherServiceTest`, `AccountServiceTest`, `StudentRepositoryTest`, `SecurityAuthorizationIntegrationTest`, `UniversityApplicationTests` | services, repository, and the full role / permission matrix for the API and UI |
| Demo (fails on purpose) | `DemoFailingUnitTest`, `DemoFailingIntegrationTest` | tagged `demo-failure`; excluded from the normal build |

GitHub Actions (`.github/workflows`):

* `ci.yml` - on every push / pull request: runs all tests (`mvn -B clean verify`), shows a results table in the run
  summary, uploads the test reports, then builds the Docker image (only if the tests passed).
* `demo-failing-tests.yml` - started by hand (Actions -> "Demo - failing tests" -> Run workflow). It includes the two
  tests that fail on purpose, so this run is expected to be red.
