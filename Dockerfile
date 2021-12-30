FROM adoptopenjdk/openjdk11

VOLUME /tmp

ADD build/libs/SampleJavaSpringService-*.jar application.jar

RUN bash -c 'touch /application.jar'

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx512m", "-Dspring.profiles.active=dev", "/application.jar"]

# Adicionando um comentário para testar o git