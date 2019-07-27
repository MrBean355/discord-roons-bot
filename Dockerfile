FROM anapsix/alpine-java
COPY build/libs/roons-bot-*.jar /roons-bot.jar
COPY src/main/resources/roons.mp3 /roons.mp3
EXPOSE 12345
ENTRYPOINT ["java", "-jar", "roons-bot.jar"]