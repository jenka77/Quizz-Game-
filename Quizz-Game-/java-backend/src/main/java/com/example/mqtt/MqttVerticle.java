package com.example.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class MqttVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MqttVerticle.class);

    @Override
    public void start() {

        // LOCALHOST / Docker-Broker:
        // Im Container spricht das Backend mit dem Hostnamen in MQTT_BROKER_URL (z.B. "mosquitto").
        String mqttBrokerHost = System.getenv("MQTT_BROKER_URL") != null
                ? System.getenv("MQTT_BROKER_URL").trim()
                : "mosquitto";

        // Port (TCP), standardmäßig 1883
        int mqttBrokerPort = 1883;
        if (System.getenv("MQTT_BROKER_PORT") != null) {
            try {
                mqttBrokerPort = Integer.parseInt(System.getenv("MQTT_BROKER_PORT"));
            } catch (NumberFormatException ignored) {
                // bleibt 1883
            }
        }

        String mqttUsername = System.getenv("MQTT_USERNAME") != null ? System.getenv("MQTT_USERNAME") : "your_mqtt_username";
        String mqttPassword = System.getenv("MQTT_PASSWORD") != null ? System.getenv("MQTT_PASSWORD") : "your_mqtt_password";

        boolean useSsl = "true".equalsIgnoreCase(String.valueOf(System.getenv("MQTT_USE_SSL")));
        boolean trustAll = "true".equalsIgnoreCase(String.valueOf(System.getenv("MQTT_SSL_TRUST_ALL")));

        // Capture-safe copies for lambda below (must be effectively final)
        final String mqttBrokerHostFinal = mqttBrokerHost;
        final int mqttBrokerPortFinal = mqttBrokerPort;
        final boolean useSslFinal = useSsl;
        final boolean trustAllFinal = trustAll;

        logger.info("MQTT connect -> host={} port={} ssl={} trustAll={}", mqttBrokerHostFinal, mqttBrokerPortFinal, useSslFinal, trustAllFinal);

        MqttClientOptions options = new MqttClientOptions()
                .setAutoKeepAlive(true)
                .setCleanSession(true)
                .setUsername(mqttUsername)
                .setPassword(mqttPassword)
                .setSsl(useSsl)
                .setTrustAll(trustAll);

        // Vert.x requires an explicit hostname verification algorithm when SSL is enabled.
        // "HTTPS" is the standard choice for TLS over TCP.
        if (useSslFinal) {
            options.setHostnameVerificationAlgorithm("HTTPS");
        }

        MqttClient mqttClient = MqttClient.create(vertx, options);

        mqttClient.connect(mqttBrokerPortFinal, mqttBrokerHostFinal, ar -> {
            if (ar.succeeded()) {
                logger.info("MQTT connected OK -> host={} port={} ssl={}", mqttBrokerHostFinal, mqttBrokerPortFinal, useSslFinal);
                MqttController mqttController = new MqttController(mqttClient, vertx);
                mqttController.registerEventBusConsumers();
                mqttController.registerMqttConsumers();

            } else {
                logger.error("MQTT connect FAILED -> host={} port={} ssl={} : {}", mqttBrokerHostFinal, mqttBrokerPortFinal, useSslFinal,
                        ar.cause() != null ? ar.cause().getMessage() : "unknown");
            }
        }
        );

    }
}
