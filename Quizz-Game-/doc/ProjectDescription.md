# Student Handout: Quiz-Game

You will work on a networked multiplayer quiz game, practically implementing client-server architectures, asynchronous event-based communication (MQTT), authentication/RFID integration, state management, and robust error handling.

---

## Project Objective

Learning Goals: You will understand how distributed systems communicate, how to integrate hardware and web frontends, and how to design robust, fault-tolerant systems.

- Development of a multiplayer quiz game with web frontend, backend, and MQTT-based communication.
- Integration of two controller types: hardware controllers (ESP32/Arduino with buttons, LEDs, OLED, RFID) and web controllers in the browser.
- Focus: clean interfaces, state machines for game flow, reliable communication, and robust handling of disconnections.

---

## Game Flow and Rules

- There is exactly one lobby/session; all players join there and play together.
- A game runs in one of these modes: 5, 10, or 20 questions.
- Each question: multiple choice with 4 options (A–D) and exactly one correct answer.
- Answer time per question: 30 seconds; after that, the question is closed and no points can be earned.
- After evaluation, the result is displayed and the game automatically advances to the next question after 3 seconds.
- Questions must not be repeated within a single game; if the question pool is too small, the game must not start (show error message).
- There is no minimum number of players (solo games are allowed).
  - A maximum of 99 players per session is set to limit MQTT load and browser performance.
- Players/controllers that are disconnected remain visible in the lobby but no longer count as active players during gameplay; rejoining a running game is not supported.

---

## Login & Authentication

### Registration and Account Management

- Users register with a username and password to create a personal account.
  - Usernames must be unique!
- Optionally, an RFID ID can be registered during signup to enable direct login at the hardware controller via RFID scan.
- Each account can have at most one RFID ID, and each RFID ID can belong to exactly one account.
- After logging in via the web frontend, users can manage their RFID ID:
  - Remove an existing RFID ID
  - Set a new RFID ID (e.g., through a dedicated setup/scan process).

### Login Models and Controller Binding

- Entering the game via the lobby with a ready mechanism:
  - Players appear in the lobby only after a successful join (login + controller binding or RFID login at the hardware controller).
  - Each player sets their status at the controller to "Ready" or "Not Ready".
  - When all active players are ready, a 3-second countdown starts; if anyone switches back to "Not Ready", the countdown is canceled.

- Login models:
  - Classic login via web frontend with username/password:
    - Allows selection of an available controller (hardware or web).
    - Management of the associated RFID ID for hardware controller usage.
  - RFID login at the hardware controller:
    - Exactly one RFID tag per account, and each RFID tag is assigned to exactly one account.
    - When a known RFID tag is scanned, the player is logged in and bound to that controller.
    - Unknown tag → Display shows "RFID card not recognized", nothing further happens.
  - Web controllers have no RFID reader; assignment only occurs via login and controller selection.

- Lobby UI: Display of all players with ready status, connection status, and bound controller.
  - The frontend updates this view via polling every 1 second to simulate real-time updates.
  - Additionally, a refresh button enables manual, immediate updates for users who want faster response.

---

## Controllers and Connection Monitoring

- **Hardware Controller (ESP32/Arduino)**:
  - 4 buttons for answers, 4 LEDs for status/feedback, OLED display, RFID reader, WiFi connectivity.
  - Display of player name, ready status, game status, own points, and error messages (e.g., unknown RFID card).

- **Web Controller (Browser)**:
  - UI accessible via separate URL (e.g., `/controller`)
    - Generates a random controller ID for each open instance.
  - Display of player name and status, 4 large answer buttons (locked after answering until the next question starts), and ready toggle.
  - Display of own points after each question.

- **Connection Monitoring**:
  - Heartbeat (ping/pong) approximately every 10 seconds; if two heartbeats are missed, the controller is marked as disconnected.
  - Before each new question, a pre-question ping is sent: controllers that do not respond within 3 seconds are removed from active players for the rest of the game (status disconnected), without extending the round rhythm.

---

## Scoring and Highscores

- Base points per correct answer: easy = 1, medium = 2, hard = 3 points; incorrect answers earn 0 points.
- Time factor (only for correct answers):
  - 0–5 s → 1.0
  - 5–10 s → 0.9
  - 10–15 s → 0.8
  - 15–20 s → 0.7
  - 20–25 s → 0.6
  - 25–30 s → 0.5
  - 30 s and above → 0.0
- Points per question: `Points = Base Points × Time Factor`; for timeouts or answers at 30 s and beyond, always 0 points.
  - Example: Hard question (3 points), answered after 8 seconds: 3 × 0.9 = 2.7 points.
  - Values should not be rounded.
- Per mode, the top 20 best results (not unique players) are maintained in descending score order and are visible in the UI.
  - A player can appear multiple times in the list if they play multiple games.
  - Sorting: first by score descending, on tie by timestamp.

---

## Error Handling

The following scenarios must be handled:

- **Controller Disconnect**: Players remain visible in the lobby but no longer participate in the game. Points are retained. Game flow is not interrupted.
- **RFID Error**: Unknown tags display an error message on the display. Duplicate RFID IDs are rejected by the backend.
- **Multiple Logins**: An account may only be active once. Decide whether a new login displaces the old one or is rejected – and document this decision.
- **Timeouts**: Answers after 30 seconds do not count. Buttons are locked until the next question starts.
- **Invalid Question Pool**: If too few questions are available, the game does not start and displays an error message in the lobby.

---

## Technical Framework

- Backend provides REST endpoints for registration/login, lobby status, controller list, game configuration, and highscores.
- MQTT is used for all game events: controller registration, ready events, questions, answers, feedback, game state, and ping/heartbeat.
- Development environment: local MQTT broker (e.g., Mosquitto via Docker Compose); for hardware tests, a university broker that can be switched via configuration.
- All persistent data (accounts, questions, highscores) are stored in a MariaDB database.
  - Questions are imported into the system via seeds/migrations.
  - Highscores are stored permanently per mode and are visible across sessions.
  - An admin/management UI is not necessary!

Further technical details (API proposals, MQTT topics, database schema) can be found in the README of the repository and under /docs.
For questions: https://matrix.to/#/#GEN1002:thm.de