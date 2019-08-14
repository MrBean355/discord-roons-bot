FROM anapsix/alpine-java
COPY build/libs/roons-bot-*.jar /roons-bot.jar
COPY src/main/resources/roons.mp3 /roons.mp3
EXPOSE 26382
ENTRYPOINT ["java", "-jar", "roons-bot.jar"]
ENV JVM_OPTS="-XX:MaxRAM=400m -Xmx300m -XX:MaxRAMFraction=1"