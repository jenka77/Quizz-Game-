# Studi Handout Quiz‑Game

Sie arbeiten an einem vernetzten Multiplayer-Quizspiel und werden dabei Client-Server-Architekturen, asynchrone Event-basierte Kommunikation (MQTT), Authentifizierung/RFID-Integration, State-Management und Fehlerfallbehandlung praktisch umsetzen.

---

## Ziel des Projekts

Lernziele: Sie verstehen, wie verteilte Systeme kommunizieren, wie man Hardware und Web-Frontend integriert, und wie man robuste, fehlertolerante Systeme designt.

- Entwicklung eines Multiplayer‑Quiz‑Games mit Web‑Frontend, Backend und MQTT‑basierter Kommunikation.  
- Integration von zwei Controller‑Typen: Hardware‑Controller (ESP32/Arduino mit Buttons, LEDs, OLED, RFID) und Web‑Controller im Browser.  
- Fokus: saubere Schnittstellen, State‑Machine für den Spielfluss, zuverlässige Kommunikation und robuste Behandlung von Verbindungsabbrüchen.  

---

## Spielfluss und Regeln

- Es gibt genau eine Lobby/Session; alle Spieler treten dort bei und spielen gemeinsam.  
- Ein Spiel läuft in einem der Modi: 5, 10 oder 20 Fragen.  
- Jede Frage: Multiple Choice mit 4 Optionen (A–D) und genau einer richtigen Antwort.  
- Antwortzeit pro Frage: 30 Sekunden; danach ist die Frage geschlossen und es gibt keine Punkte mehr.  
- Nach der Auswertung wird das Ergebnis angezeigt und nach 3 Sekunden automatisch zur nächsten Frage gewechselt.  
- Fragen dürfen in einer Partie nicht wiederholt werden; ist der Fragenpool zu klein, darf das Spiel nicht starten (Fehlermeldung anzeigen).  
- Es gibt keine Mindestspielerzahl (Solo‑Spiele sind erlaubt).
  - Eine Obergrenze wird auf 99 Spieler pro Session gesetzt, um MQTT-Last und Browser-Performance zu limitieren.   
- Spieler/Controller, die disconnected sind, bleiben in der Lobby sichtbar, zählen aber nicht mehr als aktive Spieler im laufenden Spiel; Rejoin in eine laufende Partie ist nicht vorgesehen.  

---

## Login & Authentifizierung

### Registrierung und Account-Verwaltung

- Nutzer registrieren sich mit Benutzername und Passwort und legen damit einen persönlichen Account an.
  - Nutzernamen müssen hierbei einzigartig sein!
- Optional kann bei der Registrierung eine RFID‑ID hinterlegt werden, um sich später direkt am Hardware‑Controller per RFID‑Scan anmelden zu können.  
- Pro Account ist maximal eine RFID‑ID erlaubt, und jede RFID‑ID darf nur genau einem Account zugeordnet sein.  
- Nach einem Login im Web‑Frontend können Nutzer ihre RFID‑ID verwalten:  
  - vorhandene RFID‑ID entfernen  
  - eine neue RFID‑ID setzen (z.B. durch einen zugeordneten Einrichtungs‑/Scan‑Prozess).  

### Login-Modelle und Controller-Bindung

- Einstieg ins Spiel über die Lobby mit Ready‑Mechanismus:  
  - Spieler erscheinen in der Lobby erst nach erfolgreichem Join (Login + Controllerbindung oder RFID‑Login am Hardware‑Controller).  
  - Jeder Spieler setzt seinen Status am Controller auf „Ready“ oder „Not Ready“.  
  - Wenn alle aktiven Spieler ready sind, startet ein 3‑Sekunden‑Countdown; wechselt jemand zurück auf „Not Ready“, wird der Countdown abgebrochen.  

- Login‑Modelle:  
  - Klassischer Login im Web‑Frontend mit Username/Passwort:
    - Erlaubt Auswahl eines verfügbaren Controllers (Hardware oder Web).
    - Verwaltung der zugeordneten RFID-ID zur Hardware-Controller Nutzung.
  - RFID‑Login am Hardware‑Controller:  
    - Pro Account genau ein RFID‑Tag, und jeder RFID‑Tag ist genau einem Account zugeordnet.  
    - Bei Scan eines bekannten RFID‑Tags wird der Spieler angemeldet und an diesen Controller gebunden.  
    - Unbekannter Tag → Display zeigt „RFID Karte nicht bekannt“, es passiert nichts weiter.  
  - Web‑Controller haben keinen RFID‑Reader; die Zuordnung läuft nur über Login und Controller‑Auswahl.  

- Lobby‑UI: Anzeige aller Spieler mit Ready‑Status, Verbindungsstatus und gebundenem Controller. 
  - Das Frontend aktualisiert diese Ansicht per Polling alle 1 Sekunde, um Echtzeit-Updates zu simulieren. 
  - Zusätzlich ermöglicht ein Refresh-Button eine manuelle, sofortige Aktualisierung für Nutzer, die schneller reagieren möchten.

---

## Controller und Verbindungsüberwachung

- **Hardware‑Controller (ESP32/Arduino)**:  
  - 4 Buttons für Antworten, 4 LEDs für Status/Feedback, OLED‑Display, RFID‑Reader, WLAN‑Anbindung.  
  - Anzeige von Spielername, Ready‑Status, Spielstatus, eigenen Punkten sowie Fehlermeldungen (z.B. unbekannte RFID‑Karte).  

- **Web‑Controller (Browser)**:  
  - UI erreichbar über separate URL (z.B. `/controller`)
    - Erzeugt pro offener Instanz eine zufällige Controller‑ID.  
  - Anzeige von Spielername und Status, 4 große Antwortbuttons (nach Antwort gesperrt bis zur nächsten Frage) und Ready‑Toggle.  
  - Anzeige der eigenen Punkte nach jeder Frage.  

- **Verbindungsüberwachung**:  
  - Heartbeat (Ping/Pong) ungefähr alle 10 Sekunden; bleiben zwei Heartbeats aus, wird der Controller als disconnected markiert.  
  - Vor jeder neuen Frage wird ein Pre‑Question‑Ping verschickt: Controller, die nicht innerhalb von 3 Sekunden antworten, werden für den Rest der Partie aus den aktiven Spielern entfernt (Status disconnected), ohne den Rundenrhythmus zu verlängern.  

---

## Scoring und Highscores

- Basispunkte pro richtiger Antwort: leicht = 1, mittel = 2, schwer = 3 Punkte; falsche Antworten geben 0 Punkte.  
- Zeitfaktor (nur bei richtiger Antwort):  
  - 0–5 s → 1.0  
  - 5–10 s → 0.9  
  - 10–15 s → 0.8  
  - 15–20 s → 0.7  
  - 20–25 s → 0.6  
  - 25–30 s → 0.5  
  - ab 30 s → 0.0  
- Punkte pro Frage: `Punkte = Basispunkte x Zeitfaktor`, bei Timeout oder Antwort ab 30 s immer 0.
  - Beispiel: Schwere Frage (3 Punkte), beantwortet nach 8 Sekunden: 3 × 0.9 = 2.7 Punkte.
  - Werte sollen hierbei nicht gerundet werden.
- Pro Modus werden die Top 20 besten Ergebnisse (nicht eindeutige Nutzer) nach absteigendem Score geführt und sind in der UI einsehbar.
  - Ein Spieler kann mehrmals in der Liste erscheinen, wenn er mehrere Partien spielt.
  - Sortierung: zuerst nach Score absteigend, bei Gleichstand nach Timestamp.  

---

## Fehlerfallbehandlung
Folgende Szenarien müssen definiert bearbeitet werden:

- Controller‑Disconnect: Spieler bleiben in der Lobby sichtbar, nehmen aber nicht mehr am Spiel teil. Punkte bleiben erhalten. Der Spielfluss wird nicht unterbrochen.
- RFID‑Fehler: Unbekannte Tags zeigen eine Fehlermeldung auf dem Display. Doppelte RFID‑IDs werden vom Backend abgelehnt.
- Mehrfach‑Login: Ein Account darf nur einmal aktiv sein. Entscheiden Sie, ob ein neuer Login den alten verdrängt oder wird abgelehnt – und dokumentieren Sie dies.
- Zeitüberschreitungen: Antworten nach 30 Sekunden zählen nicht. Buttons werden gesperrt, bis die nächste Frage startet.
- Ungültiger Fragenpool: Sind zu wenig Fragen verfügbar, startet das Spiel nicht und zeigt eine Fehlermeldung in der Lobby.

---

## Technikrahmen

- Backend stellt REST‑Endpunkte für Registrierung/Login, Lobby‑Status, Controller‑Liste, Spielkonfiguration und Highscores bereit.  
- MQTT wird für alle Spielereignisse genutzt: Controller‑Registrierung, Ready‑Events, Fragen, Antworten, Feedback, Game‑State, Ping/Heartbeat.  
- Entwicklungsumgebung: lokaler MQTT‑Broker (z.B. Mosquitto per Docker‑Compose); für Hardware‑Tests ein Hochschul‑Broker, der per Konfiguration umgeschaltet wird.
- Alle persistenten Daten (Accounts, Fragen, Highscores) werden in einer MariaDB-Datenbank gespeichert.
- Fragen werden durch Seeds/Migrationen ins System importiert.
  - Highscores werden pro Modus dauerhaft gelagert und sind sitzungsübergreifend einsehbar
  - Eine Admin/Verwaltungs‑UI ist nicht notwendig!

Weitere technische Details (API-Vorschläge, MQTT-Topics, Datenbankschema) finden Sie in der README im Repository sowie unter /docs.
Bei Fragen: https://matrix.to/#/#GEN1002:thm.de
