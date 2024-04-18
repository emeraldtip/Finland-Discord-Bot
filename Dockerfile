FROM alpine:3.19.1
#install nescessary java packages
RUN apk update && \
    apk add openjdk17-jdk && \
    apk add gradle
#copy repo files
COPY . .
#build it
RUN gradle build
#move config file into the correct place
RUN mv config.yml build/libs/config.yml
#run the bot
CMD java -jar build/libs/FinlandBot-*-all.jar
