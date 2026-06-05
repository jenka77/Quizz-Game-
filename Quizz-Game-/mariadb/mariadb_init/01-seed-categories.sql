-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- Categories & Scoring Configuration
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- INSERT CATEGORIES (10 Kategorien)
-- ============================================

INSERT IGNORE INTO categories (id, name, description) VALUES
(1,  'Programmierung', 'Grundlagen zu Programmiersprachen, Tools, Versionskontrolle, OOP'),
(2,  'Datenbanken', 'Relationale Datenbanken, SQL, Modellierung, Normalisierung'),
(3,  'Web-Technologien', 'HTTP, Browser, APIs, Frontend-Grundlagen, REST'),
(4,  'Netzwerke', 'TCP/IP, Routing, Protokolle, Security, DNS'),
(5,  'Betriebssysteme', 'Prozesse, Threads, Scheduling, Synchronisation, Memory'),
(6,  'IT-Sicherheit', 'Grundlagen, Angriffe, Schutzmaßnahmen, Cryptographie'),
(7,  'Algorithmen', 'Komplexität, grundlegende Algorithmen und Datenstrukturen'),
(8,  'Software Engineering', 'Anforderungen, Tests, CI/CD, Projektarbeit, Patterns'),
(9,  'Embedded Systems', 'Mikrocontroller, GPIO, Timing, Interrupts, PWM'),
(10, 'Linux & Tools', 'Shell, Rechte, Docker, Dev-Tools, Git, Prozesse');

-- Verify insertion
SELECT COUNT(*) as category_count FROM categories;
