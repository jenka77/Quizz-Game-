-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 03-seed-questions-db.sql
-- Datenbanken (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 2: DATENBANKEN (ID 2)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(7, 2, 'EASY', 'Wofür steht SQL?', 'D', 1),
(8, 2, 'EASY', 'Welcher SQL-Befehl wird verwendet, um Daten aus einer Tabelle abzurufen?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q7 (SQL)
(25, 7, 'A', 'Secure Query Language'),
(26, 7, 'B', 'Standard Query Link'),
(27, 7, 'C', 'System Question List'),
(28, 7, 'D', 'Structured Query Language'),
-- Q8 (SELECT)
(29, 8, 'A', 'SELECT'),
(30, 8, 'B', 'FETCH'),
(31, 8, 'C', 'RETRIEVE'),
(32, 8, 'D', 'QUERY');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(9, 2, 'MEDIUM', 'Was ist ein Foreign Key (FK) in einer Datenbank?', 'B', 1),
(10, 2, 'MEDIUM', 'Welche Normalform beschreibt das Entfernen von Redundanzen innerhalb von Attributen?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q9 (Foreign Key)
(33, 9, 'A', 'Ein Index auf einer Tabelle zur Beschleunigung von Abfragen'),
(34, 9, 'B', 'Ein Feld, das auf einen Datensatz (Primärschlüssel) in einer anderen Tabelle verweist'),
(35, 9, 'C', 'Ein SQL-Statement zum Löschen von Einträgen'),
(36, 9, 'D', 'Eine komplette Kopie einer Datenbank'),
-- Q10 (2NF)
(37, 10, 'A', 'Erste Normalform (1NF)'),
(38, 10, 'B', 'Zweite Normalform (2NF)'),
(39, 10, 'C', 'Dritte Normalform (3NF)'),
(40, 10, 'D', 'Boyce-Codd Normalform (BCNF)');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(11, 2, 'HARD', 'Welche Normalform eliminiert transitive Abhängigkeiten vollständig?', 'C', 1),
(12, 2, 'HARD', 'Was beschreibt das CAP-Theorem in verteilten Datenbanksystemen?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q11 (3NF)
(41, 11, 'A', '1NF - Atomic Values'),
(42, 11, 'B', '2NF - No Partial Dependencies'),
(43, 11, 'C', '3NF - No Transitive Dependencies'),
(44, 11, 'D', 'BCNF - Stricter than 3NF'),
-- Q12 (CAP)
(45, 12, 'A', 'Consistency, Availability, Partition Tolerance - man kann maximal 2 erfüllen'),
(46, 12, 'B', 'Cache, Archive, Persistence - drei Datenspeicherschichten'),
(47, 12, 'C', 'Concurrency, Access, Performance - Messkriterien für Datenbanken'),
(48, 12, 'D', 'Clustering, Aggregation, Pruning - Optimierungstechniken');

-- Erweiterung: +1 EASY, +1 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(64, 2, 'EASY', 'Welcher SQL-Befehl fügt neue Datensätze ein?', 'C', 1),
(65, 2, 'MEDIUM', 'Welche JOIN-Art liefert nur Datensätze mit Treffern auf beiden Seiten?', 'A', 1),
(66, 2, 'HARD', 'Was ist ein Vorteil von Datenbank-Indizes?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q64
(253, 64, 'A', 'SELECT'),
(254, 64, 'B', 'UPDATE'),
(255, 64, 'C', 'INSERT'),
(256, 64, 'D', 'DROP'),
-- Q65
(257, 65, 'A', 'INNER JOIN'),
(258, 65, 'B', 'LEFT JOIN'),
(259, 65, 'C', 'RIGHT JOIN'),
(260, 65, 'D', 'CROSS JOIN'),
-- Q66
(261, 66, 'A', 'Sie reduzieren automatisch den Speicherbedarf jeder Tabelle'),
(262, 66, 'B', 'Sie ersetzen Foreign Keys vollständig'),
(263, 66, 'C', 'Sie erzwingen automatisch Normalformen'),
(264, 66, 'D', 'Sie beschleunigen Such- und Filterabfragen');

-- Mockup-Verteilung fuer Kategorie 2: (2,7,1)
-- Zusatz: +4 MEDIUM
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(96, 2, 'MEDIUM', 'Wofür steht ACID in Datenbanksystemen?', 'B', 1),
(97, 2, 'MEDIUM', 'Welche Klausel begrenzt die Anzahl der Rückgabezeilen?', 'D', 1),
(98, 2, 'MEDIUM', 'Wozu dient GROUP BY in SQL?', 'A', 1),
(99, 2, 'MEDIUM', 'Welche JOIN-Variante liefert alle Zeilen links plus Treffer rechts?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(381, 96, 'A', 'Atomic Caching Integrity Distribution'),
(382, 96, 'B', 'Atomicity, Consistency, Isolation, Durability'),
(383, 96, 'C', 'Availability, Consistency, Index, Data'),
(384, 96, 'D', 'Access, Control, Isolation, Duplication'),
(385, 97, 'A', 'ORDER BY'),
(386, 97, 'B', 'HAVING'),
(387, 97, 'C', 'OFFSET'),
(388, 97, 'D', 'LIMIT'),
(389, 98, 'A', 'Zum Gruppieren von Zeilen für Aggregatfunktionen'),
(390, 98, 'B', 'Zum Löschen von Spalten'),
(391, 98, 'C', 'Zum Verschlüsseln von Tabellen'),
(392, 98, 'D', 'Zum Erstellen von Triggern'),
(393, 99, 'A', 'INNER JOIN'),
(394, 99, 'B', 'RIGHT JOIN'),
(395, 99, 'C', 'LEFT JOIN'),
(396, 99, 'D', 'CROSS JOIN');

-- Reduktion auf Zielwerte (EASY=2, HARD=1)
UPDATE questions SET is_active = 0 WHERE id IN (64, 12, 66);

-- Normalisierung: exakt aktive Fragen gemaess Zielverteilung halten
UPDATE questions
SET is_active = 0
WHERE category_id = 2
  AND id NOT IN (7, 8, 9, 10, 65, 96, 97, 98, 99, 11);

UPDATE questions
SET is_active = 1
WHERE id IN (7, 8, 9, 10, 65, 96, 97, 98, 99, 11);

-- Verify insertion
SELECT COUNT(*) as db_questions FROM questions WHERE category_id = 2 AND is_active = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 2 AND is_active = 1 GROUP BY difficulty;
