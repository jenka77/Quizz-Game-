# Ping und Pong – Ablauf im Frontend (Web-Controller)

Dieses Dokument erläutert, wie Ping und Pong im Web-Controller (`/web-controller`) umgesetzt werden.

---

## Übersicht

Es gibt **zwei getrennte Mechanismen**:

| Typ | Wer sendet | Wer antwortet | Zweck |
|-----|------------|---------------|-------|
| **Regulärer Heartbeat** | Backend → Controller | Controller → Backend | Prüfen, ob der Controller aktiv ist (alle 10 s) |
| **Pre-Question Ping** | Backend → Controller | Controller → Backend | Prüfung vor jeder Frage (während der Auswertung) |

---

## 1. Regulärer Heartbeat (alle 10 Sekunden)

### Backend-Seite
- Der `PlayerService` prüft alle 10 Sekunden die Heartbeats und sendet einen **MQTT-Ping** an alle noch verbundenen, zugewiesenen Controller.
- Topic: `{prefix}controller/{controllerId}/ping` (z.B. `group-17/controller/WEB-XXX/ping`)
- Payload: `{ "requestId": "uuid-...", "ts": 1704534000000 }`

### Frontend-Seite (Web-Controller)

**a) Empfang des Pings (MQTT)**

```javascript
// controller.js – subscribe
mqttConn.subscribe(mqttTopic("ping"));   // → {prefix}controller/WEB-XXX/ping

// controller.js – message handler
mqttConn.on("message", (topic, payloadBuf) => {
  if (String(topic).endsWith("/ping")) {
    const json = JSON.parse(payloadBuf.toString());
    const requestId = json.requestId;
    // ...
    mqttConn?.publish(mqttTopic("pong"), JSON.stringify({ requestId, ts: Date.now() }), { qos: 0 });
  }
});
```

→ Der Controller abonniert `controller/WEB-XXX/ping`. Bei eingehender Nachricht sendet er ein **Pong** an `controller/WEB-XXX/pong`.

**b) Proaktiver Heartbeat**

```javascript
// controller.js – proaktiver Heartbeat
setInterval(() => {
  mqttConn?.publish(mqttTopic("heartbeat"), JSON.stringify({ ts: Date.now() }), { qos: 0 });
}, 10000);
```

→ Der Controller sendet zusätzlich alle 10 Sekunden eine Nachricht an `controller/WEB-XXX/heartbeat`. So bleibt er im Backend als verbunden markiert, auch wenn der Ping noch nicht gelaufen ist.

### Ablauf-Schema

```
Backend (alle 10 s)              Web-Controller
        │                                  │
        │  MQTT: /ping                     │
        │  { requestId, ts }               │
        ├─────────────────────────────────►│
        │                                  │  Empfängt die Nachricht
        │                                  │  → publiziert /pong
        │  MQTT: /pong                     │
        │  { requestId, ts }               │
        │◄─────────────────────────────────┤
```

---

## 2. Pre-Question Ping (während der Auswertung)

### Backend-Seite
- Zu Beginn jeder **Auswertungs**-Phase (3 s), sofern eine weitere Frage folgt, sendet das Backend einen Pre-Question-Ping.
- Topic: `{prefix}controller/{controllerId}/ping` (identisch zum Heartbeat)
- Payload: `{ "requestId": "uuid-ping-id", "ts": ... }`

Zusätzlich übermittelt das Backend die `preQuestionPingId` im **Status**, der bei Zustandsänderungen an die Controller gesendet wird.

### Frontend-Seite – Zwei Reaktionsmöglichkeiten

**A) Über MQTT `/ping` (direkt)**

Gleiche Logik wie beim regulären Heartbeat: Der Handler `topic.endsWith("/ping")` empfängt die Nachricht und sendet das Pong. Keine Codeänderung erforderlich.

**B) Über den MQTT-Status (indirekt)**

Das Backend schickt bei relevanten Ereignissen (Spielstart, Fragenstart, Auswertung) einen Status an `{prefix}controller/{controllerId}/status`. Dieser enthält u.a.:
- `state`: `"EVALUATION"` oder `"PRE_QUESTION"`
- `preQuestionPingId`: die ID des laufenden Pings

```javascript
// controller.js – handleStatus() bei PRE_QUESTION/EVALUATION
if ((state === "PRE_QUESTION" || state === "EVALUATION") && status.preQuestionPingId) {
  // ...
  if (lastPreQuestionPingIdResponded !== status.preQuestionPingId) {
    lastPreQuestionPingIdResponded = status.preQuestionPingId;
    mqttConn?.publish(
      mqttTopic("pong"),
      JSON.stringify({ requestId: status.preQuestionPingId, ts: Date.now() }),
      { qos: 0 }
    );
  }
}
```

→ Wenn der Status `EVALUATION` oder `PRE_QUESTION` sowie eine `preQuestionPingId` enthält, sendet der Controller ein Pong mit dieser ID.

### Ablauf-Schema

```
Backend (Start Auswertung)       Web-Controller
        │                                  │
        │  1. MQTT: /ping                  │
        ├─────────────────────────────────►│  → sofortiges Pong (Handler /ping)
        │                                  │
        │  2. MQTT: /status (bei Ereignis) │
        │  { state: "EVALUATION",          │
        │    preQuestionPingId: "uuid" }   │
        ├─────────────────────────────────►│  → handleStatus() sendet Pong,
        │                                  │     falls noch nicht geschehen
```

---

## Code-Referenz

| Datei | Funktion |
|-------|----------|
| `web-controller/controller.js` | Abonnement des Topics `ping` (connect-Handler) |
| `web-controller/controller.js` | Heartbeat alle 10 s (`setInterval` nach MQTT-Connect) |
| `web-controller/controller.js` | Reaktion auf MQTT-Ping → Pong senden (`topic.endsWith("/ping")`) |
| `web-controller/controller.js` | Reaktion auf Pre-Question über Status (`handleStatus`) |
| `web-controller/controller.js` | Anzeige „Pong senden...“ auf dem LCD in `updateLCD` |

**Hinweis**: Das Topic-Präfix (`group-17/` oder `test/`) kommt aus `MQTT_MESSAGE_PREFIX` (env.js / Umgebungsvariable).

---

## Anzeige für den Benutzer

Während der Auswertung oder der Pre-Question zeigt der Web-Controller bei einem laufenden Ping an:

- **Auswertung**: `"Auswertung"` / `"Pong senden..."` / `{controllerId}`
- **Pre-Question**: `"Pre-Question"` / `"Pong senden..."` / `{controllerId}`

Damit wird bestätigt, dass der Controller im Hintergrund ein Pong sendet. Die technischen Details bleiben verborgen; die Anzeige bestätigt die erfolgreiche Ping-Antwort.
