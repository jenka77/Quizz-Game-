document.addEventListener("DOMContentLoaded", () => {
  const logoutBtn = document.getElementById("logout-btn");
  const feedback = document.getElementById("login-feedback");

  function setFeedback(message, isError) {
    if (!feedback) return;
    feedback.textContent = message;
    feedback.style.color = isError ? "#c0392b" : "#2e7d32";
  }

  function getCurrentUser() {
    const raw = sessionStorage.getItem("quizUser");
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch (error) {
      return null;
    }
  }

  function clearLocalSession() {
    sessionStorage.removeItem("quizUser");
    sessionStorage.removeItem("selectedControllerType");
    sessionStorage.removeItem("selectedControllerId");
    sessionStorage.removeItem("generatedControllerId");
    sessionStorage.removeItem("quizLoggedIn");
  }

  function closeWebControllerWindow() {
    const controllerId = sessionStorage.getItem("selectedControllerId");
    if (!controllerId) return;
    try {
      const win = window.open("", `web-controller-${controllerId}`);
      if (win && !win.closed) {
        win.close();
      }
    } catch (e) {
      /* ignore */
    }
  }

  async function logoutUser() {
    const user = getCurrentUser();
    const selectedControllerId = sessionStorage.getItem("selectedControllerId");

    // Web-Controller-Seite sofort schließen
    closeWebControllerWindow();

    async function tryUnbindController() {
      if (!user || !user.authToken) return true;
      const payload = selectedControllerId
        ? { authToken: user.authToken, controllerId: selectedControllerId }
        : { authToken: user.authToken };
      try {
        const response = await fetch("/api/players/unbind", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });
        return response.ok;
      } catch (error) {
        return false;
      }
    }

    if (!user || !user.authToken) {
      clearLocalSession();
      if (typeof window.updateLoginButtonsVisibility === "function") {
        window.updateLoginButtonsVisibility();
      }
      window.dispatchEvent(new Event("login-state-changed"));
      setFeedback("Du bist abgemeldet!", false);
      return;
    }

    await tryUnbindController();

    let lastError = "";

    try {
      const response = await fetch("/api/auth/logout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ authToken: user.authToken }),
      });

      if (response.ok || response.status === 401) {
        clearLocalSession();
        if (typeof window.updateLoginButtonsVisibility === "function") {
          window.updateLoginButtonsVisibility();
        }
        window.dispatchEvent(new Event("login-state-changed"));
        setFeedback("Logout erfolgreich.", false);
        setTimeout(() => {
          window.location.href = "lobby.html";
        }, 300);
        return;
      }

      lastError = (await response.text()).trim() || `HTTP ${response.status}`;
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
    }

    clearLocalSession();
    if (typeof window.updateLoginButtonsVisibility === "function") {
      window.updateLoginButtonsVisibility();
    }
    window.dispatchEvent(new Event("login-state-changed"));
    setFeedback(
      lastError ? `Lokal abgemeldet (Server-Logout fehlgeschlagen: ${lastError})` : "Lokal abgemeldet.",
      true
    );
    setTimeout(() => {
      window.location.href = "lobby.html";
    }, 500);
  }

  if (logoutBtn) {
    logoutBtn.addEventListener("click", logoutUser);
  }
});
