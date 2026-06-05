-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 06-seed-questions-os.sql
-- Betriebssysteme (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 5: BETRIEBSSYSTEME (ID 5)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(25, 5, 'EASY', 'Was ist ein Prozess in einem Betriebssystem?', 'B', 1),
(26, 5, 'EASY', 'Welcher der folgenden ist ein modernes Desktop-Betriebssystem?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q25 (Prozess)
(97, 25, 'A', 'Eine Datei im Dateisystem'),
(98, 25, 'B', 'Ein Programm in Ausführung mit eigenem Adressraum und Ressourcen'),
(99, 25, 'C', 'Ein Netzwerkpaket'),
(100, 25, 'D', 'Ein CPU-Register'),
-- Q26 (OS)
(101, 26, 'A', 'MS-DOS'),
(102, 26, 'B', 'Windows 3.1'),
(103, 26, 'C', 'AmigaOS'),
(104, 26, 'D', 'Linux oder Windows 10/11');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(27, 5, 'MEDIUM', 'Wofür wird ein Mutex (Mutual Exclusion Lock) typischerweise verwendet?', 'A', 1),
(28, 5, 'MEDIUM', 'Was ist Scheduling im Kontext eines Betriebssystems?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q27 (Mutex)
(105, 27, 'A', 'Schutz kritischer Abschnitte (gegenseitiger Ausschluss zwischen Threads)'),
(106, 27, 'B', 'Schnelleres Kompilieren von Programmen'),
(107, 27, 'C', 'DNS-Auflösung von Domainnamen'),
(108, 27, 'D', 'Komprimierung von Dateien'),
-- Q28 (Scheduling)
(109, 28, 'A', 'Das Ändern von Datei-Ownerships'),
(110, 28, 'B', 'Das Erstellen von Backups'),
(111, 28, 'C', 'Die Verwaltung und Vergabe von CPU-Zeit an Prozesse/Threads'),
(112, 28, 'D', 'Die Synchronisation von Netzwerk-Uhren');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(29, 5, 'HARD', 'Was ist ein Deadlock in der Prozess-Synchronisation?', 'C', 1),
(30, 5, 'HARD', 'Was ist die virtuelle Speicherverwaltung?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q29 (Deadlock)
(113, 29, 'A', 'Ein Speicherleck, das zu RAM-Erschöpfung führt'),
(114, 29, 'B', 'Ein schneller Kontextwechsel zwischen Prozessen'),
(115, 29, 'C', 'Wechselseitiges Warten blockierter Prozesse/Threads auf Ressourcen - ein Stillstand'),
(116, 29, 'D', 'Ein Timeout bei der Ping-Kommunikation'),
-- Q30 (Virtual Memory)
(117, 30, 'A', 'Auslagerung von Prozessen auf andere Rechner'),
(118, 30, 'B', 'Die Abstraktion des physischen Speichers durch Paging/Segmentierung - ermöglicht größere Adressräume'),
(119, 30, 'C', 'Das Verschlüsseln des RAM-Inhalts'),
(120, 30, 'D', 'Ein Datenbankfeature zur Speicheroptimierung');

-- Erweiterung: +1 EASY, +1 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(73, 5, 'EASY', 'Was macht der Scheduler im Betriebssystem?', 'A', 1),
(74, 5, 'MEDIUM', 'Wozu dient Paging im virtuellen Speicher?', 'C', 1),
(75, 5, 'HARD', 'Was ist ein Race Condition Problem?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q73
(289, 73, 'A', 'Er verteilt CPU-Zeit auf Prozesse/Threads'),
(290, 73, 'B', 'Er speichert Dateien dauerhaft auf SSD'),
(291, 73, 'C', 'Er verschlüsselt den Hauptspeicher'),
(292, 73, 'D', 'Er ersetzt den Kernel beim Start'),
-- Q74
(293, 74, 'A', 'Es vergrößert nur den CPU-Cache'),
(294, 74, 'B', 'Es deaktiviert virtuellen Speicher'),
(295, 74, 'C', 'Es teilt Speicher in Seiten auf und mappt virtuell auf physisch'),
(296, 74, 'D', 'Es ist nur für Datenbanken relevant'),
-- Q75
(297, 75, 'A', 'Ein geplanter Neustart des Systems'),
(298, 75, 'B', 'Ein Fehler durch konkurrierenden Zugriff ohne korrekte Synchronisation'),
(299, 75, 'C', 'Eine Form von Dateisystemfragmentierung'),
(300, 75, 'D', 'Ein Hardwaredefekt im RAM');

-- Mockup-Verteilung fuer Kategorie 5: (3,6,4)
-- Zusatz: +3 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(113, 5, 'MEDIUM', 'Welche Aussage beschreibt einen Kontextwechsel korrekt?', 'C', 1),
(114, 5, 'MEDIUM', 'Was ist ein Vorteil von Preemptive Scheduling?', 'A', 1),
(115, 5, 'MEDIUM', 'Welche Rolle hat der Kernel im Betriebssystem?', 'D', 1),
(116, 5, 'HARD', 'Was kann Thrashing im virtuellen Speicher auslösen?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(449, 113, 'A', 'Das Formatieren einer Festplatte'),
(450, 113, 'B', 'Das Kompilieren des Kernels'),
(451, 113, 'C', 'Speichern und Wiederherstellen von CPU-Zustand zwischen Tasks'),
(452, 113, 'D', 'Umschalten zwischen Dateisystemen'),
(453, 114, 'A', 'Kurze Tasks können schneller reagieren, weil unterbrochen werden darf'),
(454, 114, 'B', 'Es gibt keine Interrupts mehr'),
(455, 114, 'C', 'Nur ein Prozess darf laufen'),
(456, 114, 'D', 'Scheduling ist nicht mehr nötig'),
(457, 115, 'A', 'Nur Benutzeroberfläche anzeigen'),
(458, 115, 'B', 'Nur Netzwerkports öffnen'),
(459, 115, 'C', 'Nur Logs schreiben'),
(460, 115, 'D', 'Zentrale Steuerung von Ressourcen, Prozessen und Hardwarezugriff'),
(461, 116, 'A', 'Zu viele freie Frames'),
(462, 116, 'B', 'Sehr viele Page Faults durch zu wenig nutzbaren Hauptspeicher'),
(463, 116, 'C', 'Nur SSD statt HDD'),
(464, 116, 'D', 'Deaktivierte Prozessplanung');

-- Normalisierung: exakt aktive Fragen gemaess Zielverteilung halten
UPDATE questions
SET is_active = 0
WHERE category_id = 5
  AND id NOT IN (25, 26, 73, 27, 28, 74, 113, 114, 115, 29, 30, 75, 116);

UPDATE questions
SET is_active = 1
WHERE id IN (25, 26, 73, 27, 28, 74, 113, 114, 115, 29, 30, 75, 116);

-- Verify insertion
SELECT COUNT(*) as os_questions FROM questions WHERE category_id = 5 AND is_active = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 5 AND is_active = 1 GROUP BY difficulty;
