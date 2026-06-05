document.addEventListener("DOMContentLoaded", () => {
  const loginBtn = document.getElementById("login-btn");

  function setLoginFeedback(message, isError) {
    const feedback = document.getElementById("login-feedback");
    if (!feedback) return;
    feedback.textContent = message;
    feedback.style.color = isError ? "#c0392b" : "#2e7d32";
  }

  async function logoutCurrentSession() {
    const raw = sessionStorage.getItem("quizUser");
    if (!raw) return;
    let user;
    try {
      user = JSON.parse(raw);
    } catch (e) {
      return;
    }
    if (!user?.authToken) return;
    const controllerId = sessionStorage.getItem("selectedControllerId");
    if (controllerId) {
      try {
        await fetch("/api/players/unbind", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ authToken: user.authToken, controllerId }),
        });
      } catch (e) { /* ignore */ }
    }
    try {
      const logoutRes = await fetch("/api/auth/logout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ authToken: user.authToken }),
      });
      if (!logoutRes.ok) {
        // Token ungültig oder Fehler – trotzdem lokal aufräumen, damit neuer Login möglich ist
      }
    } catch (e) { /* ignore */ }
    sessionStorage.removeItem("quizUser");
    sessionStorage.removeItem("selectedControllerType");
    sessionStorage.removeItem("selectedControllerId");
    sessionStorage.removeItem("generatedControllerId");
    sessionStorage.removeItem("quizLoggedIn");
  }

  async function loginUser() {
    const username = document.getElementById("username")?.value.trim() || "";
    const password = document.getElementById("password")?.value || "";
    const loginRfidUid = document.getElementById("login-rfid")?.value.trim() || "";

    setLoginFeedback("", true);

    if (!username || !password) {
      setLoginFeedback("Benutzername und Passwort sind Pflicht.", true);
      return;
    }

    // Alten Spieler nicht automatisch trennen – Multiplayer (neuer Spieler = neuer Tab).
    // Benutzer kann sich manuell über „Abmelden“ trennen.
    // await logoutCurrentSession(); // entfernt, damit alter Spieler nicht gekickt wird

    const payload = { username, password, rfidUid: loginRfidUid || null };
    let lastErrorMessage = "Server nicht erreichbar.";

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        const data = await response.json();
        if (!data || !data.authToken) {
          setLoginFeedback("Login-Antwort ungültig (kein Token). Bitte erneut versuchen.", true);
          return;
        }
        sessionStorage.setItem("quizUser", JSON.stringify(data));
        sessionStorage.removeItem("selectedControllerType");
        sessionStorage.removeItem("selectedControllerId");
        sessionStorage.removeItem("generatedControllerId");
        sessionStorage.setItem("quizLoggedIn", "1");
        if (typeof window.updateLoginButtonsVisibility === "function") {
          window.updateLoginButtonsVisibility();
        }
        window.dispatchEvent(new Event("login-state-changed"));
        setLoginFeedback("Login erfolgreich. Weiterleitung...", false);
        setTimeout(() => {
          window.location.href = "lobby.html#controller-setup";
        }, 700);
        return;
      }

      lastErrorMessage = (await response.text()).trim() || `HTTP ${response.status}`;
    } catch (error) {
      lastErrorMessage = error instanceof Error ? error.message : String(error);
    }

    setLoginFeedback(`Login fehlgeschlagen: ${lastErrorMessage}`, true);
  }

  if (loginBtn) {
    loginBtn.addEventListener("click", loginUser);
  }
});
