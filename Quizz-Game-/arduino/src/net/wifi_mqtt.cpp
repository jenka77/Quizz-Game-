#include "net/wifi_mqtt.h"
#include "config.h"
#include "secrets.h"

namespace net::wifi_mqtt {

static WiFiClient g_netClient;
static PubSubClient g_client(g_netClient);

static String g_controllerId;
static uint32_t g_lastHeartbeatMs = 0;
static uint32_t g_lastRegisterMs = 0;

static String g_playerName;
static String g_controllerReadyState = "CONNECTED";
static String g_gameState = "LOBBY";
static String g_preQuestionPingId;
static int g_currentQuestionIndex = -1;
static float g_playerTotalScore = 0.0f;
static bool g_hasNewStatus = false;

static String g_lastPingRequestId;
static bool g_hasNewPing = false;

static bool g_hasNewAnswerResult = false;
static bool g_lastAnswerIsCorrect = false;
static float g_lastAnswerPoints = 0.0f;

static bool jsonGetString(const String& json, const char* key, String& out) {
  String pattern = String("\"") + key + "\"";
  int idx = json.indexOf(pattern);
  if (idx < 0) return false;
  idx = json.indexOf(':', idx + pattern.length());
  if (idx < 0) return false;
  idx++;
  while (idx < (int)json.length() && (json[idx] == ' ' || json[idx] == '\n' || json[idx] == '\r' || json[idx] == '\t')) idx++;
  if (idx >= (int)json.length() || json[idx] != '"') return false;
  int start = idx + 1;
  int end = json.indexOf('"', start);
  if (end < 0) return false;
  out = json.substring(start, end);
  return true;
}

static bool jsonGetInt(const String& json, const char* key, int& out) {
  String pattern = String("\"") + key + "\"";
  int idx = json.indexOf(pattern);
  if (idx < 0) return false;
  idx = json.indexOf(':', idx + pattern.length());
  if (idx < 0) return false;
  idx++;
  while (idx < (int)json.length() && (json[idx] == ' ' || json[idx] == '\n' || json[idx] == '\r' || json[idx] == '\t')) idx++;
  if (idx >= (int)json.length()) return false;
  bool neg = false;
  if (json[idx] == '-') { neg = true; idx++; }
  int val = 0;
  while (idx < (int)json.length() && json[idx] >= '0' && json[idx] <= '9') {
    val = val * 10 + (json[idx] - '0');
    idx++;
  }
  out = neg ? -val : val;
  return true;
}

static bool jsonGetBool(const String& json, const char* key, bool& out) {
  String pattern = String("\"") + key + "\"";
  int idx = json.indexOf(pattern);
  if (idx < 0) return false;
  idx = json.indexOf(':', idx + pattern.length());
  if (idx < 0) return false;
  idx++;
  while (idx < (int)json.length() && (json[idx] == ' ' || json[idx] == '\n' || json[idx] == '\r' || json[idx] == '\t')) idx++;
  if (idx >= (int)json.length()) return false;
  if (json.startsWith("true", idx)) { out = true; return true; }
  if (json.startsWith("false", idx)) { out = false; return true; }
  return false;
}

static bool jsonGetFloat(const String& json, const char* key, float& out) {
  String pattern = String("\"") + key + "\"";
  int idx = json.indexOf(pattern);
  if (idx < 0) return false;
  idx = json.indexOf(':', idx + pattern.length());
  if (idx < 0) return false;
  idx++;
  while (idx < (int)json.length() && (json[idx] == ' ' || json[idx] == '\n' || json[idx] == '\r' || json[idx] == '\t')) idx++;
  if (idx >= (int)json.length()) return false;
  float val = 0;
  bool neg = false;
  if (json[idx] == '-') { neg = true; idx++; }
  while (idx < (int)json.length() && json[idx] >= '0' && json[idx] <= '9') {
    val = val * 10 + (json[idx] - '0');
    idx++;
  }
  if (idx < (int)json.length() && json[idx] == '.') {
    idx++;
    float frac = 0;
    float div = 1;
    while (idx < (int)json.length() && json[idx] >= '0' && json[idx] <= '9') {
      frac = frac * 10 + (json[idx] - '0');
      div *= 10;
      idx++;
    }
    val += frac / div;
  }
  out = neg ? -val : val;
  return true;
}

static void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String topicStr = String(topic);
  String payloadStr;
  payloadStr.reserve(length + 1);
  for (unsigned int i = 0; i < length; i++) payloadStr += (char)payload[i];

  if (topicStr.endsWith("/status")) {
    jsonGetString(payloadStr, "playerName", g_playerName);
    jsonGetString(payloadStr, "controllerReadyState", g_controllerReadyState);
    jsonGetString(payloadStr, "state", g_gameState);
    jsonGetString(payloadStr, "preQuestionPingId", g_preQuestionPingId);
    jsonGetInt(payloadStr, "currentQuestionIndex", g_currentQuestionIndex);
    jsonGetFloat(payloadStr, "playerTotalScore", g_playerTotalScore);
    g_hasNewStatus = true;
    return;
  }

  if (topicStr.endsWith("/answer/result")) {
    String errMsg;
    if (jsonGetString(payloadStr, "error", errMsg) && errMsg.length() > 0) {
      g_lastAnswerIsCorrect = false;
      g_lastAnswerPoints = 0.0f;
    } else {
      jsonGetBool(payloadStr, "isCorrect", g_lastAnswerIsCorrect);
      jsonGetFloat(payloadStr, "points", g_lastAnswerPoints);
      float totalPoints = 0.0f;
      if (jsonGetFloat(payloadStr, "totalPoints", totalPoints))
        g_playerTotalScore = totalPoints;
    }
    g_hasNewAnswerResult = true;
    return;
  }

  if (topicStr.endsWith("/ping")) {
    String requestId;
    if (jsonGetString(payloadStr, "requestId", requestId) && requestId.length() > 0) {
      g_lastPingRequestId = requestId;
      g_hasNewPing = true;
    }
  }
}

String macAddressString() {
  uint8_t mac[6];
  WiFi.macAddress(mac);

  char buf[18];
  sprintf(buf, "%02X:%02X:%02X:%02X:%02X:%02X",
          mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

  return String(buf);
}

static String controllerIdFromMac() {
  String mac = macAddressString();
  mac.replace(":", "");
  mac.toUpperCase();
  return String("HW-") + mac;
}

const String& controllerId() {
  return g_controllerId;
}

static void publishRegister() {
  if (!g_client.connected() || g_controllerId.length() == 0) return;
  String topic = String(MQTT_TOPIC_PREFIX) + "controller/" + g_controllerId + "/register";
  String payload = String("{\"controllerId\":\"") + g_controllerId +
                   "\",\"type\":\"HARDWARE\",\"ts\":" + String(millis()) + "}";
  g_client.publish(topic.c_str(), payload.c_str());
  g_lastRegisterMs = millis();
}

bool connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return true;

  Serial.print("Connecting to WiFi: ");
  Serial.println(WIFI_SSID);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int retries = 40; // ~20s
  while (WiFi.status() != WL_CONNECTED && retries-- > 0) {
    delay(500);
    Serial.print('.');
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("WiFi connected, IP: ");
    Serial.println(WiFi.localIP());
    return true;
  }

  Serial.println("WiFi connection failed!");
  return false;
}

bool connectMqtt() {
  if (!connectWiFi()) return false;

  g_client.setServer(MQTT_HOST, MQTT_PORT);
  if (g_client.connected()) return true;

  Serial.print("Connecting to MQTT broker ");
  Serial.print(MQTT_HOST);
  Serial.print(':');
  Serial.println(MQTT_PORT);

  const String clientId = String("uno-") + macAddressString();

  for (int i = 0; i < 3; i++) {
    if (g_client.connect(clientId.c_str(), MQTT_USER, MQTT_PASS)) {
      Serial.println("MQTT connected!");
      g_controllerId = controllerIdFromMac();
      g_client.setCallback(mqttCallback);
      String base = String(MQTT_TOPIC_PREFIX) + "controller/" + g_controllerId + "/";
      g_client.subscribe((base + "status").c_str());
      g_client.subscribe((base + "answer/result").c_str());
      g_client.subscribe((base + "ping").c_str());
      publishRegister();
      return true;
    }

    Serial.print("MQTT connection failed, rc=");
    Serial.print(g_client.state());
    Serial.println(" -> retrying...");
    delay(2000);
  }

  Serial.println("MQTT connection could not be established.");
  return false;
}

void service() {
  if (!g_client.connected()) {
    ensureConnected();
    return;
  }

  g_client.loop();

  // Pong (pre-question ping)
  if (g_hasNewPing && g_lastPingRequestId.length() > 0) {
    g_hasNewPing = false;
    String pongTopic = String(MQTT_TOPIC_PREFIX) + "controller/" + g_controllerId + "/pong";
    String payload = String("{\"requestId\":\"") + g_lastPingRequestId + "\",\"ts\":" + String(millis()) + "}";
    g_client.publish(pongTopic.c_str(), payload.c_str());
  }

  const uint32_t now = millis();
  if (now - g_lastHeartbeatMs >= 10000) {
    g_lastHeartbeatMs = now;
    String topic = String(MQTT_TOPIC_PREFIX) + "controller/" + g_controllerId + "/heartbeat";
    String payload = String("{\"ts\":") + String(now) + "}";
    g_client.publish(topic.c_str(), payload.c_str());
  }
  if (now - g_lastRegisterMs >= 60000) {
    g_lastRegisterMs = now;
    publishRegister();
  }
}

bool ensureConnected() {
  return connectMqtt();
}

const String& playerName() {
  return g_playerName;
}

const String& controllerReadyState() {
  return g_controllerReadyState;
}

float playerTotalScore() {
  return g_playerTotalScore;
}

bool consumeNewStatus() {
  if (!g_hasNewStatus) return false;
  g_hasNewStatus = false;
  return true;
}

const String& gameState() {
  return g_gameState;
}

void publishControllerState(const char* state) {
  if (!g_client.connected() || !state || g_controllerId.length() == 0) {
#if SERIAL_DEBUG_MQTT
    Serial.println("MQTT publishControllerState SKIP: " + String(g_client.connected() ? "ok" : "disconnected") + " id=" + String(g_controllerId.length()));
#endif
    return;
  }
  String topic = String(MQTT_TOPIC_PREFIX) + "controller/" + g_controllerId + "/controller-state";
  String payload = String("{\"state\":\"") + state + "\",\"ts\":" + String(millis()) + "}";
  bool ok = g_client.publish(topic.c_str(), payload.c_str());
#if SERIAL_DEBUG_MQTT
  Serial.println("MQTT PUB controller-state: " + topic + " -> " + (ok ? "OK" : "FAIL"));
#endif
}

void publishAnswer(char answerLetter) {
  if (!g_client.connected() || g_controllerId.length() == 0) {
#if SERIAL_DEBUG_MQTT
    Serial.println("MQTT publishAnswer SKIP: disconnected or no id");
#endif
    return;
  }
  char c = (char)toupper(answerLetter);
  if (c != 'A' && c != 'B' && c != 'C' && c != 'D') return;
  String topic = String(MQTT_TOPIC_PREFIX) + "controller/" + g_controllerId + "/answer";
  String payload = String("{\"answer\":\"") + c + "\",\"ts\":" + String(millis()) + "}";
  bool ok = g_client.publish(topic.c_str(), payload.c_str());
#if SERIAL_DEBUG_MQTT
  Serial.println("MQTT PUB answer: " + topic + " " + String(c) + " -> " + (ok ? "OK" : "FAIL"));
#endif
}

void publishUnbind() {
  if (!g_client.connected() || g_controllerId.length() == 0) return;
  String topic = String(MQTT_TOPIC_PREFIX) + "controller/" + g_controllerId + "/unbind";
  String payload = String("{\"ts\":") + String(millis()) + "}";
  bool ok = g_client.publish(topic.c_str(), payload.c_str());
#if SERIAL_DEBUG_MQTT
  Serial.println("MQTT PUB unbind (Abmelden): " + topic + " -> " + (ok ? "OK" : "FAIL"));
#endif
}

void clearBoundPlayer() {
  g_playerName = "";
  g_controllerReadyState = "NOT_READY";
  g_playerTotalScore = 0.0f;
}

void publishRfid(const char* rfidUid) {
  if (!g_client.connected() || !rfidUid || g_controllerId.length() == 0) return;
  String topic = String(MQTT_TOPIC_PREFIX) + "controller/" + g_controllerId + "/rfid";
  String escaped;
  for (const char* p = rfidUid; *p; p++) {
    if (*p == '"' || *p == '\\') escaped += '\\';
    escaped += *p;
  }
  String payload = String("{\"rfidUid\":\"") + escaped + "\",\"ts\":" + String(millis()) + "}";
  bool ok = g_client.publish(topic.c_str(), payload.c_str());
#if SERIAL_DEBUG_MQTT
  Serial.println("RFID -> MQTT: UID=" + String(rfidUid) + " " + (ok ? "OK" : "FAIL"));
#else
  (void)ok;
#endif
}

int currentQuestionIndex() {
  return g_currentQuestionIndex;
}

bool consumeLastAnswerResult(bool& isCorrect, float& points) {
  if (!g_hasNewAnswerResult) return false;
  isCorrect = g_lastAnswerIsCorrect;
  points = g_lastAnswerPoints;
  g_hasNewAnswerResult = false;
  return true;
}

} // namespace net::wifi_mqtt
