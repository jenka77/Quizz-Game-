#pragma once

// =======================
// WiFi + MQTT credentials
// =======================
// ATTENTION: Credentials should NOT be published in Git-Repositories.
// For ths module the are necessary, but should not be published elsewhere.
// ACHTUNG: Zugangsdaten gehören normalerweise NICHT ins Repository.
// Für den Kurs sind sie hier hinterlegt, sollten aber nicht an dritte weitergegeben werden.

static constexpr const char* WIFI_SSID     = "MQTT-Broker";
static constexpr const char* WIFI_PASSWORD = "Cannej-poscys-cyfqy9";

static constexpr const char* MQTT_HOST     = "iti-mqtt.mni.thm.de";
static constexpr uint16_t    MQTT_PORT     = 1883;
static constexpr const char* MQTT_USER     = "group-17";
static constexpr const char* MQTT_PASS     = "2.z/Gv6F-v[s";

// Topics (Präfix muss dem Backend MQTT_MESSAGE_PREFIX entsprechen, z.B. group-17/)
static constexpr const char* MQTT_TOPIC_PREFIX = "group-17/";
