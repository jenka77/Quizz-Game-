const EvaluationUI = {
  async loadStatus() {
    try {
      const response = await fetch("/api/game/status");
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const status = await response.json();
      this.render(status);
      this.handleRouting(status);
    } catch (error) {
      this.renderError();
    }
  },

  setLobbyNavDisabled(disabled) {
    const el = document.getElementById("nav-lobby-btn");
    if (!el) return;
    if (disabled) {
      el.removeAttribute("href");
      el.classList.add("nav-disabled");
      el.onclick = (e) => e.preventDefault();
    } else {
      el.setAttribute("href", "lobby.html");
      el.classList.remove("nav-disabled");
      el.onclick = null;
    }
  },

  render(status) {
    this.setLobbyNavDisabled(status?.state === "EVALUATION");
    const countdown = document.getElementById("evaluation-countdown");
    if (countdown) {
      countdown.textContent = Number(status.countdownSeconds ?? 3);
    }

    const abortMsg = document.getElementById("evaluation-abort-msg");
    const title = document.getElementById("evaluation-title");
    if (abortMsg) abortMsg.style.display = status.gameAborted ? "block" : "none";
    if (title) title.textContent = status.gameAborted ? "Zwischenstand (Quiz abgebrochen)" : "Rundenergebnis";

    const list = document.getElementById("evaluation-list");
    if (!list) return;

    list.innerHTML = "";

    const players = Array.isArray(status.evaluationPlayers) ? status.evaluationPlayers : [];
    if (players.length === 0) {
      const empty = document.createElement("p");
      empty.textContent = "Keine Auswertungsdaten vorhanden.";
      list.appendChild(empty);
      return;
    }

    const showTotalScore = Boolean(status.gameAborted);
    players.forEach((player) => {
      const row = document.createElement("div");
      row.className = "evaluation-row";

      const left = document.createElement("div");
      const name = document.createElement("div");
      name.className = "evaluation-player";
      name.textContent = player.name || "Spieler";

      const meta = document.createElement("div");
      meta.className = "evaluation-meta";
      const result = player.result || "Auswertung";
      const controllerId = player.controllerId || "-";
      meta.textContent = showTotalScore ? `Controller: ${controllerId}` : `${result} | Controller: ${controllerId}`;

      left.appendChild(name);
      left.appendChild(meta);

      const points = document.createElement("div");
      points.className = "evaluation-points";
      points.textContent = showTotalScore ? (player.totalScoreLabel || player.points || "0 Punkte") : (player.points || "0 Punkte");

      row.appendChild(left);
      row.appendChild(points);
      list.appendChild(row);
    });
  },

  _redirecting: false,

  handleRouting(status) {
    if (this._redirecting) return;
    if (status.state === "QUESTION" || status.state === "PRE_QUESTION") {
      this._redirecting = true;
      window.location.replace("quiz.html");
      return;
    }
    if (status.state === "END") {
      this._redirecting = true;
      window.location.replace("final.html");
      return;
    }
    if (status.state === "LOBBY") {
      this._redirecting = true;
      window.location.replace("lobby.html");
    }
  },

  renderError() {
    const list = document.getElementById("evaluation-list");
    if (!list) {
      return;
    }

    list.innerHTML = "<p>Auswertung konnte nicht geladen werden.</p>";
  },
};

document.addEventListener("DOMContentLoaded", () => {
  EvaluationUI.loadStatus();

  const view = document.getElementById("view-auswertung");
  const isActive = () => (!view || view.classList.contains("is-active"));
  setInterval(() => {
    if (isActive()) EvaluationUI.loadStatus();
  }, 1000);
});
