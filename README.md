[GitHub]The primary objective of this assignment is to build a backend Java web application — the GreenThread Task Management API — for tracking work items. The service uses the Jakarta EE Servlet API, manual server management, and direct JDBC interaction with PostgreSQL (no ORM). The project is built with Java 21 and Maven, packaged as a WAR and intended for manual deployment to a standalone Apache Tomcat 10.1+ instance.

Getting started

You can run the application on a machine by opening a terminal.

Step 1: Install JDK 21 and Maven.

Step 2: Open a terminal and point to the project root folder (where pom.xml is located).

Step 3: Build the project and create the WAR artifact:
mvn clean package
This produces target/{artifact-name}.war.

Step 4: Manual deployment to Tomcat: download and unpack a standalone Apache Tomcat 10.1+ distribution, copy the generated WAR into the tomcat/webapps directory, then start the server:

Linux / macOS
bin/startup.sh

Windows
bin/startup.bat
Verify the API is reachable at http://localhost:8080/{artifact_name}/tasks.

Database setup

The project includes a ddl.sql file (located in src/main/resources) containing DDL statements to create the required database objects.

Configuration

Database credentials must be provided externally (not hardcoded). The application reads connection settings from environment variables or an external properties file. Required environment variables:
DB_URL — JDBC URL, e.g. jdbc:postgresql://localhost:5432/your_database
DB_LOGIN — database username
DB_PASSWORD — database password

On Windows PowerShell:
setx DB_LOGIN "your_login"
setx DB_PASSWORD "your_password"
setx DB_URL "jdbc:postgresql://localhost:5432/your_database"