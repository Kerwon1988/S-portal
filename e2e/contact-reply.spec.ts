import { randomUUID } from "node:crypto";

import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";

const baseURL = process.env.PORTAL_E2E_BASE_URL;
const employeeEmail = process.env.PORTAL_E2E_EMPLOYEE_EMAIL;
const employeePassword = process.env.PORTAL_E2E_EMPLOYEE_PASSWORD;
const mailpitURL = process.env.PORTAL_E2E_MAILPIT_URL ?? "http://127.0.0.1:8026";

test.describe("E2E-003 contact and reply", () => {
  test("anonymous contact reaches the inbox and a one-use continuation stays thread-scoped", async ({ page }) => {
    test.setTimeout(120_000);
    test.skip(!baseURL || !employeeEmail || !employeePassword,
      "Requires local S-portal, a synthetic INBOX_MANAGE employee, approved contact consent and Mailpit.");

    await page.goto("/es/contacto");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await expect(page.locator("#personName")).toBeEnabled();
    await page.locator("#personName").fill("E2E synthetic contact");
    await page.locator("#email").fill(`e2e-${randomUUID()}@example.test`);
    await page.locator("#reason").fill("E2E synthetic reason");
    await page.locator("#message").fill("Synthetic contact message; no personal data.");
    await page.locator('input[name="consentAccepted"]').check();

    const formAccessibility = await new AxeBuilder({ page }).analyze();
    expect(formAccessibility.violations).toEqual([]);
    await expect.poll(() => page.locator('input[name="altcha"]').inputValue()).not.toBe("");
    await page.locator('form[action="/es/contacto"] button[type="submit"]').click();
    await expect(page.getByRole("status")).toContainText(/recibido|received/i);

    await signInEmployee(page, employeeEmail!, employeePassword!);
    await page.goto("/gestion/bandeja");
    await page.getByRole("link", { name: /abrir conversación|open conversation/i }).first().click();
    await page.getByRole("textbox", { name: /mensaje|message/i }).fill("Synthetic staff response.");
    await page.locator('form[action$="/responder"] button[type="submit"]').click();
    await expect(page.getByRole("status")).toContainText(/guardado|saved/i);

    const issueResponse = page.waitForNavigation();
    await page.locator('form[action$="/enlace-continuacion"] button[type="submit"]').click();
    await issueResponse;
    const continuationHref = await page.getByRole("link", { name: /abrir enlace|open link/i }).getAttribute("href");
    expect(continuationHref).toMatch(/^\/continuar\/[^/]+$/);
    await page.goto(continuationHref!);
    await page.getByRole("textbox", { name: /mensaje|message/i }).fill("Synthetic one-use continuation.");
    await page.locator('form[action^="/continuar/"] button[type="submit"]').click();
    await expect(page.getByRole("heading", { level: 1 })).toContainText(/recibida|received/i);

    const replay = await page.goto(continuationHref!);
    expect(replay?.status()).toBe(404);
    expect(page.url()).not.toContain("token=");
    expect(mailpitURL).toMatch(/^http:\/\/(127\.0\.0\.1|localhost):/);
  });
});

async function signInEmployee(page: Page, email: string, password: string): Promise<void> {
  await page.goto("/acceso");
  await page.locator("#username").fill(email);
  await page.locator("#password").fill(password);
  await page.locator('form[action="/acceso"] button[type="submit"]').click();
  await page.goto("/gestion/bandeja");
  await expect(page.getByRole("heading", { level: 1 })).toContainText(/bandeja|inbox/i);
}
