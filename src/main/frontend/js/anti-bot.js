for (const form of document.querySelectorAll("form[data-anti-bot]")) {
  const payload = form.querySelector("[name='altcha']");
  const algorithm = form.querySelector("[name='altchaAlgorithm']");
  const challenge = form.querySelector("[name='altchaChallenge']");
  const maxnumber = form.querySelector("[name='altchaMaxnumber']");
  const salt = form.querySelector("[name='altchaSalt']");
  const signature = form.querySelector("[name='altchaSignature']");
  const fallback = form.querySelector("[name='humanConfirmation']");
  const error = form.querySelector("[data-anti-bot-error]");
  if (!payload || !algorithm || !challenge || !maxnumber || !salt || !signature) continue;

  const encodePayload = (number) => {
    const value = JSON.stringify({
      algorithm: algorithm.value,
      challenge: challenge.value,
      number,
      salt: salt.value,
      signature: signature.value
    });
    payload.value = btoa(value);
  };

  form.addEventListener("submit", async (event) => {
    if (fallback?.checked) return;
    if (!globalThis.crypto?.subtle) {
      event.preventDefault();
      if (error) error.hidden = false;
      return;
    }
    event.preventDefault();
    const submit = form.querySelector("[type='submit']");
    if (submit) submit.disabled = true;
    if (error) error.hidden = true;
    try {
      const limit = Number(maxnumber.value);
      for (let number = 0; number <= limit; number += 1) {
        const digest = new Uint8Array(await crypto.subtle.digest(
          "SHA-256",
          new TextEncoder().encode(`${salt.value}${number}`)
        ));
        if (Array.from(digest, (byte) => byte.toString(16).padStart(2, "0")).join("") === challenge.value) {
          encodePayload(number);
          form.requestSubmit(submit ?? undefined);
          return;
        }
      }
      if (error) error.hidden = false;
    } finally {
      if (submit) submit.disabled = false;
    }
  });
}
