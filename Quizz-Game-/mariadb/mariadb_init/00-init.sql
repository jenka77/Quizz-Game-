-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- Initial Schema & Configuration
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- UTILITY TABLE (optional)
-- ============================================

CREATE TABLE IF NOT EXISTS objects (
  id INT AUTO_INCREMENT PRIMARY KEY,
  message VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================
-- USERS / AUTH
-- ============================================

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(80) NULL,
  rfid_uid VARCHAR(64) NULL, -- optional (RFID)
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_rfid_uid (rfid_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================
-- QUESTION CATALOG
-- ============================================

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS questions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_id BIGINT UNSIGNED NOT NULL,
  difficulty ENUM('EASY','MEDIUM','HARD') NOT NULL,
  question_text TEXT NOT NULL,
  correct_option ENUM('A','B','C','D') NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_questions_cat_diff (category_id, difficulty),
  CONSTRAINT fk_questions_category
    FOREIGN KEY (category_id) REFERENCES categories(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS question_options (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id BIGINT UNSIGNED NOT NULL,
  option_letter ENUM('A','B','C','D') NOT NULL,
  option_text VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_qopts_question_letter (question_id, option_letter),
  KEY idx_qopts_question (question_id),
  CONSTRAINT fk_qopts_question
    FOREIGN KEY (question_id) REFERENCES questions(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================
-- SCORING CONFIGURATION
-- ============================================

CREATE TABLE IF NOT EXISTS scoring_time_buckets (
  id TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
  bucket_code ENUM('T0_5','T5_10','T10_15','T15_20','T20_25','T25_30','T30P') NOT NULL,
  min_ms INT UNSIGNED NOT NULL,
  max_ms INT UNSIGNED NULL, -- NULL = open end
  factor DECIMAL(4,2) NOT NULL, -- 1.00, 0.90, ...
  PRIMARY KEY (id),
  UNIQUE KEY uq_bucket_code (bucket_code),
  UNIQUE KEY uq_bucket_range (min_ms, max_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Time bucket seed (idempotent via INSERT IGNORE)
INSERT IGNORE INTO scoring_time_buckets (bucket_code, min_ms, max_ms, factor) VALUES
('T0_5',    0,  5000, 1.00),
('T5_10',   5000, 10000, 0.90),
('T10_15',  10000, 15000, 0.80),
('T15_20',  15000, 20000, 0.70),
('T20_25',  20000, 25000, 0.60),
('T25_30',  25000, 30000, 0.50),
('T30P',    30000, NULL, 0.00);

-- ============================================
-- ACTIVE SESSIONS (multi-user lobby)
-- ============================================
CREATE TABLE IF NOT EXISTS active_sessions (
  user_id BIGINT UNSIGNED NOT NULL,
  auth_token VARCHAR(96) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (auth_token),
  KEY idx_active_sessions_user (user_id),
  CONSTRAINT fk_active_sessions_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================
-- HARDWARE CONTROLLERS (optional)
-- ============================================

CREATE TABLE IF NOT EXISTS controllers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  controller_id VARCHAR(128) NOT NULL,
  controller_type ENUM('WEB','HARDWARE') NOT NULL,
  status ENUM('FREE','ASSIGNED','OFFLINE') NOT NULL DEFAULT 'OFFLINE',
  last_seen_at TIMESTAMP NULL DEFAULT NULL,
  assigned_user_id BIGINT UNSIGNED NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_controllers_controller_id (controller_id),
  KEY idx_controllers_status (status),
  KEY idx_controllers_assigned_user (assigned_user_id),
  CONSTRAINT fk_controllers_user
    FOREIGN KEY (assigned_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================
-- GAME SESSIONS / ROUNDS
-- ============================================

CREATE TABLE IF NOT EXISTS game_sessions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  host_user_id BIGINT UNSIGNED NULL,
  round_length ENUM('Q5','Q10','Q20') NOT NULL,
  allow_easy TINYINT(1) NOT NULL DEFAULT 1,
  allow_medium TINYINT(1) NOT NULL DEFAULT 1,
  allow_hard TINYINT(1) NOT NULL DEFAULT 1,
  state ENUM('LOBBY','COUNTDOWN','QUESTION','EVALUATION','ENDED','ABORTED') NOT NULL DEFAULT 'LOBBY',
  started_at TIMESTAMP NULL DEFAULT NULL,
  ended_at TIMESTAMP NULL DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_game_sessions_state (state),
  KEY idx_game_sessions_host (host_user_id),
  CONSTRAINT fk_game_sessions_host
    FOREIGN KEY (host_user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS game_session_categories (
  game_session_id BIGINT UNSIGNED NOT NULL,
  category_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (game_session_id, category_id),
  CONSTRAINT fk_gsc_session
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_gsc_category
    FOREIGN KEY (category_id) REFERENCES categories(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS game_session_players (
  game_session_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  controller_id BIGINT UNSIGNED NULL,
  is_ready TINYINT(1) NOT NULL DEFAULT 0,
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (game_session_id, user_id),
  KEY idx_gsp_controller (controller_id),
  CONSTRAINT fk_gsp_session
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_gsp_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_gsp_controller
    FOREIGN KEY (controller_id) REFERENCES controllers(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS game_session_questions (
  game_session_id BIGINT UNSIGNED NOT NULL,
  question_index INT UNSIGNED NOT NULL, -- 1..round_length
  question_id BIGINT UNSIGNED NOT NULL,
  asked_at TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (game_session_id, question_index),
  UNIQUE KEY uq_gsq_session_question (game_session_id, question_id),
  KEY idx_gsq_question (question_id),
  CONSTRAINT fk_gsq_session
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_gsq_question
    FOREIGN KEY (question_id) REFERENCES questions(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================
-- ANSWERS / RESULTS
-- ============================================

CREATE TABLE IF NOT EXISTS game_answers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  game_session_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  answered_option ENUM('A','B','C','D') NULL,
  response_time_ms INT UNSIGNED NULL,
  time_bucket ENUM('T0_5','T5_10','T10_15','T15_20','T20_25','T25_30','T30P') NULL,
  is_correct TINYINT(1) NOT NULL DEFAULT 0,
  points_awarded DECIMAL(6,2) NOT NULL DEFAULT 0.00,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_answer_once (game_session_id, user_id, question_id),
  KEY idx_answers_session (game_session_id),
  KEY idx_answers_user (user_id),
  KEY idx_answers_question (question_id),
  CONSTRAINT fk_ga_session
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ga_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ga_question
    FOREIGN KEY (question_id) REFERENCES questions(id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS game_session_results (
  game_session_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  total_points DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  total_response_time_ms BIGINT UNSIGNED NULL,
  correct_count INT UNSIGNED NOT NULL DEFAULT 0,
  answered_count INT UNSIGNED NOT NULL DEFAULT 0,
  computed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (game_session_id, user_id),
  KEY idx_gsr_points (total_points),
  CONSTRAINT fk_gsr_session
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_gsr_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================
-- HIGHSCORES
-- ============================================
-- Top 10 je Rundenlänge
-- Ranking: total_points DESC; tie-breaker: total_response_time_ms ASC
-- ============================================

CREATE TABLE IF NOT EXISTS highscores (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  round_length ENUM('Q5','Q10','Q20') NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  game_session_id BIGINT UNSIGNED NULL,
  total_points DECIMAL(10,2) NOT NULL,
  total_response_time_ms BIGINT UNSIGNED NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_highscores_round (round_length),
  KEY idx_highscores_rank (round_length, total_points, total_response_time_ms),
  KEY idx_highscores_user (user_id),
  CONSTRAINT fk_highscores_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_highscores_session
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ============================================
-- Schema initialization complete
-- ============================================
