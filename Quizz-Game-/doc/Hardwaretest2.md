# Hardware-Tests 2 – Zusammenfassung der durchgeführten Tests

Dieses Dokument fasst alle durchgeführten Tests und Korrekturen am Hardware-Controller (Arduino/ESP32) und am Backend für das Multiplayer-Quiz zusammen.

---

## 1. Tasten – Logik `isPressed`

**Problem:** Bei `INPUT_PULLUP` liefert ein gedrückter Taster (mit GND verbunden) LOW. `isPressed` gab fälschlicherweise HIGH für „gedrückt“ zurück.

**Korrektur:** `arduino/src/hardware/buttons.cpp`
```cpp
return BUTTON_ACTIVE_LOW ? (digitalRead(pin) == LOW) : (digitalRead(pin) == HIGH);
```

**Config:** `config.h`: `BUTTON_ACTIVE_LOW = true`

---

## 2. Unbind (Abmelden) – Schleife/Wiederholung

**Problem:** Unbind löste sich in einer Schleife alle 5 Sekunden aus.

**Korrekturen:**
- Flag `g_redMustReleaseBeforeUnbind`: Rote Taste muss vor neuem Unbind losgelassen werden
- Debounce 500 ms für das Loslassen
- `UNBIND_COOLDOWN_MS`: 15 s
- `UNBIND_HOLD_MS`: 5 s (rote Taste 5 s halten für Abmelden)

---

## 3. Rote LED dauerhaft an

**Problem:** In der LOBBY-Phase vorzeitiger Ausstieg, ohne die rote LED auszuschalten.

**Korrektur:** Aufruf von `hw::neopixel::off()` vor jedem `return`, wenn Rot/Grün in der LOBBY gedrückt wird.

---

## 4. 30-Sekunden-Wartezeit nach Unbind

**Problem:** Der Controller erschien sofort wieder in der Liste nach Unbind.

**Korrekturen:**
- `clearControllerAssignmentByControllerId`: `last_seen_at = CURRENT_TIMESTAMP - INTERVAL 2 MINUTE` (statt `CURRENT_TIMESTAMP`)
- Entfernung von `deleteControllerByControllerId` aus dem Unbind-Flow (Controller bleibt in der DB mit Status FREE)

---

## 5. Tasten während des Quiz

**Problem:** Die Tasten A/B/C/D reagierten nicht zuverlässig in der QUESTION-Phase.

**Korrekturen:**
- Flankenerkennung für die QUESTION-Phase
- Debounce auf 80 ms reduziert
- Prüfung `isMqttConnected()` vor dem Senden von Antworten
- Hinzufügen von `isMqttConnected()` in `wifi_mqtt.h/cpp`

---

## 6. Anzeige Name / Status auf OLED

**Problem:** Spielername und Status wurden auf dem OLED nicht angezeigt.

**Korrektur:** Periodische Aktualisierung des OLED alle 1,2 s (bestehendes Throttling beibehalten).

---

## 7. MQTT-Debug im Serial Monitor

**Ziel:** Empfangene und gesendete MQTT-Nachrichten nachverfolgen.

**Korrektur:** Detaillierte Logs für alle MQTT-Nachrichten:
- `[MQTT RECV] topic=... len=... payload=...`
- `-> parsed: name=... ready=... game=...`
- `[MQTT SEND] heartbeat OK` / `pong` / etc.

**Config:** `config.h`: `SERIAL_DEBUG_MQTT 1`

---

## 8. Status-Nachrichten nicht empfangen (Hauptursache)

**Problem:** `[MQTT RECV] status` war nie sichtbar; Ping wurde empfangen, Status nicht. Das JSON-Payload des Status (~250–560 Byte) überschritt die Standard-Grenze von PubSubClient (256 Byte).

**Korrekturen:**
- `arduino/include/net/wifi_mqtt.h`: `#define MQTT_MAX_PACKET_SIZE 2048` vor `#include <PubSubClient.h>`
- `arduino/src/net/wifi_mqtt.cpp`: `g_client.setBufferSize(2048)` vor der MQTT-Verbindung

---

## 9. Build- / Upload-Fehler

### Fehler 1: `Blocked key: build_flags`
- **Ursache:** Build-Pipeline erlaubte `build_flags` in `platformio.ini` nicht
- **Lösung:** `MQTT_MAX_PACKET_SIZE` in `wifi_mqtt.h` statt in `platformio.ini` definiert

### Fehler 2: `platformio.ini nicht in dem hochgeladenen Ordner`
- **Ursache:** Datei `platformio.ini` fehlte im Arduino-Ordner
- **Lösung:** Erstellung von `platformio.ini` mit Umgebung `seeed_xiao_esp32c3_informatik_projekt`

```ini
[platformio]
default_envs = seeed_xiao_esp32c3_informatik_projekt

[env:seeed_xiao_esp32c3_informatik_projekt]
platform = espressif32
board = seeed_xiao_esp32c3
framework = arduino
monitor_speed = 115200
```

### Fehler 3: Ziel `seeed_xiao_esp32c3` vs. `uno_r4_wifi`
- Das THM-Build-System erwartet `seeed_xiao_esp32c3_informatik_projekt`
- **Hinweis:** Die Pins in `config.h` (D2, D4–D7, A4, A5, etc.) entsprechen dem UNO R4. Für XIAO ESP32C3 kann ein Pin-Mapping erforderlich sein.

---

## 10. Spielername (leer)

**Problem:** `-> parsed: name=(leer)`: Der Name wurde nicht angezeigt.

**Ursache:** Der Controller war nicht mit einem Spieler in der Datenbank verbunden (gebunden).

**Lösung:** In der Lobby den Controller HW-XXXX mit einem Spieler (z. B. alice, jknt) über „Binden“ / „Zuweisen“ verbinden. Nach der Bindung sendet das Backend `controller.status.push` mit `playerName` und das OLED zeigt den Namen an.

---

## 11. Name wird korrekt angezeigt

**Ergebnis:** Nach Bindung des Controllers an „jknt“ zeigen die Logs:
```
-> parsed: name=jknt ready=READY game=QUESTION
```

---

### Beispiel: UART-Logs bei Spielende (END)

Vollständiger MQTT-Flow in der END-Phase – Status, Heartbeat und Ping/Pong funktionieren korrekt:

```
[MQTT RECV] topic=group-17/controller/HW-64E83360E548/status len=368 payload={"state":"END","message":"Spiel beendet. Endergebnis wird angezeigt.","countdownSeconds":null,"curre...
-> parsed: name=jknt ready=READY game=END
[MQTT RECV] topic=group-17/controller/HW-64E83360E548/status len=368 payload={"state":"END","message":"Spiel beendet. Endergebnis wird angezeigt.","countdownSeconds":null,"curre...
-> parsed: name=jknt ready=READY game=END
...
[MQTT SEND] heartbeat OK
...
[MQTT RECV] topic=group-17/controller/HW-64E83360E548/ping len=71 payload={"requestId":"743a6cbc-bedf-44d5-9475-8f2e02625c34","ts":1774037452842}
-> parsed: ping
[MQTT SEND] pong OK
[MQTT RECV] topic=group-17/controller/HW-64E83360E548/status len=368 payload={"state":"END","message":"Spiel beendet. Endergebnis wird angezeigt.","countdownSeconds":null,"curre...
-> parsed: name=jknt ready=READY game=END
```

---

## 12. UTF-8-Encoding

**Problem:** Falsche UART-Anzeige: „für“ → „fr“, „läuft“ → „luft“.

**Mögliche Ursache:** Encoding-Einstellungen des Serial Monitors oder des Backends (UTF-8 vs. Latin-1).

**Status:** Nicht behoben; Auswirkung nur auf die Log-Ausgabe.

---

## 13. MQTT-Flow geprüft

| Schritt                         | Status  |
|---------------------------------|---------|
| Register → Backend               | OK      |
| Heartbeat (10 s)                 | OK      |
| Status empfangen (state, message)| OK      |
| playerName im Status             | OK wenn gebunden |
| controller-state (READY)         | OK      |
| Antwort (answer A/B/C/D)         | OK wenn MQTT verbunden |
| Unbind                           | OK (5 s halten) |

---

## 14. Geänderte Dateien (Hauptsächlich)

| Komponente    | Dateien |
|---------------|---------|
| Arduino       | `main.cpp`, `buttons.cpp`, `wifi_mqtt.cpp`, `wifi_mqtt.h`, `config.h` |
| Backend Java  | `PlayerRepository.java`, `PlayerService.java`, `MqttController.java` |
| Config        | `platformio.ini` (erstellt) |

---

## 15. Zusätzliche Tests (Hardware-Tests.md)

Siehe auch `doc/Hardware-Tests.md` für:
- MQTT-Controller-ID (uppercase)
- MQTT-Prefix (group-17/)
- Flankenerkennung
- Priorität von `handleButtons()`
- OLED-Throttling
- Status-Push-Intervall (Backend)
- Username-Cache
- RFID vs. Buchstabe C (gelbe Taste)
- Nur eine Antwort pro Frage

---

## Finale Checkliste

- [x] MQTT verbunden
- [x] Status-Nachrichten empfangen
- [x] playerName angezeigt (nach Binding)
- [x] Tasten A/B/C/D in QUESTION-Phase
- [x] Unbind (5 s halten)
- [x] platformio.ini in arduino.zip enthalten
