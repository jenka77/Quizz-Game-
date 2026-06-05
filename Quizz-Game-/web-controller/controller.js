const controllerIdElement = document.getElementById("controllerId");
const controllerOwnerElement = document.getElementById("controllerOwner");
const controllerStatusLabel = document.getElementById("controllerStatusLabel");
const controllerLogoutButton = document.getElementById("controller-logout-btn");
const readyButton = document.getElementById("ready-btn");
const notReadyButton = document.getElementById("not-ready-btn");
const answerButtons = Array.from(document.querySelectorAll(".color-button"));

const params = new URLSearchParams(window.location.search);
let controllerId = params.get("id");
let controllerOwner = params.get("owner");
let hasAnsweredCurrentQuestion = false;
let currentQuestionId = null;
let lastPreQuestionPingIdResponded = null;
let playerTotalScore = 0;
let mqttConn = null;
let mqttPrefix = "group-17/";

if (!controllerId) {
  controllerId = "WEB-" + Math.random().toString(36).substring(2, 8).toUpperCase();
  params.set("id", controllerId);
  window.history.replaceState({}, "", `${window.location.pathname}?${params.toString()}`);
}
if (controllerIdElement) controllerIdElement.innerText = controllerId;

function mqttTopic(suffix) {
  const p = (mqttPrefix || "group-17/").replace(/\/$/, "") + "/";
  return p + "controller/" + controllerId + "/" + suffix.replace(/^\//, "");
}

function connectMqtt() {
  const env = (typeof window !== "undefined" && window.__ENV__) || {};
  const host = env.MQTT_BROKER_URL || "localhost";
  const port = Number(env.MQTT_BROKER_PORT) || 9001;
  mqttPrefix = env.MQTT_MESSAGE_PREFIX || "group-17/";
  const username = env.MQTT_USERNAME || "";
  const password = env.MQTT_PASSWORD || "";

  const mqttClient = (typeof window !== "undefined" && window.mqtt);
  if (!mqttClient) {
    return;
  }
  const forcedProtocol = String(env.MQTT_WS_PROTOCOL || "").trim().toLowerCase();
  const defaultProtocol = host.includes("localhost") || host.startsWith("127.") ? "ws" : "wss";
  const protocolsToTry = forcedProtocol
    ? [forcedProtocol]
    : [defaultProtocol, defaultProtocol === "wss" ? "ws" : "wss"];

  let attempt = 0;
  const tryConnect = () => {
    const protocol = protocolsToTry[Math.min(attempt, protocolsToTry.length - 1)];
    const wsUrl = `${protocol}://${host}:${port}`;
    attempt += 1;

    mqttConn = mqttClient.connect(wsUrl, {
      username: username || undefined,
      password: password || undefined,
      connectTimeout: 5000,
      reconnectPeriod: 0, // initial fallback handled manually
    });

    mqttConn.on("connect", () => {
      setControllerStatusLabel("Connected");
      updateLCD("MQTT", "Verbunden", controllerId);

      // Subscribe to messages for this controller
      mqttConn.subscribe(mqttTopic("status"));
      mqttConn.subscribe(mqttTopic("answer/result"));
      mqttConn.subscribe(mqttTopic("ping"));

      // Register as FREE (creates controller in DB)
      mqttConn.publish(
        mqttTopic("register"),
        JSON.stringify({ controllerId, type: "WEB", ts: Date.now() }),
        { qos: 0 }
      );

      // Heartbeat: alle 10 Sekunden (DesignVorschlag – 2 verpasste = disconnected)
      setInterval(() => {
        mqttConn?.publish(mqttTopic("heartbeat"), JSON.stringify({ ts: Date.now() }), { qos: 0 });
      }, 10000);

      publishControllerState("CONNECTED");
    });

    mqttConn.on("message", (topic, payloadBuf) => {
      const payloadStr = payloadBuf ? payloadBuf.toString() : "";

      if (String(topic).endsWith("/status")) {
        try {
          const status = JSON.parse(payloadStr || "{}");
          handleStatus(status);
        } catch (e) {
          updateLCD("Status Fehler", "MQTT JSON", controllerId);
        }
        return;
      }

      if (String(topic).endsWith("/answer/result")) {
        try {
          const result = JSON.parse(payloadStr || "{}");
          if (result.error) {
            updateLCD("Antwort Fehler", result.error, controllerId);
            return;
          }
          hasAnsweredCurrentQuestion = true;
          setAnswerButtonsEnabled(false);
          const isCorrect = Boolean(result.isCorrect);
          const points = Number(result.points || 0);
          playerTotalScore = Number(result.totalPoints ?? playerTotalScore) || 0;
          updateScoreDisplay(playerTotalScore);
          const line2 = isCorrect ? "Richtig!" : "Falsch!";
          updateLCD("Antwort", line2, `+${points.toFixed(2)} Punkte`);
        } catch (e) {
          updateLCD("Antwort Fehler", "MQTT JSON", controllerId);
        }
        return;
      }

      if (String(topic).endsWith("/ping")) {
        try {
          const json = JSON.parse(payloadStr || "{}");
          const requestId = json.requestId;
          if (!requestId) return;
          if (lastPreQuestionPingIdResponded === requestId) return;
          lastPreQuestionPingIdResponded = requestId;
          mqttConn?.publish(mqttTopic("pong"), JSON.stringify({ requestId, ts: Date.now() }), { qos: 0 });
        } catch (e) {
          /* ignore */
        }
      }
    });

    mqttConn.on("error", () => {
      if (!forcedProtocol && attempt < protocolsToTry.length) {
        try {
          mqttConn?.end(true);
        } catch (_) {}
        tryConnect();
        return;
      }
      updateLCD("MQTT Fehler", "Keine Verbindung", controllerId);
    });
  };

  tryConnect();
}

connectMqtt();

function setControllerOwner(name) {
  if (!controllerOwnerElement) return;
  controllerOwnerElement.innerText = name && name.trim() ? name.trim() : "-";
}

setControllerOwner(controllerOwner || "-");

window.addEventListener("message", (event) => {
  const data = event.data;
  if (
    data &&
    data.type === "controller-bound" &&
    String(data.controllerId || "").toUpperCase() === controllerId.toUpperCase() &&
    data.owner
  ) {
    controllerOwner = data.owner;
    setControllerOwner(controllerOwner);
  }
});

function updateLCD(line1, line2, line3) {
  const lines = document.querySelectorAll(".lcd-line");
  if (lines[0]) lines[0].textContent = line1 ?? "";
  if (lines[1]) lines[1].textContent = line2 ?? "";
  if (lines[2]) lines[2].textContent = line3 ?? "";
}

function setControllerStatusLabel(text) {
  if (!controllerStatusLabel) return;
  controllerStatusLabel.textContent = text;
}

function updateScoreDisplay(score) {
  const el = document.getElementById("scoreValue");
  if (!el) return;
  const s = Number(score);
  el.textContent = Number.isFinite(s) ? s.toFixed(2) : "0.00";
}

function updateQuestionProgress(index, total) {
  const el = document.getElementById("questionProgressValue");
  if (!el) return;
  const i = Number(index);
  const t = Number(total);
  if (Number.isFinite(i) && Number.isFinite(t) && t > 0) {
    el.textContent = `${i}/${t}`;
  } else {
    el.textContent = "-/-";
  }
}

function setAnswerButtonsEnabled(enabled) {
  answerButtons.forEach((button) => {
    button.disabled = !enabled;
  });
}

function publishControllerState(nextState, options = {}) {
  // QoS 1 für zuverlässige Ready/Not Ready/Offline-Übergänge (besonders bei externem Broker)
  const qos = typeof options.qos === "number" ? options.qos : 1;
  const onPublished = typeof options.onPublished === "function" ? options.onPublished : null;

  try {
    mqttConn?.publish(
      mqttTopic("controller-state"),
      JSON.stringify({ state: nextState, ts: Date.now() }),
      { qos },
      () => {
        if (onPublished) onPublished();
      }
    );
  } catch (_) {
    if (onPublished) onPublished();
  }

  if (nextState === "READY") {
    setControllerStatusLabel("Ready");
    updateLCD("Status", "READY", controllerId);
  } else if (nextState === "NOT_READY") {
    setControllerStatusLabel("Not Ready");
    updateLCD("Status", "NOT READY", controllerId);
  } else if (nextState === "OFFLINE") {
    setControllerStatusLabel("Disconnected");
    updateLCD("Status", "DISCONNECTED", controllerId);
  }
}

async function postControllerStateHttp(nextState) {
  try {
    await fetch("/api/players/controller-state", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ controllerId, state: nextState }),
    });
  } catch (_) {
    // best-effort fallback (MQTT peut suffire)
  }
}

function submitAnswer(letter) {
  if (hasAnsweredCurrentQuestion) {
    return;
  }
  // Send answer via MQTT; result comes back on /answer/result.
  mqttConn?.publish(mqttTopic("answer"), JSON.stringify({ answer: letter, ts: Date.now() }), { qos: 0 });
  setAnswerButtonsEnabled(false);
  updateLCD(`Antwort ${letter}`, "Gesendet", "Warte...");
}

function handleStatus(status) {
  const state = status?.state || "LOBBY";
  const questionIndex = status?.currentQuestionIndex;
  const totalQuestions = status?.totalQuestions;

  // Kein Quiz aktiv (Lobby oder Spiel beendet) → nie "Frage X/Y" anzeigen
  if (state === "LOBBY" || state === "END") {
    updateQuestionProgress(null, null);
  }

  if (status.playerTotalScore != null) {
    playerTotalScore = Number(status.playerTotalScore) || 0;
    updateScoreDisplay(playerTotalScore);
  }

  if ((state === "PRE_QUESTION" || state === "EVALUATION") && status.preQuestionPingId) {
    setAnswerButtonsEnabled(false);
    updateQuestionProgress(questionIndex, totalQuestions);
    const label = state === "EVALUATION" ? "Auswertung" : "Pre-Question";
    updateLCD(label, "Pong senden...", controllerId);
    if (lastPreQuestionPingIdResponded !== status.preQuestionPingId) {
      lastPreQuestionPingIdResponded = status.preQuestionPingId;
      // Backend erwartet "requestId" im Pong-Payload (wie beim /ping-Topic)
      mqttConn?.publish(
        mqttTopic("pong"),
        JSON.stringify({ requestId: status.preQuestionPingId, ts: Date.now() }),
        { qos: 0 }
      );
    }
    return;
  }
  lastPreQuestionPingIdResponded = null;

  if (state === "COUNTDOWN") {
    setAnswerButtonsEnabled(false);
    updateQuestionProgress(null, null);
    updateLCD("Spiel startet", `${status.countdownSeconds ?? 0} Sekunden`, controllerId);
    return;
  }

  if (state === "QUESTION" && status.currentQuestion) {
    updateQuestionProgress(questionIndex, totalQuestions);
    const incomingQuestionId = status.currentQuestion.id;
    if (incomingQuestionId !== currentQuestionId) {
      currentQuestionId = incomingQuestionId;
      hasAnsweredCurrentQuestion = false;
    }

    if (!hasAnsweredCurrentQuestion) {
      setAnswerButtonsEnabled(true);
      updateLCD("Frage aktiv", `${status.countdownSeconds ?? 0} Sekunden`, `Wähle A/B/C/D`);
    } else {
      setAnswerButtonsEnabled(false);
      updateLCD("Antwort gesendet", "Warte auf nächste Frage", controllerId);
    }
    return;
  }

  if (state === "EVALUATION") {
    setAnswerButtonsEnabled(false);
    updateQuestionProgress(questionIndex, totalQuestions);
    updateLCD("Auswertung", `${status.countdownSeconds ?? 0} Sekunden`, "Bitte warten");
    return;
  }

  if (state === "END") {
    setAnswerButtonsEnabled(false);
    updateQuestionProgress(null, null);
    updateLCD("Spiel beendet", "Endergebnis", "im Hauptfenster");
    return;
  }

  setAnswerButtonsEnabled(false);
  currentQuestionId = null;
  hasAnsweredCurrentQuestion = false;
  updateQuestionProgress(null, null);
  updateLCD("Warte", "auf Spielstart", controllerId);
}

function notifyControllerOffline() {
  publishControllerState("OFFLINE");
}

async function handleControllerLogout() {
  controllerLogoutButton.disabled = true;
  let closed = false;
  const closeOnce = () => {
    if (closed) return;
    closed = true;
    window.close();
  };

  // OFFLINE zuverlässig senden, dann schließen.
  // QoS 1 + kurzer Fallback-Timer, falls das Publish-Callback nicht feuert.
  const fallback = setTimeout(closeOnce, 350);
  publishControllerState("OFFLINE", {
    qos: 1,
    onPublished: () => {
      clearTimeout(fallback);
      closeOnce();
    },
  });
}

function playTone(frequency, duration = 250) {
  const context = new (window.AudioContext || window.webkitAudioContext)();
  const oscillator = context.createOscillator();
  const gainNode = context.createGain();
  oscillator.type = "sine";
  oscillator.frequency.value = frequency;
  oscillator.connect(gainNode);
  gainNode.connect(context.destination);
  oscillator.start();
  setTimeout(() => {
    oscillator.stop();
    context.close();
  }, duration);
}

// Direkte Tastenantwort (ohne Klick) für maximale Reaktivität
const keyToLetter = { q: "A", w: "B", a: "C", s: "D" };
let lastKeySubmitTs = 0;
const KEY_DEBOUNCE_MS = 100;

function handleAnswerInput(letter, fromKey) {
  if (hasAnsweredCurrentQuestion) return;
  const btn = answerButtons.find((b) => b.getAttribute("data-letter") === letter);
  if (fromKey) {
    const now = Date.now();
    if (now - lastKeySubmitTs < KEY_DEBOUNCE_MS) return;
    lastKeySubmitTs = now;
    submitAnswer(letter);
    if (btn) {
      btn.classList.add("active");
      btn.addEventListener("animationend", () => btn.classList.remove("active"), { once: true });
      const tone = parseInt(btn.getAttribute("data-tone") || "220", 10);
      playTone(tone);
    }
    return;
  }
  if (btn) {
    btn.classList.add("active");
    btn.addEventListener("animationend", () => btn.classList.remove("active"), { once: true });
    playTone(parseInt(btn.getAttribute("data-tone") || "220", 10));
  }
  submitAnswer(letter);
}

answerButtons.forEach((button) => {
  button.addEventListener("click", () => {
    const letter = button.getAttribute("data-letter");
    handleAnswerInput(letter, false);
  });
});

document.addEventListener("keydown", (event) => {
  if (event.repeat) return;
  const letter = keyToLetter[event.key?.toLowerCase()];
  if (!letter) return;
  event.preventDefault();
  handleAnswerInput(letter, true);
});

readyButton?.addEventListener("click", () => {
  publishControllerState("READY");
  postControllerStateHttp("READY");
});
notReadyButton?.addEventListener("click", () => {
  publishControllerState("NOT_READY");
  postControllerStateHttp("NOT_READY");
});
controllerLogoutButton?.addEventListener("click", handleControllerLogout);

window.addEventListener("beforeunload", notifyControllerOffline);
window.addEventListener("pagehide", notifyControllerOffline);

setAnswerButtonsEnabled(false);
updateLCD("Welcome", "Web Controller", controllerId);
