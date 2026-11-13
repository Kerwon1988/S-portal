(() => {
  const drawers = [...document.querySelectorAll(".menu-drawer")];

  const close = (drawer, restoreFocus) => {
    if (!drawer.open) return;
    drawer.open = false;
    if (restoreFocus) drawer.querySelector("summary")?.focus();
  };

  const updateDrawer = (drawer) => {
    const summary = drawer.querySelector("summary");
    const panel = drawer.querySelector(".menu-panel");
    if (!summary || !panel) return;
    summary.setAttribute("aria-expanded", String(drawer.open));
    panel.inert = !drawer.open;
    document.body.classList.toggle("menu-open", drawer.open);
  };

  const measure = () => {
    const navigation = document.querySelector("[data-primary-navigation]");
    if (!navigation) return;
    const links = navigation.querySelector("[data-primary-links]");
    const drawer = navigation.querySelector(".menu-drawer");
    if (!links || !drawer) return;
    const wasOverflowing = navigation.classList.contains("menu-overflow");
    if (wasOverflowing) navigation.classList.remove("menu-overflow");
    navigation.classList.toggle("menu-overflow", links.scrollWidth > navigation.clientWidth);
  };

  for (const drawer of drawers) {
    drawer.addEventListener("toggle", () => updateDrawer(drawer));
    drawer.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        close(drawer, true);
        updateDrawer(drawer);
      }

      if (event.key === "Tab" && drawer.open) {
        const focusable = [...drawer.querySelectorAll("a[href], button:not([disabled]), summary")];
        const first = focusable[0];
        const last = focusable.at(-1);
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last?.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first?.focus();
        }
      }
    });
  }

  document.addEventListener("click", (event) => {
    for (const drawer of drawers) {
      if (drawer.open && !drawer.contains(event.target)) {
        close(drawer, false);
        updateDrawer(drawer);
      }
    }
  });

  const resizeObserver = new ResizeObserver(measure);
  const navigation = document.querySelector("[data-primary-navigation]");
  if (navigation) resizeObserver.observe(navigation);
  document.fonts?.ready.then(measure);
  window.addEventListener("resize", measure);
  measure();
})();
