function setRegisterFeedback(message, isError) {
  const feedback = document.getElementById("register-feedback");
  if (!feedback) return;

  feedback.textContent = message;
  feedback.style.color = isError ? "#c0392b" : "#2e7d32";
}

function getRegisterApiCandidates() {
  return ["/api/auth/register"];
}

async function loginAfterRegistration(username, password) {
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  if (!response.ok) {
    const message = (await response.text()).trim() || `HTTP ${response.status}`;
    throw new Error(message);
  }

  return response.json();
}

function normalizeErrorMessage(raw, status) {
  if (!raw) {
    return `HTTP ${status}`;
  }

  const trimmed = raw.trim();
  if (trimmed.startsWith("<!doctype html") || trimmed.startsWith("<html")) {
    return `HTTP ${status} (falscher API-Endpunkt/Proxy)`;
  }

  return trimmed;
}

async function registerUser() {
  // Wenn in diesem Tab bereits eingeloggt – blockieren (außer ?add=1 = „Neuen Spieler“ aus neuem Tab)
  const isAddPlayer = new URLSearchParams(window.location.search).get("add") === "1";
  if (!isAddPlayer && sessionStorage.getItem("quizLoggedIn") === "1") {
    setRegisterFeedback("Du bist bereits eingeloggt. Bitte zuerst abmelden.", true);
    return;
  }

  const username = document.getElementById("reg-username")?.value.trim() || "";
  const rfidUidRaw = document.getElementById("reg-rfid")?.value.trim() || "";
  const password = document.getElementById("reg-password")?.value || "";
  const passwordRepeat = document.getElementById("reg-password-repeat")?.value || "";

  setRegisterFeedback("", true);

  if (!username || !password) {
    setRegisterFeedback("Benutzername und Passwort sind Pflicht.", true);
    return;
  }

  if (password.length < 8) {
    setRegisterFeedback("Passwort muss mindestens 8 Zeichen haben.", true);
    return;
  }

  if (password !== passwordRepeat) {
    setRegisterFeedback("Passwörter stimmen nicht überein.", true);
    return;
  }

  const payload = {
    username,
    password,
    rfidUid: rfidUidRaw === "" ? null : rfidUidRaw
  };

  try {
    const endpoints = getRegisterApiCandidates();
    let lastErrorMessage = "";
    let registrationSucceeded = false;

    for (const endpoint of endpoints) {
      try {
        const response = await fetch(endpoint, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        });

        if (response.ok) {
          registrationSucceeded = true;
          break;
        }

        const rawMessage = await response.text();
        lastErrorMessage = normalizeErrorMessage(rawMessage, response.status);

        // Validation/conflict errors came from backend; don't retry other endpoints.
        if (response.status === 400 || response.status === 409) {
          break;
        }
      } catch (error) {
        lastErrorMessage = error instanceof Error ? error.message : String(error);
      }
    }

    if (!registrationSucceeded) {
      setRegisterFeedback(`Registrierung fehlgeschlagen: ${lastErrorMessage}`, true);
      return;
    }

    const loginData = await loginAfterRegistration(username, password);
    if (!loginData || !loginData.authToken) {
      setRegisterFeedback("Registrierung ok, aber Anmeldung fehlgeschlagen (kein Token). Bitte mit deinem Benutzernamen einloggen.", true);
      return;
    }
    sessionStorage.setItem("quizUser", JSON.stringify(loginData));
    sessionStorage.removeItem("selectedControllerType");
    sessionStorage.removeItem("selectedControllerId");
    sessionStorage.removeItem("generatedControllerId");
    sessionStorage.setItem("quizLoggedIn", "1");

    setRegisterFeedback("Registrierung erfolgreich. Weiterleitung zur Controller-Auswahl...", false);
    setTimeout(() => {
      window.location.replace("lobby.html#controller-setup");
    }, 400);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    setRegisterFeedback(`Registrierung fehlgeschlagen: ${message}`, true);
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const registerBtn = document.getElementById("register-btn");
  const backLoginBtn = document.getElementById("back-login-btn");

  if (registerBtn) {
    registerBtn.addEventListener("click", registerUser);
  }

  if (backLoginBtn) {
    backLoginBtn.addEventListener("click", () => {
      window.location.href = "lobby.html";
    });
  }
});
