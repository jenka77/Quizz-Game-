#pragma once

#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFiS3.h>

namespace net::wifi_mqtt {

// Connects to WiFi and MQTT.
bool ensureConnected();

// Must be called regularly from loop().
void service();

// Connects to WiFi only.
bool connectWiFi();

// Connects to MQTT only.
bool connectMqtt();

// Returns the MAC-Address as string (e.g. "AA:BB:...").
String macAddressString();

// Returns hardware controller id (e.g. "HW-AABBCCDDEEFF").
const String& controllerId();

// Status empfangen via topic status (Name, Ready-Status, Punkte)
const String& playerName();
const String& controllerReadyState();
float playerTotalScore();

// true einmal pro neuem Status (zum OLED-Aktualisieren)
bool consumeNewStatus();

// Spielstatus (LOBBY, COUNTDOWN, QUESTION, etc.)
const String& gameState();

// READY oder NOT_READY via MQTT senden (topic controller-state)
void publishControllerState(const char* state);

// Antwort A/B/C/D senden (topic answer)
void publishAnswer(char answerLetter);

// Erkannte RFID-UID senden (topic rfid) für Auto-Login via Backend
void publishRfid(const char* rfidUid);

// Abmelde-Anfrage senden (topic unbind) – roter Button 2 s gedrückt
void publishUnbind();

// Setzt lokalen Spielerstatus (Name, Status, Punkte) nach Abmelden zurück.
void clearBoundPlayer();

// Index der aktuellen Frage (für answerSent-Reset)
int currentQuestionIndex();

// Ergebnis der letzten Antwort (isCorrect, Punkte)
bool consumeLastAnswerResult(bool& isCorrect, float& points);

} // namespace net::wifi_mqtt
