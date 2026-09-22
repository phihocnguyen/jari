ARG ELK_VERSION=9.1.2
FROM docker.elastic.co/logstash/logstash:${ELK_VERSION}

# JDBC input cần driver PostgreSQL — Logstash tự load mọi jar trong thư mục này
ARG PG_JDBC_VERSION=42.7.4
RUN curl -fsSL \
    -o /usr/share/logstash/logstash-core/lib/jars/postgresql-${PG_JDBC_VERSION}.jar \
    https://repo1.maven.org/maven2/org/postgresql/postgresql/${PG_JDBC_VERSION}/postgresql-${PG_JDBC_VERSION}.jar
