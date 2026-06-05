#include <Arduino.h>

#include "config.h"

#include "hardware/buttons.h"
#include "hardware/neopixel.h"
#include "hardware/oled.h"
#include "hardware/rfid.h"
#include "net/wifi_mqtt.h"

// Main program: Initialises hardware and reacts to button presses.

static void showOledMessage(const String& line1, const String& line2) {
  auto& d = hw::oled::display();
  d.clearDisplay();
  d.setCursor(0, 0);
  d.println(line1);
  d.setCursor(0, 20);
  d.println(line2);
  d.display();
}

// Nur 2 Status: Ready oder Not Ready
static String statusLabel(const String& state) {
  if (state.equalsIgnoreCase("READY")) return "Ready";
  return "Not Ready";  // NOT_READY, CONNECTED, etc.
}

/** Zeigt auf dem Display den Namen des verbundenen Spielers und dessen Status (Ready/Not Ready). */
static void showConnectedPlayerOnDisplay() {
  auto& d = hw::oled::display();
  d.clearDisplay();
  const int lineH = 18;

  const String gameState = net::wifi_mqtt::gameState();
  if (gameState.equalsIgnoreCase("END")) {
    d.clearDisplay();
    d.setCursor(0, 24);
    d.print("END");
    d.display();
    return;
  }

  String name = net::wifi_mqtt::playerName();
  name.trim();
  if (name.length() == 0) name = "-";
  if (name.length() > 13) name = name.substring(0, 13);
  d.setCursor(0, 10);
  d.println(name);

  String status = statusLabel(net::wifi_mqtt::controllerReadyState());
  d.setCursor(0, 10 + lineH);
  d.println(status);

  const float pts = net::wifi_mqtt::playerTotalScore();
  d.setCursor(0, 10 + lineH * 2);
  d.println(pts, 1);
  d.display();
}

static void showOledHeader() {
  showConnectedPlayerOnDisplay();
}

static uint32_t g_feedbackUntilMs = 0;
static uint32_t g_redPressedSinceMs = 0;  // 0 = nicht gedrückt
static uint32_t g_lastUnbindMs = 0;
static uint32_t g_lastReadyNotReadyMs = 0;
static constexpr uint32_t UNBIND_HOLD_MS = 2000;
static constexpr uint32_t UNBIND_COOLDOWN_MS = 3000;

static void handleButtons() {
  const uint32_t nowMs = millis();
  if (nowMs < g_feedbackUntilMs) return;

  const bool pressedBlue = hw::buttons::isPressed(PIN_BTN_BLUE);
  const bool pressedGreen = hw::buttons::isPressed(PIN_BTN_GREEN);
  const bool pressedYellow = hw::buttons::isPressed(PIN_BTN_YELLOW);
  const bool pressedRed = hw::buttons::isPressed(PIN_BTN_RED);
  const String state = net::wifi_mqtt::gameState();

  // Roter Button: 2 s gedrückt = Abmelden (unbind), kurzer Druck = Not Ready (in LOBBY/COUNTDOWN)
  if (pressedRed && (nowMs - g_lastUnbindMs) >= UNBIND_COOLDOWN_MS) {
    if (g_redPressedSinceMs == 0) g_redPressedSinceMs = nowMs;
    if ((nowMs - g_redPressedSinceMs) >= UNBIND_HOLD_MS) {
      g_redPressedSinceMs = 0;
      g_lastUnbindMs = nowMs;
      g_feedbackUntilMs = nowMs + 1500;
      hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 150);
      showOledMessage("Abmelden", "...");
      net::wifi_mqtt::clearBoundPlayer();
      net::wifi_mqtt::publishUnbind();
      net::wifi_mqtt::publishControllerState("OFFLINE");
      return;
    }
  } else if (!pressedRed) {
    if (g_redPressedSinceMs != 0) {
      uint32_t holdDuration = nowMs - g_redPressedSinceMs;
      g_redPressedSinceMs = 0;
      if (holdDuration < UNBIND_HOLD_MS && (state == "LOBBY" || state == "COUNTDOWN")) {
        if ((nowMs - g_lastReadyNotReadyMs) >= 250) {
          g_lastReadyNotReadyMs = nowMs;
          hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 20);
          net::wifi_mqtt::publishControllerState("NOT_READY");
          return;
        }
      }
    }
  }

  // LOBBY / COUNTDOWN: Grün = Ready (kurzer Druck)
  if (state == "LOBBY" || state == "COUNTDOWN") {
    static bool lastGreen = false;
    const bool debounceOk = (nowMs - g_lastReadyNotReadyMs) >= 250;
    const bool greenEdge = pressedGreen && !lastGreen;
    lastGreen = pressedGreen;

    if (greenEdge && debounceOk) {
      g_lastReadyNotReadyMs = nowMs;
      hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 20);
      net::wifi_mqtt::publishControllerState("READY");
      return;
    }
    if (pressedGreen || pressedRed) return;
  }

  // QUESTION : Bleu=A, Vert=B, Jaune=C, Rouge=D
  if (state.indexOf("QUESTION") == 0) {
    static int lastSeenQuestionIndex = -9999;
    static bool answerSent = false;
    static uint32_t lastAnswerMs = 0;
    const int qIndex = net::wifi_mqtt::currentQuestionIndex();
    if (qIndex != lastSeenQuestionIndex) {
      lastSeenQuestionIndex = qIndex;
      answerSent = false;
    }
    if (answerSent) {
      hw::neopixel::off();
      return;
    }
    if (nowMs - lastAnswerMs < 300) return;

    char letter = 0;
    if (pressedBlue) letter = 'A';
    else if (pressedGreen) letter = 'B';
    else if (pressedYellow) letter = 'C';
    else if (pressedRed) letter = 'D';

    if (letter) {
      lastAnswerMs = nowMs;
      answerSent = true;
      uint32_t color = (letter == 'A') ? hw::neopixel::strip().Color(0, 0, 120) :
                      (letter == 'B') ? hw::neopixel::strip().Color(0, 120, 0) :
                      (letter == 'C') ? hw::neopixel::strip().Color(120, 120, 0) :
                      hw::neopixel::strip().Color(120, 0, 0);
      hw::neopixel::flash(color, 20);
      net::wifi_mqtt::publishAnswer(letter);
      return;
    }
  }

  if (pressedYellow) {
    hw::neopixel::flash(hw::neopixel::strip().Color(120, 120, 0), 20);
    return;
  }
  if (pressedRed) {
    hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 20);
    return;
  }
  if (pressedBlue) {
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 0, 120), 20);
    return;
  }
  if (pressedGreen) {
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 20);
    return;
  }

  hw::neopixel::off();
}

void setup() {
  Serial.begin(9600);
  while (!Serial) { delay(10); }

  hw::buttons::begin();
  hw::neopixel::begin();

  hw::oled::begin();
  hw::oled::showThmLogo();

  hw::rfid::begin();

  // Initialize the WiFi and MQTT connections.
  if (net::wifi_mqtt::ensureConnected()) {
    Serial.println("MQTT test connection OK (startup).");
    Serial.print("Controller-ID (im Lobby verbinden): ");
    Serial.println(net::wifi_mqtt::controllerId());
    // OLED: immer die 3 Infos anzeigen (Name, Status, Punkte)
    showOledHeader();
  } else {
    Serial.println("MQTT test connection FAILED (startup).");
  }
}

void loop() {
  // MQTT: maintain connection, heartbeat, re-register
  net::wifi_mqtt::service();

  // Feedback-Antwort (Richtig/Falsch)
  {
    bool isCorrect = false;
    float points = 0.0f;
    if (net::wifi_mqtt::consumeLastAnswerResult(isCorrect, points)) {
      if (isCorrect) {
        hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 250);
      } else {
        hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 250);
      }
      g_feedbackUntilMs = millis() + 2500;
      showOledMessage(isCorrect ? "Richtig!" : "Falsch!",
                     String("+") + String(points, 1) + " Pkt");
    }
  }

  // OLED: aktualisieren sobald neuer Status eintrifft (END sofort angezeigt, auch während Feedback)
  if (net::wifi_mqtt::consumeNewStatus()) {
    if (net::wifi_mqtt::gameState().equalsIgnoreCase("END") || millis() >= g_feedbackUntilMs) {
      showOledHeader();
    }
  }

  // Wenn Feedback endet (Abmelden, Richtig/Falsch), Standardzustand erneut anzeigen
  // um schwarzen Bildschirm zu vermeiden; zeigt "-" / "Not Ready" / 0 wenn kein Spieler zugewiesen
  {
    static bool wasInFeedback = false;
    const bool inFeedback = millis() < g_feedbackUntilMs;
    if (wasInFeedback && !inFeedback) {
      showConnectedPlayerOnDisplay();
    }
    wasInFeedback = inFeedback;
  }

  // Keep polling for new RFID cards.
  hw::rfid::service();

  // Publish RFID via MQTT nur per Yellow-Button:
  // 1 Klick = 1 MQTT-Nachricht (falls UID vorhanden)
  {
    static bool s_lastYellowPressed = false;
    const bool yellowPressed = hw::buttons::isPressed(PIN_BTN_YELLOW);
    const bool yellowEdge = yellowPressed && !s_lastYellowPressed;
    s_lastYellowPressed = yellowPressed;
    if (yellowEdge) {
      const String& uid = hw::rfid::lastUid();
      if (uid.length() > 0) {
        net::wifi_mqtt::publishRfid(uid.c_str());
      }
    }
  }

  // Keep reacting to button presses.
  handleButtons();
}
