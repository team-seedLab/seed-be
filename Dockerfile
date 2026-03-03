FROM eclipse-temurin:21-jre

ARG USER=appuser
ARG GROUP=appgroup

ENV TZ=Asia/Seoul
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

WORKDIR /app

RUN groupadd -r ${GROUP} && \
    useradd -r -g ${GROUP} -s /bin/false ${USER}

RUN mkdir -p /app/logs && \
    chown -R ${USER}:${GROUP} /app/logs

COPY --chown=${USER}:${GROUP} build/libs/*SNAPSHOT.jar app.jar

USER ${USER}

ENTRYPOINT ["java", \
            "-Xms1024m", \
            "-Xmx2048m", \
            "app.jar"]