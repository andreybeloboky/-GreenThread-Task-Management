[GitHub]The primary objective of this assignment is to build a console-based Java application for managing a financial investment portfolio. The program connects to a PostgreSQL database, loads investment data such as stocks, bonds, and mutual funds, and processes it through a layered architecture consisting of controllers, services, repositories, and models. The project is built using Java 21 and Maven.

Getting started

You can run your Java program on a machine by opening a terminal.

**Step 1:** Install JDK 21.

**Step 2:** Open a terminal and point to the folder or package where your main program is present.

**Step 3:** Compile the program using Maven or the javac command. If you want to compile manually, navigate to the folder project > src > main > java > org.example > controller > Main and run javac .java.

**Step 4:** After compilation, run the program using the java command and specify the full package path, for example: java org.example.controller.Main. If you are using Maven, you can run mvn clean package and then execute the generated JAR file from the target directory.

This project also includes a ddl.sql file that contains DDL statements for creating all required database tables. The ddl.sql file is located in the resources folder of the project. To execute this file in PostgreSQL, open a terminal and connect to your PostgreSQL server using the command `psql -U postgres`. After connecting, create a database if needed, then run the ddl.sql script using the command `psql -U postgres -d your_database_name -f path_to_ddl.sql`. This will execute all DDL statements defined in the script and create the necessary tables for the application.

The application uses environment variables to read database credentials. The variables DB_LOGIN, DB_PASSWORD and DB_URL must be created before running the program. On Windows PowerShell you can create them using the commands: setx `DB_LOGIN "your_login"`, setx `DB_PASSWORD "your_password"`, setx `DB_URL "jdbc:postgresql://localhost:5432/your_database_name"`. After creating them, restart the terminal so the variables become available. On Linux or macOS you can add them to your shell configuration file such as ~/.bashrc or ~/.zshrc by writing export `DB_LOGIN="your_login"`, export `DB_PASSWORD="your_password"`, export` DB_URL="jdbc:postgresql://localhost:5432/your_database_name`" and then applying the changes with the command source ~/.bashrc.

If you are running the program through IntelliJ IDEA, you can set these variables by opening Run → Edit Configurations and adding `DB_LOGIN`, `DB_PASSWORD` and `DB_URL` to the Environment variables field. The program will then be able to read them using `System.getenv("DB_LOGIN")`, `System.getenv("DB_PASSWORD")` and `System.getenv("DB_URL")`.
