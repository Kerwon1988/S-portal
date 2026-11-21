import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('production topology does not publish app, database, or management ports', async () => {
  const compose = await readFile('docker-compose.prod.yml', 'utf8');
  assert.doesNotMatch(compose, /1433:|8090:|8091:/);
  assert.match(compose, /read_only: true/);
  assert.match(compose, /no-new-privileges:true/);
  assert.match(compose, /cap_drop:[\s\S]*ALL/);
});

test('CSP implementation and documentation reject unsafe bypasses', async () => {
  const csp = await readFile('src/main/java/com/simbio/core/config/security/CspNonceFilter.java', 'utf8');
  const docs = await readFile('docs/operations/uat-go-no-go.md', 'utf8');
  assert.match(csp, /Content-Security-Policy/);
  assert.doesNotMatch(csp, /unsafe-inline|unsafe-eval|Report-Only/);
  assert.match(docs, /NO-GO/);
  assert.match(docs, /PREPARADO_NO_EJECUTADO/);
});
