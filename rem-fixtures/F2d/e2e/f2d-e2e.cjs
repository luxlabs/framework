// REM F2d: browser checks of the Chromium fixture, run by esegui.sh (outside
// the sandbox, by L0). Usage:
//   node f2d-e2e.cjs <base url, e.g. http://127.0.0.1:18411> <label> <output dir>
// Environment: F2D_PLAYWRIGHT (path of the playwright package), F2D_CHROMIUM
// (Chromium executable). Every request to a host other than the fixture is
// aborted and recorded as a violation. Writes <output dir>/results-<label>.json
// and exits 0 when the checks ran (whatever their outcome), 3 on a violation,
// 4 on an internal error.
'use strict';
const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.F2D_PLAYWRIGHT);

const BASE = process.argv[2];
const LABEL = process.argv[3];
const OUT = process.argv[4];
const ALLOWED_HOST = new URL(BASE).host;
const violations = [];
const results = [];

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function open(browser, name, viewport) {
  const context = await browser.newContext({ viewport: viewport || { width: 1000, height: 700 } });
  await context.route('**/*', (route) => {
    const url = new URL(route.request().url());
    if (url.host === ALLOWED_HOST && url.protocol === 'http:') {
      return route.fallback();
    }
    violations.push({ scenario: name, url: route.request().url() });
    return route.abort();
  });
  const page = await context.newPage();
  const logs = [];
  page.on('console', (m) => logs.push(`${new Date().toISOString()} [${m.type()}] ${m.text()}`));
  page.on('pageerror', (e) => logs.push(`${new Date().toISOString()} [pageerror] ${e.message}`));
  return { context, page, logs };
}

async function gotoUi(page, ctx) {
  await page.goto(`${BASE}/${ctx}/`, { waitUntil: 'load' });
  await page.waitForSelector('#log', { state: 'attached', timeout: 30000 });
  await sleep(500);
}

async function logLines(page) {
  const text = await page.$eval('#log', (e) => e.textContent).catch(() => '');
  return text.split('\n').map((s) => s.trim()).filter((s) => s.length > 0);
}

async function waitForLog(page, predicate, timeoutMs) {
  const end = Date.now() + timeoutMs;
  for (;;) {
    const lines = await logLines(page);
    if (predicate(lines)) return lines;
    if (Date.now() > end) return null;
    await sleep(100);
  }
}

async function text(page, selector) {
  return page.$eval(selector, (e) => e.textContent.trim()).catch(() => null);
}

async function scenario(browser, id, item, expectation, viewport, body) {
  const started = Date.now();
  const { context, page, logs } = await open(browser, id, viewport);
  const details = [];
  let passed = false;
  let error = null;
  try {
    passed = await body(page, details);
  } catch (e) {
    error = String(e && e.stack ? e.stack : e);
  }
  if (!passed) {
    await page.screenshot({ path: path.join(OUT, `${LABEL}-${id}.png`), fullPage: true }).catch(() => {});
  }
  fs.writeFileSync(path.join(OUT, `${LABEL}-${id}-console.log`), logs.join('\n') + '\n');
  await context.close();
  const r = { id, item, expectation, passed, inconclusive: details.includes('INCONCLUSIVE'), details, error, ms: Date.now() - started };
  results.push(r);
  console.log(`${LABEL} ${id}: ${passed ? 'PASS' : 'FAIL'}${r.inconclusive ? ' (inconclusive)' : ''} ${details.join(' | ')}${error ? ' ERROR ' + error.split('\n')[0] : ''}`);
}

// Criterion 4: long polling push with many closely spaced messages.
async function pushRapid(page, d) {
  await gotoUi(page, 'push-rapid');
  await page.$eval('#count', (el) => {
    window.__seen = [];
    new MutationObserver(() => window.__seen.push(Number(el.textContent))).observe(el, { childList: true, characterData: true, subtree: true });
  });
  await page.click('#start');
  const end = Date.now() + 40000;
  let value = null;
  while (Date.now() < end) {
    value = await text(page, '#count');
    if (value === '300') break;
    await sleep(200);
  }
  const seen = await page.evaluate(() => window.__seen);
  let monotonic = true;
  for (let i = 1; i < seen.length; i++) if (seen[i] < seen[i - 1]) monotonic = false;
  await page.click('#ping');
  const pong = await waitForLog(page, (l) => l.some((s) => /\. pong$/.test(s)), 5000);
  const dialogs = await page.$$eval('.v-reconnect-dialog, .v-Notification-error, .v-Notification.error', (els) => els.filter((e) => e.offsetParent !== null).length);
  d.push(`final=${value}`, `updates seen=${seen.length}`, `monotonic=${monotonic}`, `pong=${!!pong}`, `error dialogs=${dialogs}`);
  return value === '300' && monotonic && !!pong && dialogs === 0;
}

// I09: push keeps working after a resynchronization with requests in flight.
async function pushResync(page, d) {
  let armed = false;
  let dropped = null;
  let resyncSeenAt = null;
  let resyncReleasedAt = null;
  await page.route((url) => url.pathname.endsWith('/PUSH') || url.pathname.includes('/PUSH/'), async (route) => {
    if (route.request().method() !== 'GET' || !armed || dropped) return route.fallback();
    const response = await route.fetch();
    const body = await response.text();
    if (body.includes('"syncId"')) {
      dropped = new Date().toISOString();
      return route.fulfill({ status: 200, headers: response.headers(), body: '' });
    }
    return route.fulfill({ response, body });
  });
  await page.route((url) => url.pathname.includes('/UIDL'), async (route) => {
    const data = route.request().postData() || '';
    if (resyncSeenAt === null && /"resynchronize"\s*:\s*true/.test(data)) {
      resyncSeenAt = Date.now();
      await sleep(2500);
      resyncReleasedAt = Date.now();
    }
    return route.fallback();
  });
  await gotoUi(page, 'i09');
  await page.click('#start');
  const startEnd = Date.now() + 10000;
  while (Date.now() < startEnd && Number(await text(page, '#tick')) < 3) await sleep(100);
  armed = true;
  const dropEnd = Date.now() + 5000;
  while (!dropped && Date.now() < dropEnd) await sleep(50);
  if (!dropped) {
    d.push('INCONCLUSIVE', 'no push message could be dropped');
    return false;
  }
  await sleep(1000);
  await page.click('#ping'); // request while the client waits for the missing message
  const resyncEnd = Date.now() + 12000;
  while (resyncSeenAt === null && Date.now() < resyncEnd) await sleep(50);
  if (resyncSeenAt === null) {
    d.push('INCONCLUSIVE', 'the client did not resynchronize');
    return false;
  }
  await sleep(300);
  await page.click('#ping').catch(() => {}); // request attempted while the resync is ongoing
  while (resyncReleasedAt === null) await sleep(50);
  await sleep(3000);
  const t1 = Number(await text(page, '#tick'));
  await sleep(4000);
  const t2 = Number(await text(page, '#tick'));
  await page.click('#ping').catch(() => {});
  let pongs = null;
  const pongEnd = Date.now() + 6000;
  while (Date.now() < pongEnd) {
    pongs = await text(page, '#pongs');
    if (pongs === '3') break;
    await sleep(200);
  }
  d.push(`dropped=${dropped}`, `resync delayed 2.5 s`, `tick after resync ${t1} -> ${t2}`, `pongs=${pongs}`);
  return t2 > t1 && pongs === '3';
}

// I11: rows stay consistent after scroll to bottom, resize, scroll again.
async function gridResize(page, d) {
  await gotoUi(page, 'i11');
  await page.waitForSelector('.v-grid-body .v-grid-row');
  const scrollBottom = () => page.$eval('#grid .v-grid-scroller-vertical', (s) => { s.scrollTop = 1000000; });
  await scrollBottom();
  await sleep(800);
  await page.setViewportSize({ width: 1000, height: 550 });
  await sleep(1200);
  await scrollBottom();
  await sleep(1000);
  const rows = await page.$eval('#grid', (grid) => {
    const body = grid.querySelector('.v-grid-tablewrapper').getBoundingClientRect();
    return Array.from(grid.querySelectorAll('.v-grid-body .v-grid-row'))
      .map((r) => ({ top: r.getBoundingClientRect().top, bottom: r.getBoundingClientRect().bottom, text: r.querySelector('.v-grid-cell').textContent.trim() }))
      .filter((r) => r.bottom > body.top && r.top < body.bottom)
      .sort((a, b) => a.top - b.top)
      .map((r) => r.text);
  });
  const ids = rows.map((t) => Number(t.replace('row ', '')));
  let consecutive = ids.length > 3;
  for (let i = 1; i < ids.length; i++) if (ids[i] !== ids[i - 1] + 1) consecutive = false;
  d.push(`visible rows: ${rows.join(', ')}`);
  return consecutive && ids[ids.length - 1] === 99;
}

// I29: closing a Window while dragging or resizing leaves no curtain.
function windowCurtain(handleSelector) {
  return async (page, d) => {
    await gotoUi(page, 'i29');
    await page.click('#open');
    await page.waitForSelector('#closing-window', { state: 'visible', timeout: 10000 });
    const box = await page.$eval(handleSelector, (e) => { const r = e.getBoundingClientRect(); return { x: r.left + r.width / 2, y: r.top + r.height / 2 }; });
    await page.mouse.move(box.x, box.y);
    await page.mouse.down();
    await page.mouse.move(box.x + 15, box.y + 15, { steps: 5 });
    const during = await page.$$eval('.v-window-draggingCurtain, .v-window-resizingCurtain', (els) => els.length);
    await page.waitForSelector('#closing-window', { state: 'detached', timeout: 10000 });
    await page.mouse.up();
    await sleep(500);
    const after = await page.$$eval('.v-window-draggingCurtain, .v-window-resizingCurtain', (els) => els.filter((e) => e.isConnected).length);
    await page.mouse.click(...(await page.$eval('#probe', (e) => { const r = e.getBoundingClientRect(); return [r.left + r.width / 2, r.top + r.height / 2]; })));
    const probe = await waitForLog(page, (l) => l.some((s) => /\. probe$/.test(s)), 4000);
    d.push(`curtains during=${during}`, `curtains after close=${after}`, `page clickable=${!!probe}`);
    if (during === 0) d.push('INCONCLUSIVE', 'no curtain was created by the gesture');
    return during > 0 && after === 0 && !!probe;
  };
}

// I41: ChangeListener after the first upload.
async function uploadChange(page, d) {
  const f1 = path.join(OUT, 'upload-1.txt');
  const f2 = path.join(OUT, 'upload-2.txt');
  fs.writeFileSync(f1, 'first file\n');
  fs.writeFileSync(f2, 'second file\n');
  await gotoUi(page, 'i41-change');
  await page.setInputFiles('#upload input[type=file]', f1);
  const first = await waitForLog(page, (l) => l.some((s) => /\. change$/.test(s)), 4000);
  await page.click('#upload .v-button');
  const finished = await waitForLog(page, (l) => l.some((s) => /\. finished$/.test(s)), 8000);
  await sleep(800);
  await page.setInputFiles('#upload input[type=file]', f2);
  const second = await waitForLog(page, (l) => l.filter((s) => /\. change$/.test(s)).length >= 2, 4000);
  d.push(`first change=${!!first}`, `finished=${!!finished}`, `change after first upload=${!!second}`, `log=${JSON.stringify(await logLines(page))}`);
  return !!first && !!finished && !!second;
}

// I41: no upload without a selected file.
async function uploadNoSelection(page, d) {
  await gotoUi(page, 'i41-noselect');
  const disabled = await page.$eval('#upload .v-button', (b) => b.classList.contains('v-disabled'));
  await page.click('#upload .v-button', { force: true });
  await sleep(2500);
  const afterButton = await logLines(page);
  await page.click('#programmatic');
  await sleep(2500);
  const afterProgrammatic = await logLines(page);
  const uploaded = (lines) => lines.some((s) => /receiving|finished/.test(s));
  d.push(`submit button disabled=${disabled}`, `upload from button=${uploaded(afterButton)}`, `upload from submitUpload()=${uploaded(afterProgrammatic)}`, `log=${JSON.stringify(afterProgrammatic)}`);
  return disabled && !uploaded(afterButton) && !uploaded(afterProgrammatic);
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch({
    executablePath: process.env.F2D_CHROMIUM,
    headless: true,
    args: [
      '--host-resolver-rules=MAP * ~NOTFOUND , EXCLUDE 127.0.0.1',
      '--disable-background-networking',
      '--disable-component-update',
      '--disable-domain-reliability',
      '--disable-sync',
      '--no-first-run',
      '--no-default-browser-check',
      '--metrics-recording-only',
    ],
  });
  try {
    await scenario(browser, 'push-rapid', 'C4', 'pass on base and candidate', null, pushRapid);
    await scenario(browser, 'i09-resync', 'I09', 'red on base, green on candidate', null, pushResync);
    await scenario(browser, 'i11-grid-resize', 'I11', 'red on base, green on candidate', { width: 1000, height: 600 }, gridResize);
    await scenario(browser, 'i29-drag', 'I29', 'red on base, green on candidate', null, windowCurtain('#closing-window .v-window-header'));
    await scenario(browser, 'i29-resize', 'I29', 'red on base, green on candidate', null, windowCurtain('#closing-window .v-window-resizebox'));
    await scenario(browser, 'i41-change', 'I41', 'red on base, green on candidate', null, uploadChange);
    await scenario(browser, 'i41-noselect', 'I41', 'red on base, green on candidate', null, uploadNoSelection);
  } finally {
    await browser.close();
  }
  fs.writeFileSync(path.join(OUT, `results-${LABEL}.json`), JSON.stringify({ label: LABEL, base: BASE, violations, results }, null, 2));
  if (violations.length > 0) {
    console.log(`${LABEL}: ${violations.length} network violation(s)`);
    process.exit(3);
  }
  process.exit(0);
})().catch((e) => {
  console.error(e);
  process.exit(4);
});
