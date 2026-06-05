-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 10-seed-questions-embedded.sql
-- Embedded Systems (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 9: EMBEDDED SYSTEMS (ID 9)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(49, 9, 'EASY', 'Wofür steht GPIO?', 'D', 1),
(50, 9, 'EASY', 'Welche Komponente wird verwendet, um digitale Signale zu erzeugen oder zu lesen?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q49 (GPIO)
(193, 49, 'A', 'General Purpose Graph Output'),
(194, 49, 'B', 'Global Peripheral I/O'),
(195, 49, 'C', 'Generic Processing Gate Output'),
(196, 49, 'D', 'General Purpose Input/Output'),
-- Q50 (GPIO Pin)
(197, 50, 'A', 'Ein Kondensator'),
(198, 50, 'B', 'Ein Widerstand'),
(199, 50, 'C', 'Ein GPIO-Pin'),
(200, 50, 'D', 'Ein Crystal Oszillator');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(51, 9, 'MEDIUM', 'Was ist PWM (Pulse Width Modulation)?', 'B', 1),
(52, 9, 'MEDIUM', 'Welcher Standard wird häufig für Kommunikation zwischen Mikrocontrollern und Sensoren genutzt?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q51 (PWM)
(201, 51, 'A', 'Permanent Web Memory'),
(202, 51, 'B', 'Pulsweitenmodulation - Variation der Impulsbreite für Analog-ähnliche Ausgaben (z.B. LED-Helligkeit)'),
(203, 51, 'C', 'Private WiFi Mode'),
(204, 51, 'D', 'Parallel Wire Multiplexing'),
-- Q52 (I2C/SPI)
(205, 52, 'A', 'I2C oder SPI'),
(206, 52, 'B', 'UART'),
(207, 52, 'C', 'CAN-Bus'),
(208, 52, 'D', 'Ethernet');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(53, 9, 'HARD', 'Warum nutzt man Interrupts in Embedded Systems?', 'C', 1),
(54, 9, 'HARD', 'Was ist ein Watchdog-Timer?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q53 (Interrupts)
(209, 53, 'A', 'Um Code langsamer zu machen'),
(210, 53, 'B', 'Um mehr RAM zu reservieren'),
(211, 53, 'C', 'Um auf externe Ereignisse sofort zu reagieren statt ständig zu pollen - effizient und responsiv'),
(212, 53, 'D', 'Um SQL-Abfragen zu beschleunigen'),
-- Q54 (Watchdog)
(213, 54, 'A', 'Ein Sensor zur Temperaturüberwachung'),
(214, 54, 'B', 'Ein Timer, der das System zurücksetzt, wenn es hängen bleibt oder nicht reagiert'),
(215, 54, 'C', 'Ein GPIO-Pin mit spezieller Funktion'),
(216, 54, 'D', 'Eine Debug-Komponente für Fehleranalyse');

-- Erweiterung: +1 EASY, +1 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(85, 9, 'EASY', 'Wofür wird ein ADC in Mikrocontrollern genutzt?', 'B', 1),
(86, 9, 'MEDIUM', 'Welche Aussage zu Interrupts ist korrekt?', 'D', 1),
(87, 9, 'HARD', 'Wozu dient ein RTOS in Embedded-Projekten?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q85
(337, 85, 'A', 'Nur digitale Signale erzeugen'),
(338, 85, 'B', 'Analoge Signale in digitale Werte umwandeln'),
(339, 85, 'C', 'WLAN-Verbindung herstellen'),
(340, 85, 'D', 'Firmware verschlüsseln'),
-- Q86
(341, 86, 'A', 'Interrupts werden nur bei Polling genutzt'),
(342, 86, 'B', 'Interrupts blockieren den Controller dauerhaft'),
(343, 86, 'C', 'Interrupts funktionieren nur ohne Timer'),
(344, 86, 'D', 'Interrupts erlauben schnelle Reaktion auf externe Ereignisse'),
-- Q87
(345, 87, 'A', 'Vorhersagbares Scheduling und deterministisches Task-Verhalten'),
(346, 87, 'B', 'Nur grafische Benutzeroberflächen'),
(347, 87, 'C', 'Ausschließlich Datenbank-Transaktionen'),
(348, 87, 'D', 'Automatische SQL-Optimierung');

-- Mockup-Verteilung fuer Kategorie 9: (1,5,5)
-- Zusatz: +2 MEDIUM, +2 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(130, 9, 'MEDIUM', 'Wofür wird UART typischerweise genutzt?', 'A', 1),
(131, 9, 'MEDIUM', 'Was ist ein Debouncing-Verfahren?', 'D', 1),
(132, 9, 'HARD', 'Welche Eigenschaft hat ein Echtzeitsystem?', 'B', 1),
(133, 9, 'HARD', 'Warum ist Interrupt-Priorisierung wichtig?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(517, 130, 'A', 'Serielle Kommunikation zwischen Komponenten'),
(518, 130, 'B', 'Video-Rendering auf GPUs'),
(519, 130, 'C', 'Speicherkompression im Betriebssystem'),
(520, 130, 'D', 'Dateiverschlüsselung'),
(521, 131, 'A', 'Übertakten von Mikrocontrollern'),
(522, 131, 'B', 'Automatisches Flashen von Firmware'),
(523, 131, 'C', 'ADC-Kalibrierung'),
(524, 131, 'D', 'Entprellen von Tastern zur Vermeidung mehrfacher Trigger'),
(525, 132, 'A', 'Er reagiert nur bei Leerlauf'),
(526, 132, 'B', 'Zeitliche Anforderungen müssen vorhersagbar eingehalten werden'),
(527, 132, 'C', 'Er braucht immer eine GUI'),
(528, 132, 'D', 'Er nutzt niemals Interrupts'),
(529, 133, 'A', 'Damit alle Interrupts gleich behandelt werden'),
(530, 133, 'B', 'Damit Polling entfällt'),
(531, 133, 'C', 'Damit kritische Ereignisse bevorzugt und rechtzeitig bearbeitet werden'),
(532, 133, 'D', 'Damit UART deaktiviert wird');

-- Reduktion auf Zielwerte (EASY=1)
UPDATE questions SET is_active = 0 WHERE id IN (50, 85);

-- Normalisierung: exakt aktive Fragen gemaess Zielverteilung halten
UPDATE questions
SET is_active = 0
WHERE category_id = 9
  AND id NOT IN (49, 51, 52, 86, 130, 131, 53, 54, 87, 132, 133);

UPDATE questions
SET is_active = 1
WHERE id IN (49, 51, 52, 86, 130, 131, 53, 54, 87, 132, 133);

-- Verify insertion
SELECT COUNT(*) as embedded_questions FROM questions WHERE category_id = 9 AND is_active = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 9 AND is_active = 1 GROUP BY difficulty;
