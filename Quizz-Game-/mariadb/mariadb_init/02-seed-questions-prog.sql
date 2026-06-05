-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 02-seed-questions-prog.sql
-- Programmierung (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 1: PROGRAMMIERUNG (ID 1)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(1, 1, 'EASY', 'Wofür steht die Abkürzung IDE?', 'B', 1),
(2, 1, 'EASY', 'Welche der folgenden ist eine Programmiersprache?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q1 (IDE)
(1, 1, 'A', 'Internet Data Exchange'),
(2, 1, 'B', 'Integrated Development Environment'),
(3, 1, 'C', 'Internal Debug Engine'),
(4, 1, 'D', 'Interface Description Editor'),
-- Q2 (Programmiersprache)
(5, 2, 'A', 'HTML'),
(6, 2, 'B', 'CSS'),
(7, 2, 'C', 'Python'),
(8, 2, 'D', 'JSON');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(3, 1, 'MEDIUM', 'Was macht der git-Befehl „git commit"?', 'C', 1),
(4, 1, 'MEDIUM', 'Welches Paradigma beschreibt die objektorientierte Programmierung (OOP)?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q3 (git commit)
(9, 3, 'A', 'Erstellt automatisch einen Merge Request'),
(10, 3, 'B', 'Löscht nicht getrackte Dateien'),
(11, 3, 'C', 'Speichert Änderungen als Snapshot in der Repository-Historie'),
(12, 3, 'D', 'Schreibt Änderungen direkt ins Remote-Repository'),
-- Q4 (OOP)
(13, 4, 'A', 'Ein Konzept, das Daten und Funktionen in „Objekten" kapselt'),
(14, 4, 'B', 'Ein funktionales Paradigma, das nur reine Funktionen nutzt'),
(15, 4, 'C', 'Eine Methode zur manuellen Speicherverwaltung'),
(16, 4, 'D', 'Ein Debugging-Verfahren für schnellere Kompilierung');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(5, 1, 'HARD', 'Was beschreibt die Big-O-Notation primär?', 'A', 1),
(6, 1, 'HARD', 'Welches Principle beschreibt SOLID in der Softwareentwicklung richtig?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q5 (Big-O)
(17, 5, 'A', 'Die asymptotische Wachstumsrate des Aufwands in Abhängigkeit von der Eingabegröße n'),
(18, 5, 'B', 'Die exakte Laufzeit eines Algorithmus in Millisekunden'),
(19, 5, 'C', 'Den durchschnittlichen CPU-Takt eines Systems'),
(20, 5, 'D', 'Die Anzahl der möglichen Bugs im Code'),
-- Q6 (SOLID)
(21, 6, 'A', 'Nur Single Inheritance nutzen'),
(22, 6, 'B', 'Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion'),
(23, 6, 'C', 'Strukturelles Logging und Objekt Debugging'),
(24, 6, 'D', 'Statische Variable und Override-Limitierungen');

-- Erweiterung: +1 EASY, +1 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(61, 1, 'EASY', 'Welche Datenstruktur speichert Schlüssel-Wert-Paare?', 'D', 1),
(62, 1, 'MEDIUM', 'Wofür wird ein Unit-Test primär genutzt?', 'B', 1),
(63, 1, 'HARD', 'Was beschreibt Dependency Injection korrekt?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q61
(241, 61, 'A', 'Array'),
(242, 61, 'B', 'Stack'),
(243, 61, 'C', 'Queue'),
(244, 61, 'D', 'Hash Map'),
-- Q62
(245, 62, 'A', 'Layout von UI-Seiten automatisiert erstellen'),
(246, 62, 'B', 'Eine kleine isolierte Funktionseinheit verifizieren'),
(247, 62, 'C', 'Nur Integrationsfehler in Produktion suchen'),
(248, 62, 'D', 'Automatisch Datenbank-Schemas migrieren'),
-- Q63
(249, 63, 'A', 'Abhängigkeiten werden von außen bereitgestellt statt intern erzeugt'),
(250, 63, 'B', 'Abhängigkeiten werden immer als globale Variablen gespeichert'),
(251, 63, 'C', 'Abhängigkeiten dürfen nie als Interfaces modelliert werden'),
(252, 63, 'D', 'Abhängigkeiten sind nur in Frontend-Projekten wichtig');

-- Mockup-Verteilung fuer Kategorie 1: (3,5,6)
-- Zusatz: +2 MEDIUM, +3 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(91, 1, 'MEDIUM', 'Welche Aufgabe hat ein Compiler?', 'A', 1),
(92, 1, 'MEDIUM', 'Wann ist Refactoring sinnvoll?', 'C', 1),
(93, 1, 'HARD', 'Was ist ein Vorteil von Immutable Objects?', 'B', 1),
(94, 1, 'HARD', 'Welcher Ansatz minimiert Seiteneffekte im Code?', 'D', 1),
(95, 1, 'HARD', 'Was beschreibt das Liskov-Substitutionsprinzip korrekt?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(361, 91, 'A', 'Er übersetzt Quellcode in maschinennahe Darstellungen'),
(362, 91, 'B', 'Er verteilt Requests im Netzwerk'),
(363, 91, 'C', 'Er speichert nur Logdateien'),
(364, 91, 'D', 'Er ersetzt den Debugger'),
(365, 92, 'A', 'Nur wenn alles bereits langsam ist'),
(366, 92, 'B', 'Ausschließlich vor einem Release'),
(367, 92, 'C', 'Wenn Struktur verbessert werden soll, ohne Verhalten zu ändern'),
(368, 92, 'D', 'Wenn man neue Hardware installiert'),
(369, 93, 'A', 'Objekte sind immer schneller'),
(370, 93, 'B', 'Nebenläufigkeit wird sicherer und einfacher nachvollziehbar'),
(371, 93, 'C', 'Es gibt nie mehr Speicherbedarf'),
(372, 93, 'D', 'Sie vermeiden jede Ausnahme'),
(373, 94, 'A', 'Globale Variablen konsequent einsetzen'),
(374, 94, 'B', 'Überall static verwenden'),
(375, 94, 'C', 'Direkter Datenbankzugriff in jeder Methode'),
(376, 94, 'D', 'Reine Funktionen und klare Schnittstellen nutzen'),
(377, 95, 'A', 'Subtypen müssen Basistypen korrekt ersetzen können'),
(378, 95, 'B', 'Subtypen dürfen Basistypen immer einschränken'),
(379, 95, 'C', 'Vererbung ersetzt Tests'),
(380, 95, 'D', 'Interfaces sind optional');

-- Normalisierung: exakt aktive Fragen gemaess Zielverteilung halten
UPDATE questions
SET is_active = 0
WHERE category_id = 1
  AND id NOT IN (1, 2, 61, 3, 4, 62, 91, 92, 5, 6, 63, 93, 94, 95);

UPDATE questions
SET is_active = 1
WHERE id IN (1, 2, 61, 3, 4, 62, 91, 92, 5, 6, 63, 93, 94, 95);

-- Verify insertion
SELECT COUNT(*) as prog_questions FROM questions WHERE category_id = 1 AND is_active = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 1 AND is_active = 1 GROUP BY difficulty;
