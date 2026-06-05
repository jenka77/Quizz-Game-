# Controller-Routen – Backend-API

Dieses Dokument fasst die controllerbezogenen HTTP-Routen (Web- und Hardware-Controller) zusammen, die vom Frontend verwendet werden.

---

## 1) Verfügbare Controller auflisten

### `GET /api/controllers/available`

Liefert die Liste der Controller aus der Datenbank inklusive ermittelter Verfügbarkeit.

### Beispielantwort (200)

```json
{
  "webUnlimited": true,
  "controllers": [
    {
      "controllerId": "HW-1234",
      "controllerType": "HARDWARE",
      "status": "FREE",
      "available": true,
      "assignedUserId": null,
      "assignedUsername": null
    },
    {
      "controllerId": "WEB-AB12CD",
      "controllerType": "WEB",
      "status": "ASSIGNED",
      "available": false,
      "assignedUserId": 7,
      "assignedUsername": "alice"
    }
  ],
  "hardwareControllers": [
    {
      "controllerId": "HW-1234",
      "controllerType": "HARDWARE",
      "status": "FREE",
      "available": true,
      "assignedUserId": null,
      "assignedUsername": null
    }
  ]
}
```

---

## 2) Controller einem Spieler zuweisen

### `POST /api/players/bind`

Weist einen Controller (Web/Hardware) dem angemeldeten Spieler zu.

### Body

```json
{
  "authToken": "token",
  "controllerId": "WEB-AB12CD",
  "controllerType": "web-controller"
}
```

### Antworten

- `200`: Controller zugewiesen
- `401`: Benutzer nicht angemeldet
- `409`: Controller bereits belegt oder Spielerlimit erreicht
- `400`: ungültiger Payload / ungültiger Typ / Hardware-Controller nicht gefunden  
  Zusätzlich bei Typ-Wechsel ohne Abmeldung: „Bitte zuerst vom Web-Controller abmelden.“ oder „Bitte zuerst vom Hardware-Controller abmelden.“

---

## 3) Controller-Zuordnung aufheben

### `POST /api/players/unbind`

Entfernt die Zuordnung Controller ↔ Spieler.

### Body

```json
{
  "authToken": "token",
  "controllerId": "WEB-AB12CD"
}
```

`controllerId` kann weggelassen oder leer sein, um alle Zuordnungen des Spielers zu löschen.

---

## 4) Controller-Status aktualisieren

### `POST /api/players/controller-state`

Wird von Web-Controller und Hardware-Controller genutzt, um den Laufzeitstatus zu melden.

### Body

```json
{
  "controllerId": "WEB-AB12CD",
  "state": "READY"
}
```

Mögliche Werte für `state`: `READY`, `NOT_READY`, `CONNECTED`, `OFFLINE`.

---

## 5) Spieler kicken

### `POST /api/players/kick`

Entfernt einen anderen Spieler: Session und Controller-Zuordnungen werden gelöscht.

### Body

```json
{
  "authToken": "token",
  "targetUserId": 12
}
```

---

## 6) Lobby-Status (Frontend-Polling)

### `GET /api/lobby/status`

Liefert die Spieler sowie den Status ihrer Controller (`connected`, `ready`, `notReady`).

Dient zur Darstellung von `Ready / Not Ready / Connected / Disconnected`.

Optional: `lobbyMessage`, `lobbyMessageType`, `lobbyMessageId` – RFID-Meldungen (z.B. „RFID nicht erkannt“, „Spieler in der Session hinzugefügt“).

---

## 7) Hardware-Controller trennen (ohne authToken)

### `POST /api/controllers/unbind-hardware`

Trennt einen Hardware-Controller per ID (z.B. nach Drücken des roten Buttons am Arduino).

### Body

```json
{
  "controllerId": "HW-1234"
}
```

---

## 8) Hardware-Controller aus Liste entfernen

### `POST /api/controllers/delete-hardware`

Entfernt einen Hardware-Controller aus der Datenbank (Löschen-Button in der Lobby).

### Body

```json
{
  "controllerId": "HW-1234"
}
```

---

## 9) Controller registrieren

### `POST /api/controllers/register`

Registriert einen Controller (z.B. vor dem Bind), damit er in der Liste erscheint.

### Body

```json
{
  "controllerId": "HW-1234",
  "controllerType": "HARDWARE"
}
```
