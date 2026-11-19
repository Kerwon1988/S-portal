export async function solveAltchaChallenge(challenge, cryptoProvider = globalThis.crypto) {
  if (challenge?.algorithm !== "SHA-256"
      || typeof challenge.challenge !== "string"
      || !/^[0-9a-f]{64}$/.test(challenge.challenge)
      || typeof challenge.salt !== "string"
      || typeof challenge.signature !== "string"
      || !/^[0-9a-f]{64}$/.test(challenge.signature)
      || !Number.isSafeInteger(challenge.maxnumber)
      || challenge.maxnumber < 1
      || challenge.maxnumber > 4_194_304
      || !cryptoProvider?.subtle) {
    throw new Error("Invalid ALTCHA challenge");
  }

  const encoder = new TextEncoder();
  for (let number = 0; number <= challenge.maxnumber; number += 1) {
    const digest = new Uint8Array(await cryptoProvider.subtle.digest(
      "SHA-256",
      encoder.encode(`${challenge.salt}${number}`)
    ));
    const solution = Array.from(digest, (byte) => byte.toString(16).padStart(2, "0")).join("");
    if (solution === challenge.challenge) {
      return btoa(JSON.stringify({
        algorithm: challenge.algorithm,
        challenge: challenge.challenge,
        number,
        salt: challenge.salt,
        signature: challenge.signature
      }));
    }
  }
  throw new Error("ALTCHA challenge has no solution within its maximum");
}

if (typeof document !== "undefined") {
  for (const form of document.querySelectorAll("form[data-anti-bot]")) {
    const payload = form.querySelector("[name='altcha']");
    const fallback = form.querySelector("[name='humanConfirmation']");
    const error = form.querySelector("[data-anti-bot-error]");
    if (!payload || !error) continue;

    let solving = false;
    form.addEventListener("submit", async (event) => {
      if (fallback?.checked || payload.value) return;
      event.preventDefault();
      if (solving) return;
      solving = true;

      const submit = form.querySelector("[type='submit']");
      if (submit) submit.disabled = true;
      error.hidden = true;
      try {
        const challengeUrl = form.dataset.challengeUrl;
        if (!challengeUrl) throw new Error("ALTCHA challenge URL is missing");
        const response = await fetch(challengeUrl, {
          method: "GET",
          credentials: "same-origin",
          cache: "no-store",
          headers: { Accept: "application/json" }
        });
        if (!response.ok) throw new Error("ALTCHA challenge request failed");
        payload.value = await solveAltchaChallenge(await response.json());
        if (submit) submit.disabled = false;
        if (submit) form.requestSubmit(submit);
        else form.requestSubmit();
      } catch {
        error.hidden = false;
        error.focus();
      } finally {
        solving = false;
        if (submit) submit.disabled = false;
      }
    });
  }
}
