import { mkdir } from "node:fs/promises";
import { join } from "node:path";

import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

const baseURL = process.env.PORTAL_E2E_BASE_URL;
const matrix = [
  { language: "es", theme: "light", viewport: { width: 320, height: 800 } },
  { language: "es", theme: "dark", viewport: { width: 320, height: 800 } },
  { language: "en", theme: "light", viewport: { width: 1280, height: 800 } },
  { language: "en", theme: "dark", viewport: { width: 1280, height: 800 } },
  { language: "es", theme: "light", viewport: { width: 1280, height: 800 } },
  { language: "es", theme: "dark", viewport: { width: 1280, height: 800 } },
  { language: "en", theme: "light", viewport: { width: 320, height: 800 } },
  { language: "en", theme: "dark", viewport: { width: 320, height: 800 } }
];

for (const { language, theme, viewport } of matrix) {
  test(`public page: ${language}, ${theme}, ${viewport.width}px`, async ({ page }) => {
    if (!baseURL) {
      throw new Error("E2E precondition missing: start S-portal and set PORTAL_E2E_BASE_URL.");
    }

    await page.setViewportSize(viewport);
    await page.context().addCookies([{
      name: "sp_theme",
      value: theme,
      url: baseURL,
      sameSite: "Lax"
    }]);
    await page.goto(`/${language}`);
    await expect(page.getByRole("main")).toBeVisible();
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(viewport.width);
    await expect(page.locator("head style:not([nonce])")).toHaveCount(0);
    const headerTargets = await page.locator("header a, header button, header summary").evaluateAll((targets) =>
      targets.map((target) => {
        const bounds = target.getBoundingClientRect();
        return { width: bounds.width, height: bounds.height };
      }).filter((bounds) => bounds.width > 0 && bounds.height > 0)
    );
    expect(headerTargets.every((bounds) => bounds.width >= 44 && bounds.height >= 44)).toBe(true);

    const accessibility = await new AxeBuilder({ page }).analyze();
    expect(accessibility.violations).toEqual([]);

    await mkdir("visual", { recursive: true });
    await page.screenshot({
      path: join("visual", `${language}-${theme}-${viewport.width}.png`),
      fullPage: true
    });
  });
}

test("drawer opens and closes with the keyboard", async ({ page }) => {
  if (!baseURL) {
    throw new Error("E2E precondition missing: start S-portal and set PORTAL_E2E_BASE_URL.");
  }
  await page.setViewportSize({ width: 320, height: 800 });
  await page.goto("/es");
  const menu = page.locator(".menu-drawer > summary");
  await menu.focus();
  await page.keyboard.press("Enter");
  await expect(menu).toHaveAttribute("aria-expanded", "true");
  const links = page.locator(".menu-drawer .menu-panel a");
  await page.keyboard.press("Tab");
  await expect(links.first()).toBeFocused();
  await page.keyboard.press("Shift+Tab");
  await expect(menu).toBeFocused();
  await page.keyboard.press("Shift+Tab");
  await expect(links.last()).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(menu).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(links.first()).toBeFocused();
  await page.keyboard.press("Escape");
  await expect(menu).toHaveAttribute("aria-expanded", "false");
  await expect(menu).toBeFocused();
});

test("theme toggle persists for anonymous users", async ({ page }) => {
  if (!baseURL) {
    throw new Error("E2E precondition missing: start S-portal and set PORTAL_E2E_BASE_URL.");
  }
  await page.context().addCookies([{
    name: "sp_theme",
    value: "light",
    url: baseURL,
    sameSite: "Lax"
  }]);
  await page.goto("/es");
  const toggle = page.locator("[data-theme-toggle]");
  await expect(toggle).toHaveAttribute("aria-pressed", "false");

  const responsePromise = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/preferences/theme"
      && response.request().method() === "POST"
  );
  await toggle.click();
  const response = await responsePromise;

  expect(response.status()).toBe(204);
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await expect(toggle).toHaveAttribute("aria-pressed", "true");
  await expect.poll(async () => page.evaluate(() => localStorage.getItem("sp_theme"))).toBe("dark");
  await expect.poll(async () =>
    (await page.context().cookies(baseURL)).find((cookie) => cookie.name === "sp_theme")?.value
  ).toBe("dark");

  await page.reload();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await expect(toggle).toHaveAttribute("aria-pressed", "true");
});

test("language switch preserves the current public path", async ({ page }) => {
  if (!baseURL) {
    throw new Error("E2E precondition missing: start S-portal and set PORTAL_E2E_BASE_URL.");
  }
  await page.goto("/es");
  await page.locator('form[action="/preferences/locale/en"] button').click();
  await expect(page).toHaveURL(/\/en$/);
  await expect(page.locator("html")).toHaveAttribute("lang", "en");
});

test("frontend error reporting sends only bounded, non-identifying diagnostics", async ({ page }) => {
  if (!baseURL) {
    throw new Error("E2E precondition missing: start S-portal and set PORTAL_E2E_BASE_URL.");
  }
  const receivedPayloads: Record<string, unknown>[] = [];
  await page.route("**/api/telemetry/errors", async (route) => {
    receivedPayloads.push(route.request().postDataJSON() as Record<string, unknown>);
    await route.fulfill({ status: 204 });
  });
  await page.goto("/es");
  const consentPanel = page.getByRole("region", { name: "Preferencias de informes técnicos de errores" });
  const manageConsent = page.getByRole("button", { name: "Preferencias de informes de errores" });
  await expect(consentPanel).toBeVisible();
  const dispatchSyntheticError = () => page.evaluate(() => {
    const error = new TypeError("private@example.com token=secret");
    error.stack = [
      "TypeError: private@example.com",
      "    at submitForm (http://localhost:8090/assets/app.0123456789abcdef.js:12:34)",
      "    at private (http://localhost:8090/users/private@example.com?token=secret:1:2)"
    ].join("\n");
    window.dispatchEvent(new ErrorEvent("error", { error }));
  });
  await dispatchSyntheticError();
  await page.waitForTimeout(200);
  expect(receivedPayloads).toHaveLength(0);

  await consentPanel.getByRole("button", { name: "Aceptar" }).click();
  await expect(consentPanel).toBeHidden();
  await expect(manageConsent).toBeFocused();
  await expect.poll(() => page.evaluate(() =>
    localStorage.getItem("portal.error-telemetry-consent.v1")
  )).toBe("granted");
  await dispatchSyntheticError();
  await expect.poll(() => receivedPayloads).toHaveLength(1);
  const serializedPayload = JSON.stringify(receivedPayloads[0]);
  expect(serializedPayload).not.toContain("private@example.com");
  expect(serializedPayload).not.toContain("secret");
  expect(receivedPayloads[0]).toMatchObject({
    eventType: "UNCAUGHT_JS",
    errorName: "TypeError",
    locale: "es",
    path: "/es"
  });
  expect(receivedPayloads[0].stack).toBe("at app.0123456789abcdef.js:12:34");

  await manageConsent.click();
  await expect(consentPanel.getByRole("heading")).toBeFocused();
  await consentPanel.getByRole("button", { name: "Rechazar" }).click();
  await expect(consentPanel).toBeHidden();
  await expect.poll(() => page.evaluate(() =>
    localStorage.getItem("portal.error-telemetry-consent.v1")
  )).toBe("denied");
  await dispatchSyntheticError();
  await page.waitForTimeout(200);
  expect(receivedPayloads).toHaveLength(1);
});

test("frontend error reporting stays disabled when consent cannot be saved", async ({ page }) => {
  if (!baseURL) {
    throw new Error("E2E precondition missing: start S-portal and set PORTAL_E2E_BASE_URL.");
  }
  await page.addInitScript(() => {
    const originalSetItem = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key, value) {
      if (key === "portal.error-telemetry-consent.v1") {
        throw new DOMException("Storage unavailable", "QuotaExceededError");
      }
      originalSetItem.call(this, key, value);
    };
  });
  let receivedReport = false;
  await page.route("**/api/telemetry/errors", async (route) => {
    receivedReport = true;
    await route.fulfill({ status: 204 });
  });
  await page.goto("/es");
  const consentPanel = page.getByRole("region", { name: "Preferencias de informes técnicos de errores" });
  await consentPanel.getByRole("button", { name: "Aceptar" }).click();
  await expect(page.getByRole("alert")).toContainText("No se pudo guardar la preferencia");
  await expect(consentPanel).toBeVisible();
  await page.evaluate(() => {
    const error = new TypeError("synthetic diagnostic");
    window.dispatchEvent(new ErrorEvent("error", { error }));
  });
  await page.waitForTimeout(200);
  expect(receivedReport).toBe(false);
});

test("desktop navigation moves overflowing links into the accessible drawer", async ({ page }) => {
  if (!baseURL) {
    throw new Error("E2E precondition missing: start S-portal and set PORTAL_E2E_BASE_URL.");
  }
  await page.setViewportSize({ width: 768, height: 800 });

  const verifyOverflowDrawer = async (language: "es" | "en") => {
    await page.goto(`/${language}`);
    await page.evaluate(() => {
      const primaryLinks = document.querySelector("[data-primary-links]");
      const menuPanel = document.querySelector(".menu-panel");
      if (!primaryLinks || !menuPanel) throw new Error("Primary navigation markup is missing.");

      for (let index = 0; index < 8; index += 1) {
        const label = `Synthetic navigation item ${index}`;
        for (const list of [primaryLinks, menuPanel]) {
          const item = document.createElement("li");
          const link = document.createElement("a");
          link.href = `/synthetic-${index}`;
          link.textContent = label;
          item.append(link);
          list.append(item);
        }
      }
      window.dispatchEvent(new Event("resize"));
    });

    const navigation = page.locator("[data-primary-navigation]");
    await expect(navigation).toHaveClass(/menu-overflow/);
    await expect(page.locator("[data-primary-links]")).toBeHidden();
    expect(await page.evaluate(() =>
      document.documentElement.scrollWidth <= document.documentElement.clientWidth
    )).toBe(true);
    const menu = page.locator(".menu-drawer > summary");
    await expect(menu).toBeVisible();
    await menu.click();
    await expect(menu).toHaveAttribute("aria-expanded", "true");
    await expect(page.locator(".menu-drawer .menu-panel a")).toHaveCount(
      await page.locator("[data-primary-links] a").count()
    );
    await expect(page.locator(".menu-drawer .menu-panel a").filter({
      hasText: "Synthetic navigation item 7"
    })).toHaveCount(1);
    await expect(page.locator(".menu-drawer .menu-panel a[aria-current='page']")).toHaveCount(1);
  };

  await verifyOverflowDrawer("es");
  await verifyOverflowDrawer("en");
});

test("HTMX mutations send CSRF and expose accessible errors", async ({ page }) => {
  if (!baseURL) {
    throw new Error("E2E precondition missing: start S-portal and set PORTAL_E2E_BASE_URL.");
  }
  await page.goto("/es");
  const csrf = await page.evaluate(() => ({
    token: document.querySelector('meta[name="_csrf"]')?.getAttribute("content"),
    header: document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content")
  }));
  if (!csrf.token || !csrf.header) throw new Error("CSRF metadata is missing.");

  await page.evaluate(() => {
    const button = document.createElement("button");
    button.id = "htmx-theme-preference";
    button.type = "button";
    button.textContent = "Save synthetic preference";
    button.setAttribute("hx-post", "/preferences/theme");
    button.setAttribute("hx-vals", '{"theme":"dark"}');
    button.setAttribute("hx-target", "#preference-errors");
    document.body.append(button);
    (window as Window & { htmx: { process: (target: Element) => void } }).htmx.process(button);
  });
  const mutation = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/preferences/theme"
      && response.request().method() === "POST"
  );
  await page.locator("#htmx-theme-preference").click();
  const response = await mutation;
  expect(response.status()).toBe(204);
  expect(response.request().headers()[csrf.header.toLowerCase()]).toBe(csrf.token);

  await page.route("**/preferences/theme", (route) => route.fulfill({ status: 403, body: "Forbidden" }));
  await page.locator("#htmx-theme-preference").click();
  const errors = page.locator("#preference-errors");
  await expect(errors).toHaveAttribute("role", "alert");
  await expect(errors).toHaveAttribute("tabindex", "-1");
  await expect(errors).toContainText("No se pudo completar la solicitud");
  await expect(errors).toBeFocused();
});
