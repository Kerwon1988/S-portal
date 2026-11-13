const focusedElementsByTarget = new WeakMap();

document.addEventListener("htmx:beforeSwap", (event) => {
  const target = event.detail.target;
  const active = document.activeElement;
  if (target instanceof Element && active instanceof HTMLElement
      && target.contains(active) && active.id) {
    focusedElementsByTarget.set(target, active.id);
  }
});

document.addEventListener("htmx:afterSwap", (event) => {
  const target = event.detail.target;
  if (!(target instanceof Element)) return;
  const focusId = focusedElementsByTarget.get(target);
  focusedElementsByTarget.delete(target);
  if (focusId) target.querySelector(`#${CSS.escape(focusId)}`)?.focus({ preventScroll: true });
});

document.addEventListener("htmx:configRequest", (event) => {
  const method = (event.detail.verb || "get").toLowerCase();
  if (["get", "head", "options", "trace"].includes(method)) return;
  const token = document.querySelector('meta[name="_csrf"]')?.content;
  const header = document.querySelector('meta[name="_csrf_header"]')?.content;
  if (token && header) event.detail.headers[header] = token;
});

document.addEventListener("htmx:responseError", (event) => {
  const target = event.detail.target;
  if (!target) return;
  target.setAttribute("role", "alert");
  target.setAttribute("tabindex", "-1");
  target.textContent = document.documentElement.lang === "en"
    ? "The request could not be completed. Please try again."
    : "No se pudo completar la solicitud. Inténtalo de nuevo.";
  target.focus({ preventScroll: true });
});

document.addEventListener("click", (event) => {
  const dismiss = event.target.closest("[data-dismiss-toast]");
  if (dismiss) dismiss.closest("[data-toast]")?.remove();
});
