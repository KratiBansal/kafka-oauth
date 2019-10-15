# Kafka OAuth2 Library

### Sand Config File
Configure this environment variable to a path of file containing SAND configs.

- KAFKA_SAND_CONFIG_PATH: path to properties file which holds config for sand server. Defaults: /etc/kafka/sand.properties

### Kafka-Sand Configuration
Add the below properties in your SAND configs file(KAFKA_SAND_CONFIG_PATH)

```
sand.server.url=FIXME
sand.token_path=/oauth2/token
sand.token_verify_path=/warden/token/allowed
sand.client.id=FIXME
sand.client.secret=FIXME
sand.client.scopes="FIXME1,FIXME2"
sand.service.scopes=hydra
sand.service.target_scopes="FIXME1,FIXME2"
sand.service.resource=FIXME
sand.service.action=any
```

### Kafka Console Commands

To use the Kafka commands like `kafka-console-producer.sh` and `kafka-console-consumer.sh`, you first needs a client.properties file, which should contain something like the following:

```bash
security.protocol=SASL_SSL
ssl.endpoint.identification.algorithm=
ssl.keystore.type=BCFKS
ssl.secure.random.implementation=DEFAULT
ssl.truststore.type=BCFKS
ssl.truststore.location=/var/private/ssl/kafka.truststore.bcfks
ssl.truststore.password=<truststore_password>
sasl.mechanism=OAUTHBEARER
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;
sasl.login.callback.handler.class=com.coupa.kafka.security.sasl.oauth.KafkaBrokerTokenCreator
```

To use the `kafka-console-producer.sh`, use:

```bash
export KAFKA_SAND_CONFIG_PATH=/etc/kafka/sand.properties
sudo KAFKA_OPTS="-Djava.security.properties=/etc/kafka/bcfips.java.security -Djavax.net.ssl.trustStore=/var/private/ssl/kafka.truststore.bcfks -Djavax.net.ssl.trustStorePassword=<truststore_password>" /srv/kafka/current/kafka_2.12-2.0.0/bin/kafka-console-producer.sh --broker-list localhost:9092 --producer.config client.properties --topic test-topic1
```

To use the `kafka-console-consumer.sh`, use:

```
export KAFKA_SAND_CONFIG_PATH=/etc/kafka/sand.properties
sudo KAFKA_OPTS="-Djava.security.properties=/etc/kafka/bcfips.java.security -Djavax.net.ssl.trustStore=/var/private/ssl/kafka.truststore.bcfks -Djavax.net.ssl.trustStorePassword=<truststore_password>" /srv/kafka/current/kafka_2.12-2.0.0/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --consumer.config client.properties --topic test-topic1 --from-beginning
```

You can also add `-Dlog4j.configuration=file:/srv/kafka/2.12-2.0.0/kafka_2.12-2.0.0/config/log4j.properties` to the `KAFKA_OPTS` variable.

### Kafka Server Configuration

Add this properties in server.properties

- security.inter.broker.protocol=SASL_PLAINTEXT or (SASL_SSL)
- sasl.mechanism.inter.broker.protocol=OAUTHBEARER
- sasl.enabled.mechanisms=OAUTHBEARER
- listener.name.sasl_plaintext.oauthbearer.sasl.login.callback.handler.class=com.coupa.kafka.security.sasl.oauth.KafkaBrokerTokenCreator
- listener.name.sasl_plaintext.oauthbearer.sasl.server.callback.handler.class=com.coupa.kafka.security.sasl.oauth.KafkaTokenValidator
- listeners=SASL_PLAINTEXT://:{PORT} or (SASL_SSL://:{PORT})
- advertised.listeners=SASL_PLAINTEXT://{HOST_IP}:{PORT} or (SASL_SSL://{HOST_IP}:{PORT})

### JAAS Security Configuration

1. Create an file called kafka_server_jaas.conf with this content:
    ```
    KafkaServer {
        org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required ;
    };
    ```
2. Add this file to config path of kafka. Add `-Djava.security.auth.login.config=<path_to_kafka_server_jaas.conf>` at java args to load the configuration file.


### How to build/package the library.

```bash
mvn3/maven/mvn package
```
