# Datenbankschema (MariaDB)

Dieses Dokument beschreibt die Tabellen der MariaDB-Datenbank im Projekt **Quiz-Game** (Group 17).
Quelle: `mariadb/mariadb_init/00-init.sql` (Schema) und Seed-Dateien im selben Ordner.

## Überblick (ER-Relationen)

- `users` 1─∞ `active_sessions`
- `users` 1─∞ `controllers` (über `controllers.assigned_user_id`)
- `categories` 1─∞ `questions` 1─∞ `question_options`
- `users` 1─∞ `game_sessions` (Host über `game_sessions.host_user_id`)
- `game_sessions` ∞─∞ `categories` (über `game_session_categories`)
- `game_sessions` ∞─∞ `users` (über `game_session_players`)
- `game_sessions` 1─∞ `game_session_questions` → `questions`
- `game_sessions` 1─∞ `game_answers` (zusätzlich pro `user_id` und `question_id`)
- `game_sessions` 1─∞ `game_session_results` (pro `user_id`)
- `users` 1─∞ `highscores` (optional Referenz auf `game_sessions`)

## Tabellen

### `objects` (optional)

- **Zweck**: Utility-/Demo-Tabelle.
- **Spalten**
  - `id` INT, PK, AUTO_INCREMENT
  - `message` VARCHAR(255), NOT NULL
  - `created_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP

### `users`

- **Zweck**: User-Accounts (Login), optional RFID.
- **Spalten**
  - `id` BIGINT UNSIGNED, PK, AUTO_INCREMENT
  - `username` VARCHAR(64), UNIQUE, NOT NULL
  - `password_hash` VARCHAR(255), NOT NULL
  - `display_name` VARCHAR(80), NULL
  - `rfid_uid` VARCHAR(64), NULL, UNIQUE
  - `created_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
  - `updated_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### `categories`

- **Zweck**: Kategorien des Fragenkatalogs.
- **Spalten**
  - `id` BIGINT UNSIGNED, PK, AUTO_INCREMENT
  - `name` VARCHAR(64), UNIQUE, NOT NULL
  - `description` VARCHAR(255), NULL
  - `created_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP

### `questions`

- **Zweck**: Fragen (mit Schwierigkeit und korrekter Option).
- **Spalten**
  - `id` BIGINT UNSIGNED, PK, AUTO_INCREMENT
  - `category_id` BIGINT UNSIGNED, FK → `categories.id`, NOT NULL
  - `difficulty` ENUM('EASY','MEDIUM','HARD'), NOT NULL
  - `question_text` TEXT, NOT NULL
  - `correct_option` ENUM('A','B','C','D'), NOT NULL
  - `is_active` TINYINT(1), DEFAULT 1
  - `created_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
  - `updated_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- **Indizes**
  - `idx_questions_cat_diff (category_id, difficulty)`

### `question_options`

- **Zweck**: Antwort-Optionen A–D pro Frage.
- **Spalten**
  - `id` BIGINT UNSIGNED, PK, AUTO_INCREMENT
  - `question_id` BIGINT UNSIGNED, FK → `questions.id`, NOT NULL
  - `option_letter` ENUM('A','B','C','D'), NOT NULL
  - `option_text` VARCHAR(255), NOT NULL
- **Constraints**
  - UNIQUE `uq_qopts_question_letter (question_id, option_letter)`

### `scoring_time_buckets`

- **Zweck**: Zeit-Buckets für Score-Faktor (Antwortzeit → Multiplikator).
- **Spalten**
  - `id` TINYINT UNSIGNED, PK, AUTO_INCREMENT
  - `bucket_code` ENUM('T0_5','T5_10','T10_15','T15_20','T20_25','T25_30','T30P'), UNIQUE
  - `min_ms` INT UNSIGNED, NOT NULL
  - `max_ms` INT UNSIGNED, NULL (NULL = Open End)
  - `factor` DECIMAL(4,2), NOT NULL

### `active_sessions`

- **Zweck**: Aktive Login-Sessions (Multi-User Lobby).
- **Spalten**
  - `user_id` BIGINT UNSIGNED, FK → `users.id`, NOT NULL
  - `auth_token` VARCHAR(96), PK, NOT NULL
  - `created_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
- **Indizes**
  - `idx_active_sessions_user (user_id)`

### `controllers`

- **Zweck**: Controller-Registry (Web/Hardware), Zuordnung zu Usern.
- **Spalten**
  - `id` BIGINT UNSIGNED, PK, AUTO_INCREMENT
  - `controller_id` VARCHAR(128), UNIQUE, NOT NULL
  - `controller_type` ENUM('WEB','HARDWARE'), NOT NULL
  - `status` ENUM('FREE','ASSIGNED','OFFLINE'), DEFAULT 'OFFLINE'
  - `last_seen_at` TIMESTAMP, NULL
  - `assigned_user_id` BIGINT UNSIGNED, FK → `users.id`, NULL
  - `created_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
  - `updated_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- **Indizes**
  - `idx_controllers_status (status)`
  - `idx_controllers_assigned_user (assigned_user_id)`

### `game_sessions`

- **Zweck**: Spielrunden / Sessions (Konfiguration + Status).
- **Spalten**
  - `id` BIGINT UNSIGNED, PK, AUTO_INCREMENT
  - `host_user_id` BIGINT UNSIGNED, FK → `users.id`, NULL
  - `round_length` ENUM('Q5','Q10','Q20'), NOT NULL
  - `allow_easy` TINYINT(1), DEFAULT 1
  - `allow_medium` TINYINT(1), DEFAULT 1
  - `allow_hard` TINYINT(1), DEFAULT 1
  - `state` ENUM('LOBBY','COUNTDOWN','QUESTION','EVALUATION','ENDED','ABORTED'), DEFAULT 'LOBBY'
  - `started_at` TIMESTAMP, NULL
  - `ended_at` TIMESTAMP, NULL
  - `created_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
  - `updated_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- **Indizes**
  - `idx_game_sessions_state (state)`
  - `idx_game_sessions_host (host_user_id)`

### `game_session_categories`

- **Zweck**: Auswahl der Kategorien pro Session.
- **Spalten**
  - `game_session_id` BIGINT UNSIGNED, FK → `game_sessions.id`, PK-Teil
  - `category_id` BIGINT UNSIGNED, FK → `categories.id`, PK-Teil
- **PK**
  - `(game_session_id, category_id)`

### `game_session_players`

- **Zweck**: Spieler-Teilnahme an einer Session.
- **Spalten**
  - `game_session_id` BIGINT UNSIGNED, FK → `game_sessions.id`, PK-Teil
  - `user_id` BIGINT UNSIGNED, FK → `users.id`, PK-Teil
  - `controller_id` BIGINT UNSIGNED, FK → `controllers.id`, NULL
  - `is_ready` TINYINT(1), DEFAULT 0
  - `joined_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
- **Indizes**
  - `idx_gsp_controller (controller_id)`

### `game_session_questions`

- **Zweck**: Reihenfolge der Fragen in einer Session.
- **Spalten**
  - `game_session_id` BIGINT UNSIGNED, FK → `game_sessions.id`, PK-Teil
  - `question_index` INT UNSIGNED, PK-Teil (1..round_length)
  - `question_id` BIGINT UNSIGNED, FK → `questions.id`, NOT NULL
  - `asked_at` TIMESTAMP, NULL
- **Constraints**
  - UNIQUE `uq_gsq_session_question (game_session_id, question_id)`

### `game_answers`

- **Zweck**: Antworten pro Spieler und Frage (inkl. Antwortzeit + Punkte).
- **Spalten**
  - `id` BIGINT UNSIGNED, PK, AUTO_INCREMENT
  - `game_session_id` BIGINT UNSIGNED, FK → `game_sessions.id`, NOT NULL
  - `user_id` BIGINT UNSIGNED, FK → `users.id`, NOT NULL
  - `question_id` BIGINT UNSIGNED, FK → `questions.id`, NOT NULL
  - `answered_option` ENUM('A','B','C','D'), NULL
  - `response_time_ms` INT UNSIGNED, NULL
  - `time_bucket` ENUM('T0_5','T5_10','T10_15','T15_20','T20_25','T25_30','T30P'), NULL
  - `is_correct` TINYINT(1), DEFAULT 0
  - `points_awarded` DECIMAL(6,2), DEFAULT 0.00
  - `created_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
- **Constraints**
  - UNIQUE `uq_answer_once (game_session_id, user_id, question_id)`
- **Indizes**
  - `idx_answers_session (game_session_id)`
  - `idx_answers_user (user_id)`
  - `idx_answers_question (question_id)`

### `game_session_results`

- **Zweck**: Aggregierte Ergebnisse pro Spieler und Session.
- **Spalten**
  - `game_session_id` BIGINT UNSIGNED, FK → `game_sessions.id`, PK-Teil
  - `user_id` BIGINT UNSIGNED, FK → `users.id`, PK-Teil
  - `total_points` DECIMAL(10,2), DEFAULT 0.00
  - `total_response_time_ms` BIGINT UNSIGNED, NULL
  - `correct_count` INT UNSIGNED, DEFAULT 0
  - `answered_count` INT UNSIGNED, DEFAULT 0
  - `computed_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
- **Indizes**
  - `idx_gsr_points (total_points)`

### `highscores`

- **Zweck**: Highscore-Einträge (Top 20 pro Rundenlänge, konfigurierbar via API-Parameter `limit`).
- **Spalten**
  - `id` BIGINT UNSIGNED, PK, AUTO_INCREMENT
  - `round_length` ENUM('Q5','Q10','Q20'), NOT NULL
  - `user_id` BIGINT UNSIGNED, FK → `users.id`, NOT NULL
  - `game_session_id` BIGINT UNSIGNED, FK → `game_sessions.id`, NULL
  - `total_points` DECIMAL(10,2), NOT NULL
  - `total_response_time_ms` BIGINT UNSIGNED, NULL
  - `created_at` TIMESTAMP, DEFAULT CURRENT_TIMESTAMP
- **Indizes**
  - `idx_highscores_round (round_length)`
  - `idx_highscores_rank (round_length, total_points, total_response_time_ms)`
  - `idx_highscores_user (user_id)`

