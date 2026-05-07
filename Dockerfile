FROM eclipse-temurin:21-jre

ARG USER=appuser
ARG GROUP=appgroup

ENV TZ=Asia/Seoul
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-kor && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd -r ${GROUP} && \
    useradd -r -g ${GROUP} -s /bin/false ${USER}

RUN mkdir -p /app/logs && \
    chown -R ${USER}:${GROUP} /app/logs

COPY --chown=${USER}:${GROUP} build/libs/*SNAPSHOT.jar app.jar

USER ${USER}

ENTRYPOINT ["java", \
            "-Xms1024m", \
            "-Xmx2048m", \
            "-jar", \
            "app.jar"]
