FROM strimzi/kafka:0.17.0-kafka-2.4.0
USER root:root
RUN mkdir -p /opt/kafka/plugins/debezium
COPY ./debezium-connector-mongodb/ /opt/kafka/plugins/debezium/
USER 1001