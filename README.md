# EMC-Finland-Discordbot

## Building: 
Run gradlew build
```
./gradlew build
```
and the output jar (FinlandBot-version-all.jar) will be in the build/libs folder

## Running:
Create the file "config.yml" with the following structure:
```yml
token: #discord bot token
seed: #seed for voterID generation
```
And then run the jar file (the one ending with "-all.jar") in your shell of choice.
The Java version used in the project is Java 17.

## Or docker
1. Make a data folder, where all the bot data will be stored.
2. Create a config.yml file with the same template as above in the new data folder
3. Create a file called **docker-compose.yml** with the following template
```yml
services:
  finlandbot:
    build: .
    container_name: finlandbot
    restart: unless-stopped
    volumes: 
      - /path/to/the/data/folder/on/your/local/drive:/app/data
```
4. Run
```
docker compose up -d
```
to build and run the container