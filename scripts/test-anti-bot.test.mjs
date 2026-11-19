import assert from "node:assert/strict";
import { createHash, webcrypto } from "node:crypto";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import test from "node:test";

const source = await readFile(resolve("src/main/frontend/js/anti-bot.js"), "utf8");
const antiBot = await import(`data:text/javascript;base64,${Buffer.from(source).toString("base64")}`);
const registrationTemplate = await readFile(
  resolve("src/main/resources/templates/customer/registration.html"),
  "utf8"
);
const challengeTemplate = await readFile(
  resolve("src/main/resources/templates/fragments/anti-bot.html"),
  "utf8"
);

function challengeFor(salt, number) {
  return {
    algorithm: "SHA-256",
    challenge: createHash("sha256").update(`${salt}${number}`).digest("hex"),
    maxnumber: 256,
    salt,
    signature: "a".repeat(64)
  };
}

test("solves a challenge and serializes the official ALTCHA payload", async () => {
  const challenge = challengeFor(`${"a".repeat(48)}|1798761660`, 91);
  const encoded = await antiBot.solveAltchaChallenge(challenge, webcrypto);
  const payload = JSON.parse(Buffer.from(encoded, "base64").toString("utf8"));

  assert.deepEqual(payload, {
    algorithm: challenge.algorithm,
    challenge: challenge.challenge,
    number: 91,
    salt: challenge.salt,
    signature: challenge.signature
  });
});

test("rejects malformed or unsupported challenges", async () => {
  await assert.rejects(
    antiBot.solveAltchaChallenge({ algorithm: "SHA-512" }, webcrypto),
    /Invalid ALTCHA challenge/
  );
  await assert.rejects(
    antiBot.solveAltchaChallenge({ ...challengeFor("salt", 1), maxnumber: 4_194_305 }, webcrypto),
    /Invalid ALTCHA challenge/
  );
});

test("registration form wires the same-origin challenge and accessible fallback", () => {
  assert.match(registrationTemplate, /data-challenge-url=@\{\{?\/anti-bot\/challenge/);
  assert.match(registrationTemplate, /fragments\/anti-bot :: challenge/);
  assert.match(challengeTemplate, /name="altchaFallback"/);
  assert.match(challengeTemplate, /name="humanConfirmation"/);
  assert.match(challengeTemplate, /type="module"/);
});
