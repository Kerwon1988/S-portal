import { createHmac, randomUUID } from "node:crypto";
import { setTimeout as delay } from "node:timers/promises";

import AxeBuilder from "@axe-core/playwright";
import { expect, test, type APIRequestContext, type Page } from "@playwright/test";

const baseURL = process.env.PORTAL_E2E_BASE_URL;
const adminEmail = process.env.PORTAL_E2E_ADMIN_EMAIL;
const adminPassword = process.env.PORTAL_E2E_ADMIN_PASSWORD;
const adminTotpSecret = process.env.PORTAL_E2E_ADMIN_TOTP_SECRET;
const mailpitBaseURL = process.env.PORTAL_E2E_MAILPIT_URL ?? "http://127.0.0.1:8026";

type MailpitMessageSummary = {
  ID?: string;
  id?: string;
};

type MailpitMessage = {
  HTML?: unknown;
  Text?: unknown;
  Snippet?: unknown;
  html?: unknown;
  text?: unknown;
  snippet?: unknown;
};

test.use({ screenshot: "off", trace: "off" });

test.describe("E2E-001 invitation, permissions, and CMS editing", () => {
  test("MFA admin invites a worker, the invitation is consumed, and CMS access is enforced", async ({
    browser,
    page,
    request
  }) => {
    test.setTimeout(120_000);
    const missing = [
      ["PORTAL_E2E_BASE_URL", baseURL],
      ["PORTAL_E2E_ADMIN_EMAIL", adminEmail],
      ["PORTAL_E2E_ADMIN_PASSWORD", adminPassword],
      ["PORTAL_E2E_ADMIN_TOTP_SECRET", adminTotpSecret]
    ].filter(([, value]) => !value).map(([name]) => name);
    if (missing.length > 0) {
      throw new Error(`E2E-001 prerequisites are missing: ${missing.join(", ")}.`);
    }

    const portalURL = requireLoopbackUrl("PORTAL_E2E_BASE_URL", baseURL!);
    const mailpitURL = requireLoopbackUrl("PORTAL_E2E_MAILPIT_URL", mailpitBaseURL);
    const portalOrigin = portalURL.origin;
    await page.context().setExtraHTTPHeaders({ "Accept-Language": "en" });
    await page.setViewportSize({ width: 1280, height: 800 });
    await signInAdministrator(page, adminEmail!, adminPassword!, adminTotpSecret!);

    const invitationEmail = `e2e-${randomUUID()}@example.test`;
    await page.goto("/gestion/trabajadores/invitacion");
    await page.locator("#worker-email").fill(invitationEmail);
    await page.locator('form[action="/gestion/trabajadores/invitacion"] button[type="submit"]').click();
    await expect(page.getByRole("status")).toBeVisible();
    const invitationToken = await waitForInvitationToken(request, mailpitURL, invitationEmail, portalOrigin);

    const workerPassword = `E2e-${randomUUID()}-Long9!`;
    const workerContext = await browser.newContext({
      baseURL: portalURL.toString(),
      extraHTTPHeaders: { "Accept-Language": "es" }
    });
    try {
      const workerPage = await workerContext.newPage();
      await workerPage.setViewportSize({ width: 390, height: 844 });
      await workerPage.goto("/invitacion/aceptar");
      await expect(workerPage.getByRole("heading", { level: 1 })).toBeVisible();
      await workerPage.locator('input[name="token"]').fill(invitationToken);
      await workerPage.locator("#display-name").fill("Synthetic E2E worker");
      await workerPage.locator("#invite-password").fill(workerPassword);
      await workerPage.locator('input[name="termsAccepted"]').check();
      await workerPage.locator('input[name="privacyAccepted"]').check();

      const invitationAccessibility = await new AxeBuilder({ page: workerPage }).analyze();
      expect(invitationAccessibility.violations).toEqual([]);
      await workerPage.locator('form[action="/invitacion/aceptar"] button[type="submit"]').click();
      await expect(workerPage.getByRole("main"))
        .toContainText(/la invitación se ha aceptado|the invitation was accepted/i);

      await workerPage.goto("/invitacion/aceptar");
      await workerPage.locator('input[name="token"]').fill(invitationToken);
      await workerPage.locator("#display-name").fill("Synthetic replay attempt");
      await workerPage.locator("#invite-password").fill(workerPassword);
      await workerPage.locator('input[name="termsAccepted"]').check();
      await workerPage.locator('input[name="privacyAccepted"]').check();
      await workerPage.locator('form[action="/invitacion/aceptar"] button[type="submit"]').click();
      await expect(workerPage.getByRole("main"))
        .toContainText(/no se pudo aceptar la invitación|invitation could not be accepted/i);

      await page.goto(`/gestion/trabajadores?q=${encodeURIComponent(invitationEmail)}`);
      await expect(page.getByRole("table")).toContainText(invitationEmail);

      await workerPage.goto("/acceso");
      await workerPage.locator("#username").fill(invitationEmail);
      await workerPage.locator("#password").fill(workerPassword);
      await workerPage.locator('form[action="/acceso"] button[type="submit"]').click();
      const deniedPageResponse = await workerPage.goto("/gestion/contenido");
      expect(deniedPageResponse?.status()).toBe(403);
      await expect(workerPage.locator("#title")).toHaveCount(0);
    } finally {
      await workerContext.close();
    }

    await page.goto("/gestion/contenido");
    const homePageRow = page.locator("tbody tr").filter({ hasText: "home" }).first();
    await homePageRow.locator('a[href*="locale="]').click();
    await page.locator("#title").fill(`E2E draft ${randomUUID()}`);
    await page.locator("#block-type-0").selectOption("TEXT");
    await page.locator("#block-heading-0").fill("Synthetic E2E content");
    await page.locator("#block-body-0").fill("This unpublished draft verifies authorized CMS editing.");
    const contentAccessibility = await new AxeBuilder({ page }).analyze();
    expect(contentAccessibility.violations).toEqual([]);
    await page.locator('form button[type="submit"]').first().click();
    await expect(page.locator("#title")).toHaveValue(/E2E draft/);
  });
});

async function signInAdministrator(
  page: Page,
  email: string,
  password: string,
  totpSecret: string
): Promise<void> {
  await page.goto("/acceso");
  await page.locator("#username").fill(email);
  await page.locator("#password").fill(password);
  await page.locator('form[action="/acceso"] button[type="submit"]').click();
  if (new URL(page.url()).pathname !== "/cuenta/seguridad/mfa") {
    throw new Error("E2E-001 requires an administrator account with MFA already enrolled.");
  }
  await page.locator("#mfa-code").fill(currentTotp(totpSecret));
  await page.locator('form[action="/cuenta/seguridad/mfa/verificar"] button[type="submit"]').click();
  await page.goto("/gestion/trabajadores/invitacion");
  await expect(page.locator("#worker-email")).toBeVisible();
}

async function waitForInvitationToken(
  request: APIRequestContext,
  mailpitURL: URL,
  recipient: string,
  portalOrigin: string
): Promise<string> {
  const searchURL = new URL("/api/v1/search", mailpitURL);
  searchURL.searchParams.set("query", `to:${recipient}`);
  const deadline = Date.now() + 30_000;

  while (Date.now() < deadline) {
    const searchResponse = await request.get(searchURL.toString());
    if (!searchResponse.ok()) {
      throw new Error(`Mailpit invitation search failed with status ${searchResponse.status()}.`);
    }
    const search = await searchResponse.json() as {
      messages?: MailpitMessageSummary[];
      Messages?: MailpitMessageSummary[];
    };
    const messages = search.messages ?? search.Messages ?? [];
    for (const message of messages) {
      const messageId = message.ID ?? message.id;
      if (!messageId) continue;

      const detailResponse = await request.get(
        new URL(`/api/v1/message/${encodeURIComponent(messageId)}`, mailpitURL).toString()
      );
      if (!detailResponse.ok()) {
        throw new Error(`Mailpit invitation retrieval failed with status ${detailResponse.status()}.`);
      }
      const detail = await detailResponse.json() as MailpitMessage;
      const body = [detail.HTML, detail.Text, detail.Snippet, detail.html, detail.text, detail.snippet]
        .filter((value): value is string => typeof value === "string")
        .join("\n");
      for (const candidate of body.match(/https?:\/\/[^\s"'<>]+/g) ?? []) {
        try {
          const link = new URL(candidate.replaceAll("&amp;", "&").replace(/[),.]+$/, ""));
          if (link.origin !== portalOrigin || link.pathname !== "/invitacion/aceptar") continue;
          const token = link.searchParams.get("token");
          if (token) return token;
        } catch {
          continue;
        }
      }
    }
    await delay(1_000);
  }

  throw new Error("Mailpit did not receive a worker invitation within 30 seconds.");
}

function currentTotp(secret: string): string {
  const key = decodeBase32(secret);
  const counter = Buffer.alloc(8);
  counter.writeBigUInt64BE(BigInt(Math.floor(Date.now() / 30_000)));
  const digest = createHmac("sha1", key).update(counter).digest();
  const offset = digest[digest.length - 1] & 0x0f;
  const binary = digest.readUInt32BE(offset) & 0x7fffffff;
  return String(binary % 1_000_000).padStart(6, "0");
}

function decodeBase32(secret: string): Buffer {
  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  const normalized = secret.replace(/[\s=]/g, "").toUpperCase();
  const bytes: number[] = [];
  let buffer = 0;
  let bits = 0;

  for (const character of normalized) {
    const value = alphabet.indexOf(character);
    if (value < 0) throw new Error("E2E-001 TOTP secret must be base32.");
    buffer = (buffer << 5) | value;
    bits += 5;
    if (bits >= 8) {
      bits -= 8;
      bytes.push((buffer >> bits) & 0xff);
      buffer &= (1 << bits) - 1;
    }
  }

  if (bytes.length === 0) throw new Error("E2E-001 TOTP secret is empty.");
  return Buffer.from(bytes);
}

function requireLoopbackUrl(name: string, value: string): URL {
  let parsed: URL;
  try {
    parsed = new URL(value);
  } catch {
    throw new Error(`${name} must be a valid loopback URL.`);
  }
  if (parsed.protocol !== "http:"
      || !["localhost", "127.0.0.1", "[::1]"].includes(parsed.hostname)) {
    throw new Error(`${name} must point to a local HTTP service; external targets are not allowed.`);
  }
  return parsed;
}
