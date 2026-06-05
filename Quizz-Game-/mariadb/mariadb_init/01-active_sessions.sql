-- Migration: add active_sessions for multi-user lobby
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
