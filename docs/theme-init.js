"use strict";
/* Apply the saved appearance before first paint. Keep in sync with applyTheme() in app.js. */
(function () {
  try {
    var saved = JSON.parse(localStorage.getItem("amischolar.preferences") || "{}").theme;
    var choice = saved === "light" || saved === "dark" || saved === "system" ? saved : "system";
    var dark = choice === "dark" || (choice !== "light" && window.matchMedia("(prefers-color-scheme: dark)").matches);
    document.documentElement.dataset.theme = dark ? "dark" : "light";
    document.documentElement.dataset.themeChoice = choice;
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute("content", dark ? "#0e1522" : "#172b49");
  } catch (e) {
    document.documentElement.dataset.theme = "light";
  }
})();
