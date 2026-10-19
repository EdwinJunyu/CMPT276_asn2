# CMPT276 ASN2 — Rate CS Teaching Staff

## How to run locally

Install **JDK 21** and **PostgreSQL**, and set `JAVA_HOME` to your JDK 21 installation. The project includes Maven Wrapper, so a separate Maven installation is not required. The first run requires an internet connection to download dependencies. Run all commands below from the project root.

1. Start PostgreSQL and create a database. For example, using an existing local `postgres` user:

   ```sh
   psql -U postgres -c "CREATE DATABASE asn2_chen;"
   ```

   Skip this step if the database already exists. The application's database user must have permission to connect to the database and create or update tables. The current configuration uses Hibernate `ddl-auto: update` to maintain the table structure automatically.

2. Configure the database connection and start the application. Replace the example username and password with your local credentials.

   **Windows PowerShell:**

   ```powershell
   $env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/asn2_chen'
   $env:SPRING_DATASOURCE_USERNAME = 'postgres'
   $env:SPRING_DATASOURCE_PASSWORD = 'your-local-password'
   $env:PORT = '8080'
   .\mvnw.cmd spring-boot:run
   ```

   **macOS / Linux:**

   ```sh
   export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/asn2_chen'
   export SPRING_DATASOURCE_USERNAME='postgres'
   export SPRING_DATASOURCE_PASSWORD='your-local-password'
   export PORT=8080
   sh ./mvnw spring-boot:run
   ```
3. Open [http://localhost:8080/ratings](http://localhost:8080/ratings). Visiting `/` also redirects to the ratings list. Press `Ctrl+C` to stop the application.

## How to deploy

The following steps deploy to **Render Web Service + Render Postgres** using the repository's existing `Dockerfile`. The Dockerfile builds the application with Maven `verify`, then runs the generated JAR with Java 21.

1. Push the project to a Git repository that Render can access. Ensure that `Dockerfile`, `pom.xml`, `mvnw`, `.mvn/`, and `src/` are committed.
2. Create a **Postgres** database on Render and record its internal hostname, port, database name, username, and password. Place the Web Service in the same region as the database to use an internal connection. 
3. Create a **Web Service**, connect the repository, select the deployment branch, and choose the **Docker** runtime. If the project is at the repository root, keep the default Root Directory and use `./Dockerfile` for Dockerfile Path. If the project is in a subdirectory, set Root Directory to that project directory. Keep the Dockerfile's default startup behavior without overriding Docker Command.
4. Configure the following environment variables for the Web Service:

   | Variable | Value |
   | --- | --- |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<internal-host>:5432/<database-name>` |
   | `SPRING_DATASOURCE_USERNAME` | Render database username |
   | `SPRING_DATASOURCE_PASSWORD` | Render database password |
   | `PORT` | Keep the value provided by Render; the application reads it automatically |

   Replace the placeholders in angle brackets with your database details. The JDBC URL must start with `jdbc:postgresql://`; do not paste Render's `postgresql://user:password@host/database` URL directly. Set the username and password in their respective variables, and do not commit credentials to the repository.
