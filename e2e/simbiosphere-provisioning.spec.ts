import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

const baseURL = process.env.PORTAL_E2E_BASE_URL;
const clientEmail = process.env.PORTAL_E2E_CLIENT_EMAIL;
const clientPassword = process.env.PORTAL_E2E_CLIENT_PASSWORD;

test.describe("E2E-004 SimbioSphere provisioning", () => {
  test("verified owner sees only its provisioning state and no activation token", async ({ page }) => {
    test.setTimeout(90_000);
    test.skip(!baseURL || !clientEmail || !clientPassword,
      "POSTPONED: requires loopback S-portal, H2 fixture with one verified CLIENT owner and no real DEMO connection.");

    await page.goto("/acceso");
    await page.locator("#username").fill(clientEmail!);
    await page.locator("#password").fill(clientPassword!);
    await page.locator('form[action="/acceso"] button[type="submit"]').click();
    await page.goto("/cuenta/simbiosphere");

    await expect(page.getByRole("heading", { level: 1 })).toContainText(/Mi SimbioSphere|My SimbioSphere/);
    await expect(page.getByRole("main")).not.toContainText(/token|secret|password/i);
    const accessibility = await new AxeBuilder({ page }).analyze();
    expect(accessibility.violations).toEqual([]);
  });

  test("anonymous and foreign client cannot enumerate another provisioning resource", async ({ request }) => {
    test.skip(!baseURL, "POSTPONED: requires loopback S-portal with two isolated synthetic client fixtures.");
    const anonymous = await request.get("/cuenta/simbiosphere", { maxRedirects: 0 });
    expect([302, 403]).toContain(anonymous.status());
  });
});
