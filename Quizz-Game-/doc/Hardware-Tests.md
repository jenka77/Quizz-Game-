# Hardware-Tests – Durchgeführte Versuche für das Quiz-Spiel

Dieses Dokument fasst alle relevanten Tests und Anpassungen zusammen, die vorgenommen wurden, damit der Hardware-Controller (Arduino) während des Quiz-Spiels funktioniert. Trotz dieser Maßnahmen traten weiterhin Probleme auf.

---

## 1. Button-Logik (Verdrahtung)

**Ziel:** Sicherstellen, dass gedrückte Tasten korrekt erkannt werden.

- Anpassung in `buttons.cpp`: Bei `BUTTON_ACTIVE_LOW = true` bedeutet gedrückt = LOW (GND), nicht HIGH.
- Option in `config.h`: `BUTTON_ACTIVE_LOW = false` testen, falls die Verdrahtung umgekehrt ist (Button zu VCC statt GND).

---

## 2. MQTT-Controller-ID (Großschreibung)

**Ziel:** Topics zwischen Arduino und Backend zur Deckung bringen.

- Controller-IDs werden überall in Uppercase normalisiert (z. B. `HW-48CA435B641C`).
- Backend und Arduino müssen dieselbe Schreibweise verwenden, sonst werden Status/Antwort nicht zugeordnet.

---

## 3. MQTT-Prefix (group-17/)

**Ziel:** Sicherstellen, dass Arduino und Backend dieselben Topics abonnieren bzw. publizieren.

- `.env` (Backend): `MQTT_MESSAGE_PREFIX=group-17/`
- `arduino/include/secrets.h`: `MQTT_TOPIC_PREFIX = "group-17/"`
- Beide müssen exakt identisch sein (inkl. Schrägstrich).

---

## 4. Flankenerkennung für Antwort-Buttons

**Ziel:** Mehrfache Auslösung bei gehaltenem Button vermeiden.

- Für A/B/C/D: Nutzung von `blueEdge`, `greenEdge`, `yellowEdge`, `redEdge` (Flanken-Erkennung).
- Nur beim Wechsel von „nicht gedrückt“ zu „gedrückt“ wird die Antwort gesendet.

---

## 5. Priorität von handleButtons()

**Ziel:** Antworten möglichst verzögerungsfrei senden.

- `handleButtons()` wird direkt nach `net::wifi_mqtt::service()` aufgerufen, vor OLED-Updates.
- OLED-Updates sollen die Button-Verarbeitung nicht blockieren.

---

## 6. OLED-Throttling

**Ziel:** Überlast des Displays und Blockade des Hauptprogramms vermeiden.

- OLED wird maximal alle ca. 1,2 Sekunden aktualisiert.
- Keine OLED-Updates bei Backend-Fehlern, um zusätzliche Last zu vermeiden.

---

## 7. Status-Push-Intervall (Backend)

**Ziel:** Last auf Datenbank und MQTT reduzieren.

- Intervall von 400 ms auf 1000 ms erhöht.
- Weniger SQL-Abfragen und weniger Netzverkehr.

---

## 8. Username-Cache (Backend)

**Ziel:** DB-Abfragen für `findAssignedUsernameByControllerId` reduzieren.

- Cache mit TTL von 2 Sekunden für den Benutzernamen pro Controller.
- Invalidierung bei Bind/Unbind.

---

## 9. Controller nicht löschen bei Unbind

**Ziel:** Controller soll nach Abmelden wieder in der Liste erscheinen.

- `deleteControllerByControllerId` wurde aus dem Unbind-Flow entfernt.
- Controller bleibt in der DB (Status FREI) und kann per Heartbeat wieder sichtbar werden.

---

## 10. RFID/Heartbeat-Cooldown nach Unbind

**Ziel:** Controller schneller wieder nutzbar machen.

- `RFID_IGNORE_AFTER_UNBIND_MS` von 30 000 ms auf 5 000 ms reduziert.

---

## 11. Unbind-Schutz (keine Schleife)

**Ziel:** Verhindern, dass Unbind mehrfach ausgelöst wird (z. B. bei klemmender Taste).

- Flag `g_redMustReleaseBeforeUnbind`: Nach Unbind muss die rote Taste erst losgelassen werden.
- Unbind-Cooldown von 5 Sekunden zwischen zwei Unbind-Vorgängen.

---

## 12. Ready/Not-Ready in mehr Phasen

**Ziel:** Bereit/Nicht bereit auch außerhalb von LOBBY/COUNTDOWN ermöglichen.

- `canToggleReady = !state.startsWith("QUESTION")`: Erlaubt in LOBBY, COUNTDOWN, END usw.
- In der QUESTION-Phase nur Antwort-Buttons (A/B/C/D).

---

## 13. Gelbe Taste: RFID vs. Buchstabe C

**Ziel:** Während des Quiz soll Gelb nur „C“ senden, kein RFID.

- RFID-Publikation nur, wenn `!gameState().startsWith("QUESTION")`.
- In QUESTION-Phase: Gelb = Buchstabe C.

---

## 14. Nur erste Antwort pro Frage

**Ziel:** Nur die erste gesendete Antwort pro Frage zählen.

- Arduino: `answerSent`-Flag, Reset bei neuer Frage (`lastSeenQuestionIndex`).
- Backend: `currentQuestionAnswers.containsKey(userId)` lehnt Doppel-Antworten ab.

---

## 15. MQTT-Verbindung vor publishAnswer prüfen

**Ziel:** Keine sinnlosen Publikationen bei getrennter Verbindung.

- Vor `publishAnswer` und `publishControllerState`: `isMqttConnected()` prüfen.
- Serial-Ausgabe: `>>> SKIP (MQTT nicht verbunden)`.

---

## 16. Serial-Debug (Terminal)

**Ziel:** Nachvollziehen, ob Tasten erkannt werden und welcher Zustand herrscht.

- Alle 2 s: `DBG Btn:0100 state=QUESTION MQTT=1` (Btn: Blau, Grün, Gelb, Rot).
- Bei Aktion: z. B. `>>> Blau -> A`, `>>> Grün -> Bereit`, `>>> Ergebnis erhalten: Richtig +2.5 Pkt -> Gesamt 12.5`.

---

## 17. Feedback-Anzeige (OLED)

**Ziel:** Punkte und Ergebnis direkt auf dem Display anzeigen.

- Nach Antwort: Anzeige von „Richtig! +X.X Pkt“ bzw. „Falsch (0 Pkt)“ und Gesamtpunktzahl.
- Anzeige ca. 2,5 s, danach Rückkehr zur Standardansicht (Name, Status, Punkte).

---

## 18. Getesteter Code: handleButtons() (nicht funktionsfähig)

Der folgende Code wurde getestet, hat jedoch leider nicht wie erwartet funktioniert. Er dient als Referenz für zukünftige Versuche.

```cpp
static void handleButtons() {
    const uint32_t nowMs = millis();

    // Tasten lesen
    const bool pressedBlue   = hw::buttons::isPressed(PIN_BTN_BLUE);
    const bool pressedGreen  = hw::buttons::isPressed(PIN_BTN_GREEN);
    const bool pressedYellow = hw::buttons::isPressed(PIN_BTN_YELLOW);
    const bool pressedRed    = hw::buttons::isPressed(PIN_BTN_RED);

    // Flankenerkennung für alle Tasten
    static bool lastBlue = false, lastGreen = false, lastYellow = false, lastRed = false;
    const bool blueEdge   = pressedBlue   && !lastBlue;
    const bool greenEdge  = pressedGreen  && !lastGreen;
    const bool yellowEdge = pressedYellow && !lastYellow;
    const bool redEdge    = pressedRed    && !lastRed;

    lastBlue = pressedBlue;
    lastGreen = pressedGreen;
    lastYellow = pressedYellow;
    lastRed = pressedRed;

    const String state = net::wifi_mqtt::gameState();
    const bool inFeedback = nowMs < g_feedbackUntilMs;

    // -----------------------------
    // ROTE TASTE: Abmelden / Nicht bereit
    // -----------------------------
    if (!inFeedback) {  // Die rote Taste wird nur für Abmelden blockiert
        if (pressedRed && (nowMs - g_lastUnbindMs) >= UNBIND_COOLDOWN_MS) {
            if (g_redPressedSinceMs == 0) g_redPressedSinceMs = nowMs;

            if ((nowMs - g_redPressedSinceMs) >= UNBIND_HOLD_MS) {
                // Abmelden
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
        } else if (!pressedRed && g_redPressedSinceMs != 0) {
            // Rote Taste losgelassen → Nicht bereit bei kurzem Drücken
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

    // -----------------------------
    // GRÜNE TASTE: Bereit
    // -----------------------------
    if ((state == "LOBBY" || state == "COUNTDOWN") && greenEdge && !inFeedback) {
        if ((nowMs - g_lastReadyNotReadyMs) >= 250) {
            g_lastReadyNotReadyMs = nowMs;
            hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 20);
            net::wifi_mqtt::publishControllerState("READY");
            return;
        }
    }

    // -----------------------------
    // QUESTION-Phase: Antwort-Tasten
    // -----------------------------
    if (state.startsWith("QUESTION")) {
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
        if (nowMs - lastAnswerMs < 300) return;  // Entprellung

        char letter = 0;
        if (blueEdge)   letter = 'A';
        else if (greenEdge) letter = 'B';
        else if (yellowEdge) letter = 'C';
        else if (redEdge) letter = 'D';

        if (letter) {
            lastAnswerMs = nowMs;

            // LED-Farbe
            uint32_t color = (letter == 'A') ? hw::neopixel::strip().Color(0, 0, 120) :
                             (letter == 'B') ? hw::neopixel::strip().Color(0, 120, 0) :
                             (letter == 'C') ? hw::neopixel::strip().Color(120, 120, 0) :
                             hw::neopixel::strip().Color(120, 0, 0);

            hw::neopixel::flash(color, 20);

            if (net::wifi_mqtt::isMqttConnected()) {
                net::wifi_mqtt::publishAnswer(letter);
                answerSent = true;
            }
            return;
        }
    }

    // -----------------------------
    // FEEDBACK / EINFACHES AUFLEUCHTEN
    // -----------------------------
    if (blueEdge)   hw::neopixel::flash(hw::neopixel::strip().Color(0, 0, 120), 20);
    if (greenEdge)  hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 20);
    if (yellowEdge) hw::neopixel::flash(hw::neopixel::strip().Color(120, 120, 0), 20);
    if (redEdge && !inFeedback) hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 20);

    if (!blueEdge && !greenEdge && !yellowEdge && !redEdge) hw::neopixel::off();
}
```

---

## Mögliche verbleibende Ursachen

- **Broker/VPN:** Backend im Docker-Container erreicht `iti-mqtt.mni.thm.de` möglicherweise nicht.
- **Latenz:** Verzögerungen zwischen MQTT, Backend und Arduino können die Interaktion beeinträchtigen.
- **Verdrahtung:** Falls `BUTTON_ACTIVE_LOW` nicht zur Schaltung passt, reagieren die Tasten nicht.

---

## Checkliste zur Fehlersuche

1. [ ] Arduino zeigt „MQTT verbunden!“ im Serial-Monitor.
2. [ ] `DBG Btn:` ändert sich beim Drücken der Tasten.
3. [ ] Backend-Log: `MQTT verbunden OK` und `MQTT-Abonnements OK`.
4. [ ] Backend-Log: `Controller-Register empfangen`, `Antwort empfangen`.
5. [ ] Keine Meldung `Controller-State ignoriert` oder `Antwort abgelehnt`.
6. [ ] Hardware-Controller in der Lobby verbinden und vor Spielstart auf Bereit drücken.
