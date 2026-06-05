const FinalUI = {
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

  render(status) {
    const list = document.getElementById("final-results-list");
    if (!list) {
      return;
    }

    list.innerHTML = "";

    const rows = Array.isArray(status.finalResults) ? [...status.finalResults] : [];
    rows.sort((a, b) => (Number(b.totalScore) || 0) - (Number(a.totalScore) || 0));

    if (rows.length === 0) {
      const empty = document.createElement("p");
      empty.textContent = "Noch keine Endergebnisse vorhanden.";
      list.appendChild(empty);
      return;
    }

    rows.forEach((result, index) => {
      const row = document.createElement("div");
      row.className = "final-row";

      const left = document.createElement("div");
      const rank = document.createElement("div");
      rank.className = "final-rank";
      rank.textContent = `Platz ${index + 1}`;

      const name = document.createElement("div");
      name.className = "final-player";
      name.textContent = result.name || `Spieler ${result.userId || "-"}`;

      left.appendChild(rank);
      left.appendChild(name);

      const score = document.createElement("div");
      score.className = "final-score";
      score.textContent = result.totalScoreLabel || `${Number(result.totalScore || 0)} Punkte`;

      row.appendChild(left);
      row.appendChild(score);
      list.appendChild(row);
    });
  },

  _redirecting: false,

  handleRouting(status) {
    if (this._redirecting) return;
    if (status.state === "LOBBY") {
      this._redirecting = true;
      window.location.replace("lobby.html");
      return;
    }
    if (status.state === "QUESTION") {
      this._redirecting = true;
      window.location.replace("quiz.html");
      return;
    }
    if (status.state === "EVALUATION") {
      this._redirecting = true;
      window.location.replace("auswertung.html");
    }
  },

  renderError() {
    const list = document.getElementById("final-results-list");
    if (!list) {
      return;
    }
    list.innerHTML = "<p>Finale Ergebnisse konnten nicht geladen werden.</p>";
  },
};

document.addEventListener("DOMContentLoaded", () => {
  FinalUI.loadStatus();

  const view = document.getElementById("view-final");
  const isActive = () => (!view || view.classList.contains("is-active"));
  setInterval(() => {
    if (isActive()) FinalUI.loadStatus();
  }, 2000);
});
