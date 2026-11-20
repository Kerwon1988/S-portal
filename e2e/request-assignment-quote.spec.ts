import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Browser, type Page } from "@playwright/test";
import { randomUUID } from "node:crypto";

const baseURL = process.env.PORTAL_E2E_BASE_URL;
const clientEmail = process.env.PORTAL_E2E_CLIENT_EMAIL;
const clientPassword = process.env.PORTAL_E2E_CLIENT_PASSWORD;
const employeeEmail = process.env.PORTAL_E2E_EMPLOYEE_EMAIL;
const employeePassword = process.env.PORTAL_E2E_EMPLOYEE_PASSWORD;
const employeeUserId = process.env.PORTAL_E2E_EMPLOYEE_USER_ID;

test.describe("E2E-002 request assignment and quote", () => {
  test("owner request flows through assignment, quote and owner decision", async ({ browser }) => {
    test.setTimeout(120_000);
    const missing = [
      ["PORTAL_E2E_BASE_URL", baseURL],
      ["PORTAL_E2E_CLIENT_EMAIL", clientEmail],
      ["PORTAL_E2E_CLIENT_PASSWORD", clientPassword],
      ["PORTAL_E2E_EMPLOYEE_EMAIL", employeeEmail],
      ["PORTAL_E2E_EMPLOYEE_PASSWORD", employeePassword],
      ["PORTAL_E2E_EMPLOYEE_USER_ID", employeeUserId]
    ].filter(([, value]) => !value).map(([name]) => name);
    if (missing.length > 0) {
      throw new Error(`E2E-002 POSTPONED prerequisites are missing: ${missing.join(", ")}; seed a verified client with a valid tax identifier and an active authorized employee.`);
    }

    const clientContext = await browser.newContext({ baseURL, extraHTTPHeaders: { "Accept-Language": "en" } });
    const employeeContext = await browser.newContext({ baseURL, extraHTTPHeaders: { "Accept-Language": "es" } });
    try {
      const client = await clientContext.newPage();
      await client.setViewportSize({ width: 390, height: 844 });
      await signIn(client, clientEmail!, clientPassword!);
      await client.goto("/cuenta/solicitudes/nueva");
      const title = `Synthetic quote ${randomUUID()}`;
      await client.locator("#title").fill(title);
      await client.locator("#description").fill("Synthetic E2E request for assignment and quote evidence.");
      await client.locator("#category").selectOption("WEB_APPLICATION");
      await client.locator('form button[type="submit"]').click();
      await expect(client).toHaveURL(/\/cuenta\/solicitudes\/[0-9a-f-]+/);
      const requestPath = new URL(client.url()).pathname;
      const requestId = requestPath.split("/").at(-1)!;

      const employee = await employeeContext.newPage();
      await employee.setViewportSize({ width: 1280, height: 800 });
      await signIn(employee, employeeEmail!, employeePassword!);
      await employee.goto(`/gestion/solicitudes/${requestId}`);
      await employee.locator("#responsible").selectOption(employeeUserId!);
      await employee.locator('form[action$="/asignacion"] button[type="submit"]').click();
      await expect(employee.locator("#main-content")).toContainText(employeeUserId!);

      await employee.locator("#target").selectOption("EN_ANALISIS");
      await employee.locator('form[action$="/operacion"] button[type="submit"]').click();
      await employee.locator("#netAmount").fill("100.0000");
      await employee.locator("#currency").fill("EUR");
      await employee.locator("#taxRate").fill("21.0000");
      await employee.locator("#validUntil").fill("2030-12-31");
      await employee.locator('form[action$="/presupuesto"] button[type="submit"]').click();
      await employee.locator('form[action$="/presupuesto/1/enviar"] button[type="submit"]').click();
      await expect(employee.locator("#quote")).toContainText(/enviado|sent/i);
      const accessibility = await new AxeBuilder({ page: employee }).analyze();
      expect(accessibility.violations).toEqual([]);

      await client.goto(requestPath);
      const quote = client.locator("article").filter({ hasText: /100\.0000 EUR/ });
      await expect(quote).toBeVisible();
      await quote.getByRole("checkbox").check();
      await quote.getByRole("button", { name: /accept|aceptar/i }).click();
      await expect(client.locator("#main-content")).toContainText(/accepted|aceptado/i);

      const foreignContext = await browser.newContext({ baseURL });
      try {
        const denied = await foreignContext.request.get(`/cuenta/solicitudes/${requestId}`, { maxRedirects: 0 });
        expect(denied.status()).toBe(302);
        expect(denied.headers()["location"]).toMatch(/\/acceso(?:\?|$)/);
      } finally {
        await foreignContext.close();
      }
    } finally {
      await clientContext.close();
      await employeeContext.close();
    }
  });
});

async function signIn(page: Page, email: string, password: string): Promise<void> {
  await page.goto("/acceso");
  await page.locator("#username").fill(email);
  await page.locator("#password").fill(password);
  await page.locator('form[action="/acceso"] button[type="submit"]').click();
  await expect(page).not.toHaveURL(/\/acceso$/);
}
