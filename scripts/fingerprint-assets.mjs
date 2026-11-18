import { createHash } from "node:crypto";
import { copyFile, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const outputDirectory = join(root, "target", "classes", "static", "assets");
const generatedDirectory = join(root, "target", "frontend");
const inputs = [
  ["app.css", join(generatedDirectory, "app.css")],
  ["htmx.js", join(root, "node_modules", "htmx.org", "dist", "htmx.min.js")],
  ["alpine-csp.js", join(root, "node_modules", "@alpinejs", "csp", "dist", "cdn.min.js")],
  ["theme.js", join(root, "src", "main", "frontend", "js", "theme.js")],
  ["menu.js", join(root, "src", "main", "frontend", "js", "menu.js")],
  ["htmx-config.js", join(root, "src", "main", "frontend", "js", "htmx-config.js")],
  ["error-reporter.js", join(root, "src", "main", "frontend", "js", "error-reporter.js")],
  ["anti-bot.js", join(root, "src", "main", "frontend", "js", "anti-bot.js")],
  ["simbio-logo.png", join(root, "src", "main", "frontend", "images", "simbio-logo.png")]
];
const manifest = {};

await rm(outputDirectory, { recursive: true, force: true });
await mkdir(outputDirectory, { recursive: true });

for (const [logicalName, source] of inputs) {
  const contents = await readFile(source);
  const fingerprint = createHash("sha256").update(contents).digest("hex").slice(0, 16);
  const extension = logicalName.slice(logicalName.lastIndexOf("."));
  const fileName = `${logicalName.slice(0, -extension.length)}.${fingerprint}${extension}`;
  const destination = join(outputDirectory, fileName);
  await mkdir(dirname(destination), { recursive: true });
  await copyFile(source, destination);
  manifest[logicalName] = `/assets/${fileName}`;
}

await writeFile(
  join(outputDirectory, "asset-manifest.json"),
  `${JSON.stringify(manifest, null, 2)}\n`,
  "utf8"
);
