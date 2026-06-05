# Ablauf Hardware-Controller → Backend → Frontend

## Prüfen, ob der Klick erkannt wird

Wenn die LEDs aufleuchten, aber nichts auf dem Frontend (Auswertungsbildschirm) erscheint:

1. **Controller verbinden** in der Lobby (Button „Verbinden“ auf dem HW-XXX)
2. **Grünen Button (Ready) drücken**, bevor das Spiel gestartet wird
3. **ID prüfen**: Beim Start zeigt der Arduino „Controller-ID (im Lobby verbinden): HW-XXXX“ im Serial Monitor – das ist die zu verbindende ID
4. **Backend-Logs**: Bei `Answer rejected: controller 'X' not in game. In game: [Y, Z]` → ID X stimmt nicht mit Y/Z überein; den richtigen Controller verbinden

## Voraussetzungen

1. **Arduino** mit WiFi verbunden (`MQTT-Broker` oder eigener SSID in `secrets.h`)
2. **Backend** und **Arduino** nutzen denselben MQTT-Broker (`iti-mqtt.mni.thm.de` bei THM)
3. **MQTT_MESSAGE_PREFIX** = `group-17/` (identisch im Backend und Arduino)

## RFID-Login (automatischer Scan)

1. **Karte auflegen** → Hardware liest UID, zeigt sie im Serial an, sendet via MQTT ans Backend
2. Backend bindet den Spieler (falls RFID bekannt) an diesen Hardware-Controller

## Schritt 1: Lobby – Hardware verbinden

1. **Arduino einschalten** → WiFi-Verbindung → MQTT → publiziert `controller/HW-XXXXXXXX/register`
2. Backend empfängt die Registrierung und fügt den Controller in die DB ein bzw. aktualisiert ihn (Status=FREE)
3. **Frontend**: Zu `lobby.html` gehen, Bereich „Hardware-Controller verbinden“
4. Die Liste „Verfügbare Hardware-Controller“ wird alle 4 s aktualisiert (bzw. 200 ms für RFID-Meldungen)
5. **Auf „HW-XXXXXXXX – Verbinden“ klicken**, um den Controller mit dem eingeloggten Spieler zu verbinden
6. Oder die ID manuell eingeben (z.B. RFID `1CF8464A` → `HW-1CF8464A`) und auf „Hardware-Controller verbinden“ klicken

→ Der Controller ist nun dem Spieler **zugewiesen** (ASSIGNED in der DB).

## Schritt 2: Ready

1. **Am Arduino**: **grünen Button** (Ready) drücken
2. Der Arduino publiziert `controller/HW-XXX/controller-state` mit `{"state":"READY"}`
3. Das Backend muss die Nachricht empfangen und den Ready-Status aktualisieren

**Wichtig**: Zeigt das Backend in den Logs:
- `Controller-State ignoriert: HW-XXX nicht in DB` → Controller wurde nicht registriert (MQTT, Broker prüfen)
- `Controller-State ignoriert: HW-XXX nicht gebunden` → Controller wurde noch nicht in der Lobby verbunden (Schritt 1)

## Schritt 3: Spiel starten

1. **Alle Spieler** (auch Hardware) müssen **Ready** sein (grüner Button)
2. In der Lobby auf „Spiel starten“ klicken
3. Das Spiel holt die zugewiesenen und READY-Spieler über `fetchActiveReadyPlayers`
4. Hardware-Controller werden berücksichtigt, wenn sie zugewiesen und Ready sind

## Schritt 4: Auf Fragen antworten

1. Während der QUESTION-Phase empfängt der Arduino den Status (Zustand, Frage) via `controller/HW-XXX/status`
2. **Blau=A, Grün=B, Gelb=C, Rot=D**
3. Der Arduino publiziert `controller/HW-XXX/answer` mit `{"answer":"A"}`
4. Das Backend prüft, ob der Controller in `controllerToUserIds` ist (Spiel läuft)
5. Bei Erfolg → sendet `answer/result` an den Arduino

## Ping/Pong (Pre-Question)

Das Backend sendet vor jeder Frage einen Ping, um die Anwesenheit der Controller zu prüfen.

**Wichtig**: Kein Spieler (Web oder Hardware) wird bei fehlendem Pong getrennt. MQTT-Latenz und Arduino-Verarbeitung können 3 Sekunden überschreiten – der Hardware bleibt im Spiel.

## Debug

### Arduino (Serial Monitor, 9600 Baud)
- `MQTT PUB controller-state: group-17/controller/HW-XXX/controller-state -> OK` → Sendung erfolgreich
- `MQTT PUB answer: ... -> OK` → Antwort gesendet
- `MQTT publishControllerState SKIP: disconnected` → nicht mit MQTT verbunden
- `MQTT publishAnswer SKIP` → ebenso

### Backend (docker logs java-backend)
- `Controller-State empfangen: HW-XXX -> READY` → Nachricht empfangen
- `Controller-State aktualisiert: HW-XXX = READY` → verarbeitet
- `Controller-State ignoriert: ... nicht gebunden` → **Controller in der Lobby verbinden**
- `Answer empfangen via MQTT: controller=HW-XXX answer=A`
- `Answer akzeptiert` oder `Answer abgelehnt: ... Controller ist keinem aktiven Spieler zugeordnet`

### Checkliste falls nichts funktioniert
1. [ ] Arduino mit demselben WiFi verbunden wie das Backend (Broker erreichbar)
2. [ ] Arduino zeigt „MQTT connected!“ beim Start
3. [ ] Hardware in „Verfügbare Hardware-Controller“ sichtbar (Lobby-Liste)
4. [ ] Auf „Verbinden“ klicken, um den Controller zu verbinden
5. [ ] Grünen Button (Ready) nach der Verbindung drücken
6. [ ] Backend-Logs prüfen: keine „ignoriert“-Meldungen
7. [ ] Für Antworten: Spiel muss in der QUESTION-Phase sein
