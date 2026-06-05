-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 08-seed-questions-algo.sql
-- Algorithmen (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 7: ALGORITHMEN (ID 7)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(37, 7, 'EASY', 'Welche Datenstruktur arbeitet nach dem FIFO-Prinzip (First In, First Out)?', 'C', 1),
(38, 7, 'EASY', 'Welche Datenstruktur arbeitet nach dem LIFO-Prinzip (Last In, First Out)?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q37 (FIFO)
(145, 37, 'A', 'Stack'),
(146, 37, 'B', 'Heap'),
(147, 37, 'C', 'Queue'),
(148, 37, 'D', 'Tree'),
-- Q38 (LIFO)
(149, 38, 'A', 'Stack'),
(150, 38, 'B', 'Queue'),
(151, 38, 'C', 'Linked List'),
(152, 38, 'D', 'Hash Table');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(39, 7, 'MEDIUM', 'Welche Laufzeit hat die binäre Suche im Worst Case?', 'A', 1),
(40, 7, 'MEDIUM', 'Welcher Sortier-Algorithmus hat im Durchschnitt O(n log n) Laufzeit?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q39 (Binary Search)
(153, 39, 'A', 'O(log n)'),
(154, 39, 'B', 'O(n)'),
(155, 39, 'C', 'O(n log n)'),
(156, 39, 'D', 'O(1)'),
-- Q40 (Sorting)
(157, 40, 'A', 'Bubble Sort'),
(158, 40, 'B', 'Merge Sort oder Quick Sort'),
(159, 40, 'C', 'Insertion Sort'),
(160, 40, 'D', 'Selection Sort');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(41, 7, 'HARD', 'Welche Voraussetzung muss erfüllt sein, damit der Dijkstra-Algorithmus funktioniert?', 'D', 1),
(42, 7, 'HARD', 'Welches Problem beschreibt das „Traveling Salesman Problem" (TSP)?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q41 (Dijkstra)
(161, 41, 'A', 'Der Graph muss vollständig sein'),
(162, 41, 'B', 'Es dürfen nur negative Kanten existieren'),
(163, 41, 'C', 'Es dürfen nur ungerichtete Kanten existieren'),
(164, 41, 'D', 'Keine negativen Kantengewichte'),
-- Q42 (TSP)
(165, 42, 'A', 'Das Sortieren von Passwörtern'),
(166, 42, 'B', 'Die schnellste Route durch eine Datenbank'),
(167, 42, 'C', 'Die kürzeste Rundreise, die alle Orte genau einmal besucht'),
(168, 42, 'D', 'Die Komprimierung von Text-Dateien');

-- Erweiterung: +1 EASY, +1 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(79, 7, 'EASY', 'Welche Komplexität hat linearer Suchalgorithmus im Worst Case?', 'C', 1),
(80, 7, 'MEDIUM', 'Welcher Algorithmus nutzt typischerweise eine Prioritätswarteschlange?', 'A', 1),
(81, 7, 'HARD', 'Was ist eine Eigenschaft von dynamischer Programmierung?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q79
(313, 79, 'A', 'O(1)'),
(314, 79, 'B', 'O(log n)'),
(315, 79, 'C', 'O(n)'),
(316, 79, 'D', 'O(n log n)'),
-- Q80
(317, 80, 'A', 'Dijkstra'),
(318, 80, 'B', 'Bubble Sort'),
(319, 80, 'C', 'Linear Search'),
(320, 80, 'D', 'Depth-Limited Parsing'),
-- Q81
(321, 81, 'A', 'Sie verbietet jede Form von Rekursion'),
(322, 81, 'B', 'Sie funktioniert nur mit sortierten Arrays'),
(323, 81, 'C', 'Sie nutzt ausschließlich Graphen'),
(324, 81, 'D', 'Sie speichert Teilergebnisse, um Wiederholungen zu vermeiden');

-- Mockup-Verteilung fuer Kategorie 7: (8,2,4)
-- Zusatz: +5 EASY, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(123, 7, 'EASY', 'Welcher Algorithmus durchsucht ein Array elementweise?', 'B', 1),
(124, 7, 'EASY', 'Wofür steht BFS bei Graphalgorithmen?', 'A', 1),
(125, 7, 'EASY', 'Welche Datenstruktur nutzt man oft für BFS?', 'D', 1),
(126, 7, 'EASY', 'Was ist das Ziel eines Sortieralgorithmus?', 'C', 1),
(127, 7, 'EASY', 'Welche Komplexität hat binäre Suche im Best Case?', 'A', 1),
(128, 7, 'HARD', 'Wann ist Memoization besonders effektiv?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(489, 123, 'A', 'Binary Search Tree traversal'),
(490, 123, 'B', 'Linear Search'),
(491, 123, 'C', 'Dijkstra'),
(492, 123, 'D', 'A*'),
(493, 124, 'A', 'Breadth-First Search'),
(494, 124, 'B', 'Binary Fast Sort'),
(495, 124, 'C', 'Balanced Function Scan'),
(496, 124, 'D', 'Basic File System'),
(497, 125, 'A', 'Stack'),
(498, 125, 'B', 'Heap'),
(499, 125, 'C', 'Hash Table'),
(500, 125, 'D', 'Queue'),
(501, 126, 'A', 'Dateien komprimieren'),
(502, 126, 'B', 'Netzwerkports scannen'),
(503, 126, 'C', 'Elemente in eine definierte Reihenfolge bringen'),
(504, 126, 'D', 'RAM vergrößern'),
(505, 127, 'A', 'O(1)'),
(506, 127, 'B', 'O(n)'),
(507, 127, 'C', 'O(log n)'),
(508, 127, 'D', 'O(n log n)'),
(509, 128, 'A', 'Wenn es keine überlappenden Teilprobleme gibt'),
(510, 128, 'B', 'Nur bei unsortierten Listen'),
(511, 128, 'C', 'Wenn Teilprobleme mehrfach auftreten und Ergebnisse wiederverwendet werden'),
(512, 128, 'D', 'Nur bei rekursionsfreien Algorithmen');

-- Reduktion auf Zielwerte (MEDIUM=2)
UPDATE questions SET is_active = 0 WHERE id = 80;

-- Normalisierung: exakt aktive Fragen gemaess Zielverteilung halten
UPDATE questions
SET is_active = 0
WHERE category_id = 7
  AND id NOT IN (37, 38, 79, 123, 124, 125, 126, 127, 39, 40, 41, 42, 81, 128);

UPDATE questions
SET is_active = 1
WHERE id IN (37, 38, 79, 123, 124, 125, 126, 127, 39, 40, 41, 42, 81, 128);

-- Verify insertion
SELECT COUNT(*) as algo_questions FROM questions WHERE category_id = 7 AND is_active = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 7 AND is_active = 1 GROUP BY difficulty;
