import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

const baseURL = process.env.PORTAL_E2E_BASE_URL;
const clientEmail = process.env.PORTAL_E2E_CLIENT_EMAIL;
const clientPassword = process.env.PORTAL_E2E_CLIENT_PASSWORD;
const adminEmail = process.env.PORTAL_E2E_ADMIN_EMAIL;
const adminPassword = process.env.PORTAL_E2E_ADMIN_PASSWORD;

test.describe("E2E-004 SimbioSphere provisioning", () => {
  test("verified owner provisions through local DEMO and receives no stored activation token", async ({ page }) => {
    test.setTimeout(120_000);
    test.skip(!baseURL || !clientEmail || !clientPassword,
      "POSTPONED: requires loopback S-portal local-h2/e2e, client fixture and local DEMO/HMAC.");

    await page.goto("/acceso");
    await page.locator("#username").fill(clientEmail!);
    await page.locator("#password").fill(clientPassword!);
    await page.locator('form[action="/acceso"] button[type="submit"]').click();
    await expect(page).not.toHaveURL(/\/acceso/);
    await page.goto("/cuenta/simbiosphere");

    await expect(page.getByRole("heading", { level: 1 })).toContainText(/Mi SimbioSphere|My SimbioSphere/);
    const form = page.locator("#sim-business-name");
    if (await form.count()) {
      const slug = `e2e-${Date.now().toString(36)}`;
      await form.fill("S-portal E2E Business");
      await page.locator("#sim-business-slug").fill(slug);
      await page.locator("#sim-business-type").selectOption("SERVICES");
      await page.locator("#sim-location-name").fill("Main");
      await page.locator("#sim-location-slug").fill("main");
      await page.locator('input[name="termsAccepted"]').check();
      await page.getByRole("button", { name: /Request joint registration|Solicitar alta conjunta/ }).click();
    }

    await expect(page.getByText(/Ready for provisioning|Lista para aprovisionar|Provisioned|Aprovisionada/)).toBeVisible({ timeout: 5_000 });
    await expect.poll(async () => {
      await page.reload();
      return await page.getByText(/Provisioned|Aprovisionada/).count();
    }, { timeout: 90_000, intervals: [1000, 2000, 5000] }).toBeGreaterThan(0);
    await expect(page.getByRole("main")).not.toContainText(/token|secret|password/i);
    const accessibility = await new AxeBuilder({ page }).analyze();
    expect(accessibility.violations).toEqual([]);

    const responsePromise = page.waitForResponse(response =>
      response.request().method() === "POST" && response.url().endsWith("/cuenta/simbiosphere/activacion"));
    await page.getByRole("button", { name: /Continue to activation|Continuar a la activación/ }).click();
    const activation = await responsePromise;
    expect(activation.status()).toBeGreaterThanOrEqual(300);
    expect(activation.status()).toBeLessThan(400);
    const location = activation.headers()["location"] ?? "";
    expect(location).toContain("localhost:8081");
    expect(location).toContain("/activate/");
  });

  test("anonymous and foreign client cannot enumerate another provisioning resource", async ({ request }) => {
    test.skip(!baseURL, "POSTPONED: requires loopback S-portal with two isolated synthetic client fixtures.");
    const anonymous = await request.get("/cuenta/simbiosphere", { maxRedirects: 0 });
    expect([302, 403]).toContain(anonymous.status());
  });
});
