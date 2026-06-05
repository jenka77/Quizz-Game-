# Quiz-Game Multiplayer – Design Draft

This document consolidates the most important technical design decisions for the multiplayer quiz game project based on the requirements from the handout.

---

## 1. Architecture Overview

The system consists of **four main components** that communicate via REST (authentication, state) and MQTT (live game mechanics):

| Component               | Function                                               | Technology                           |
|-------------------------|--------------------------------------------------------|--------------------------------------|
| **Backend**             | Authentication, lobby management, game logic, database | Java and Vert.x                      |
| **Web Frontend**        | Player dashboard, lobby view, highscores               | React/Vue.js, REST/Polling (1s)      |
| **Web Controller**      | Browser-based handheld controller                      | HTML/JavaScript, MQTT over WebSocket |
| **Hardware Controller** | ESP32/Arduino with buttons, LEDs, OLED, RFID           | Arduino IDE, MQTT, WiFi              |

**Communication Patterns:**
- **REST**: Login, registration, lobby status query, highscores
- **MQTT**: Real-time game events (questions, answers, ready, ping/heartbeat)
- **Polling**: Frontend updates lobby every 1s + manual refresh button (no WebSockets)

---

## 2. Database Schema (Proposal)

An expandable proposal for the database schema, including a handful of pre-prepared question/answer pairs, can be found in the database folder of the repo.

---

## 3. Game State Machine (Proposal)

The backend manages game flow through the following states with explicit transitions:

```
LOBBY
  ↓ (all players ready + countdown elapsed)
COUNTDOWN (3 seconds)
  ↓ (countdown finished)
QUESTION (30 seconds per question)
  ↓ (all players answered OR 30s timeout)
EVALUATION (show results, 3 seconds)
  ↓ (next question exists)
QUESTION (repeat)
  ↓ (last question + evaluation finished)
END (results overview)
  ↓ (manual reset)
LOBBY
```

**Transition Conditions:**
- Ready status must only be fulfilled by "not disconnected" players
- Pre-question ping 3 seconds before question: timeout → controller becomes `DISCONNECTED`, no longer counts
- After question, automatic transition after 3 seconds (no manual advancement needed)

---

## 4. MQTT Topic Structure

**All topics follow the prefix `group-XX/` (from the template). This is mandatory to prevent groups from interfering with each other!**

Proposal for real-time events:

### Controller & Status
- `group-XX/controller/{controllerId}/register` – Controller registers
- `group-XX/controller/{controllerId}/ping` – Backend sends heartbeat
- `group-XX/controller/{controllerId}/pong` – Controller responds

### Player & Ready
- `group-XX/player/{playerId}/ready` – Player sets ready/not-ready
- `group-XX/player/{playerId}/status` – Backend sends current player status

### Game Events
- `group-XX/game/state` – Backend sends state transition (LOBBY/COUNTDOWN/QUESTION/EVALUATION/END)
- `group-XX/game/countdown` – 3-second countdown ticks (3, 2, 1)
- `group-XX/game/question` – New question (id, text, options, duration)
- `group-XX/player/{playerId}/answer` – Player sends answer (questionId, selectedOption, timestamp)
- `group-XX/player/{playerId}/result` – Backend sends result (correct/incorrect, points, correct answer)

### Ping/Heartbeat Rules
- **Regular**: Every 10 seconds (2 missed = disconnected)
- **Pre-Question**: 3 seconds before each question (timeout = permanent disconnect from "active players")

Payload example:
```json
{ "requestId": "uuid-123", "ts": 1704534000000, "fw": "v1.0" }
```

---

## 5. REST API (Proposal)

### Authentication
- `POST /api/auth/register` – (username, password)
- `POST /api/auth/login` – (username, password)

### Lobby & Controller
- `GET /api/lobby/status` – All players + status (for polling)
- `GET /api/controllers/available` – Available controllers (free/assigned/disconnected)
- `POST /api/players/bind` – (controllerId, controllerType) Bind controller to player

### Game Config & Start
- `POST /api/game/config` – (mode=5|10|20, categories[], difficulties[])
  - **Validation**: At least `mode` questions available, otherwise error
- `POST /api/game/start` – (COUNTDOWN state sent via MQTT)

### Highscores
- `GET /api/highscores/{mode}` – (mode=5|10|20, limit=20) → (username, score, created_at)

---

## 6. Controller Implementation

### Web Controller (Browser, `/controller`)
- **Registration**: Random controller ID, MQTT connect via WebSocket
- **UI**: Player name, ready toggle, 4 large answer buttons (A–D), points display after question
- **Messaging**: MQTT publish to `group-XX/player/{playerId}/answer` + listen on `group-XX/player/{playerId}/result`
- **Lockout**: After answering, buttons are locked until new question starts

### Hardware Controller (ESP32/Arduino)
- **Login Options**:
  1. RFID scan (direct join): Unknown tag → "RFID card not recognized" on OLED
  2. Web frontend login + controller selection
- **Display (OLED)**: Player name, ready status, current points, error messages if applicable
- **Feedback (LEDs)**:
  - Green = correct, Red = incorrect, Blue = ready, Yellow = waiting
  - Pulsing during countdown
- **Debouncing**: Button debouncing at least 50ms
- **MQTT**: Reconnect logic on connection loss, debug output via Serial

**Hardware controllers are provided via the MICRO remote lab and are only available within the VPN using the public MQTT broker (iti-mqtt.mni.thm.de)**

- **MQTT Configuration**:
  - Local (development): MQTT broker via Docker Compose (wss://<localhost|mosquitto>:<1883|9001>)
  - Hardware tests: wss://iti-mqtt.mni.thm.de:<1883|9001> (TLS, only accessible within VPN)
  - Switcher: Environment variable MQTT_BROKER_HOST

---
