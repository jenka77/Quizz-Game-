-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 12-seed-users.sql
-- Demo Users
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- INSERT DEMO USERS
-- Password: test1234 (SHA2-256 hashed)
-- ============================================

INSERT IGNORE INTO users (id, username, password_hash, display_name) VALUES
(1, 'alice', SHA2('test1234', 256), 'Alice'),
(2, 'bob', SHA2('test1234', 256), 'Bob'),
(3, 'charlie', SHA2('test1234', 256), 'Charlie'),
(4, 'diana', SHA2('test1234', 256), 'Diana'),
(5, 'bernd', SHA2('test1234', 256), 'Bernd');

-- Verify insertion
SELECT COUNT(*) as user_count FROM users;
