FROM public.ecr.aws/z9u4r7b2/java-base

VOLUME /tmp

ADD build/libs/SampleJavaSpringService-*.jar application.jar

RUN bash -c 'touch /application.jar'

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx512m", "-Dspring.profiles.active=dev", "/application.jar"]