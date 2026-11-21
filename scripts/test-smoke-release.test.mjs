import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import test from 'node:test';

test('release smoke refuses an absent or non-HTTPS target', () => {
  const absent = spawnSync(process.execPath, ['scripts/smoke-release.mjs'], { env: { ...process.env, PORTAL_RELEASE_BASE_URL: '' } });
  const insecure = spawnSync(process.execPath, ['scripts/smoke-release.mjs'], { env: { ...process.env, PORTAL_RELEASE_BASE_URL: 'http://localhost:8090' } });
  assert.equal(absent.status, 2);
  assert.equal(insecure.status, 2);
  assert.match(absent.stderr.toString(), /approved absolute HTTPS URL/);
});
