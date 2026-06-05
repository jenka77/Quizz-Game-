# Quiz-Game Multiplayer – Design Draft

Dieses Dokument konsolidiert die wichtigsten technischen Entwurfsentscheidungen für das Multiplayer-Quiz-Game Projekt basierend auf den Anforderungen aus dem Handout.

---

## 1. Architektur-Übersicht

Das System besteht aus **vier Hauptkomponenten**, die über REST (Authentifizierung, State) und MQTT (Live-Spielmechanik) kommunizieren:

| Komponente              | Funktion                                                   | Technologie                                     |
|-------------------------|------------------------------------------------------------|-------------------------------------------------|
| **Backend**             | Authentifizierung, Lobby-Verwaltung, Game-Logic, Datenbank | Node.js/Express oder Python/FastAPI, PostgreSQL |
| **Web-Frontend**        | Spieler-Dashboard, Lobby-Ansicht, Highscores               | React/Vue.js, REST/Polling (1s)                 |
| **Web-Controller**      | Browser-basierter Handheld-Controller                      | HTML/JavaScript, MQTT über WebSocket            |
| **Hardware-Controller** | ESP32/Arduino mit Buttons, LEDs, OLED, RFID                | Arduino IDE, MQTT, WiFi                         |

**Kommunikationsmuster:**
- **REST**: Login, Registrierung, Lobby-Status-Abfrage, Highscores
- **MQTT**: Echtzeit-Spielevents (Fragen, Antworten, Ready, Ping/Heartbeat)
- **Polling**: Frontend aktualisiert Lobby alle 1s + manueller Refresh-Button (keine WebSockets)

---

## 2. Datenbankschema (Vorschlag)

Einen von Ihnen erweiterbaren Vorschlag für das Datenbankschema, inklusive einer handvoll an vorgefertigten Frage/Antwort Paaren, finden Sie im Datenbank Ordner des Repos.

---

## 3. Game-State-Machine (Vorschlag)

Das Backend verwaltet den Spielfluss über folgende States mit expliziten Übergängen:

```
LOBBY
  ↓ (alle Spieler ready + Countdown abgelaufen)
COUNTDOWN (3 Sekunden)
  ↓ (Countdown beendet)
QUESTION (30 Sekunden pro Frage)
  ↓ (alle Spieler geantwortet ODER 30s Timeout)
EVALUATION (Ergebnisse anzeigen, 3 Sekunden)
  ↓ (next question exists)
QUESTION (repeat)
  ↓ (letzte Frage + Evaluation beendet)
END (Ergebnisse-Übersicht)
  ↓ (manueller Reset)
LOBBY
```

**Transition-Bedingungen:**
- Ready-Status muss nur von "nicht disconnected" Spielern erfüllt sein
- Pre-Question-Ping 3 Sekunden vor Frage: Timeout → Controller wird `DISCONNECTED`, zählt nicht mehr
- Nach Frage 3 Sekunden automatischer Übergang (kein manuelles Weiterklicken)

---

## 4. MQTT Topic-Struktur

**Alle Topics folgen dem Präfix `group-XX/` (aus dem Template). Dies ist zwingend notwendig um die Gruppen untereinander nicht zubeeinflussen!**

Vorschlag für Echtzeit-Events:

### Controller & Status
- `group-XX/controller/{controllerId}/register` – Controller meldet sich an
- `group-XX/controller/{controllerId}/ping` – Backend sendet Heartbeat
- `group-XX/controller/{controllerId}/pong` – Controller antwortet

### Player & Ready
- `group-XX/player/{playerId}/ready` – Player setzt ready/not-ready
- `group-XX/player/{playerId}/status` – Backend sendet aktuellen Player-Status

### Game-Events
- `group-XX/game/state` – Backend sendet State-Übergang (LOBBY/COUNTDOWN/QUESTION/EVALUATION/END)
- `group-XX/game/countdown` – 3-Sekunden-Countdown-Ticks (3, 2, 1)
- `group-XX/game/question` – Neue Frage (id, text, options, duration)
- `group-XX/player/{playerId}/answer` – Player sendet Antwort (questionId, selectedOption, timestamp)
- `group-XX/player/{playerId}/result` – Backend sendet Ergebnis (richtig/falsch, punkte, korrekte Antwort)

### Ping/Heartbeat Rules
- **Regulär**: Alle 10 Sekunden (2 verpasste = disconnected)
- **Pre-Question**: 3 Sekunden vor jeder Frage (Timeout = permanenter Disconnect aus "active players")

Payload-Beispiel:
```json
{ "requestId": "uuid-123", "ts": 1704534000000, "fw": "v1.0" }
```

---

## 5. REST API (Vorschlag)

### Authentication
- `POST /api/auth/register` – (username, password)
- `POST /api/auth/login` – (username, password)

### Lobby & Controller
- `GET /api/lobby/status` – Alle Spieler + Status (für Polling)
- `GET /api/controllers/available` – Verfügbare Controller (frei/zugewiesen/disconnected)
- `POST /api/players/bind` – (controllerId, controllerType) Controller an Player binden

### Game Config & Start
- `POST /api/game/config` – (mode=5|10|20, categories[], difficulties[])
  - **Validierung**: Mindestens `mode` Fragen verfügbar, sonst Error
- `POST /api/game/start` – (per MQTT wird `COUNTDOWN` State gesendet)

### Highscores
- `GET /api/highscores/{mode}` – (mode=5|10|20, limit=20) → (username, score, created_at)

---

## 6. Controller-Implementierung

### Web-Controller (Browser, `/controller`)
- **Registrierung**: Zufällige Controller-ID, MQTT-Connect über WebSocket
- **UI**: Spielername, Ready-Toggle, 4 große Antwortbuttons (A–D), Punkteanzeige nach Frage
- **Messaging**: MQTT Publish auf `group-XX/player/{playerId}/answer` + Listen auf `group-XX/player/{playerId}/result`
- **Lockout**: Nach Antwortabgabe Buttons sperren bis neue Frage startet

### Hardware-Controller (ESP32/Arduino)
- **Login-Optionen**:
  1. RFID-Scan (Direct-Join): Unbekannter Tag → „RFID nicht bekannt" auf OLED
  2. Web-Frontend Login + Controller-Auswahl
- **Display (OLED)**: Spielername, Ready-Status, aktuelle Punkte, ggf. Fehlermeldungen
- **Feedback (LEDs)**:
  - Grün = richtig, Rot = falsch, Blau = ready, Gelb = waiting
  - Pulsing bei Countdown
- **Debouncing**: Button-Entprellung mindestens 50ms
- **MQTT**: Reconnect-Logik bei Verbindungsabbruch, Debug-Ausgaben per Serial

**Hardware Controller werden über das MICRO-Remote-Labor bereitgestellt und stehen Ihnen nur innerhalb des VPN unter der nutzung des öffentlichen MQTT-Brokers (iti-mqtt.mni.thm.de) zur Verfügung**

- **MQTT**
  - Lokal (Entwicklung): MQTT Broker per Docker Compose (localhost:1883/9001)
  - Hardware-Tests: iti-mqtt.mni.thm.de:1883/9001 (TLS, nur im VPN erreichbar)
  - Umschalter: Env-Variable MQTT_BROKER_HOST


---
