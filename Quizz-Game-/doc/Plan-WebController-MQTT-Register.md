# Plan: Web-Controller-Registrierung per MQTT

## Ziel

Beim Öffnen des Web-Controllers:
1. Wird automatisch eine ID erzeugt
2. Diese ID wird per MQTT an das Backend gesendet
3. Das Backend legt den Controller in der Datenbank an (Status FREE)

Der Controller erscheint danach in der Liste verfügbarer Controller (Controller-Konfiguration) und kann nach dem Login von einem Spieler ausgewählt werden.

---

## Aktueller Ablauf (zu ändern)

```
[controller-config]                                   [web-controller]
        |                                                      |
   Auswahl aus Liste / "Web-Controller öffnen"                  |
        |                                                      |
   Liste von /api/controllers                                   |
        |                                                      |
   POST /api/players/bind ────────────────────── Falls kein ?id= → erzeugt "WEB-xxx"
   (erstellt Controller in DB)                                  |
        |                                                      |
   Öffnet Popup ?id=WEB-xxx                       PUBLISH controller/ID/register
        |                                                      POST controller-state (CONNECTED)
        └──────────────────────────────────────────────────────┘
```

**Hinweis**: Der Controller wird in der DB erst beim **Bind** (Login + Auswahl) angelegt, sofern nicht bereits per MQTT-Register erfasst. Die MQTT-Registrierung beim Öffnen des Web-Controllers sorgt dafür, dass er vor dem Bind in der Liste erscheint.

---

## Ziel-Ablauf

```
[Web-Controller wird geöffnet]
        |
   Erzeugt ID (WEB-xxx), falls nicht in der URL
        |
   MQTT-Verbindung (WebSocket)
        |
   PUBLISH group-17/controller/{id}/register
   Payload: { "controllerId": "...", "type": "WEB", "ts": ... }
        |
        v
[Backend MQTT]
   Subscribe: group-17/controller/+/register
        |
   Empfängt Nachricht → extrahiert controllerId
        |
   INSERT INTO controllers (controller_id, controller_type, status)
   VALUES (id, 'WEB', 'FREE')
   (falls nicht vorhanden; falls vorhanden und FREE → UPDATE last_seen_at)
        |
        v
[controller-config] lädt /api/controllers/available
   → Der neue Controller erscheint (FREE, verfügbar)
        |
   Spieler loggt ein → wählt Controller → bind (bestehender Ablauf)
```

---

## Konformität mit dem Design (DesignProposal.md)

| Element | Design | Implementierung |
|---------|--------|-----------------|
| Register-Topic | `group-XX/controller/{controllerId}/register` | ✓ Dieses Format nutzen |
| Web-Controller | „Erzeugt pro offener Instanz eine zufällige Controller-ID“ | ✓ Bereits so umgesetzt |
| MQTT | Web-Controller nutzt MQTT | ✓ Für Register hinzuzufügen |

---

## Zu ändernde / anzulegende Dateien

### 1. Backend (Java)

| Datei | Aktion |
|-------|--------|
| `MqttController.java` | Auf `controller/+/register` subscriben, Nachricht verarbeiten |
| `MqttVerticle.java` | (optional) Subscription prüfen |
| `ControllerRepository.java` | `insertControllerIfNotExists(controllerId, type)` ergänzen |
| `PlayerRepository.java` | Oder vorhandene Logik wiederverwenden – prüfen, ob `insertControllerAssignment` für Insert ohne userId angepasst werden kann |

**Backend-Logik**:
- Neues Topic: `{prefix}controller/+/register`
- Handler: `controllerId` aus Topic oder Payload extrahieren
- Falls Controller nicht in DB → INSERT (controller_id, controller_type='WEB', status='FREE')
- Falls vorhanden und status=FREE → UPDATE last_seen_at
- Falls vorhanden und ASSIGNED → nichts ändern (nicht überschreiben)

### 2. Web-Controller (Frontend)

| Datei | Aktion |
|-------|--------|
| `controller.html` | MQTT-Script (Paho oder mqtt.js) einbinden |
| `controller.js` | MQTT-Verbindung, Register-Publikation beim Laden |
| `env.js` | Sicherstellen, dass MQTT_BROKER_URL/PORT für WebSocket stimmen |

**Abhängigkeit**: MQTT-Client für den Browser  
- **Paho MQTT** (Eclipse): `https://unpkg.com/mqtt/dist/mqtt.min.js` oder CDN  
- Oder **mqtt.js** (npm), falls Build existiert

**WebSocket**: Der MQTT-Broker muss per WebSocket erreichbar sein.  
- THM: `wss://iti-mqtt.mni.thm.de:9001` (oder `ws://` ohne TLS)  
- Lokal mit Docker: `ws://mosquitto:9001` oder `ws://localhost:9001`

### 3. Datenbank

Keine Migration nötig. Die Tabelle `controllers` unterstützt bereits:
- `controller_id` (UNIQUE)
- `controller_type` (WEB, HARDWARE)
- `status` (FREE, ASSIGNED, OFFLINE)
- `assigned_user_id` (NULL bei FREE)

---

## Implementierungsdetails

### A. Format der MQTT-Register-Nachricht

**Topic**: `group-17/controller/WEB-ABC123/register`

**Payload (JSON)**:
```json
{
  "controllerId": "WEB-ABC123",
  "type": "WEB",
  "ts": 1709900000000
}
```

Die `controllerId` kann bei leerem Payload auch aus dem Topic abgeleitet werden.

### B. Repository – Insert falls nicht vorhanden

```sql
INSERT INTO controllers (controller_id, controller_type, status, last_seen_at)
VALUES (?, 'WEB', 'FREE', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE last_seen_at = CURRENT_TIMESTAMP;
```

Hinweis: Mit `ON DUPLICATE KEY UPDATE` wird nur `last_seen_at` gesetzt. `status` und `assigned_user_id` sollten bei ASSIGNED-Controllern nicht geändert werden.

Sichere Variante:
```sql
INSERT INTO controllers (controller_id, controller_type, status, last_seen_at)
SELECT ?, 'WEB', 'FREE', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM controllers WHERE controller_id = ?);
```

In Java, einfacher Ansatz:
1. `findControllerById(controllerId)`
2. Falls leer → `insertController(controllerId, 'WEB', 'FREE')`
3. Falls vorhanden und status=FREE → `updateLastSeen(controllerId)`

### C. Web-Controller – Reihenfolge der Schritte

1. `?id=` aus der URL lesen
2. Falls nicht vorhanden: `controllerId = "WEB-" + random(6)`
3. URL per `history.replaceState` aktualisieren
4. Verbindung zum MQTT-Broker (WebSocket) herstellen
5. Nach Verbindung: Publish auf `controller/{id}/register`
6. Weiter mit Heartbeat, controller-state usw.

### D. Fehlerbehandlung

- MQTT nicht erreichbar: Hinweis anzeigen, REST-Funktionen (Heartbeat, Antworten) beibehalten, damit das Spiel weiterläuft
- Register schlägt im Backend fehl: Controller kann später beim Bind nachgezogen werden (bestehender Fallback-Ablauf)

---

## Sequenzdiagramm

```
Web-Controller          MQTT-Broker           Backend (Java)        Datenbank
     |                       |                      |                    |
     |--- Connect WS ------->|                      |                    |
     |<-- Connected ---------|                      |                    |
     |                       |                      |                    |
     |--- PUB register ----->|                      |                    |
     |   (controller/ID/reg) |                      |                    |
     |                       |--- Message --------->|                    |
     |                       |                      |--- INSERT/UPDATE ->|
     |                       |                      |<-------------------|
     |                       |                      |                    |
     |   (Controller sichtbar in /api/controllers/available)             |
```

---

## Wichtige Punkte

1. **WebSocket-Port**: Der Browser kann den MQTT-TCP-Port 1883 nicht nutzen. Erforderlich ist der WebSocket-Port (z.B. 9001).
2. **CORS / gleiche Herkunft**: Bei anderer Broker-Domain prüfen, ob CORS/WebSocket-Einstellungen die Nutzung erlauben.
3. **Doppelte Registrierung**: Ein einfacher Seiten-Reload darf keine Duplikate erzeugen; der `INSERT` muss bedingt sein (ID vorhanden + FREE → nur `last_seen_at` aktualisieren).
4. **controller-config**: Zwei Varianten sind gültig:
   - Direktes Öffnen des Web-Controllers → MQTT-Register → Erscheinen in der Liste → Verbinden im Lobby
   - Klick auf „Web-Controller öffnen“ in der Lobby → ID erzeugen + Bind → Popup öffnen

---

## Empfohlene Reihenfolge der Aufgaben

1. [ ] Backend: `ControllerRepository.insertOrUpdateFreeController(controllerId, type)`
2. [ ] Backend: `MqttController` – Subscribe auf `controller/+/register` + Handler
3. [ ] Web-Controller: MQTT-Client (Paho/mqtt.js) einbinden
4. [ ] Web-Controller: MQTT-Verbindung + Register-Publish beim Laden
5. [ ] Tests: Web-Controller öffnen → in DB und Controller-Liste prüfen
6. [x] MQTT-Register implementiert – Web-Controller publiziert beim Laden; controller-config verwendet die Liste
