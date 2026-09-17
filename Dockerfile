FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN javac Main.java

CMD ["java", "Main"]
