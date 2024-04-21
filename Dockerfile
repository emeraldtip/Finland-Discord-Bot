FROM alpine:3.19.1
#install nescessary java packages
RUN apk update && \
    apk add openjdk17-jdk && \
    apk add gradle && \
    apk add git
#copy repo files
COPY . .
#build it
RUN gradle build

#set up app folder
RUN mkdir app
RUN mv build/libs/FinlandBot-*-all.jar app/
RUN mkdir app/data

#run the bot
CMD java -jar app/FinlandBot-*-all.jar