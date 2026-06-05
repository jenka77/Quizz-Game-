const GAME_IN_PROGRESS_STATES = ["COUNTDOWN", "PRE_QUESTION", "QUESTION", "EVALUATION"];

function setLobbyNavDisabled(disabled) {
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
}

const QuizUI = {
  _redirecting: false,

  async loadStatus() {
    try {
      const response = await fetch("/api/game/status");
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const status = await response.json();
      this.renderStatus(status);
    } catch (error) {
      this.renderFallback();
    }
  },

  renderStatus(status) {
    setLobbyNavDisabled(GAME_IN_PROGRESS_STATES.includes(status?.state || ""));
    const progress = document.getElementById("quiz-progress");
    const subtitle = document.getElementById("quiz-subtitle");
    const category = document.getElementById("quiz-category");
    const questionText = document.getElementById("quiz-question-text");
    const timer = document.getElementById("quiz-timer");
    if (!progress || !subtitle || !category || !questionText) return;

    if (timer) {
      if (status.state === "QUESTION" && Number.isFinite(Number(status.countdownSeconds))) {
        timer.textContent = `${Number(status.countdownSeconds)} Sekunden`;
      } else if (status.state === "EVALUATION" && Number.isFinite(Number(status.countdownSeconds))) {
        timer.textContent = `Auswertung: ${Number(status.countdownSeconds)} Sekunden`;
      } else if (status.state === "COUNTDOWN" && Number.isFinite(Number(status.countdownSeconds))) {
        timer.textContent = `Start in ${Number(status.countdownSeconds)} Sekunden`;
      } else {
        timer.textContent = "30 Sekunden";
      }
    }

    if (status.state === "EVALUATION") {
      if (!this._redirecting) {
        this._redirecting = true;
        window.location.replace("auswertung.html");
      }
      return;
    }

    if (status.state === "END") {
      if (!this._redirecting) {
        this._redirecting = true;
        window.location.replace("final.html");
      }
      return;
    }

    if (status.state !== "QUESTION" || !status.currentQuestion) {
      const totalQuestions = Number(status.totalQuestions || 10);
      progress.textContent = `1/${totalQuestions}`;
      subtitle.textContent = status.message || "Warte auf die erste Frage";
      category.textContent = "Kategorie: - | Schwierigkeit: -";
      questionText.textContent = "Warte auf die erste Frage...";
      this.renderOptions([]);
      return;
    }

    const totalQuestions = Number(status.totalQuestions || 10);
    const questionIndex = Number(status.currentQuestionIndex || 1);
    const currentQuestion = status.currentQuestion;

    progress.textContent = `${questionIndex}/${totalQuestions}`;
    subtitle.textContent = `Frage ${questionIndex} von ${totalQuestions}`;
    category.textContent = `Kategorie: ${currentQuestion.categoryId ?? "-"} | Schwierigkeit: ${currentQuestion.difficulty || "-"}`;
    questionText.textContent = currentQuestion.questionText || "Keine Frage vorhanden.";
    this.renderOptions(Array.isArray(currentQuestion.options) ? currentQuestion.options : []);
  },

  renderOptions(options) {
    const defaults = {
      A: "A) -",
      B: "B) -",
      C: "C) -",
      D: "D) -",
    };

    options.forEach((option) => {
      if (option?.letter) {
        defaults[option.letter] = `${option.letter}) ${option.text}`;
      }
    });

    document.querySelectorAll(".answer-btn").forEach((button) => {
      const letter = button.dataset.letter;
      button.textContent = defaults[letter] || `${letter}) -`;
    });
  },

  renderFallback() {
    setLobbyNavDisabled(false);
    const subtitle = document.getElementById("quiz-subtitle");
    const category = document.getElementById("quiz-category");
    const questionText = document.getElementById("quiz-question-text");
    const timer = document.getElementById("quiz-timer");
    if (subtitle) subtitle.textContent = "Spielstatus konnte nicht geladen werden.";
    if (category) category.textContent = "Kategorie: - | Schwierigkeit: -";
    if (questionText) questionText.textContent = "Bitte zuerst das Spiel in der Lobby starten.";
    if (timer) timer.textContent = "30 Sekunden";
    this.renderOptions([]);
  },
};

document.addEventListener("DOMContentLoaded", () => {
  QuizUI.loadStatus();

  const view = document.getElementById("view-quiz");
  const isActive = () => (!view || view.classList.contains("is-active"));
  setInterval(() => {
    if (isActive()) QuizUI.loadStatus();
  }, 1000);
});
