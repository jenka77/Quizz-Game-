document.addEventListener("DOMContentLoaded", () => {
  const userInfo = document.getElementById("rfid-user-info");
  const currentRfid = document.getElementById("current-rfid");
  const rfidUserLabel = document.getElementById("rfid-user-label");
  const rfidLabel = document.getElementById("rfid-label");

  function getAuthToken() {
    if (sessionStorage.getItem("quizLoggedIn") !== "1") return null;
    const raw = sessionStorage.getItem("quizUser") || localStorage.getItem("quizUser");
    if (!raw) return null;
    try {
      const user = JSON.parse(raw);
      return user?.authToken || null;
    } catch (error) {
      return null;
    }
  }

  function renderUser(user) {
    const username = (user && user.userId && (user.username || "").trim()) ? user.username.trim() : "-";
    const rfid = (user && user.rfidUid != null && String(user.rfidUid).trim() !== "") ? String(user.rfidUid).trim() : "-";
    if (userInfo) userInfo.textContent = `Eingeloggt als: ${username}`;
    if (currentRfid) currentRfid.textContent = rfid;
    if (rfidUserLabel) rfidUserLabel.textContent = username;
    if (rfidLabel) rfidLabel.textContent = rfid;
  }

  async function fetchAndRenderCurrentUser() {
    const authToken = getAuthToken();
    if (!authToken) {
      renderUser(null);
      return;
    }
    try {
      const response = await fetch("/api/auth/me", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ authToken }),
      });
      if (response.ok) {
        const data = await response.json();
        const existing = sessionStorage.getItem("quizUser");
        let merged = { ...data };
        if (existing) {
          try {
            const parsed = JSON.parse(existing);
            merged = { ...parsed, username: data.username, rfidUid: data.rfidUid };
          } catch (e) {
            merged = { ...data, authToken };
          }
        } else {
          merged = { ...data, authToken };
        }
        if (!merged.authToken && authToken) merged.authToken = authToken;
        sessionStorage.setItem("quizUser", JSON.stringify(merged));
        renderUser(merged);
      } else {
        renderUser(null);
      }
    } catch (error) {
      const raw = sessionStorage.getItem("quizUser");
      if (raw) {
        try {
          renderUser(JSON.parse(raw));
        } catch (e) {
          renderUser(null);
        }
      } else {
        renderUser(null);
      }
    }
  }

  window.addEventListener("login-state-changed", fetchAndRenderCurrentUser);

  fetchAndRenderCurrentUser();
});
