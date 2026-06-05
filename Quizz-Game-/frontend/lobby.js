const PlayerLobby = {
  refreshIntervalMs: 200,  // Schnelles Polling für sofortige RFID-Meldungen (gelbe Taste)
  _lobbyMessageHideTimer: null,
  _lastDisplayedMessageId: null,
  _dismissedMessageId: null,

  async fetchPlayers(cacheBust = false) {
    const url = cacheBust ? `/api/lobby/status?t=${Date.now()}` : "/api/lobby/status";
    const response = await fetch(url, { cache: "no-store" });
    if (!response.ok) {
      throw new Error(`/api/lobby/status -> HTTP ${response.status}`);
    }
    const data = await response.json();
    this.showLobbyMessage(data.lobbyMessage, data.lobbyMessageType, data.lobbyMessageId);
    return Array.isArray(data.players) ? data.players : [];
  },

  showLobbyMessage(text, typeOrError, messageId) {
    const el = document.getElementById("lobby-rfid-message");
    if (!el) return;
    if (!text || typeof text !== "string") {
      if (this._lobbyMessageHideTimer) {
        clearTimeout(this._lobbyMessageHideTimer);
        this._lobbyMessageHideTimer = null;
      }
      el.textContent = "";
      el.className = "lobby-rfid-message";
      el.style.display = "none";
      return;
    }
    const type = typeOrError === true ? "error" : (typeOrError === false ? "info" : (typeOrError || "info"));
    const id = messageId != null ? messageId : (Date.now());
    if (this._dismissedMessageId === id) {
      return; // Bereits 3 s angezeigt und ausgeblendet (gleiche Meldung)
    }
    if (this._lastDisplayedMessageId === id && el.style.display === "block") {
      return; // Timer nicht bei jedem Poll neu starten
    }
    this._lastDisplayedMessageId = id;
    el.textContent = text;
    el.className = "lobby-rfid-message " + type;
    el.style.display = "block";
    if (this._lobbyMessageHideTimer) {
      clearTimeout(this._lobbyMessageHideTimer);
    }
    this._lobbyMessageHideTimer = setTimeout(() => {
      el.textContent = "";
      el.className = "lobby-rfid-message";
      el.style.display = "none";
      this._dismissedMessageId = id;
      this._lobbyMessageHideTimer = null;
    }, 3000);
  },

  mapPlayer(playerItem) {
    const type = (playerItem.controllerType || "UNKNOWN").toUpperCase();
    const typeLabel = type === "WEB" ? "Web-Controller" : type === "HARDWARE" ? "Hardware-Controller" : "Controller";

    return {
      userId: Number(playerItem.userId || 0),
      name: playerItem.name || `Player ${playerItem.userId || "-"}`,
      controller: `${typeLabel} (${playerItem.controllerId || "-"})`,
      connected: Boolean(playerItem.connected),
      ready: Boolean(playerItem.ready),
      notReady: Boolean(playerItem.notReady),
    };
  },

  buildStatusElement(player) {
    const statusElement = document.createElement("span");
    statusElement.classList.add("status");

    if (!player.connected) {
      statusElement.classList.add("disconnected");
      statusElement.textContent = "Disconnected";
      return statusElement;
    }

    if (player.ready) {
      statusElement.classList.add("ready");
      statusElement.textContent = "Ready";
      return statusElement;
    }

    if (player.notReady) {
      statusElement.classList.add("not-ready");
      statusElement.textContent = "Not Ready";
      return statusElement;
    }

    statusElement.classList.add("not-ready");
    statusElement.textContent = "Connected";
    return statusElement;
  },

  renderPlayers(playersRaw) {
    const playersList = document.getElementById("players-list");
    if (!playersList) return;
    playersList.innerHTML = "";
    const currentUserId = this.getCurrentUserId();

    if (!Array.isArray(playersRaw) || playersRaw.length === 0) {
      const empty = document.createElement("p");
      empty.textContent = "Keine Spieler gefunden.";
      playersList.appendChild(empty);
      return;
    }

    const players = playersRaw.map((item) => this.mapPlayer(item));

    players.forEach((player) => {
      const row = document.createElement("div");
      row.className = "player-row";

      const left = document.createElement("div");
      left.className = "player-main";
      const name = document.createElement("strong");
      name.textContent = player.name;

      const tag = document.createElement("span");
      tag.className = "tag";
      tag.textContent = player.controller;

      left.appendChild(name);
      if (currentUserId && player.userId === currentUserId) {
        const logoutBtn = document.createElement("button");
        logoutBtn.className = "btn btn-gray player-logout-btn";
        logoutBtn.textContent = "Abmelden";
        logoutBtn.addEventListener("click", () => {
          document.getElementById("logout-btn")?.click();
        });
        left.appendChild(logoutBtn);
      } else if (currentUserId && player.userId) {
        const kickBtn = document.createElement("button");
        kickBtn.className = "btn player-kick-btn";
        kickBtn.textContent = "Kick";
        kickBtn.addEventListener("click", () => {
          this.kickPlayer(player.userId, player.name);
        });
        left.appendChild(kickBtn);
      }
      left.appendChild(tag);
      row.appendChild(left);
      row.appendChild(this.buildStatusElement(player));

      playersList.appendChild(row);
    });
  },

  getCurrentUserId() {
    const raw = sessionStorage.getItem("quizUser");
    if (!raw) {
      return null;
    }
    try {
      const user = JSON.parse(raw);
      const userId = Number(user?.userId);
      return Number.isFinite(userId) && userId > 0 ? userId : null;
    } catch (error) {
      return null;
    }
  },

  async kickPlayer(targetUserId, targetName) {
    const raw = sessionStorage.getItem("quizUser");
    if (!raw) {
      return;
    }

    let authToken = null;
    try {
      const user = JSON.parse(raw);
      authToken = user?.authToken || null;
    } catch (error) {
      authToken = null;
    }

    if (!authToken) {
      return;
    }

    const confirmed = window.confirm(`Spieler "${targetName}" wirklich kicken?`);
    if (!confirmed) {
      return;
    }

    try {
      const response = await fetch("/api/players/kick", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ authToken, targetUserId }),
      });

      if (!response.ok) {
        const message = (await response.text()).trim() || `HTTP ${response.status}`;
        throw new Error(message);
      }

      await this.refreshPlayers();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      alert(`Kick fehlgeschlagen: ${message}`);
    }
  },

  async refreshPlayers(manualRefresh = false) {
    const refreshBtn = document.getElementById("lobby-refresh-btn");
    if (manualRefresh && refreshBtn) {
      refreshBtn.disabled = true;
      refreshBtn.textContent = "Aktualisiere…";
    }
    try {
      const players = await this.fetchPlayers(manualRefresh);
      this.renderPlayers(players);
      if (manualRefresh && refreshBtn) {
        refreshBtn.textContent = "Aktualisiert";
        setTimeout(() => {
          refreshBtn.textContent = "Aktualisieren";
          refreshBtn.disabled = false;
        }, 800);
      }
    } catch (error) {
      const playersList = document.getElementById("players-list");
      playersList.innerHTML = "<p>Fehler beim Laden der Spieler. Backend auf Port 8080 starten.</p>";
      if (refreshBtn) {
        refreshBtn.textContent = "Aktualisieren";
        refreshBtn.disabled = false;
      }
    }
  },

  _refreshIntervalId: null,

  isLobbyViewActive() {
    const view = document.getElementById("view-lobby");
    if (view) return view.classList.contains("is-active");
    return true; // lobby.html standalone
  },

  startAutoRefresh() {
    this.refreshPlayers();
    this._refreshIntervalId = setInterval(() => {
      if (this.isLobbyViewActive()) this.refreshPlayers();
    }, this.refreshIntervalMs);
  },

  stopAutoRefresh() {
    if (this._refreshIntervalId) {
      clearInterval(this._refreshIntervalId);
      this._refreshIntervalId = null;
    }
  },

};

const GameConfigUI = {
  init() {
    if (!document.querySelector("[data-question-count]")) {
      return;
    }

    this.bindEvents();
    this.loadConfig();
  },

  bindEvents() {
    document.querySelectorAll("[data-question-count]").forEach((button) => {
      button.addEventListener("click", () => {
        this.setSelectedQuestionCount(Number(button.dataset.questionCount));
        this.saveConfig();
      });
    });

    document.querySelectorAll("[data-category-id]").forEach((checkbox) => {
      checkbox.addEventListener("change", () => this.saveConfig());
    });

    document.querySelectorAll("[data-difficulty]").forEach((checkbox) => {
      checkbox.addEventListener("change", () => this.saveConfig());
    });

    document.getElementById("game-start-btn")?.addEventListener("click", async () => {
      const config = await this.saveConfig();
      if (!config) {
        return;
      }

      if (this.isSelectionExceedingQuizTotal(config)) {
        this.setFeedback(
          "Die ausgewählten Kategorien enthalten mehr Fragen als der gewählte Quizmodus erlaubt.",
          true
        );
        return;
      }

      if (!config.valid) {
        this.setFeedback(config.message || "Spielkonfiguration ist noch ungültig.", true);
        return;
      }

      try {
        const response = await fetch("/api/game/start", { method: "POST" });
        if (!response.ok) {
          const message = (await response.text()).trim() || `HTTP ${response.status}`;
          throw new Error(message);
        }

        const status = await response.json();
        this.setFeedback("Countdown gestartet. Wechsel zum Countdown-Bildschirm...", false);
        GameStatusUI.renderStatus(status);
        window.location.href = "countdown.html";
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        this.setFeedback(message, true);
      }
    });
  },

  async loadConfig() {
    try {
      const response = await fetch("/api/game/config");
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const text = await response.text();
      let config;
      try {
        config = JSON.parse(text);
      } catch (e) {
        throw new Error("Ungültige Server-Antwort.");
      }
      this.renderConfig(config);
    } catch (error) {
      this.setFeedback(error instanceof Error ? error.message : "Spielkonfiguration konnte nicht geladen werden.", true);
    }
  },

  async saveConfig() {
    const payload = this.readConfigFromDom();

    try {
      const response = await fetch("/api/game/config", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const message = (await response.text()).trim() || `HTTP ${response.status}`;
        throw new Error(message);
      }
      const text = await response.text();
      let config;
      try {
        config = JSON.parse(text);
      } catch (e) {
        throw new Error("Ungültige Server-Antwort.");
      }
      this.renderConfig(config);
      return config;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.setFeedback(message, true);
      return null;
    }
  },

  readConfigFromDom() {
    const selectedQuestionButton = document.querySelector("[data-question-count].btn-green");

    return {
      questionCount: Number(selectedQuestionButton?.dataset.questionCount || 5),
      categoryIds: Array.from(document.querySelectorAll("[data-category-id]:checked")).map((checkbox) =>
        Number(checkbox.dataset.categoryId)
      ),
      difficulties: Array.from(document.querySelectorAll("[data-difficulty]:checked")).map(
        (checkbox) => checkbox.dataset.difficulty
      ),
    };
  },

  renderConfig(config) {
    this.setSelectedQuestionCount(config.questionCount);

    document.querySelectorAll("[data-category-id]").forEach((checkbox) => {
      checkbox.checked = Array.isArray(config.categoryIds) && config.categoryIds.includes(Number(checkbox.dataset.categoryId));
    });

    document.querySelectorAll("[data-difficulty]").forEach((checkbox) => {
      checkbox.checked = Array.isArray(config.difficulties) && config.difficulties.includes(checkbox.dataset.difficulty);
    });

    const countElement = document.getElementById("available-questions-count");
    if (countElement) {
      countElement.textContent = `Passende Fragen: ${config.availableQuestionCount}`;
    }

    this.renderCategoryStats(config.categoryStats);

    const startButton = document.getElementById("game-start-btn");
    if (startButton) {
      startButton.disabled = !config.valid || this.isSelectionExceedingQuizTotal(config);
    }

    if (this.isSelectionExceedingQuizTotal(config)) {
      this.setFeedback(
        "Die ausgewählten Kategorien enthalten mehr Fragen als der gewählte Quizmodus erlaubt.",
        true
      );
      return;
    }

    this.setFeedback(config.message || "", !config.valid);
  },

  setSelectedQuestionCount(questionCount) {
    document.querySelectorAll("[data-question-count]").forEach((button) => {
      const isSelected = Number(button.dataset.questionCount) === Number(questionCount);
      button.classList.toggle("btn-green", isSelected);
    });
  },

  renderCategoryStats(categoryStats) {
    if (!Array.isArray(categoryStats)) {
      return;
    }

    categoryStats.forEach((item) => {
      const categoryId = Number(item.categoryId);
      if (!Number.isFinite(categoryId)) {
        return;
      }

      const nameElement = document.querySelector(`[data-category-name-for="${categoryId}"]`);
      if (nameElement && item.name) {
        nameElement.textContent = item.name;
      }

      const countElement = document.querySelector(`[data-category-count-for="${categoryId}"]`);
      if (!countElement) {
        return;
      }

      const easy = Number(item.easyCount) || 0;
      const medium = Number(item.mediumCount) || 0;
      const hard = Number(item.hardCount) || 0;
      countElement.textContent = `(${easy}, ${medium}, ${hard})`;
    });
  },

  /** true wenn nicht genug Fragen für den gewählten Modus verfügbar */
  isSelectionExceedingQuizTotal(config) {
    const questionCount = Number(config?.questionCount) || 0;
    const availableQuestionCount = Number(config?.availableQuestionCount) || 0;
    return availableQuestionCount < questionCount;
  },

  setFeedback(message, isError) {
    const feedback = document.getElementById("game-config-feedback");
    if (!feedback) {
      return;
    }

    feedback.textContent = message;
    feedback.style.color = isError ? "#c0392b" : "#2e7d32";
  },
};

const GameStatusUI = {
  async loadStatus() {
    const statusMessage = document.getElementById("game-status-message");
    if (!statusMessage) {
      return;
    }

    try {
      const response = await fetch("/api/game/status");
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const status = await response.json();
      this.renderStatus(status);
    } catch (error) {
      statusMessage.textContent = "Spielstatus konnte nicht geladen werden.";
    }
  },

  renderStatus(status) {
    const statusMessage = document.getElementById("game-status-message");
    const questionBox = document.getElementById("game-question-box");

    if (!statusMessage || !questionBox) {
      return;
    }

    statusMessage.textContent = status.message || "Warte auf Spielstart.";
    questionBox.innerHTML = "";

    if (status.state === "QUESTION" && status.currentQuestion) {
      const title = document.createElement("p");
      title.className = "count";
      title.textContent = `Frage ${status.currentQuestionIndex}: ${status.currentQuestion.questionText}`;
      questionBox.appendChild(title);

      if (Array.isArray(status.currentQuestion.options)) {
        status.currentQuestion.options.forEach((option) => {
          const optionElement = document.createElement("p");
          optionElement.textContent = `${option.letter}: ${option.text}`;
          questionBox.appendChild(optionElement);
        });
      }
    }
  },
};

document.addEventListener("DOMContentLoaded", async () => {
  try {
    const res = await fetch("/api/game/status");
    if (res.ok) {
      const status = await res.json();
      const state = status?.state || "LOBBY";
      if (state === "COUNTDOWN" || state === "PRE_QUESTION") {
        window.location.replace("countdown.html");
        return;
      }
      if (state === "QUESTION") {
        window.location.replace("quiz.html");
        return;
      }
      if (state === "EVALUATION") {
        window.location.replace("auswertung.html");
        return;
      }
    }
  } catch (_) {
    /* ignore, allow lobby */
  }

  if (window.location.hash === "#lobby-main") {
    const el = document.getElementById("lobby-main");
    if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  // Hardware-Controller per ID direkt mit dem eingeloggten Spieler verbinden
  const hwIdInput = document.getElementById("hardware-controller-id-input");
  const hwBindBtn = document.getElementById("hardware-controller-bind-btn");
  const hwUnbindBtn = document.getElementById("hardware-controller-unbind-btn");
  const hwFeedback = document.getElementById("hardware-controller-feedback");
  const availableHwList = document.getElementById("available-hardware-list");

  function updateHardwareUnbindButtonVisibility() {
    if (!hwUnbindBtn) return;
    const type = sessionStorage.getItem("selectedControllerType") || "";
    const id = sessionStorage.getItem("selectedControllerId") || "";
    const isHardware = type.toLowerCase().includes("hardware") || id.toUpperCase().startsWith("HW-");
    hwUnbindBtn.style.display = isHardware && id ? "inline-block" : "none";
  }

  async function loadAvailableHardwareControllers() {
    if (!availableHwList) return;
    updateHardwareUnbindButtonVisibility();
    try {
      const response = await fetch(`/api/controllers/available?t=${Date.now()}`, { cache: "no-store" });
      if (!response.ok) return;
      const data = await response.json();
      const hardware = Array.isArray(data.hardwareControllers) ? data.hardwareControllers : [];
      availableHwList.innerHTML = "";
      if (hardware.length === 0) {
        availableHwList.innerHTML = "<p class=\"rfid-meta\">Keine Hardware-Controller. Gelbe Taste drücken (Register via MQTT).</p>";
        return;
      }
      hardware.forEach((c) => {
        const avail = Boolean(c.available);
        const card = document.createElement("div");
        card.className = `controller-card ${avail ? "available" : "unavailable"}`;
        card.innerHTML = `
          <span class="controller-card-title">Hardware-Controller</span>
          <span class="controller-card-id">ID: ${(c.controllerId || "?").replace(/</g, "&lt;")}</span>
          <div class="controller-card-footer">
            <span class="controller-card-badge ${avail ? "verfügbar" : "belegt"}">${avail ? "Verfügbar" : "Belegt"}</span>
            <button type="button" class="btn btn-gray small controller-card-delete" title="Hardware aus Liste entfernen">Löschen</button>
          </div>
        `;
        const deleteBtn = card.querySelector(".controller-card-delete");
        if (deleteBtn) {
          deleteBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            e.preventDefault();
            doHardwareDelete(c.controllerId);
          });
        }
        if (avail) {
          card.addEventListener("click", (e) => {
            if (e.target.closest(".controller-card-delete")) return;
            doHardwareBind(c.controllerId);
          });
          card.setAttribute("role", "button");
          card.setAttribute("tabindex", "0");
          card.addEventListener("keydown", (e) => {
            if (e.target.closest(".controller-card-delete")) return;
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              doHardwareBind(c.controllerId);
            }
          });
        } else {
          card.addEventListener("click", (e) => {
            if (e.target.closest(".controller-card-delete")) {
              e.stopPropagation();
              doHardwareDelete(c.controllerId);
            }
          });
        }
        availableHwList.appendChild(card);
      });
    } catch (_) {
      /* non-blocking */
    }
  }

  async function doHardwareDelete(controllerId) {
    if (!controllerId || !controllerId.trim()) return;
    setHwFeedback("Lösche...", false);
    try {
      const resp = await fetch("/api/controllers/delete-hardware", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ controllerId: controllerId.trim() }),
      });
      if (resp.ok) {
        setHwFeedback("Hardware-Controller aus Liste entfernt.", false);
        updateHardwareUnbindButtonVisibility();
        PlayerLobby.refreshPlayers();
        loadAvailableHardwareControllers();
      } else {
        const msg = (await resp.text()).trim() || "Löschen fehlgeschlagen.";
        setHwFeedback(msg, true);
      }
    } catch (e) {
      setHwFeedback("Löschen fehlgeschlagen: " + (e?.message || "Netzwerkfehler"), true);
    }
  }

  async function doHardwareBind(controllerId) {
    if (!controllerId || !controllerId.trim()) return;
    if (hwIdInput) hwIdInput.value = controllerId.trim();
    setHwFeedback("Verbinde...", false);
    const rawUser = sessionStorage.getItem("quizUser");
    if (!rawUser) {
      setHwFeedback("Bitte zuerst einloggen.", true);
      return;
    }
    let authToken = null;
    try {
      const user = JSON.parse(rawUser);
      authToken = user?.authToken || null;
    } catch (e) {
      authToken = null;
    }
    if (!authToken) {
      setHwFeedback("Authentifizierung fehlt. Bitte neu einloggen.", true);
      return;
    }
    try {
      await fetch("/api/controllers/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ controllerId: controllerId.trim(), controllerType: "HARDWARE" }),
      }).catch(() => {});
      const response = await fetch("/api/players/bind", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          authToken,
          controllerId: controllerId.trim(),
          controllerType: "hardware-controller",
        }),
      });
      if (!response.ok) {
        const message = (await response.text()).trim() || `HTTP ${response.status}`;
        throw new Error(message);
      }
      sessionStorage.setItem("selectedControllerType", "hardware-controller");
      sessionStorage.setItem("selectedControllerId", controllerId.trim());
      setHwFeedback(`Hardware-Controller ${controllerId} wurde erfolgreich verbunden.`, false);
      updateHardwareUnbindButtonVisibility();
      PlayerLobby.refreshPlayers();
      loadAvailableHardwareControllers();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setHwFeedback(`Hardware-Controller konnte nicht verbunden werden: ${message}`, true);
    }
  }

  function setHwFeedback(message, isError) {
    if (!hwFeedback) return;
    hwFeedback.textContent = message;
    hwFeedback.style.color = isError ? "#c0392b" : "#2e7d32";
  }

  hwUnbindBtn?.addEventListener("click", async () => {
    const controllerId = sessionStorage.getItem("selectedControllerId");
    const rawUser = sessionStorage.getItem("quizUser");
    if (!controllerId || !rawUser) return;
    let authToken = null;
    try {
      const user = JSON.parse(rawUser);
      authToken = user?.authToken || null;
    } catch (e) {
      authToken = null;
    }
    setHwFeedback("Trenne…", false);
    try {
      let resp;
      if (authToken) {
        resp = await fetch("/api/players/unbind", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ authToken, controllerId }),
        });
      } else {
        resp = await fetch("/api/controllers/unbind-hardware", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ controllerId }),
        });
      }
      if (!resp.ok) {
        const msg = (await resp.text()).trim() || `HTTP ${resp.status}`;
        throw new Error(msg);
      }
      sessionStorage.removeItem("selectedControllerType");
      sessionStorage.removeItem("selectedControllerId");
      setHwFeedback("Hardware-Controller getrennt. Du kannst jetzt einen Web-Controller wählen.", false);
      updateHardwareUnbindButtonVisibility();
      PlayerLobby.refreshPlayers();
      loadAvailableHardwareControllers();
    } catch (error) {
      setHwFeedback(`Trennen fehlgeschlagen: ${error.message}`, true);
    }
  });

  hwBindBtn?.addEventListener("click", async () => {
    const rawId = hwIdInput?.value.trim() || "";
    if (!rawId) {
      setHwFeedback(
        "Bitte Hardware-Controller-ID / RFID eingeben (z.B. 1CF8464A oder HW-1CF8464A).",
        true
      );
      return;
    }
    // Mapping: eingegebene RFID -> Controller-ID im Format HW-<RFID>
    let cleaned = rawId.toUpperCase().replace(/\s+/g, "");
    if (cleaned.startsWith("RFIDTAGUID:")) {
      cleaned = cleaned.replace("RFIDTAGUID:", "");
    }
    cleaned = cleaned.replace(/^0X/, "");
    const controllerId = cleaned.startsWith("HW-") ? cleaned : `HW-${cleaned}`;

    const rawUser = sessionStorage.getItem("quizUser");
    if (!rawUser) {
      setHwFeedback("Bitte zuerst einloggen.", true);
      return;
    }

    let authToken = null;
    try {
      const user = JSON.parse(rawUser);
      authToken = user?.authToken || null;
    } catch (e) {
      authToken = null;
    }

    if (!authToken) {
      setHwFeedback("Authentifizierung fehlt. Bitte neu einloggen.", true);
      return;
    }

    const existingId = sessionStorage.getItem("selectedControllerId");
    if (existingId && existingId !== controllerId) {
      setHwFeedback("Du hast bereits einen Controller verbunden. Bitte zuerst abmelden.", true);
      return;
    }

    try {
      // 1) Sicherstellen, dass der Hardware-Controller in der DB existiert
      await fetch("/api/controllers/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ controllerId, controllerType: "HARDWARE" }),
      }).catch(() => {});

      // 2) Controller mit dem Spieler verbinden
      const response = await fetch("/api/players/bind", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          authToken,
          controllerId,
          controllerType: "hardware-controller",
        }),
      });

      if (!response.ok) {
        const message = (await response.text()).trim() || `HTTP ${response.status}`;
        throw new Error(message);
      }

      sessionStorage.setItem("selectedControllerType", "hardware-controller");
      sessionStorage.setItem("selectedControllerId", controllerId);
      setHwFeedback(`Hardware-Controller ${controllerId} wurde erfolgreich verbunden.`, false);
      PlayerLobby.refreshPlayers();
      loadAvailableHardwareControllers();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setHwFeedback(`Hardware-Controller konnte nicht verbunden werden: ${message}`, true);
    }
  });

  // Schließt alle geöffneten Web-Controller-Fenster (auch freie) anhand der Controller-IDs
  function closeAllWebControllerWindows(controllerIds) {
    if (!Array.isArray(controllerIds)) return;
    for (const id of controllerIds) {
      if (id == null || String(id).trim() === "") continue;
      try {
        const win = window.open("", `web-controller-${id}`);
        if (win && !win.closed) {
          win.close();
        }
      } catch (e) {
        /* ignore */
      }
    }
  }

  // Admin-Button: komplette Session zurücksetzen (alle Spieler + Controller + Fenster schließen)
  const resetBtn = document.getElementById("reset-session-btn");
  resetBtn?.addEventListener("click", async () => {
    if (!window.confirm("Session wirklich zurücksetzen? Alle Spieler und Controller werden entfernt. Alle geöffneten Controller-Fenster werden geschlossen.")) {
      return;
    }
    try {
      const response = await fetch("/api/admin/reset-session", { method: "POST" });
      if (!response.ok) {
        const msg = (await response.text()).trim() || `HTTP ${response.status}`;
        alert("Reset fehlgeschlagen: " + msg);
        return;
      }
      const data = await response.json();
      if (data && Array.isArray(data.controllerIds)) {
        closeAllWebControllerWindows(data.controllerIds);
      }
      // Session clientseitig leeren
      sessionStorage.removeItem("quizUser");
      sessionStorage.removeItem("selectedControllerType");
      sessionStorage.removeItem("selectedControllerId");
      sessionStorage.removeItem("generatedControllerId");
      sessionStorage.removeItem("quizLoggedIn");
      // Lobby neu laden -> keine Spieler / Controller mehr
      window.location.href = "lobby.html";
    } catch (error) {
      alert("Reset fehlgeschlagen: " + (error?.message || error));
    }
  });

  const lobbyRefreshBtn = document.getElementById("lobby-refresh-btn");
  if (lobbyRefreshBtn) {
    lobbyRefreshBtn.addEventListener("click", () => PlayerLobby.refreshPlayers(true));
  }

  const hwIntervalMs = 4000;
  let hwIntervalId = null;
  const gameStatusIntervalMs = 2500;
  let gameStatusIntervalId = null;

  function isLobbyViewActive() {
    const view = document.getElementById("view-lobby");
    if (view) return view.classList.contains("is-active");
    return true;
  }

  function startLobbyIntervals() {
    if (!hwIntervalId) {
      loadAvailableHardwareControllers();
      hwIntervalId = setInterval(() => {
        if (isLobbyViewActive()) loadAvailableHardwareControllers();
      }, hwIntervalMs);
    }
    if (!gameStatusIntervalId) {
      GameStatusUI.loadStatus();
      gameStatusIntervalId = setInterval(() => {
        if (isLobbyViewActive()) GameStatusUI.loadStatus();
      }, gameStatusIntervalMs);
    }
  }

  function stopLobbyIntervals() {
    if (hwIntervalId) {
      clearInterval(hwIntervalId);
      hwIntervalId = null;
    }
    if (gameStatusIntervalId) {
      clearInterval(gameStatusIntervalId);
      gameStatusIntervalId = null;
    }
  }

  PlayerLobby.startAutoRefresh();
  window.addEventListener("lobby-refresh-request", () => PlayerLobby.refreshPlayers(true));
  window.addEventListener("pagehide", () => {
    PlayerLobby.stopAutoRefresh();
    stopLobbyIntervals();
  });
  window.addEventListener("beforeunload", () => {
    PlayerLobby.stopAutoRefresh();
    stopLobbyIntervals();
  });
  GameConfigUI.init();
  startLobbyIntervals();
  updateHardwareUnbindButtonVisibility();

  const hash = window.location.hash;
  if (hash === "#controller-setup") {
    const section = document.getElementById("controller-setup");
    if (section) {
      section.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }
});
