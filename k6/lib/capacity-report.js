const DEFAULT_MAX_ERROR_RATE = 0.05;
const DEFAULT_MAX_P95_MS = 3000;

function envFloat(name, fallback) {
  const v = __ENV[name];
  if (v === undefined || v === '') return fallback;
  const n = parseFloat(v);
  return Number.isNaN(n) ? fallback : n;
}

function envInt(name, fallback) {
  const v = __ENV[name];
  if (v === undefined || v === '') return fallback;
  const n = parseInt(v, 10);
  return Number.isNaN(n) ? fallback : n;
}

function parseSteps(prefix, envName, fallback) {
  const raw = __ENV[envName];
  const list = (raw || fallback)
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => parseInt(s, 10))
    .filter((n) => !Number.isNaN(n));
  return list.map((value) => ({ tag: `${prefix}_${value}`, value, label: `${value}` }));
}

function collectStepMetrics(data, steps) {
  const duration = data.metrics?.http_req_duration;
  const failed = data.metrics?.http_req_failed;
  const reqs = data.metrics?.http_reqs;
  const submetrics = duration?.submetrics || [];

  return steps.map((step) => {
    const sub = findTaggedSubmetric(submetrics, 'http_req_duration', step.tag);
    const p95 = sub?.values?.['p(95)'] ?? null;
    const avg = sub?.values?.avg ?? null;

    const reqSub = findTaggedSubmetric(reqs?.submetrics || [], 'http_reqs', step.tag);
    const reqCount = reqSub?.values?.count ?? 0;
    const stepDurationSec = envInt('K6_STEP_SECONDS', 90);
    const rps = stepDurationSec > 0 ? reqCount / stepDurationSec : 0;

    const failSub = findTaggedSubmetric(failed?.submetrics || [], 'http_req_failed', step.tag);
    const errorRate = failSub?.values?.rate ?? 0;

    return { ...step, p95, avg, errorRate, rps, reqCount };
  });
}

function findTaggedSubmetric(submetrics, metricBase, tag) {
  const candidates = [
    `${metricBase}{scenario:${tag}}`,
    `${metricBase}{scenario:vu_step_${tag.replace(/^vu_/, '')}}`,
    `${metricBase}{scenario:rps_step_${tag.replace(/^rps_/, '')}}`,
    `${metricBase}{capacity_step:${tag}}`,
  ];
  for (const name of candidates) {
    const sub = submetrics.find((m) => m.name === name);
    if (sub) return sub;
  }
  return null;
}

function findMaxSustainable(results, maxErrorRate, maxP95Ms) {
  let best = null;
  for (const row of results) {
    const ok = row.errorRate <= maxErrorRate && row.p95 !== null && row.p95 <= maxP95Ms;
    if (ok) best = row;
  }
  return best;
}

export function buildCapacityReport(data, kind, steps) {
  const maxErrorRate = envFloat('K6_MAX_ERROR_RATE', DEFAULT_MAX_ERROR_RATE);
  const maxP95Ms = envInt('K6_MAX_P95_MS', DEFAULT_MAX_P95_MS);
  const results = collectStepMetrics(data, steps);
  const sustainable = findMaxSustainable(results, maxErrorRate, maxP95Ms);

  const lines = [
    '',
    '══════════════════════════════════════════════════════════',
    `  CAPACITY REPORT — ${kind.toUpperCase()}`,
    '══════════════════════════════════════════════════════════',
    `  Tiêu chí ổn định: error ≤ ${(maxErrorRate * 100).toFixed(0)}%, p95 ≤ ${maxP95Ms}ms`,
    '',
    '  Step          p95       avg       error%    req/s',
    '  ─────────────────────────────────────────────────────',
  ];

  for (const row of results) {
    const p95 = row.p95 !== null ? `${(row.p95 / 1).toFixed(0)}ms`.padStart(8) : '     n/a';
    const avg = row.avg !== null ? `${(row.avg / 1).toFixed(0)}ms`.padStart(8) : '     n/a';
    const err = `${(row.errorRate * 100).toFixed(2)}%`.padStart(8);
    const rps = row.rps.toFixed(1).padStart(8);
    const marker =
      row.errorRate <= maxErrorRate && row.p95 !== null && row.p95 <= maxP95Ms ? ' ✓' : ' ✗';
    lines.push(`  ${row.label.padEnd(12)} ${p95} ${avg} ${err} ${rps}${marker}`);
  }

  lines.push('');
  if (sustainable) {
    if (kind === 'vus') {
      lines.push(`  ► Max concurrent users (ước tính): ~${sustainable.value} VU`);
      lines.push(`    (p95=${sustainable.p95.toFixed(0)}ms, error=${(sustainable.errorRate * 100).toFixed(2)}%)`);
    } else {
      lines.push(`  ► Max throughput (ước tính): ~${sustainable.value} req/s (target)`);
      lines.push(`    (p95=${sustainable.p95.toFixed(0)}ms, error=${(sustainable.errorRate * 100).toFixed(2)}%, thực tế ~${sustainable.rps.toFixed(1)} req/s)`);
    }
  } else {
    lines.push('  ► Không có step nào đạt tiêu chí ổn định — giảm load hoặc nới K6_MAX_P95_MS / K6_MAX_ERROR_RATE');
  }

  lines.push('  ✓ = đạt tiêu chí ổn định');
  lines.push('══════════════════════════════════════════════════════════');
  lines.push('');
  return lines.join('\n');
}

export function getVuSteps() {
  return parseSteps('vu', 'K6_VU_STEPS', '5,10,20,30,50,75,100,150,200');
}

export function getRpsSteps() {
  return parseSteps('rps', 'K6_RPS_STEPS', '10,25,50,75,100,125,150,200');
}
