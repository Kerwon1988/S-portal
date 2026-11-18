import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import vm from "node:vm";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const reporterSource = readFileSync(
  resolve(root, "src/main/frontend/js/error-reporter.js"),
  "utf8"
);

function createElement() {
  const listeners = new Map();
  return {
    hidden: false,
    addEventListener(type, listener) {
      listeners.set(type, listener);
    },
    setAttribute() {},
    focus() {},
    click() {
      listeners.get("click")?.();
    }
  };
}

function loadReporter(savedConsent = null) {
  const elements = new Map([
    ["[data-error-consent-panel]", createElement()],
    ["#error-telemetry-consent-title", createElement()],
    ["[data-error-consent-accept]", createElement()],
    ["[data-error-consent-reject]", createElement()],
    ["[data-error-consent-manage]", createElement()],
    ["[data-error-consent-storage-error]", createElement()],
    ['meta[name="_csrf"]', { content: "csrf-token" }],
    ['meta[name="_csrf_header"]', { content: "X-CSRF-TOKEN" }],
    ['meta[name="portal-correlation-id"]', { content: "correlation-id" }]
  ]);
  const windowListeners = new Map();
  const documentListeners = new Map();
  const fetchCalls = [];
  const storedValues = new Map();
  if (savedConsent !== null) storedValues.set("portal.error-telemetry-consent.v1", savedConsent);

  const window = {
    location: { pathname: "/es" },
    localStorage: {
      getItem(key) {
        return storedValues.get(key) ?? null;
      },
      setItem(key, value) {
        storedValues.set(key, value);
      }
    },
    addEventListener(type, listener) {
      windowListeners.set(type, listener);
    }
  };
  const document = {
    documentElement: { lang: "es" },
    querySelector(selector) {
      return elements.get(selector) ?? null;
    },
    addEventListener(type, listener) {
      documentListeners.set(type, listener);
    }
  };

  vm.runInNewContext(reporterSource, {
    Date,
    JSON,
    Set,
    String,
    TextEncoder,
    document,
    fetch(endpoint, options) {
      fetchCalls.push({ endpoint, options });
      return Promise.resolve({ ok: true });
    },
    window
  });

  return { elements, fetchCalls, storedValues, windowListeners, documentListeners };
}

test("error reports remain disabled until explicit consent", () => {
  const reporter = loadReporter();
  const onError = reporter.windowListeners.get("error");

  onError({ error: new Error("synthetic error") });
  assert.equal(reporter.fetchCalls.length, 0);

  reporter.elements.get("[data-error-consent-reject]").click();
  onError({ error: new Error("synthetic error") });
  assert.equal(reporter.fetchCalls.length, 0);
});

test("accepted error reports use the same-origin endpoint and CSRF protection", () => {
  const reporter = loadReporter();
  const onError = reporter.windowListeners.get("error");

  onError({ error: new Error("synthetic error") });
  assert.equal(reporter.fetchCalls.length, 0);

  reporter.elements.get("[data-error-consent-accept]").click();
  onError({ error: new Error("synthetic error") });

  assert.equal(reporter.storedValues.get("portal.error-telemetry-consent.v1"), "granted");
  assert.equal(reporter.fetchCalls.length, 1);
  assert.equal(reporter.fetchCalls[0].endpoint, "/api/telemetry/errors");
  assert.equal(reporter.fetchCalls[0].options.method, "POST");
  assert.equal(reporter.fetchCalls[0].options.credentials, "same-origin");
  assert.equal(reporter.fetchCalls[0].options.headers["X-CSRF-TOKEN"], "csrf-token");
});

test("public layout has no cookie banner or third-party analytics provider", () => {
  const layout = readFileSync(
    resolve(root, "src/main/resources/templates/layouts/base.html"),
    "utf8"
  );
  const theme = readFileSync(resolve(root, "src/main/frontend/js/theme.js"), "utf8");
  const localeResolver = readFileSync(
    resolve(root, "src/main/java/com/simbio/core/config/i18n/PortalLocaleResolver.java"),
    "utf8"
  );

  assert.doesNotMatch(layout, /data-cookie-(?:banner|notice)|id="cookie-notice"/i);
  assert.doesNotMatch(layout, /googletagmanager|google-analytics|gtag|hotjar|facebook\.net/i);
  assert.match(theme, /sp_theme=/);
  assert.match(localeResolver, /LANGUAGE_COOKIE = "sp_lang"/);
});
