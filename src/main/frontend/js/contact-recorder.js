export const MAX_AUDIO_SECONDS = 120;

export function chooseRecorderMimeType(mediaRecorder = globalThis.MediaRecorder) {
  if (!mediaRecorder?.isTypeSupported) return null;
  return ["audio/webm;codecs=opus", "audio/mp4", "audio/ogg;codecs=opus"]
    .find((type) => mediaRecorder.isTypeSupported(type)) ?? null;
}

export function validAudioDuration(duration) {
  return Number.isFinite(duration) && duration > 0 && duration <= MAX_AUDIO_SECONDS;
}

if (typeof document !== "undefined") {
  for (const uploadRoot of document.querySelectorAll("[data-contact-uploads]")) {
    const input = uploadRoot.querySelector("input[type='file']");
    const list = uploadRoot.querySelector("[data-upload-list]");
    if (!input || !list) continue;
    const render = () => {
      list.replaceChildren();
      Array.from(input.files ?? []).forEach((file, index) => {
        const item = document.createElement("li");
        item.className = "flex items-center justify-between gap-3 rounded border border-border p-2";
        const name = document.createElement("span");
        name.textContent = `${file.name} (${file.size} bytes)`;
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "min-h-11 underline";
        remove.textContent = uploadRoot.dataset.removeLabel ?? "Remove";
        remove.setAttribute("aria-label", `${remove.textContent}: ${file.name}`);
        remove.addEventListener("click", () => {
          try {
            const transfer = new DataTransfer();
            Array.from(input.files ?? []).forEach((candidate, candidateIndex) => {
              if (candidateIndex !== index) transfer.items.add(candidate);
            });
            input.files = transfer.files;
            render();
          } catch {
            input.value = "";
            render();
          }
        });
        item.append(name, remove);
        list.append(item);
      });
    };
    input.addEventListener("change", render);
  }

  for (const root of document.querySelectorAll("[data-contact-recorder]")) {
    const recordButton = root.querySelector("[data-recorder-start]");
    const stopButton = root.querySelector("[data-recorder-stop]");
    const discardButton = root.querySelector("[data-recorder-discard]");
    const preview = root.querySelector("[data-recorder-preview]");
    const status = root.querySelector("[data-recorder-status]");
    const audioInput = root.querySelector("[name='audio']");
    const durationInput = root.querySelector("[name='audioDurationSeconds']");
    let recorder;
    let stream;
    let timer;
    let startedAt;
    let chunks = [];
    let objectUrl;

    const setStatus = (key) => {
      if (status) status.textContent = root.dataset[key] ?? "";
    };
    const cleanupStream = () => {
      clearTimeout(timer);
      stream?.getTracks().forEach((track) => track.stop());
      stream = undefined;
      if (stopButton) stopButton.disabled = true;
      if (recordButton) recordButton.disabled = false;
    };
    const discard = () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      objectUrl = undefined;
      if (preview) { preview.hidden = true; preview.removeAttribute("src"); }
      if (audioInput) audioInput.value = "";
      if (durationInput) durationInput.value = "";
      if (discardButton) discardButton.hidden = true;
      setStatus("recorderIdle");
    };

    if (!globalThis.MediaRecorder || !navigator.mediaDevices?.getUserMedia) {
      if (recordButton) recordButton.hidden = true;
      setStatus("recorderUnavailable");
    }

    recordButton?.addEventListener("click", async () => {
      const mimeType = chooseRecorderMimeType();
      if (!mimeType) {
        setStatus("recorderUnsupported");
        return;
      }
      try {
        stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        chunks = [];
        recorder = new MediaRecorder(stream, { mimeType });
        recorder.addEventListener("dataavailable", (event) => {
          if (event.data.size > 0) chunks.push(event.data);
        });
        recorder.addEventListener("stop", () => {
          const seconds = Math.max(1, Math.ceil((performance.now() - startedAt) / 1000));
          if (!validAudioDuration(seconds)) { discard(); cleanupStream(); return; }
          const extension = mimeType.includes("mp4") ? "m4a" : mimeType.includes("ogg") ? "ogg" : "webm";
          const blob = new Blob(chunks, { type: mimeType });
          const file = new File([blob], `contact-audio.${extension}`, { type: mimeType });
          if (audioInput) {
            try {
              const transfer = new DataTransfer();
              transfer.items.add(file);
              audioInput.files = transfer.files;
            } catch {
              setStatus("recorderFallback");
              cleanupStream();
              return;
            }
          }
          if (durationInput) durationInput.value = String(seconds);
          objectUrl = URL.createObjectURL(blob);
          if (preview) { preview.src = objectUrl; preview.hidden = false; }
          if (discardButton) discardButton.hidden = false;
          setStatus("recorderReady");
          cleanupStream();
        }, { once: true });
        recorder.start(1000);
        startedAt = performance.now();
        if (recordButton) recordButton.disabled = true;
        if (stopButton) stopButton.disabled = false;
        setStatus("recorderRecording");
        timer = setTimeout(() => recorder?.state === "recording" && recorder.stop(), MAX_AUDIO_SECONDS * 1000);
      } catch {
        cleanupStream();
        setStatus("recorderPermission");
      }
    });
    stopButton?.addEventListener("click", () => {
      if (recorder?.state === "recording") recorder.stop();
    });
    discardButton?.addEventListener("click", discard);
    audioInput?.addEventListener("change", () => {
      const file = audioInput.files?.[0];
      if (!file || durationInput?.value) return;
      const probe = new Audio();
      const url = URL.createObjectURL(file);
      probe.addEventListener("loadedmetadata", () => {
        const seconds = Math.ceil(probe.duration);
        if (validAudioDuration(seconds) && durationInput) {
          durationInput.value = String(seconds);
          setStatus("recorderReady");
        } else {
          audioInput.value = "";
          setStatus("recorderTooLong");
        }
        URL.revokeObjectURL(url);
      }, { once: true });
      probe.addEventListener("error", () => {
        audioInput.value = "";
        URL.revokeObjectURL(url);
        setStatus("recorderInvalid");
      }, { once: true });
      probe.src = url;
    });
  }
}
