# ngrok & Web-Controller – Spielen vom Smartphone

Diese Anleitung erklärt, wie Sie das Quiz-Spiel mit dem **Web-Controller** auf dem Smartphone spielen können, indem Sie Ihren lokalen Server mit **ngrok** im Internet erreichbar machen.

---

## Übersicht

| Komponente        | Port | Beschreibung                                           |
|-------------------|------|--------------------------------------------------------|
| Web-Controller    | 81   | Spielsteuerung (Buttons A/B/C/D, Ready, Not Ready)     |
| Backend (API)     | 8080 | Wird über den Web-Controller (Nginx-Proxy) erreicht    |
| Frontend (Lobby)  | 80   | Host-Steuerung, Spiel starten                          |

**Wichtig:** Der Web-Controller (Port 81) leitet API-Anfragen (`/api`) automatisch an das Backend weiter. Es genügt daher, **nur Port 81** mit ngrok zu exponieren, um vom Smartphone aus zu spielen.

---

## Voraussetzungen

1. **Docker** und **Docker Compose** installiert
2. Ein **ngrok-Konto** (kostenlos unter [ngrok.com](https://ngrok.com))
3. Smartphone und PC im **gleichen WLAN** (für lokale Tests) oder ngrok für Remote-Zugriff
4. **MQTT-Broker:** Wenn Sie den THM-Broker (`iti-mqtt.mni.thm.de`) nutzen, muss Ihr Smartphone ebenfalls Zugriff darauf haben (z. B. über VPN). Bei lokalem Mosquitto: PC und Smartphone im gleichen Netzwerk.

---

## 1. Projekt starten

Öffnen Sie ein Terminal im Projektverzeichnis:

```bash
cd group-17
docker compose up --build
```

Warten Sie, bis alle Container laufen:

```
✔ Container mariadb       Created
✔ Container mosquitto     Created   # Nur bei Profil "local"
✔ Container java-backend  Created
✔ Container frontend      Created
✔ Container web-controller Created
```

**Prüfen:**

```bash
docker ps
```

Der Web-Controller sollte auf Port 81 laufen. Lokal testen: [http://localhost:81](http://localhost:81)

---

## 2. ngrok installieren

### Linux (Debian/Ubuntu)

```bash
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
  | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null \
  && echo "deb https://ngrok-agent.s3.amazonaws.com buster main" \
  | sudo tee /etc/apt/sources.list.d/ngrok.list \
  && sudo apt update \
  && sudo apt install ngrok
```

### Alternative (alle Systeme)

1. Konto erstellen: [https://dashboard.ngrok.com/signup](https://dashboard.ngrok.com/signup)
2. Auth-Token holen: [https://dashboard.ngrok.com/get-started/your-authtoken](https://dashboard.ngrok.com/get-started/your-authtoken)
3. ngrok herunterladen: [https://ngrok.com/download](https://ngrok.com/download)

---

## 3. ngrok einrichten

Token hinzufügen (nur einmal nötig):

```bash
ngrok config add-authtoken IHR_AUTH_TOKEN
```

Ersetzen Sie `IHR_AUTH_TOKEN` durch Ihren Token aus dem ngrok-Dashboard.

---

## 4. Web-Controller mit ngrok exponieren

In einem **zweiten Terminal** (Projekt läuft weiter im ersten):

```bash
ngrok http 81
```

**Ausgabe (Beispiel):**

```
ngrok

Session Status                online
Account                       IhrName (Plan: Free)
Version                       3.x.x
Region                        Europe (eu)
Latency                       -
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://xxxx-xx-xx-xx-xx.ngrok-free.app -> http://localhost:81
```

Die **Forwarding-URL** (z. B. `https://xxxx.ngrok-free.app`) ist Ihre öffentliche Adresse.

---

## 5. Vom Smartphone aus spielen

1. **URL aufrufen:** Öffnen Sie die ngrok-URL auf Ihrem Smartphone im Browser.
2. **Optional:** Bei der ersten Nutzung zeigt ngrok eine Warnseite („Visit Site“) – auf **Visit Site** tippen.
3. **Web-Controller laden:** Sie sehen die Controller-Oberfläche (Buttons A, B, C, D, Ready, Not Ready).
4. **Host-Seite:** Der **Spielleiter** startet das Spiel über das Frontend – entweder:
   - Lokal auf dem PC: [http://localhost](http://localhost)
   - Oder ebenfalls über ngrok (siehe Hinweis unten), wenn der Host remote ist

---

## 6. Befehlsübersicht

| Aktion                | Befehl                                    |
|-----------------------|-------------------------------------------|
| Projekt starten       | `cd group-17 && docker compose up --build`|
| Projekt stoppen       | `docker compose down`                     |
| ngrok starten         | `ngrok http 81`                           |
| ngrok mit fester URL* | `ngrok http 81 --domain=ihr-domain.ngrok-free.app` |

\* Feste URL nur mit ngrok-Paid-Plan.

---

## 7. Optional: Frontend (Lobby) ebenfalls exponieren

Falls der **Host** (Spielleiter) ebenfalls vom Smartphone oder von außen zugreifen soll:

**Option A – Zwei ngrok-Tunnel (separate Terminals):**

Terminal 1 (Web-Controller):

```bash
ngrok http 81
```

Terminal 2 (Frontend/Lobby):

```bash
ngrok http 80
```

**Option B – Eine ngrok-Config-Datei** (`ngrok.yml` im Projektverzeichnis):

```yaml
version: "3"
tunnels:
  web-controller:
    addr: 81
    proto: http
  frontend:
    addr: 80
    proto: http
```

Starten mit:

```bash
ngrok start --all
```

---

## 8. MQTT & Smartphone

Der Web-Controller verbindet sich aus dem **Browser** zum MQTT-Broker:

| Broker         | Voraussetzung für Smartphone                          |
|----------------|-------------------------------------------------------|
| THM (iti-mqtt) | Smartphone muss im **THM-VPN** sein (oder Broker erreichbar) |
| Lokal (Mosquitto) | Smartphone und PC im **gleichen WLAN**; Mosquitto mit `docker compose --profile local up` starten |

**Prüfen:** Nach dem Laden der ngrok-URL sollte im Web-Controller „Status: Verbunden“ bzw. „Connected“ erscheinen. Wenn nicht, ist der MQTT-Broker vom Smartphone aus nicht erreichbar.

---

## 9. Troubleshooting

| Problem                          | Lösung                                                    |
|----------------------------------|-----------------------------------------------------------|
| ngrok: „command not found“       | ngrok installieren (siehe Abschnitt 2)                    |
| ngrok: „authtoken“ fehlt         | `ngrok config add-authtoken IHR_TOKEN` ausführen          |
| Web-Controller lädt nicht        | `docker ps` – sind alle Container aktiv? Port 81 frei?    |
| MQTT nicht verbunden (Smartphone)| VPN prüfen (THM) oder gleiches WLAN (lokal)               |
| „Visit Site“-Seite von ngrok     | Normale Einschränkung der Free-Version, einmal bestätigen |

---

## Zusammenfassung

1. **Projekt starten:** `docker compose up --build`
2. **ngrok starten:** `ngrok http 81`
3. **URL auf Smartphone öffnen** und als Spieler mitspielen.
4. **Host** startet das Spiel über [http://localhost](http://localhost) (oder eigenen ngrok-Tunnel für Port 80).
