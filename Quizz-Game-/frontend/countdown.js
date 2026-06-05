const CountdownUI = {
  /** Lokaler Countdown: 3, 2, 1 (je 1 s), dann GO!, dann Warten auf QUESTION (3 s Ping) vor Weiterleitung */
  localSeconds: null,
  localIntervalId: null,
  goDisplayedAt: null,
  _redirecting: false,
  _countdownStarted: false,

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

  display(value, circle, total) {
    const valueEl = document.getElementById("countdown-value");
    if (valueEl) {
      valueEl.textContent = value;
      valueEl.classList.toggle("is-go", value === "GO!");
    }
    const isGo = value === "GO!";
    const ratio = isGo ? 0 : (typeof value === "number" ? value / (total || 1) : 1);
    this.updateCircle(circle, ratio);
  },

  startLocalCountdown(initialSeconds, circle) {
    if (this._countdownStarted) return;
    this._countdownStarted = true;
    this.localSeconds = Math.max(1, Math.min(3, Math.floor(Number(initialSeconds)) || 3));
    const total = this.localSeconds;

    const tick = () => {
      this.display(this.localSeconds, circle, total);
      if (this.localSeconds <= 0) {
        if (this.localIntervalId) {
          clearInterval(this.localIntervalId);
          this.localIntervalId = null;
        }
        this.display("GO!", circle, total);
        this.goDisplayedAt = Date.now();
        return;
      }
      this.localSeconds -= 1;
    };

    tick();
    this.localIntervalId = setInterval(tick, 1000);
  },

  resetCountdown() {
    if (this.localIntervalId) {
      clearInterval(this.localIntervalId);
      this.localIntervalId = null;
    }
    this.localSeconds = null;
    this._countdownStarted = false;
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

  renderStatus(status) {
    const state = status?.state || "LOBBY";
    this.setLobbyNavDisabled(state === "COUNTDOWN" || state === "PRE_QUESTION");
    const seconds = Number(status?.countdownSeconds ?? 0);
    const valueEl = document.getElementById("countdown-value");
    const circle = document.getElementById("countdown-progress");

    if (!valueEl || !circle) return;

    if (state === "QUESTION") {
      if (!this._redirecting) {
        this._redirecting = true;
        window.location.replace("quiz.html");
      }
      return;
    }

    if (state === "END") {
      this.display(0, circle, 1);
      return;
    }

    if (state === "LOBBY") {
      this.resetCountdown();
      this.display(3, circle, 3);
      return;
    }

    if (state === "COUNTDOWN" || state === "PRE_QUESTION") {
      if (!this._countdownStarted && !this.goDisplayedAt) {
        this.startLocalCountdown(seconds || 3, circle);
      }
    } else if (!this.goDisplayedAt) {
      this.resetCountdown();
      this.display(3, circle, 3);
    }

    // Nach GO! nicht sofort weiterleiten – warten bis Backend in QUESTION wechselt,
    // damit die 3 Sekunden Ping (PRE_QUESTION) ablaufen
  },

  updateCircle(circle, ratio) {
    const r = 54;
    const circumference = 2 * Math.PI * r;
    const offset = circumference * (1 - Math.max(0, Math.min(1, ratio)));
    if (circle) {
      circle.style.strokeDasharray = `${circumference}`;
      circle.style.strokeDashoffset = `${offset}`;
    }
  },

  renderFallback() {
    const valueEl = document.getElementById("countdown-value");
    const circle = document.getElementById("countdown-progress");
    this.display(3, circle, 3);
  },
};

document.addEventListener("DOMContentLoaded", () => {
  CountdownUI.localSeconds = null;
  CountdownUI.localIntervalId = null;
  CountdownUI.goDisplayedAt = null;
  CountdownUI._redirecting = false;
  CountdownUI._countdownStarted = false;
  CountdownUI.loadStatus();

  const view = document.getElementById("view-countdown");
  const isActive = () => (!view || view.classList.contains("is-active"));
  setInterval(() => {
    if (isActive()) CountdownUI.loadStatus();
  }, 600);
});
