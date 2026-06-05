function isLoggedInForNav() {
  if (sessionStorage.getItem("quizLoggedIn") !== "1") return false;
  const raw = sessionStorage.getItem("quizUser");
  if (!raw) return false;
  try {
    const user = JSON.parse(raw);
    return !!(user && user.authToken);
  } catch (e) {
    return false;
  }
}

function updateLoginButtonsVisibility() {
  const goRegisterBtn = document.getElementById("go-register-btn");
  const loggedIn = isLoggedInForNav();
  if (goRegisterBtn) {
    goRegisterBtn.style.display = loggedIn ? "none" : "inline-block";
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const goRegisterBtn = document.getElementById("go-register-btn");

  if (goRegisterBtn) {
    goRegisterBtn.addEventListener("click", (e) => {
      e.preventDefault();
      window.location.href = "register.html";
    });
  }

  updateLoginButtonsVisibility();
  window.updateLoginButtonsVisibility = updateLoginButtonsVisibility;

  // Nachladen nach Seitenaufruf (Race mit Login/Redirect)
  setTimeout(updateLoginButtonsVisibility, 150);
});

window.addEventListener("login-state-changed", updateLoginButtonsVisibility);
document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "visible") updateLoginButtonsVisibility();
});
