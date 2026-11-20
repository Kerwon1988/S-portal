import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import test from "node:test";

const source = await readFile(resolve("src/main/frontend/js/contact-recorder.js"), "utf8");
const recorder = await import(`data:text/javascript;base64,${Buffer.from(source).toString("base64")}`);
const template = await readFile(resolve("src/main/resources/templates/messaging/contact-preview.html"), "utf8");

test("selects only explicitly supported recorder codecs", () => {
  const recorderApi = { isTypeSupported: (type) => type === "audio/mp4" };
  assert.equal(recorder.chooseRecorderMimeType(recorderApi), "audio/mp4");
  assert.equal(recorder.chooseRecorderMimeType({ isTypeSupported: () => false }), null);
});

test("enforces the strict 120 second client duration boundary", () => {
  assert.equal(recorder.validAudioDuration(120), true);
  assert.equal(recorder.validAudioDuration(120.01), false);
  assert.equal(recorder.validAudioDuration(0), false);
  assert.equal(recorder.validAudioDuration(Infinity), false);
});

test("requests recording only from an accessible button and keeps file fallback", () => {
  assert.match(template, /data-recorder-start/);
  assert.match(template, /name="audio"[^>]*type="file"|type="file"[^>]*name="audio"/);
  assert.match(template, /data-contact-recorder/);
  assert.match(template, /data-upload-list/);
  assert.match(source, /transfer\.items\.add/);
  assert.match(source, /addEventListener\("click"/);
  assert.match(source, /getUserMedia\(\{ audio: true \}\)/);
});
