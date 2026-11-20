const baseUrl = process.env.PORTAL_RELEASE_BASE_URL;
if (!baseUrl || !baseUrl.startsWith('https://')) {
  console.error('PORTAL_RELEASE_BASE_URL must be an approved absolute HTTPS URL');
  process.exit(2);
}
const paths = ['/', '/es', '/en', '/acceso', '/actuator/health'];
let failed = false;
for (const path of paths) {
  const response = await fetch(new URL(path, baseUrl), { redirect: 'manual', signal: AbortSignal.timeout(10000) });
  if (path === '/actuator/health') {
    if (response.status !== 403 && response.status !== 404) {
      console.error(`${path} is publicly reachable: ${response.status}`);
      failed = true;
    }
  } else if (response.status >= 400) {
    console.error(`${path}: ${response.status}`);
    failed = true;
  } else {
    console.log(`${path}: ${response.status}`);
  }
}
process.exit(failed ? 1 : 0);
