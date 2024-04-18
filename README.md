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

## Or docker (It's pretty much butchered to work)
```
sudo docker build -t emeraldtip/finlandbot .
```
to build the image and
```
sudo docker run -i -t emeraldtip/finlandbot .
```
