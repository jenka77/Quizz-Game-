# Checkliste: Hardware-Controller – Vollständige Implementierung

Dieses Dokument listet alle offenen Punkte für die vollständige Implementierung des Hardware-Controllers (ESP32/Arduino) auf – basierend auf DesignVorschlag.md und ProjektBeschreibung.md.

---

## 1. Hardware & Grundaufbau

### Basis-Komponenten
- [ ] **ESP32 oder Arduino** mit WiFi-Modul
- [ ] **4 Antwort-Buttons** (A, B, C, D)
- [ ] **4 LEDs** für Status/Feedback (Grün, Rot, Blau, Gelb)
- [ ] **OLED-Display** (I2C oder SPI)
- [ ] **RFID-Reader** (z.B. RC522)
- [ ] **WLAN-Anbindung** für MQTT-Kommunikation

### Verdrahtung & Schaltung
- [ ] Button-Pins konfigurieren (mit Pull-down/up)
- [ ] LED-Pins konfigurieren
- [ ] OLED-Bibliothek (z.B. Adafruit_SSD1306, SH1106) einbinden
- [ ] RFID-Bibliothek (z.B. MFRC522) einbinden

---

## 2. MQTT-Kommunikation

### Verbindung
- [ ] **MQTT-Client** (z.B. PubSubClient) einrichten
- [ ] **WiFi-Verbindung** zu konfigurierbarem Netzwerk
- [ ] **Broker-Konfiguration**:
  - Lokal: `localhost:1883` oder `mosquitto:1883` (Docker)
  - Hardware-Tests: `iti-mqtt.mni.thm.de:1883` (TLS, nur im VPN)
  - Umschalter über Env-Variable oder Config

### Topics (Präfix `group-17/` oder `group-XX/`)

| Aktion | Topic | Richtung |
|--------|-------|----------|
| Controller registrieren | `controller/{controllerId}/register` | ESP32 → Broker |
| Ping empfangen | `controller/{controllerId}/ping` | Broker → ESP32 |
| Pong senden | `controller/{controllerId}/pong` | ESP32 → Broker |
| Heartbeat senden | `controller/{controllerId}/heartbeat` | ESP32 → Broker |
| Status empfangen | `controller/{controllerId}/status` | Broker → ESP32 |
| Controller-State senden | `controller/{controllerId}/controller-state` | ESP32 → Broker |
| Antwort senden | `controller/{controllerId}/answer` | ESP32 → Broker |
| Ergebnis empfangen | `controller/{controllerId}/answer/result` | Broker → ESP32 |

### Reconnect-Logik
- [ ] **Reconnect bei Verbindungsabbruch** implementieren
- [ ] **Automatischer Reconnect** mit Backoff bei Fehlern
- [ ] **Debug-Ausgaben per Serial** für Entwicklung

---

## 3. Controller-ID & Registrierung

### Controller-ID
- [ ] **Format**: `HW-{RFID-UID}` oder `HW-{eindeutige-ID}` (z.B. Chip-ID)
- [ ] ID beim Start generieren/lesen
- [ ] **Register-Nachricht** beim ersten Connect senden:
  ```json
  { "controllerId": "HW-ABC123", "type": "HARDWARE", "ts": 1709900000000 }
  ```

---

## 4. Login-Optionen

### Option A: RFID-Login (Direct-Join)
- [ ] **RFID-Tag scannen** beim Start oder in Lobby
- [ ] **REST-API aufrufen**: `POST /api/auth/login-rfid` mit `{ "rfidUid": "04 9C 64..." }`
  - *Backend-Endpoint muss noch implementiert werden*
- [ ] Bei **bekanntem Tag**: Benutzer wird angemeldet, Controller gebunden
- [ ] Bei **unbekanntem Tag**: OLED zeigt „RFID nicht bekannt“ / „RFID Karte nicht bekannt“
- [ ] Keine weitere Aktion bei unbekanntem Tag

### Option B: Web-Frontend + manuelle Zuordnung
- [ ] Nutzer loggt sich im Web ein
- [ ] Nutzer trägt Hardware-Controller-ID (z.B. `HW-1CF8464A`) ein
- [ ] Bind über `POST /api/players/bind`
- [ ] Controller muss in DB existieren (`/api/controllers/register` vorher aufrufen)

---

## 5. OLED-Display (Anzeigen)

| Anzeige | Inhalt |
|---------|--------|
| Spielername | Nach erfolgreichem Login/Bind |
| Ready-Status | „Ready“ / „Not Ready“ |
| Spielstatus | LOBBY, COUNTDOWN, QUESTION, EVALUATION, END |
| Eigene Punkte | Nach jeder Frage aktualisiert |
| Fehlermeldungen | z.B. „RFID nicht bekannt“, „Verbindung verloren“ |
| Fragentext | Optional: Kurze Anzeige der aktuellen Frage |
| Countdown | 3, 2, 1 bei COUNTDOWN; 30 s bei QUESTION |

### Implementierung
- [ ] Display-Initialisierung
- [ ] Text-Rendering für alle Zustände
- [ ] Aktualisierung bei eingehenden MQTT-Status-Nachrichten

---

## 6. LED-Feedback

| Farbe | Bedeutung |
|-------|-----------|
| **Grün** | Richtige Antwort |
| **Rot** | Falsche Antwort |
| **Blau** | Ready |
| **Gelb** | Waiting / Not Ready |

### Zusätzlich
- [ ] **Pulsing bei Countdown** (z.B. alle 500 ms blinken)
- [ ] LEDs nach Feedback für kurze Zeit aktiv (z.B. 2 s), dann aus

---

## 7. Buttons & Antworten

### 4 Antwort-Buttons (A–D)
- [ ] Button A → Antwort „A“
- [ ] Button B → Antwort „B“
- [ ] Button C → Antwort „C“
- [ ] Button D → Antwort „D“

### Debouncing
- [ ] **Mindestens 50 ms** Entprellung (laut DesignVorschlag)
- [ ] Keine Doppelauslösung bei kurzem Drücken

### Lockout
- [ ] Nach Antwortabgabe Buttons **sperren** bis neue Frage startet
- [ ] Während COUNTDOWN, EVALUATION, END: Buttons inaktiv

---

## 8. Heartbeat & Ping/Pong

### Regulärer Heartbeat
- [ ] Alle **10 Sekunden** Heartbeat senden: `controller/{id}/heartbeat`
- [ ] Payload: `{ "ts": 1709900000000 }`
- [ ] Oder: Auf Ping vom Backend reagieren und Pong senden

### Pre-Question Ping
- [ ] Auf `controller/{id}/ping` subscriben
- [ ] Bei Empfang: **Pong** senden mit `{ "requestId": "...", "ts": ... }`
- [ ] Timeout 3 s: Keine Antwort → Controller wird disconnected (Backend-Logik)

---

## 9. Status-Empfang

- [ ] Auf `controller/{id}/status` subscriben
- [ ] Status enthält: `state`, `currentQuestion`, `countdownSeconds`, `playerTotalScore`, `preQuestionPingId`, etc.
- [ ] Display und LEDs entsprechend aktualisieren
- [ ] Bei `preQuestionPingId` im Status: Pong senden (falls nicht schon per Ping geschehen)

---

## 10. Antworten senden

- [ ] Bei QUESTION-State: Button-Druck → Antwort (A/B/C/D) senden
- [ ] Topic: `controller/{id}/answer`
- [ ] Payload: `{ "answer": "A", "ts": 1709900000000 }`
- [ ] Auf `controller/{id}/answer/result` warten
- [ ] Result enthält: `isCorrect`, `points`, `totalPoints`
- [ ] LED-Feedback (Grün/Rot) anzeigen

---

## 11. Ready/Not Ready

- [ ] **Ready-Button** oder Toggle
- [ ] State senden über `controller/{id}/controller-state`
- [ ] Payload: `{ "state": "READY" }` oder `{ "state": "NOT_READY" }`
- [ ] Alternativ: REST `POST /api/players/controller-state`

---

## 12. Backend-Ergänzungen (für Hardware)

| Aufgabe | Status |
|---------|--------|
| `POST /api/auth/login-rfid` – Login per RFID-UID | [ ] Offen |
| RFID-UID → User lookup, Session erstellen, Controller binden | [ ] Offen |
| Fehlercode „RFID nicht bekannt“ bei unbekanntem Tag | [ ] Offen |
| Hardware-Controller in `controllers`-Tabelle (Register) | [x] Vorhanden |
| MQTT-Handler für Hardware (ping, pong, answer, status) | [x] Vorhanden |

---

## 13. Umgebung & Tests

### Lokale Entwicklung
- [ ] MQTT-Broker (Mosquitto) per Docker erreichbar
- [ ] ESP32 im gleichen Netzwerk wie Broker
- [ ] Test mit lokalem Backend (Port 8080)

### Hardware-Tests (MICRO-Remote-Labor)
- [ ] VPN-Verbindung zur THM
- [ ] Broker: `iti-mqtt.mni.thm.de` (TLS, Port 1883/9001)
- [ ] Backend muss von Hardware aus erreichbar sein (Netzwerk-/Firewall-Konfiguration)

---

## 14. Fehlerbehandlung

| Szenario | Verhalten |
|----------|-----------|
| RFID unbekannt | OLED: „RFID nicht bekannt“, keine Aktion |
| RFID doppelt vergeben | Backend lehnt ab; Fehlermeldung auf OLED |
| MQTT verbindungslos | Reconnect, ggf. „Verbindung verloren“ anzeigen |
| Timeout bei Antwort | Buttons gesperrt, 0 Punkte (Backend-Logik) |
| Pre-Question Ping verpasst | Controller disconnected (Backend-Logik) |

---

## 15. Übersicht: Abhängigkeiten

```
[ESP32]                    [MQTT Broker]              [Backend]
   |                              |                         |
   |--- register ---------------->|                         |
   |--- heartbeat --------------->|                         |
   |<-- ping ---------------------|                         |
   |--- pong -------------------->|                         |
   |<-- status -------------------|<---- (Status-Push) -----|
   |--- controller-state -------->|                         |
   |--- answer ------------------>|                         |
   |<-- answer/result -------------|<---- (Antwort-Result) --|
   |                              |                         |
   |                    [RFID-Login: REST direkt zum Backend]
   |--- POST /api/auth/login-rfid -------------------------->|
   |<-- { authToken, userId, username } ---------------------|
```

---

## Zusammenfassung

| Kategorie | Anzahl offener Punkte |
|-----------|------------------------|
| Hardware & Grundaufbau | 7 |
| MQTT-Kommunikation | 6 |
| Controller-ID & Registrierung | 3 |
| Login (RFID + Web) | 5 |
| OLED-Display | 3 |
| LED-Feedback | 3 |
| Buttons & Debouncing | 4 |
| Heartbeat & Ping/Pong | 3 |
| Status & Antworten | 4 |
| Backend (RFID-Login) | 2 |
| Umgebung & Tests | 3 |
| Fehlerbehandlung | 5 |

**Gesamt: ca. 48 Implementierungspunkte** für die vollständige Hardware-Controller-Integration.
