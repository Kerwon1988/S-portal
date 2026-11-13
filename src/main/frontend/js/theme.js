(() => {
  const cookie = (name) => document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(`${name}=`))
    ?.split("=")[1];

  const setTheme = (theme, persist = false) => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("sp_theme", theme);
    document.cookie = `sp_theme=${theme}; Path=/; Max-Age=31536000; SameSite=Lax${location.protocol === "https:" ? "; Secure" : ""}`;
    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
      button.setAttribute("aria-pressed", String(theme === "dark"));
      const label = theme === "dark" ? button.dataset.labelLight : button.dataset.labelDark;
      button.setAttribute("aria-label", label);
      button.textContent = label;
    });
    if (!persist) return;
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (token && header) {
      fetch("/preferences/theme", {
        method: "POST",
        headers: { [header]: token, "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ theme })
      }).then((response) => {
        if (!response.ok) throw new Error("Unable to persist the theme preference");
      }).catch(() => {
        const errorRegion = document.querySelector("#preference-errors");
        if (errorRegion) {
          errorRegion.textContent = document.documentElement.lang === "en"
            ? "Your theme preference could not be saved."
            : "No se pudo guardar tu preferencia de tema.";
        }
      });
    }
  };

  const initialTheme = () => {
    const serverPreference = document.documentElement.dataset.themePreference;
    if (serverPreference === "light" || serverPreference === "dark") return serverPreference;
    const saved = cookie("sp_theme") || localStorage.getItem("sp_theme");
    if (saved === "light" || saved === "dark") return saved;
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  };

  setTheme(initialTheme());
  document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-theme-toggle]");
    if (button) setTheme(document.documentElement.dataset.theme === "dark" ? "light" : "dark", true);
  });
})();
