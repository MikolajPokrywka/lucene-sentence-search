FROM openjdk:8 as builder

COPY . .

RUN ./gradlew installDist

# Second stage - only jre
FROM openjdk:8u171-jre-slim
COPY --from=builder build/install/tm /tm

CMD ["tm/bin/tm", "--port", "80"]
