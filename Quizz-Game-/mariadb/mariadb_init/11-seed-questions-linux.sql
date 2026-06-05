-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 11-seed-questions-linux.sql
-- Linux & Tools (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 10: LINUX & TOOLS (ID 10)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(55, 10, 'EASY', 'Welcher Befehl listet Dateien in einem Verzeichnis auf?', 'A', 1),
(56, 10, 'EASY', 'Welcher Befehl zeigt das aktuelle Arbeitsverzeichnis an?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q55 (ls)
(217, 55, 'A', 'ls'),
(218, 55, 'B', 'cd'),
(219, 55, 'C', 'pwd'),
(220, 55, 'D', 'rm'),
-- Q56 (pwd)
(221, 56, 'A', 'ls'),
(222, 56, 'B', 'cd'),
(223, 56, 'C', 'pwd'),
(224, 56, 'D', 'cat');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(57, 10, 'MEDIUM', 'Was bewirkt der Befehl „chmod 755 datei"?', 'C', 1),
(58, 10, 'MEDIUM', 'Welches Tool wird für die Versionskontrolle moderner Software-Projekte verwendet?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q57 (chmod)
(225, 57, 'A', 'Setzt nur Leserechte für alle'),
(226, 57, 'B', 'Macht die Datei unsichtbar'),
(227, 57, 'C', 'Owner: rwx, Gruppe: r-x, Andere: r-x (755 = rwxr-xr-x)'),
(228, 57, 'D', 'Löscht Ausführrechte für den Owner'),
-- Q58 (Git)
(229, 58, 'A', 'Git'),
(230, 58, 'B', 'Subversion (SVN)'),
(231, 58, 'C', 'Mercurial (Hg)'),
(232, 58, 'D', 'CVS');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(59, 10, 'HARD', 'Was ist ein Docker-Image?', 'B', 1),
(60, 10, 'HARD', 'Welcher Linux-Kernel-Mechanismus wird von Docker zur Isolation verwendet?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q59 (Docker Image)
(233, 59, 'A', 'Ein laufender Container mit aktiven Prozessen'),
(234, 59, 'B', 'Ein unveränderliches Paket/Template mit allen Abhängigkeiten zur Container-Erzeugung'),
(235, 59, 'C', 'Ein Docker-Netzwerkinterface'),
(236, 59, 'D', 'Ein Volume mit Datenbankdaten'),
-- Q60 (Containers)
(237, 60, 'A', 'Virtual Machines (VMs)'),
(238, 60, 'B', 'Hypervisor-Technologie'),
(239, 60, 'C', 'SSH-Tunnel'),
(240, 60, 'D', 'Namespaces und cgroups - Kernel-Features zur Prozess- und Ressourcen-Isolation');

-- Erweiterung: +1 EASY, +1 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(88, 10, 'EASY', 'Welcher Befehl erstellt ein neues Verzeichnis?', 'C', 1),
(89, 10, 'MEDIUM', 'Was zeigt der Befehl "ps" typischerweise an?', 'B', 1),
(90, 10, 'HARD', 'Wofür wird Docker Compose hauptsächlich verwendet?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q88
(349, 88, 'A', 'rmdir'),
(350, 88, 'B', 'touch'),
(351, 88, 'C', 'mkdir'),
(352, 88, 'D', 'cp'),
-- Q89
(353, 89, 'A', 'Nur Netzwerkports'),
(354, 89, 'B', 'Laufende Prozesse'),
(355, 89, 'C', 'Dateisystemrechte'),
(356, 89, 'D', 'Container-Images'),
-- Q90
(357, 90, 'A', 'Nur Java-Projekte lokal zu kompilieren'),
(358, 90, 'B', 'Dateiberechtigungen unter Linux zu verwalten'),
(359, 90, 'C', 'SQL-Migrationen in MariaDB automatisch zu patchen'),
(360, 90, 'D', 'Mehrere Container-Services gemeinsam zu definieren und zu starten');

-- Mockup-Verteilung fuer Kategorie 10: (9,3,2)
-- Zusatz: +6 EASY
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(134, 10, 'EASY', 'Welcher Befehl zeigt den Inhalt einer Textdatei an?', 'A', 1),
(135, 10, 'EASY', 'Wozu dient der Befehl cd?', 'D', 1),
(136, 10, 'EASY', 'Welches Zeichen steht in der Shell oft für das Home-Verzeichnis?', 'B', 1),
(137, 10, 'EASY', 'Welcher Befehl verschiebt oder benennt Dateien um?', 'C', 1),
(138, 10, 'EASY', 'Welcher Befehl erstellt eine leere Datei?', 'A', 1),
(139, 10, 'EASY', 'Welcher Befehl entfernt ein leeres Verzeichnis?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(533, 134, 'A', 'cat'),
(534, 134, 'B', 'pwd'),
(535, 134, 'C', 'ps'),
(536, 134, 'D', 'grep'),
(537, 135, 'A', 'Dateien auflisten'),
(538, 135, 'B', 'Dateiinhalt anzeigen'),
(539, 135, 'C', 'Prozesse beenden'),
(540, 135, 'D', 'Ins Verzeichnis wechseln'),
(541, 136, 'A', '/'),
(542, 136, 'B', '~'),
(543, 136, 'C', '#'),
(544, 136, 'D', '*'),
(545, 137, 'A', 'cp'),
(546, 137, 'B', 'ls'),
(547, 137, 'C', 'mv'),
(548, 137, 'D', 'chmod'),
(549, 138, 'A', 'touch'),
(550, 138, 'B', 'mkdir'),
(551, 138, 'C', 'rmdir'),
(552, 138, 'D', 'rm'),
(553, 139, 'A', 'rm -rf'),
(554, 139, 'B', 'touch'),
(555, 139, 'C', 'pwd'),
(556, 139, 'D', 'rmdir');

-- Reduktion auf Zielwerte (HARD=2)
UPDATE questions SET is_active = 0 WHERE id = 90;

-- Normalisierung: exakt aktive Fragen gemaess Zielverteilung halten
UPDATE questions
SET is_active = 0
WHERE category_id = 10
  AND id NOT IN (55, 56, 88, 134, 135, 136, 137, 138, 139, 57, 58, 89, 59, 60);

UPDATE questions
SET is_active = 1
WHERE id IN (55, 56, 88, 134, 135, 136, 137, 138, 139, 57, 58, 89, 59, 60);

-- Verify insertion
SELECT COUNT(*) as linux_questions FROM questions WHERE category_id = 10 AND is_active = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 10 AND is_active = 1 GROUP BY difficulty;

-- ============================================
-- SUMMARY: Total questions per category
-- ============================================
SELECT 
  c.id,
  c.name,
  COUNT(q.id) as total_questions,
  SUM(CASE WHEN q.difficulty = 'EASY' THEN 1 ELSE 0 END) as easy_count,
  SUM(CASE WHEN q.difficulty = 'MEDIUM' THEN 1 ELSE 0 END) as medium_count,
  SUM(CASE WHEN q.difficulty = 'HARD' THEN 1 ELSE 0 END) as hard_count
FROM categories c
LEFT JOIN questions q ON c.id = q.category_id
  AND q.is_active = 1
GROUP BY c.id, c.name
ORDER BY c.id;
