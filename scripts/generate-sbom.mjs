import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { createHash } from 'node:crypto';

const lock = JSON.parse(await readFile('package-lock.json', 'utf8'));
const pom = await readFile('pom.xml', 'utf8');
const components = [];
for (const [path, item] of Object.entries(lock.packages || {})) {
  if (!path.startsWith('node_modules/') || !item.version) continue;
  components.push({ type: 'library', name: path.slice(13), version: item.version, purl: `pkg:npm/${encodeURIComponent(path.slice(13))}@${item.version}`, licenses: item.license ? [{ license: { id: item.license } }] : undefined, properties: [{ name: 'source', value: 'package-lock.json' }] });
}
for (const match of pom.matchAll(/<dependency>\s*<groupId>([^<]+)<\/groupId>\s*<artifactId>([^<]+)<\/artifactId>(?:\s*<version>([^<]+)<\/version>)?/gs)) {
  const version = match[3] || 'managed-by-parent';
  components.push({ type: 'library', group: match[1], name: match[2], version, purl: `pkg:maven/${match[1]}/${match[2]}@${version}`, properties: [{ name: 'source', value: 'pom.xml' }] });
}
const content = { bomFormat: 'CycloneDX', specVersion: '1.5', serialNumber: `urn:uuid:${crypto.randomUUID()}`, version: 1, metadata: { timestamp: new Date().toISOString(), component: { type: 'application', name: 's-portal', version: '0.0.1-SNAPSHOT' } }, components };
await mkdir('target', { recursive: true });
await writeFile('target/s-portal-manifest.cdx.json', JSON.stringify(content, null, 2));
const digest = createHash('sha256').update(JSON.stringify(content)).digest('hex');
console.log(JSON.stringify({ components: components.length, sha256: digest, path: 'target/s-portal-manifest.cdx.json' }));
