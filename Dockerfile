FROM alpine:3.19.1
RUN apk update && \
    apk add openjdk17-jdk && \
    apk add gradle
COPY . .
RUN gradle build
CMD java -jar  build/libs/FinlandBot-0.0.36-all.jar
