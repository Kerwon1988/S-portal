const baseUrl = process.env.PORTAL_PERF_BASE_URL;
const requests = Number(process.env.PORTAL_PERF_REQUESTS || 100);
const concurrency = Number(process.env.PORTAL_PERF_CONCURRENCY || 5);
if (!baseUrl || !baseUrl.startsWith('http')) process.exit(2);
const durations = [];
let failures = 0;
async function request() {
  const start = performance.now();
  try {
    const response = await fetch(new URL('/es', baseUrl), { signal: AbortSignal.timeout(10000) });
    if (!response.ok) failures++;
  } catch { failures++; }
  durations.push(performance.now() - start);
}
for (let offset = 0; offset < requests; offset += concurrency) {
  await Promise.all(Array.from({ length: Math.min(concurrency, requests - offset) }, request));
}
durations.sort((a, b) => a - b);
const p = q => durations[Math.min(durations.length - 1, Math.ceil(q * durations.length) - 1)];
const result = { requests, concurrency, failures, errorRate: failures / requests, p95Ms: p(.95), p99Ms: p(.99) };
console.log(JSON.stringify(result));
if (failures || result.p95Ms > 800 || result.p99Ms > 1500) process.exit(1);
