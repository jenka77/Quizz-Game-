-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 07-seed-questions-security.sql
-- IT-Sicherheit (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 6: IT-SICHERHEIT (ID 6)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(31, 6, 'EASY', 'Was ist Phishing?', 'A', 1),
(32, 6, 'EASY', 'Welche ist eine sichere Passwort-Praktik?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q31 (Phishing)
(121, 31, 'A', 'Betrug, um an Zugangsdaten über gefälschte Nachrichten/Seiten zu kommen'),
(122, 31, 'B', 'Ein legales Hacking-Zertifikat'),
(123, 31, 'C', 'Ein Kompressions-Algorithmus'),
(124, 31, 'D', 'Ein Firewall-Rule-Set'),
-- Q32 (Passwort)
(125, 32, 'A', 'Dasselbe Passwort überall nutzen'),
(126, 32, 'B', 'Ein starkes, eindeutiges Passwort mit Groß-, Kleinbuchstaben, Zahlen, Sonderzeichen'),
(127, 32, 'C', 'Ein Passwort mit Geburtsdatum aufschreiben'),
(128, 32, 'D', 'Passwörter in Browser speichern');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(33, 6, 'MEDIUM', 'Wozu dient ein Salt beim Passwort-Hashing?', 'D', 1),
(34, 6, 'MEDIUM', 'Was ist eine Brute-Force-Attacke?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q33 (Salt)
(129, 33, 'A', 'Ersetzt das Passwort durch Klartext'),
(130, 33, 'B', 'Verkürzt Hashes für schnellere Logins'),
(131, 33, 'C', 'Entfernt Sonderzeichen aus Passwörtern'),
(132, 33, 'D', 'Verhindert gleiche Hashes bei gleichen Passwörtern (Rainbow Tables)'),
-- Q34 (Brute-Force)
(133, 34, 'A', 'Ein Angriff auf MQTT über WebSockets'),
(134, 34, 'B', 'Das Manipulieren von DNS-Einträgen'),
(135, 34, 'C', 'Das systematische Durchprobieren aller möglichen Passwörter/Schlüssel'),
(136, 34, 'D', 'Ein Angriff durch defekte RAM-Bausteine');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(35, 6, 'HARD', 'Was ist SQL-Injection?', 'B', 1),
(36, 6, 'HARD', 'Welcher Sicherheits-Standard gilt für Webseiten mit sensiblen Daten?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q35 (SQL-Injection)
(137, 35, 'A', 'Ein Angriff auf MQTT durch fehlende Authentifizierung'),
(138, 35, 'B', 'Einschleusen von SQL-Code über Benutzereingaben, um Queries zu manipulieren'),
(139, 35, 'C', 'Ein Angriff durch defekte Datenbankzertifikate'),
(140, 35, 'D', 'Ein Angriff auf Bluetooth-Pairing'),
-- Q36 (HTTPS)
(141, 36, 'A', 'HTTPS mit gültigem TLS/SSL-Zertifikat und Verschlüsselung'),
(142, 36, 'B', 'HTTP mit starken Passwörtern'),
(143, 36, 'C', 'FTP mit 2FA (Two-Factor Authentication)'),
(144, 36, 'D', 'TELNET mit Firewall-Protection');

-- Erweiterung: +1 EASY, +1 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(76, 6, 'EASY', 'Wofür steht die Abkürzung MFA?', 'D', 1),
(77, 6, 'MEDIUM', 'Was ist der Hauptzweck einer Firewall?', 'A', 1),
(78, 6, 'HARD', 'Welcher Angriff nutzt ungeprüfte Eingaben in Webformularen?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q76
(301, 76, 'A', 'Managed File Access'),
(302, 76, 'B', 'Multiple Firewall Access'),
(303, 76, 'C', 'Main Function Authentication'),
(304, 76, 'D', 'Multi-Factor Authentication'),
-- Q77
(305, 77, 'A', 'Unerwünschten Verkehr filtern und nur erlaubte Verbindungen zulassen'),
(306, 77, 'B', 'Passwörter im Klartext speichern'),
(307, 77, 'C', 'Logs automatisch löschen'),
(308, 77, 'D', 'Datenbankabfragen optimieren'),
-- Q78
(309, 78, 'A', 'ARP Spoofing'),
(310, 78, 'B', 'Cross-Site Scripting (XSS)'),
(311, 78, 'C', 'Port Mirroring'),
(312, 78, 'D', 'Checksum Collision');

-- Mockup-Verteilung fuer Kategorie 6: (1,6,6)
-- Zusatz: +3 MEDIUM, +3 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(117, 6, 'MEDIUM', 'Was ist das Ziel von Least Privilege?', 'A', 1),
(118, 6, 'MEDIUM', 'Welche Aussage zu Hashing ist korrekt?', 'C', 1),
(119, 6, 'MEDIUM', 'Wann ist 2FA besonders sinnvoll?', 'D', 1),
(120, 6, 'HARD', 'Was beschreibt ein Replay-Angriff?', 'B', 1),
(121, 6, 'HARD', 'Welche Maßnahme hilft gegen SQL-Injection am besten?', 'A', 1),
(122, 6, 'HARD', 'Was leistet ein SIEM-System?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(465, 117, 'A', 'Benutzer erhalten nur die minimal nötigen Rechte'),
(466, 117, 'B', 'Jeder Nutzer wird Administrator'),
(467, 117, 'C', 'Alle Dienste laufen als root'),
(468, 117, 'D', 'Passwörter werden im Klartext gespeichert'),
(469, 118, 'A', 'Hashes sind immer rückwärts berechenbar'),
(470, 118, 'B', 'Hashing ersetzt Verschlüsselung in jedem Fall'),
(471, 118, 'C', 'Ein guter Hash erzeugt für gleiche Eingaben gleiche Outputs'),
(472, 118, 'D', 'Hashing braucht keine Algorithmen'),
(473, 119, 'A', 'Nur bei lokalen Testsystemen'),
(474, 119, 'B', 'Nur wenn kein Passwort existiert'),
(475, 119, 'C', 'Nur für Gastkonten'),
(476, 119, 'D', 'Bei Konten mit kritischen oder sensiblen Daten'),
(477, 120, 'A', 'Angriff auf physische Netzwerkkabel'),
(478, 120, 'B', 'Abfangen und erneutes Senden gültiger Nachrichten'),
(479, 120, 'C', 'Änderung von Quellcode im Repository'),
(480, 120, 'D', 'Löschen von Firewalleinträgen'),
(481, 121, 'A', 'Prepared Statements mit Parametern einsetzen'),
(482, 121, 'B', 'Nur Sonderzeichen verbieten'),
(483, 121, 'C', 'SQL-Logs deaktivieren'),
(484, 121, 'D', 'Nur GET-Requests erlauben'),
(485, 122, 'A', 'Es ersetzt Firewalls vollständig'),
(486, 122, 'B', 'Es speichert nur Backups'),
(487, 122, 'C', 'Es verwaltet ausschließlich Zertifikate'),
(488, 122, 'D', 'Es korreliert Sicherheitsereignisse zentral für Monitoring/Alerting');

-- Reduktion auf Zielwerte (EASY=1)
UPDATE questions SET is_active = 0 WHERE id IN (32, 76);

-- Normalisierung: exakt aktive Fragen gemaess Zielverteilung halten
UPDATE questions
SET is_active = 0
WHERE category_id = 6
  AND id NOT IN (31, 33, 34, 77, 117, 118, 119, 35, 36, 78, 120, 121, 122);

UPDATE questions
SET is_active = 1
WHERE id IN (31, 33, 34, 77, 117, 118, 119, 35, 36, 78, 120, 121, 122);

-- Verify insertion
SELECT COUNT(*) as security_questions FROM questions WHERE category_id = 6 AND is_active = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 6 AND is_active = 1 GROUP BY difficulty;
