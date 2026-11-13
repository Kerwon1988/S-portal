(() => {
  const endpoint = "/api/telemetry/errors";
  const consentStorageKey = "portal.error-telemetry-consent.v1";
  const maximumReports = 5;
  const windowMilliseconds = 60_000;
  const assetFrame = /\/assets\/((?:app|htmx|alpine-csp|theme|menu|htmx-config|error-reporter)\.[0-9a-f]{16}\.js):(\d+):(\d+)/;
  const eventTypes = new Set([
    "UNCAUGHT_JS",
    "UNHANDLED_REJECTION",
    "RESOURCE_ERROR",
    "HTMX_NETWORK",
    "HTMX_SERVER_ERROR"
  ]);
  const errorNames = new Set([
    "Error",
    "TypeError",
    "ReferenceError",
    "RangeError",
    "SyntaxError",
    "URIError",
    "EvalError",
    "NetworkError",
    "HttpError",
    "PromiseRejection",
    "ResourceError"
  ]);
  const safePathSegments = new Set([
    "simbiosphere", "servicios", "sobre-nosotros", "legal", "contacto",
    "acceso", "registro", "verificar-email", "recuperar", "restablecer",
    "invitacion", "cuenta", "perfil", "solicitudes", "gestion", "clientes",
    "trabajadores", "cms", "auditoria", "errores", "bandeja"
  ]);
  const reportTimes = [];
  const consentPanel = document.querySelector("[data-error-consent-panel]");
  const consentTitle = document.querySelector("#error-telemetry-consent-title");
  const acceptConsentButton = document.querySelector("[data-error-consent-accept]");
  const rejectConsentButton = document.querySelector("[data-error-consent-reject]");
  const manageConsentButton = document.querySelector("[data-error-consent-manage]");
  const storageError = document.querySelector("[data-error-consent-storage-error]");

  if (!consentPanel || !consentTitle || !acceptConsentButton || !rejectConsentButton
      || !manageConsentButton || !storageError) {
    console.error("Error telemetry consent controls are unavailable; reporting is disabled.");
    return;
  }

  const readConsent = () => {
    try {
      const storedConsent = window.localStorage.getItem(consentStorageKey);
      return storedConsent === "granted" || storedConsent === "denied" ? storedConsent : null;
    } catch {
      return null;
    }
  };

  const setConsentPanelVisible = (visible) => {
    consentPanel.hidden = !visible;
    manageConsentButton.setAttribute("aria-expanded", String(visible));
  };

  const savedConsent = readConsent();
  let consent = savedConsent === "granted" ? "granted" : "denied";
  setConsentPanelVisible(savedConsent === null);

  const safeErrorName = (value) => errorNames.has(value) ? value : "Error";

  const safeStack = (stack) => String(stack || "")
    .split("\n")
    .slice(0, 12)
    .map((line) => {
      const match = line.match(assetFrame);
      return match ? `at ${match[1]}:${match[2]}:${match[3]}` : "";
    })
    .filter(Boolean)
    .join("\n")
    .slice(0, 2048);

  const safePath = () => {
    const segments = window.location.pathname.split("/").filter(Boolean);
    const safeSegments = segments.map((segment, index) => {
      if (index === 0 && ["es", "en"].includes(segment)) return segment;
      return safePathSegments.has(segment) ? segment : "{segment}";
    });
    return `/${safeSegments.join("/")}`.slice(0, 1024);
  };

  const send = (eventType, errorName, stack) => {
    if (consent !== "granted" || !eventTypes.has(eventType)) return;
    const now = Date.now();
    while (reportTimes.length && now - reportTimes[0] >= windowMilliseconds) {
      reportTimes.shift();
    }
    if (reportTimes.length >= maximumReports) return;

    const csrf = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (!csrf || !csrfHeader) return;
    const payload = {
      eventType,
      errorName: safeErrorName(errorName),
      stack: safeStack(stack),
      locale: ["en", "es"].includes(document.documentElement.lang) ? document.documentElement.lang : "",
      correlationId: document.querySelector('meta[name="portal-correlation-id"]')?.content || "",
      path: safePath()
    };
    const body = JSON.stringify(payload);
    if (new TextEncoder().encode(body).length > 4096) return;

    reportTimes.push(now);
    void fetch(endpoint, {
      method: "POST",
      credentials: "same-origin",
      redirect: "error",
      headers: {
        "Content-Type": "application/json",
        [csrfHeader]: csrf
      },
      body
    }).catch(() => {});
  };

  window.addEventListener("error", (event) => {
    const isResourceError = !event.error;
    send(
      isResourceError ? "RESOURCE_ERROR" : "UNCAUGHT_JS",
      isResourceError ? "ResourceError" : event.error?.name,
      event.error?.stack
    );
  });

  window.addEventListener("unhandledrejection", (event) => {
    send("UNHANDLED_REJECTION", event.reason?.name, event.reason?.stack);
  });

  document.addEventListener("htmx:sendError", () => {
    send("HTMX_NETWORK", "NetworkError", "");
  });

  document.addEventListener("htmx:responseError", (event) => {
    if (Number(event.detail?.xhr?.status) >= 500) {
      send("HTMX_SERVER_ERROR", "HttpError", "");
    }
  });

  manageConsentButton.addEventListener("click", () => {
    storageError.hidden = true;
    setConsentPanelVisible(true);
    consentTitle.focus();
  });

  const saveConsent = (decision) => {
    const previousConsent = consent;
    if (decision === "denied") {
      consent = "denied";
      reportTimes.length = 0;
    }
    try {
      window.localStorage.setItem(consentStorageKey, decision);
      consent = decision;
      reportTimes.length = 0;
      storageError.hidden = true;
      setConsentPanelVisible(false);
      manageConsentButton.focus();
    } catch {
      consent = decision === "denied" ? "denied" : previousConsent;
      storageError.hidden = false;
    }
  };

  acceptConsentButton.addEventListener("click", () => saveConsent("granted"));
  rejectConsentButton.addEventListener("click", () => saveConsent("denied"));

  window.addEventListener("storage", (event) => {
    if (event.key !== consentStorageKey) return;
    consent = event.newValue === "granted" ? "granted" : "denied";
    reportTimes.length = 0;
    setConsentPanelVisible(event.newValue !== "granted" && event.newValue !== "denied");
  });
})();
