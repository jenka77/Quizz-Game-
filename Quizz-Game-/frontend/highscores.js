const HighscoresUI = {
  selectedQuestionCount: 5,

  init() {
    document.querySelectorAll(".mode-btn").forEach((button) => {
      button.addEventListener("click", () => {
        const questionCount = Number(button.dataset.questionCount || 5);
        this.selectedQuestionCount = questionCount;
        this.renderModeSelection();
        this.loadHighscores();
      });
    });

    document.getElementById("highscores-back-lobby-btn")?.addEventListener("click", () => {
      window.location.href = "lobby.html#lobby-main";
    });

    this.renderModeSelection();
    this.loadHighscores();
  },

  renderModeSelection() {
    document.querySelectorAll(".mode-btn").forEach((button) => {
      const questionCount = Number(button.dataset.questionCount || 5);
      button.classList.toggle("is-active", questionCount === this.selectedQuestionCount);
    });
  },

  async loadHighscores() {
    const tbody = document.getElementById("highscores-body");
    if (!tbody) {
      return;
    }

    try {
      const response = await fetch(`/api/highscores/${this.selectedQuestionCount}`);
      if (!response.ok) {
        throw new Error((await response.text()).trim() || `HTTP ${response.status}`);
      }

      const data = await response.json();
      const entries = Array.isArray(data.entries) ? data.entries : [];
      this.renderTable(entries);
    } catch (error) {
      tbody.innerHTML = "";
      const msg = error instanceof Error ? error.message : String(error);
      const row = document.createElement("tr");
      row.innerHTML = `<td colspan="4">Highscores konnten nicht geladen werden: ${msg}</td>`;
      tbody.appendChild(row);
    }
  },

  renderTable(entries) {
    const tbody = document.getElementById("highscores-body");
    if (!tbody) {
      return;
    }

    tbody.innerHTML = "";
    if (entries.length === 0) {
      const row = document.createElement("tr");
      row.innerHTML = "<td colspan='4'>Keine Highscores für diesen Modus vorhanden.</td>";
      tbody.appendChild(row);
      return;
    }

    // Alle Scores (Ränge 1–20) werden angezeigt, ohne Auslassungspunkte dazwischen.
    entries.forEach((entry) => {
      const rank = entry.rank ?? "-";
      const points = Number(entry.points || 0);
      let rankClass = "rank-default";
      if (rank === 1) rankClass = "rank-1";
      else if (rank === 2) rankClass = "rank-2";
      else if (rank === 3) rankClass = "rank-3";

      const row = document.createElement("tr");
      row.className = rankClass;

      const badgeClass = rank <= 3 ? `rank-badge ${rankClass}` : "rank-badge";

      row.innerHTML = `
        <td><span class="${badgeClass}">${rank}</span></td>
        <td>${entry.username ?? "-"}</td>
        <td>${Number.isInteger(points) ? points : points.toFixed(2)}</td>
        <td>${this.formatDate(entry.createdAt)}</td>
      `;
      tbody.appendChild(row);
    });
  },

  formatDate(value) {
    if (!value) {
      return "-";
    }
    // Wenn keine Zeitzone (Z oder ±HH:MM), als UTC behandeln für korrekte lokale Zeit-Anzeige
    let str = String(value).trim();
    if (str && !/Z|[+-]\d{2}:?\d{2}$/.test(str)) {
      str = str.replace(" ", "T") + "Z";
    }

    const date = new Date(str);
    if (Number.isNaN(date.getTime())) {
      return String(value);
    }

    return date.toLocaleString("de-DE", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  },
};

document.addEventListener("DOMContentLoaded", () => {
  HighscoresUI.init();
});
