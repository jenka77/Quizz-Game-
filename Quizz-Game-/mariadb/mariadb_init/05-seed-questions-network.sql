-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 05-seed-questions-network.sql
-- Netzwerke (6 Fragen: 2x EASY, 2x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 4: NETZWERKE (ID 4)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(19, 4, 'EASY', 'Welches Protokoll nutzt typischerweise Port 80?', 'C', 1),
(20, 4, 'EASY', 'Was ist eine IP-Adresse?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q19 (Port 80)
(73, 19, 'A', 'HTTPS'),
(74, 19, 'B', 'FTP'),
(75, 19, 'C', 'HTTP'),
(76, 19, 'D', 'SSH'),
-- Q20 (IP-Adresse)
(77, 20, 'A', 'Eine eindeutige numerische Adresse zum Identifizieren von Geräten in einem Netzwerk'),
(78, 20, 'B', 'Ein Passwort für Netzwerk-Authentifizierung'),
(79, 20, 'C', 'Ein Routing-Protokoll'),
(80, 20, 'D', 'Eine Datenbankverbindungs-Zeichenkette');

-- MEDIUM Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(21, 4, 'MEDIUM', 'Was ist NAT (Network Address Translation)?', 'A', 1),
(22, 4, 'MEDIUM', 'Welches OSI-Modell-Layer arbeitet mit IP-Adressen?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q21 (NAT)
(81, 21, 'A', 'Übersetzt private IPs zu einer öffentlichen IP (und Ports) - ermöglicht mehreren Geräten einen Internetzugang'),
(82, 21, 'B', 'Verschlüsselt Datenpakete auf Layer 2 automatisch'),
(83, 21, 'C', 'Synchronisiert Uhren im Netzwerk'),
(84, 21, 'D', 'Verhindert Paketverlust durch automatische Retransmission'),
-- Q22 (OSI Layer 3)
(85, 22, 'A', 'Layer 2 (Data Link)'),
(86, 22, 'B', 'Layer 3 (Network)'),
(87, 22, 'C', 'Layer 4 (Transport)'),
(88, 22, 'D', 'Layer 7 (Application)');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(23, 4, 'HARD', 'Wozu dient TLS/SSL primär?', 'D', 1),
(24, 4, 'HARD', 'Was beschreibt das TCP-Handshake-Verfahren (3-Way Handshake)?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q23 (TLS/SSL)
(89, 23, 'A', 'Komprimierung von Bildern in HTTP'),
(90, 23, 'B', 'Routing über mehrere Hops in großen Netzwerken'),
(91, 23, 'C', 'Load Balancing auf Layer 4 (Transport)'),
(92, 23, 'D', 'Verschlüsselung und Integrität bei der Verbindung'),
-- Q24 (TCP Handshake)
(93, 24, 'A', 'Ein Verfahren zum Komprimieren von TCP-Paketen'),
(94, 24, 'B', 'Das Beenden einer TCP-Verbindung nach Datenaustausch'),
(95, 24, 'C', 'Ein Verfahren zum Aufbau einer zuverlässigen Verbindung: SYN, SYN-ACK, ACK'),
(96, 24, 'D', 'Ein DNS-Auflösungsalgorithmus');

-- Erweiterung: +1 EASY, +1 MEDIUM, +1 HARD
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(70, 4, 'EASY', 'Welche Komponente verbindet Netzwerke auf IP-Ebene?', 'B', 1),
(71, 4, 'MEDIUM', 'Welches Protokoll arbeitet verbindungsorientiert?', 'D', 1),
(72, 4, 'HARD', 'Was beschreibt CIDR in Netzwerken?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q70
(277, 70, 'A', 'Switch'),
(278, 70, 'B', 'Router'),
(279, 70, 'C', 'Access Point'),
(280, 70, 'D', 'Repeater'),
-- Q71
(281, 71, 'A', 'UDP'),
(282, 71, 'B', 'ICMP'),
(283, 71, 'C', 'ARP'),
(284, 71, 'D', 'TCP'),
-- Q72
(285, 72, 'A', 'Eine Verschlüsselungsmethode für Router-Konfigurationen'),
(286, 72, 'B', 'Ein Protokoll für DNS-Replikation'),
(287, 72, 'C', 'Eine Schreibweise für IP-Netze mit Präfixlänge, z.B. /24'),
(288, 72, 'D', 'Eine Methode zum Lastenausgleich per NAT');

-- Mockup-Verteilung fuer Kategorie 4: (8,9,2)
-- Zusatz: +5 EASY, +6 MEDIUM
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(102, 4, 'EASY', 'Welche Geräte arbeiten typischerweise auf OSI-Layer 2?', 'C', 1),
(103, 4, 'EASY', 'Wofür steht DNS?', 'A', 1),
(104, 4, 'EASY', 'Welcher Port wird standardmäßig für HTTPS genutzt?', 'D', 1),
(105, 4, 'EASY', 'Welches Protokoll dient zur Namensauflösung im Netz?', 'B', 1),
(106, 4, 'EASY', 'Wozu dient ein Access Point?', 'A', 1),
(107, 4, 'MEDIUM', 'Was ist der Zweck von DHCP?', 'B', 1),
(108, 4, 'MEDIUM', 'Welche Aussage zu Subnetzmasken ist korrekt?', 'D', 1),
(109, 4, 'MEDIUM', 'Welches Protokoll wird für sichere Fernadministration verwendet?', 'C', 1),
(110, 4, 'MEDIUM', 'Was beschreibt Paketverlust in einem Netzwerk?', 'A', 1),
(111, 4, 'MEDIUM', 'Wofür wird ICMP hauptsächlich genutzt?', 'B', 1),
(112, 4, 'MEDIUM', 'Welche Metrik beschreibt Verzögerung in Millisekunden?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
(405, 102, 'A', 'Router und Firewall'),
(406, 102, 'B', 'DNS-Server und DHCP-Server'),
(407, 102, 'C', 'Switches und Bridges'),
(408, 102, 'D', 'Load Balancer und Proxy'),
(409, 103, 'A', 'Domain Name System'),
(410, 103, 'B', 'Dynamic Node Service'),
(411, 103, 'C', 'Distributed Network Socket'),
(412, 103, 'D', 'Data Naming Standard'),
(413, 104, 'A', '80'),
(414, 104, 'B', '21'),
(415, 104, 'C', '25'),
(416, 104, 'D', '443'),
(417, 105, 'A', 'ARP'),
(418, 105, 'B', 'DNS'),
(419, 105, 'C', 'BGP'),
(420, 105, 'D', 'NTP'),
(421, 106, 'A', 'Drahtlose Clients mit dem Netzwerk verbinden'),
(422, 106, 'B', 'Dateien komprimieren'),
(423, 106, 'C', 'SQL-Abfragen optimieren'),
(424, 106, 'D', 'TLS-Zertifikate erstellen'),
(425, 107, 'A', 'Paketverschlüsselung'),
(426, 107, 'B', 'Automatische Vergabe von IP-Konfigurationen'),
(427, 107, 'C', 'Routing zwischen Autonomous Systems'),
(428, 107, 'D', 'Namensauflösung von Domains'),
(429, 108, 'A', 'Sie ersetzt die IP-Adresse'),
(430, 108, 'B', 'Sie wird nur bei IPv6 benutzt'),
(431, 108, 'C', 'Sie bestimmt nur den Host-Anteil'),
(432, 108, 'D', 'Sie trennt Netz- und Hostanteil einer Adresse'),
(433, 109, 'A', 'Telnet'),
(434, 109, 'B', 'FTP'),
(435, 109, 'C', 'SSH'),
(436, 109, 'D', 'TFTP'),
(437, 110, 'A', 'Pakete erreichen ihr Ziel nicht oder nur teilweise'),
(438, 110, 'B', 'Pakete kommen immer doppelt an'),
(439, 110, 'C', 'Bandbreite verdoppelt sich'),
(440, 110, 'D', 'DNS wird deaktiviert'),
(441, 111, 'A', 'Nur für Datei-Transfer'),
(442, 111, 'B', 'Diagnose und Fehlermeldungen, z.B. bei ping'),
(443, 111, 'C', 'Webseiten-Rendering'),
(444, 111, 'D', 'Passwortspeicherung'),
(445, 112, 'A', 'Throughput'),
(446, 112, 'B', 'Jitter'),
(447, 112, 'C', 'Loss Ratio'),
(448, 112, 'D', 'Latenz');

-- Reduktion auf Zielwerte (HARD=2)
UPDATE questions SET is_active = 0 WHERE id = 72;

-- Normalisierung: exakt aktive Fragen gemaess Zielverteilung halten
UPDATE questions
SET is_active = 0
WHERE category_id = 4
  AND id NOT IN (19, 20, 70, 102, 103, 104, 105, 106, 21, 22, 71, 107, 108, 109, 110, 111, 112, 23, 24);

UPDATE questions
SET is_active = 1
WHERE id IN (19, 20, 70, 102, 103, 104, 105, 106, 21, 22, 71, 107, 108, 109, 110, 111, 112, 23, 24);

-- Verify insertion
SELECT COUNT(*) as network_questions FROM questions WHERE category_id = 4 AND is_active = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 4 AND is_active = 1 GROUP BY difficulty;
