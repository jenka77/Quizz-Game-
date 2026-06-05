package com.example.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;

public class MqttService {

    private final MqttClient mqttClient;

    final String mqttMessagePrefix = System.getenv("MQTT_MESSAGE_PREFIX") != null ? System.getenv("MQTT_MESSAGE_PREFIX") : "test/";

    public MqttService(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void publishGameStart() {
        JsonObject data = new JsonObject().put("action", "start");
        mqttClient.publish(mqttMessagePrefix + "game/start", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
    }

    public void publishGameStop() {
        JsonObject data = new JsonObject().put("action", "stop");
        mqttClient.publish(mqttMessagePrefix + "game/stop", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
    }

    public void publishQuestion(JsonObject question) {
        mqttClient.publish(mqttMessagePrefix + "game/question", question.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
    }

    public void publishPreQuestionPing(String controllerId, String pingId) {
        JsonObject payload = new JsonObject()
                .put("requestId", pingId)
                .put("ts", System.currentTimeMillis());
        String topic = mqttMessagePrefix + "controller/" + controllerId + "/ping";
        mqttClient.publish(topic, payload.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
    }

    /** group-XX/game/state – Backend sendet State-Übergang (LOBBY/COUNTDOWN/QUESTION/EVALUATION/END) */
    public void publishGameState(String state, Integer countdownSeconds, String message) {
        JsonObject payload = new JsonObject()
                .put("state", state != null ? state : "LOBBY")
                .put("ts", System.currentTimeMillis());
        if (countdownSeconds != null) {
            payload.put("countdownSeconds", countdownSeconds);
        }
        if (message != null && !message.isBlank()) {
            payload.put("message", message);
        }
        mqttClient.publish(mqttMessagePrefix + "game/state", payload.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
    }

    /** group-XX/game/countdown – 3-Sekunden-Countdown-Ticks (3, 2, 1) */
    public void publishCountdownTick(int seconds) {
        JsonObject payload = new JsonObject()
                .put("tick", seconds)
                .put("ts", System.currentTimeMillis());
        mqttClient.publish(mqttMessagePrefix + "game/countdown", payload.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
    }
}
