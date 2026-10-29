# =====================================================================
# Dockerfile for Subsystem 3: Career Events & Resources
# Builds ONE image that runs our whole service:
#   - the Spring Boot backend (the API: /events, /users/..., ...)
#   - the React frontend, served by Spring Boot as plain files
#
# It uses a "multi-stage build": three steps, each in its own temporary
# container. Only the LAST stage becomes the final image, so the final
# image has Java and our app, but not Node, npm, Maven or source code.
# That keeps it small.
#
#   Stage 1 "frontend": Node builds the React app into static files (dist/)
#   Stage 2 "backend":  Maven builds the Spring Boot jar, with dist/ inside it
#   Stage 3 (final):    a small Java runtime that just runs the jar
#
# Build:  docker build -t events-resources .
# Run:    docker compose up      (easiest: starts the app AND PostgreSQL)
#   or:   docker run -p 8080:8080 -e DB_URL=jdbc:postgresql://<db-host>:5432/events events-resources
# Then open http://localhost:8080
# =====================================================================


# ---------------------------------------------------------------------
# STAGE 1: build the React frontend
# ---------------------------------------------------------------------
# node:22 = the same Node version as our CI and the old Dockerfile.
# "AS frontend" names this stage so stage 2 can copy files out of it.
FROM node:22 AS frontend

# All following commands run inside /app in this temporary container.
WORKDIR /app

# Copy ONLY package.json + package-lock.json first.
# Docker caches each step: if these two files didn't change, the next
# step (npm ci) is reused from cache, so rebuilds after a code change
# don't re-download every package.
COPY frontend/package*.json ./

# npm ci = install exactly what package-lock.json lists (same as CI).
RUN npm ci

# Now copy the rest of the frontend source code.
COPY frontend/ .

# Build-time settings for Vite. ARG = a value you can pass with
#   docker build --build-arg VITE_DEV_TOKEN=staff-token -t events-resources .
# Vite reads VITE_* variables while building and bakes them into the JS.
#   VITE_DEV_TOKEN: which test token the page uses (student by default;
#                   use staff-token to demo creating events)
ARG VITE_DEV_TOKEN=student-token
ENV VITE_DEV_TOKEN=${VITE_DEV_TOKEN}

# vite build = bundle the app into frontend/dist/ (HTML, JS, CSS files).
RUN npm run build


# ---------------------------------------------------------------------
# STAGE 2: build the Spring Boot backend (with the frontend inside it)
# ---------------------------------------------------------------------
# eclipse-temurin:17-jdk = Java 17 with the compiler (JDK), which Maven
# needs. pom.xml says <java.version>17</java.version>.
FROM eclipse-temurin:17-jdk AS backend

WORKDIR /app

# Copy the Maven wrapper and pom.xml first (same caching trick as npm):
# the dependency download below is reused until pom.xml changes.
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn

# Make sure the wrapper script is allowed to run (it can lose its
# "executable" flag when a repo is cloned on Windows).
RUN chmod +x mvnw

# Download every library pom.xml needs, before copying our code.
# -B = batch mode (plain logs). dependency:go-offline = "download it all now".
RUN ./mvnw -B dependency:go-offline

# Now copy our Java source code.
COPY backend/src src

# Spring Boot automatically serves any file in src/main/resources/static/
# at the root URL. So copying the React build there means:
#   http://localhost:8080/        -> index.html (our React page)
#   http://localhost:8080/events  -> the API, same as before
# --from=frontend = take the files from stage 1.
COPY --from=frontend /app/dist src/main/resources/static

# package = compile and build the jar into target/.
# -DskipTests: the tests need a running PostgreSQL, which doesn't exist
# while the image is being built. CI already runs every test on each PR.
RUN ./mvnw -B -DskipTests package


# ---------------------------------------------------------------------
# STAGE 3: the final, small image that actually runs
# ---------------------------------------------------------------------
# 17-jre = Java 17 runtime only (no compiler): smaller and safer.
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy just the built jar from stage 2 and name it app.jar.
# (Spring Boot names it events-resources-0.0.1-SNAPSHOT.jar; the * matches it.)
COPY --from=backend /app/target/*.jar app.jar

# Database connection. application.properties reads these three values.
# The defaults point at a database service named "db", which is what
# docker-compose.yml creates. Override them with -e when running the
# image somewhere else (for example, the class's shared docker-compose).
ENV DB_URL=jdbc:postgresql://db:5432/events \
    DB_USER=postgres \
    DB_PASSWORD=dev

# Spring Boot listens on port 8080 inside the container.
# (EXPOSE is documentation; -p 8080:8080 or compose's "ports" opens it.)
EXPOSE 8080

# The command that runs when the container starts.
CMD ["java", "-jar", "app.jar"]