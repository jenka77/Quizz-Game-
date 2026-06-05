-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 04-seed-questions-web.sql
-- Web-Technologien (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 3: WEB-TECHNOLOGIEN (ID 3)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(13, 3, 'EASY', 'Wofür steht HTTP?', 'A', 1),
(14, 3, 'EASY', 'Welcher HTTP-Status-Code signalisiert einen erfolgreichen Request?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q13 (HTTP)
(49, 13, 'A', 'Hypertext Transfer Protocol'),
(50, 13, 'B', 'High Throughput Transfer Process'),
(51, 13, 'C', 'Host Transfer Protocol'),
(52, 13, 'D', 'Hyperlink Transmission Package'),
-- Q14 (Status 200)
(53, 14, 'A', '301 Moved Permanently'),
(54, 14, 'B', '200 OK'),
(55, 14, 'C', '404 Not Found'),
(56, 14, 'D', '500 Internal Server Error');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(15, 3, 'MEDIUM', 'Wofür wird CORS (Cross-Origin Resource Sharing) primär benötigt?', 'D', 1),
(16, 3, 'MEDIUM', 'Was ist REST (Representational State Transfer) im Web?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q15 (CORS)
(57, 15, 'A', 'Um HTML schneller im Browser zu rendern'),
(58, 15, 'B', 'Um Cookies automatisch zu deaktivieren'),
(59, 15, 'C', 'Um SQL-Injection auf dem Server zu verhindern'),
(60, 15, 'D', 'Um Cross-Origin Requests kontrolliert zu erlauben/zu verbieten'),
-- Q16 (REST)
(61, 16, 'A', 'Ein Datenbank-Abfrage-Standard'),
(62, 16, 'B', 'Ein Architektur-Stil für Web-APIs mit Ressourcen, HTTP-Methoden und standardisierten URLs'),
(63, 16, 'C', 'Ein Framework zur Client-seitigen Datenverschlüsselung'),
(64, 16, 'D', 'Ein SSL/TLS-Zertifikatsformat');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(17, 3, 'HARD', 'Was ist die Same-Origin-Policy im Browser?', 'B', 1),
(18, 3, 'HARD', 'Welcher HTTP-Header kontrolliert das Caching-Verhalten im Browser?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q17 (Same-Origin)
(65, 17, 'A', 'Ein HTTP-Header zur Kompression von Ressourcen'),
(66, 17, 'B', 'Sicherheits-Regel des Browsers: Skripte dürfen nur auf Ressourcen mit gleicher Origin zugreifen'),
(67, 17, 'C', 'Ein Server-seitiger Datenbankindex'),
(68, 17, 'D', 'Ein MQTT-Topic-Präfix für IoT-Geräte'),
-- Q18 (Cache-Control)
(69, 18, 'A', 'Cache-Control'),
(70, 18, 'B', 'Content-Type'),
(71, 18, 'C', 'Authorization'),
(72, 18, 'D', 'Access-Control-Allow-Origin');

-- Erweiterung: +1 EASY, +1 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(67, 3, 'EASY', 'Welche Sprache wird direkt im Browser für Interaktivität ausgeführt?', 'B', 1),
(68, 3, 'MEDIUM', 'Welcher HTTP-Statuscode steht für Unauthorized?', 'C', 1),
(69, 3, 'HARD', 'Was bedeutet idempotent bei HTTP-Methoden?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q67
(265, 67, 'A', 'SQL'),
(266, 67, 'B', 'JavaScript'),
(267, 67, 'C', 'Bash'),
(268, 67, 'D', 'YAML'),
-- Q68
(269, 68, 'A', '200'),
(270, 68, 'B', '201'),
(271, 68, 'C', '401'),
(272, 68, 'D', '500'),
-- Q69
(273, 69, 'A', 'Mehrfaches Ausführen hat denselben fachlichen Effekt wie einmaliges Ausführen'),
(274, 69, 'B', 'Die Methode ist immer sicher und ohne Nebenwirkungen'),
(275, 69, 'C', 'Die Methode darf nur per HTTPS genutzt werden'),
(276, 69, 'D', 'Die Methode ist nur für JSON-Responses erlaubt');

-- Mockup-Verteilung fuer Kategorie 3: (5,1,1)
-- Zusatz: +2 EASY
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(100, 3, 'EASY', 'Welches HTML-Element verlinkt eine externe CSS-Datei?', 'A', 1),
(101, 3, 'EASY', 'Welches Protokoll wird für verschlüsseltes HTTP verwendet?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(397, 100, 'A', '<link>'),
(398, 100, 'B', '<style>'),
(399, 100, 'C', '<script>'),
(400, 100, 'D', '<meta>'),
(401, 101, 'A', 'FTP'),
(402, 101, 'B', 'SMTP'),
(403, 101, 'C', 'SSH'),
(404, 101, 'D', 'HTTPS');

-- Reduktion auf Zielwerte (MEDIUM=1, HARD=1)
UPDATE questions SET is_active = 0 WHERE id IN (16, 68, 18, 69);

-- Normalisierung: exakt aktive Fragen gemaess Zielverteilung halten
UPDATE questions
SET is_active = 0
WHERE category_id = 3
  AND id NOT IN (13, 14, 67, 100, 101, 15, 17);

UPDATE questions
SET is_active = 1
WHERE id IN (13, 14, 67, 100, 101, 15, 17);

-- Verify insertion
SELECT COUNT(*) as web_questions FROM questions WHERE category_id = 3 AND is_active = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 3 AND is_active = 1 GROUP BY difficulty;
