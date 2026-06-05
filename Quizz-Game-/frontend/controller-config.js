document.addEventListener("DOMContentLoaded", () => {
  const rfidLabel = document.getElementById("rfid-label");
  const rfidUserLabel = document.getElementById("rfid-user-label");
  const selectedLabel = document.getElementById("selected-controller-label");
  const generatedIdLabel = document.getElementById("generated-controller-id");
  const feedback = document.getElementById("controller-feedback");
  const continueBtn = document.getElementById("continue-lobby-btn");
  const backBtn = document.getElementById("back-lobby-btn");
  const playersList = document.getElementById("players-list");
  const openMyWebControllerBtn = document.getElementById("open-my-web-controller-btn");

  // Referenz auf zuletzt geöffnetes Web-Controller-Fenster (es können mehrere offen sein).
  let webControllerWindow = null;
  let webControllerCloseWatchTimer = null;

  function getCurrentUser() {
    const raw = sessionStorage.getItem("quizUser");
    if (!raw) return null;
    try {
      const user = JSON.parse(raw);
      if (!user || !user.authToken) {
        sessionStorage.removeItem("quizUser");
        sessionStorage.removeItem("quizLoggedIn");
        return null;
      }
      return user;
    } catch (error) {
      return null;
    }
  }

  function isLoggedIn() {
    if (sessionStorage.getItem("quizLoggedIn") !== "1") return false;
    const user = getCurrentUser();
    if (!user || !user.authToken) {
      sessionStorage.removeItem("quizLoggedIn");
      sessionStorage.removeItem("quizUser");
      return false;
    }
    return true;
  }

  function setFeedback(message, isError) {
    if (!feedback) return;
    feedback.textContent = message;
    feedback.style.color = isError ? "#c0392b" : "#2e7d32";
  }

  function setSelectedController(type, id) {
    sessionStorage.setItem("selectedControllerType", type);
    sessionStorage.setItem("selectedControllerId", id);
    if (selectedLabel) {
      selectedLabel.textContent = `${type} (${id})`;
    }
    if (continueBtn) {
      continueBtn.disabled = false;
    }
  }

  function buildWebControllerUrl(controllerId, ownerName) {
    const protocol = window.location.protocol;
    const host = window.location.hostname || "localhost";
    return `${protocol}//${host}:81?id=${encodeURIComponent(controllerId)}&owner=${encodeURIComponent(ownerName)}`;
  }

  async function postJson(path, payload) {
    const response = await fetch(path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      const message = (await response.text()).trim() || `HTTP ${response.status}`;
      throw new Error(message);
    }
    return response.json();
  }

  function stopWebControllerCloseWatch() {
    if (webControllerCloseWatchTimer) {
      clearInterval(webControllerCloseWatchTimer);
      webControllerCloseWatchTimer = null;
    }
  }

  function clearSelectedControllerLocalState() {
    sessionStorage.removeItem("selectedControllerType");
    sessionStorage.removeItem("selectedControllerId");
    if (selectedLabel) selectedLabel.textContent = "-";
    if (continueBtn) continueBtn.disabled = true;
  }

  function startWebControllerCloseWatch(controllerId) {
    stopWebControllerCloseWatch();
    webControllerCloseWatchTimer = setInterval(async () => {
      if (!webControllerWindow || !webControllerWindow.closed) {
        return;
      }

      stopWebControllerCloseWatch();
      webControllerWindow = null;

      // Web-Controller wurde geschlossen (oder Abmelden) → Backend und Frontend bereinigen
      try {
        await sendControllerOffline(controllerId);
      } finally {
        await unbindCurrentController();
        clearSelectedControllerLocalState();
        await loadLobbyPreview();
        window.dispatchEvent(new CustomEvent("lobby-refresh-request"));
        setFeedback("Web-Controller geschlossen: Du kannst jetzt einen neuen Controller wählen.", false);
      }
    }, 800);
  }

  async function bindController(controllerId, controllerType) {
    const user = getCurrentUser();
    if (!user?.authToken) {
      throw new Error("Nicht eingeloggt. Bitte Abmelden und erneut einloggen.");
    }
    await postJson("/api/players/bind", {
      authToken: user.authToken,
      controllerId,
      controllerType,
    });
    return user;
  }

  function normalizeTypeForBind(type) {
    const normalized = String(type || "").trim().toUpperCase();
    return normalized === "HARDWARE" ? "hardware-controller" : "web-controller";
  }

  function labelForType(type) {
    const normalized = String(type || "").trim().toUpperCase();
    if (normalized === "HARDWARE") return "Hardware-Controller";
    if (normalized === "WEB") return "Web-Controller";
    return "Controller";
  }

  function clearSelectionState() {
    sessionStorage.removeItem("selectedControllerType");
    sessionStorage.removeItem("selectedControllerId");
    if (selectedLabel) selectedLabel.textContent = "-";
    setFeedback("Controller-Auswahl zurückgesetzt.", false);
  }

  async function sendControllerOffline(controllerId) {
    if (!controllerId) return;
    try {
      await postJson("/api/players/controller-state", {
        controllerId,
        state: "OFFLINE",
      });
    } catch (e) {
      // Ignorieren – Controller könnte bereits getrennt sein
    }
  }

  async function unbindCurrentController() {
    const user = getCurrentUser();
    const controllerId = sessionStorage.getItem("selectedControllerId");
    if (!user?.authToken || !controllerId) return;
    try {
      await postJson("/api/players/unbind", {
        authToken: user.authToken,
        controllerId,
      });
    } catch (e) {
      // Unbind kann fehlschlagen, z.B. wenn bereits getrennt – trotzdem lokalen State zurücksetzen
    }
  }

  function closeWebControllerPopup() {
    if (webControllerWindow && !webControllerWindow.closed) {
      webControllerWindow.close();
      webControllerWindow = null;
    }
  }

  function renderPreviewPlayers(players) {
    if (!playersList) return;
    playersList.innerHTML = "";

    if (!Array.isArray(players) || players.length === 0) {
      playersList.innerHTML = "<p>Keine aktiven Spieler.</p>";
      return;
    }

    players.slice(0, 5).forEach((player) => {
      const row = document.createElement("div");
      row.className = "player-row";

      const left = document.createElement("div");
      left.className = "player-main";
      const name = document.createElement("strong");
      name.textContent = player.name || "Spieler";

      const tag = document.createElement("span");
      tag.className = "tag";
      tag.textContent = `${player.controllerType || "Controller"} ${player.controllerId || "-"}`;

      left.appendChild(name);
      left.appendChild(tag);

      const status = document.createElement("span");
      status.className = "status";
      const connected = Boolean(player.connected);
      const ready = Boolean(player.ready);
      const notReady = Boolean(player.notReady);

      if (!connected) {
        status.classList.add("disconnected");
        status.textContent = "Disconnected";
      } else if (ready) {
        status.classList.add("ready");
        status.textContent = "Ready";
      } else if (notReady) {
        status.classList.add("not-ready");
        status.textContent = "Not Ready";
      } else {
        status.classList.add("not-ready");
        status.textContent = "Connected";
      }

      row.appendChild(left);
      row.appendChild(status);
      playersList.appendChild(row);
    });
  }

  async function loadAvailableControllers() {
    // Liste entfernt – nur noch Preview-Players nutzen
    return;
  }

  async function loadLobbyPreview() {
    try {
      const response = await fetch("/api/lobby/status");
      if (!response.ok) {
        return;
      }
      const data = await response.json();
      renderPreviewPlayers(Array.isArray(data.players) ? data.players : []);
    } catch (error) {
      // Non-blocking preview
    }
  }

  async function isCurrentUserStillConnectedWithController(controllerId) {
    const user = getCurrentUser();
    const currentUserId = Number(user?.userId || 0);
    if (!currentUserId || !controllerId) return false;

    try {
      const response = await fetch(`/api/lobby/status?t=${Date.now()}`, { cache: "no-store" });
      if (!response.ok) return false;
      const data = await response.json();
      const players = Array.isArray(data.players) ? data.players : [];
      const me = players.find((p) => Number(p.userId || 0) === currentUserId);
      if (!me) return false;
      if (!me.connected) return false;
      return String(me.controllerId || "").trim().toUpperCase() === String(controllerId).trim().toUpperCase();
    } catch (_) {
      // Wenn wir nicht prüfen können, lieber konservativ: "noch verbunden"
      return true;
    }
  }

  function initSelectionState() {
    const selectedType = sessionStorage.getItem("selectedControllerType");
    const selectedId = sessionStorage.getItem("selectedControllerId");
    if (selectedType && selectedId) {
      if (selectedLabel) {
        selectedLabel.textContent = `${selectedType} (${selectedId})`;
      }
      if (continueBtn) {
        continueBtn.disabled = false;
      }
    }
  }

  let controllerSetupDone = false;
  function runFullControllerSetup() {
    if (controllerSetupDone) return;
    controllerSetupDone = true;
    openMyWebControllerBtn?.addEventListener("click", async () => {
      if (!isLoggedIn()) {
        setFeedback("Bitte zuerst einloggen, um einen Web-Controller zu öffnen.", true);
        return;
      }
      const existingId = sessionStorage.getItem("selectedControllerId");
      const existingType = sessionStorage.getItem("selectedControllerType") || "";
      if (existingId) {
        // Web-Controller-Fenster noch offen → Benutzer soll dort Abmelden oder schließen
        if (webControllerWindow && !webControllerWindow.closed) {
          setFeedback(
            "Du hast bereits einen Controller verbunden. Bitte den vorherigen Web-Controller schließen oder dort auf „Abmelden“ klicken.",
            true
          );
          return;
        }

        const stillConnected = await isCurrentUserStillConnectedWithController(existingId);
        const isHardware = existingType.toLowerCase().includes("hardware") || (existingId || "").toUpperCase().startsWith("HW-");
        if (stillConnected && isHardware) {
          setFeedback(
            "Bitte zuerst Hardware-Controller trennen (Button „Hardware-Controller trennen“) oder am Hardware Abmelden (Roter Button lang drücken).",
            true
          );
          return;
        }
        if (stillConnected) {
          setFeedback(
            "Du hast bereits einen Controller verbunden. Bitte den vorherigen Web-Controller schließen oder dort auf „Abmelden“ klicken.",
            true
          );
          return;
        }
        clearSelectedControllerLocalState();
      }
      const newId = "WEB-" + Math.random().toString(36).substring(2, 8).toUpperCase();

      try {
        await fetch("/api/controllers/register", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ controllerId: newId, controllerType: "WEB" }),
        });
      } catch (_) {
        // non-blocking: MQTT-Register des Popups kann Eintrag auch erzeugen
      }

      let user;
      try {
        user = await bindController(newId, "web-controller");
        setSelectedController("web-controller", newId);
        if (selectedLabel) {
          selectedLabel.textContent = `Web-Controller (${newId})`;
        }
        if (generatedIdLabel) {
          generatedIdLabel.textContent = newId;
        }
        setFeedback("Web-Controller wurde erfolgreich mit deinem Account verbunden.", false);
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        setFeedback(`Web-Controller konnte nicht verbunden werden: ${message}`, true);
        return;
      }

      const protocol = window.location.protocol;
      const host = window.location.hostname || "localhost";
      const ownerName = user?.username || `User-${user?.userId || "?"}`;
      const url = `${protocol}//${host}:81?id=${encodeURIComponent(newId)}&owner=${encodeURIComponent(
        ownerName
      )}`;

      webControllerWindow = window.open(
        url,
        `web-controller-${newId}`,
        "popup,width=450,height=700"
      );
      startWebControllerCloseWatch(newId);
      if (webControllerWindow) {
        try {
          webControllerWindow.focus();
        } catch (_) {
          // ignore
        }
        try {
          webControllerWindow.postMessage(
            { type: "controller-bound", controllerId: newId, owner: ownerName },
            "*"
          );
        } catch (_) {
          // ignore cross-origin
        }
      }
      setFeedback("Web-Controller geöffnet und mit deinem Account verbunden.", false);
      await loadLobbyPreview();
      window.dispatchEvent(new CustomEvent("lobby-refresh-request"));
    });

    continueBtn?.addEventListener("click", () => {
      window.location.href = "lobby.html#lobby-main";
    });
    backBtn?.addEventListener("click", async () => {
      const selectedId = sessionStorage.getItem("selectedControllerId");
      if (selectedId) {
        backBtn.disabled = true;
        try {
          stopWebControllerCloseWatch();
          await sendControllerOffline(selectedId);
          await unbindCurrentController();
          closeWebControllerPopup();
          clearSelectionState();
          await loadAvailableControllers();
          await loadLobbyPreview();
        } finally {
          backBtn.disabled = false;
        }
      } else {
        if (window.history.length > 1) {
          window.history.back();
        } else {
          window.location.href = "lobby.html";
        }
      }
    });

    const user = getCurrentUser();
    if (rfidUserLabel) {
      rfidUserLabel.textContent = user?.username || "–";
    }
    if (rfidLabel) {
      rfidLabel.textContent = user?.rfidUid || "–";
    }

    initSelectionState();
    loadAvailableControllers();
    // loadLobbyPreview entfernt: lobby.js / PlayerLobby aktualisiert bereits die Spielerliste
  }

  if (!window.location.pathname.includes("lobby")) {
    if (!isLoggedIn()) {
      openMyWebControllerBtn?.addEventListener("click", () => {
        setFeedback("Bitte zuerst einloggen, um einen Web-Controller zu öffnen.", true);
      });
    }
    window.location.href = "lobby.html";
    return;
  }

  // Immer runFullControllerSetup – der Button prüft isLoggedIn() beim Klick.
  // Nach Registrierung/Redirect kann sessionStorage einen Tick brauchen; der Handler prüft dynamisch.
  runFullControllerSetup();

  window.addEventListener("beforeunload", () => {
    stopWebControllerCloseWatch();
  });
});
