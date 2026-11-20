const fallbackIntervalMs = 15_000;

function scheduledDelay(element, failed) {
  const interval = Number(element.dataset.pollIntervalMs) || fallbackIntervalMs;
  const backoff = Number(element.dataset.pollBackoffMs) || Math.max(interval * 4, 60_000);
  return failed ? Math.max(backoff, interval) : interval;
}

function replaceSchedule(element, failed) {
  element.removeAttribute("hx-trigger");
  element.setAttribute(
    "hx-trigger",
    `every ${Math.round(scheduledDelay(element, failed) / 1000)}s [document.visibilityState === 'visible']`
  );
  window.htmx?.process(element);
}

document.addEventListener("htmx:afterRequest", (event) => {
  const target = event.target?.closest?.("[data-inbox-poll]");
  if (!target) return;
  replaceSchedule(target, !event.detail?.successful);
});

document.addEventListener("visibilitychange", () => {
  if (document.visibilityState !== "visible") return;
  for (const element of document.querySelectorAll("[data-inbox-poll]")) {
    replaceSchedule(element, false);
  }
});
