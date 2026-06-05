## Login-Policy und Mehrfach-Login

Dieses Dokument beschreibt das Login-Verhalten der Anwendung und begründet, warum wir uns für „neuer Login verdrängt den alten“ auf Server-Seite entschieden haben.

---

### 1. Zielvorgabe aus dem Handout

In `ProjektBeschreibung.md` steht:

> Mehrfach‑Login: Ein Account darf nur einmal aktiv sein. Entscheiden Sie, ob ein neuer Login den alten verdrängt oder wird abgelehnt – und dokumentieren Sie dies.

Damit ist die Frage: Darf ein Benutzer mit demselben Account mehrfach gleichzeitig eingeloggt sein – und wenn nicht, wie wird damit umgegangen?

---

### 2. Gewählte Strategie

**Server-seitig** (Backend, `AuthService`):

- Pro `userId` existiert **maximal ein gültiges Session-Token**.
- Beim Login:
  - `createSessionToken(userId)` entfernt zunächst das alte Token und löscht die zugehörige Session in der Datenbank:
    - `sessionTokenByUserId.remove(userId)`
    - `sessionsByToken.remove(oldToken)`
    - `authRepository.deleteSessionByUserId(userId)`
  - Dann wird ein **neues Token erzeugt** und gespeichert.
- Ergebnis: Ein Account ist **immer nur mit genau einer aktiven Session** am Backend vertreten.

**Client-seitig** (Browser):

- Im Browser wird die aktuelle Session in `sessionStorage` gehalten:
  - `sessionStorage.setItem("quizUser", JSON.stringify(data))`
  - `sessionStorage.setItem("quizLoggedIn", "1")`
- Ein neuer Login im **gleichen Browser** ersetzt die vorherige Session im `sessionStorage`:
  - Der alte Nutzer wird lokal „vergessen“
  - Der neue Nutzer übernimmt UI, Lobby, Controllerauswahl etc.
- In anderen Browsern/Rechnern kann derselbe Account sich zwar neu einloggen, aber durch das Server-Verhalten wird die **alte Session ungültig** (Token invalidiert).

Damit erfüllen wir die Bedingung „Ein Account darf nur einmal aktiv sein“ aus Sicht des Backends; auf Client-Seite ist immer **genau ein User pro Browser-Tab** aktiv.

---

### 3. Warum „neuer Login verdrängt den alten“ sinnvoll ist

**a) Konsistenter Spielfluss**

- Ein Quiz-Spiel hat klare Rollen: Ein Benutzer steuert **seinen** Controller, seine Punkte und seine Lobby-Zeile.
- Würden alte Sessions parallel aktiv bleiben, könnten veraltete Controller/Browserfenster weiterhin:
  - Heartbeats senden
  - Antworten abgeben
  - Ready-Status toggeln
- Das würde zu schwer nachvollziehbaren Zuständen führen (z.B. zwei Browserfenster, die denselben Spieler steuern).

Mit dem gewählten Ansatz:

- Genau **eine** Session ist gültig → es gibt genau **eine Quelle** für Heartbeats, Antworten und Ready-Status pro Spieler.

**b) Einfachere Fehlerbehandlung**

- Wenn ein Spieler z.B. in der Hochschule im Labor eingeloggt war und zu Hause neu einloggt, ist klar:
  - Die alte Session ist nicht mehr aktiv.
  - Ein vergessener Logout im Labor erzeugt keine „Geister“-Heartbeats.
- Im Fehlerfall (Token veraltet/ungültig) wird der Spieler beim nächsten Request sauber abgemeldet.

**c) Sicherheit und Datenschutz**

- Ein gestohlener oder geleakter Token kann nicht dauerhaft parallel genutzt werden:
  - Der „legitime“ Login erzeugt ein neues Token.
  - Das alte Token wird serverseitig invalidiert und in der Datenbank gelöscht.
- Das senkt das Risiko, dass ein alter Token unbemerkt aktiv bleibt.

**d) Einfache UI-Logik**

- Im Frontend muss nur **eine** aktive Session pro Tab berücksichtigt werden:
  - `quizUser` in `sessionStorage`
  - Login-Buttons/Logout-Buttons können sich daran orientieren.
- Mehrere Spieler gleichzeitig werden durch **mehrere Browser-Tabs/Fenster** unterstützt (jeder Tab mit eigener Session im `sessionStorage`), aber nicht durch mehrfach parallele Logins desselben Accounts.

---

### 4. Alternative: „Neuer Login wird abgelehnt“ (warum wir das nicht gewählt haben)

Eine mögliche Alternative wäre:

- Wenn ein Account bereits eingeloggt ist, weitere Logins mit einer Fehlermeldung zu blockieren („Du bist bereits eingeloggt“).

Nachteile dieser Variante für unser Projekt:

- Benutzer müssen sich immer zuerst „merken“, wo sie noch eingeloggt sind, und sich dort aktiv abmelden.
- In typischen Labor-Szenarien (Wechsel von Rechnern, kurze Sessions, Logout vergessen) ist das unpraktisch:
  - Ein vergessener Login würde den Benutzer effektiv aussperren, bis der alte Tab zufällig schließt oder der Dozent eingreift.
- Für ein Übungs-/Projekt-Setup ist es hilfreicher, wenn der **neue Login den alten einfach überschreibt**, ohne interaktive Konfliktlösung.

Deshalb wurde bewusst die „Verdrängen“-Variante gewählt.

---

### 5. Zusammenfassung für das Pflichtenheft

- **Mehrfach-Login-Policy**:
  - Ein Account kann serverseitig nur **eine** aktive Session haben.
  - Bei einem neuen Login wird die alte Session ungültig gemacht (Token invalidiert, Datenbanksession gelöscht).
  - Im Browser existiert pro Tab genau ein aktiver Login (Speicherung in `sessionStorage`).
- **Begründung**:
  - Verhindert widersprüchliche Aktionen aus mehreren Fenstern.
  - Reduziert Fehlerquellen (Geister-Controller, vergessene Logins).
  - Erhöht Sicherheit, da alte Tokens nicht parallel nutzbar bleiben.
  - Passt gut zur Arbeitsweise im Labor (Rechnerwechsel, kurze Sessions).

