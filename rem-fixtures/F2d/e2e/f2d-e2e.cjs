// REM F2d: browser checks of the Chromium fixture, run by esegui.sh (outside
// the sandbox, by L0). Usage:
//   node f2d-e2e.cjs <base url, e.g. http://127.0.0.1:18411> <label> <output dir>
// Environment: F2D_PLAYWRIGHT (path of the playwright package), F2D_CHROMIUM
// (Chromium executable), F2D_SCENARIOS (optional, comma separated ids: only
// those scenarios run), F2D_REPEAT (optional, "id=n,…": run scenario id n
// times, as id-r1 … id-rn). Every request to a host other than the fixture is
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

async function gotoUi(page, ctx, query) {
  await page.goto(`${BASE}/${ctx}/${query ? '?' + query : ''}`, { waitUntil: 'load' });
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

const ONLY = (process.env.F2D_SCENARIOS || '').split(',').map((x) => x.trim()).filter((x) => x);
const REPEAT = {};
for (const kv of (process.env.F2D_REPEAT || '').split(',').map((x) => x.trim()).filter((x) => x)) {
  const [k, v] = kv.split('=');
  REPEAT[k] = Math.max(1, Math.min(20, Number(v) || 1));
}

async function scenario(browser, id, item, expectation, viewport, body) {
  if (ONLY.length > 0 && !ONLY.includes(id)) return;
  const n = REPEAT[id] || 1;
  for (let i = 1; i <= n; i++) {
    await runScenario(browser, n > 1 ? `${id}-r${i}` : id, item, expectation, viewport, body);
  }
}

async function runScenario(browser, id, item, expectation, viewport, body) {
  const started = Date.now();
  const { context, page, logs } = await open(browser, id, viewport);
  const details = [];
  let passed = false;
  let error = null;
  try {
    passed = await body(page, details, id, logs);
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

// I09, second scenario: resynchronization triggered by a theme change (no
// missing message) while push messages arrive every 100 ms and the user
// clicks; the resync request is held 2 s in the browser, so the clicks reach
// the server first. Push must keep working afterwards and a new request must
// be answered; resynchronizations must not repeat.
// Diagnostics of one repetition: push messages received by the client
// (onMessage logs "Received push"), sync ids of XHR responses, long polling
// requests opened, finished, failed and still pending, and the server-side
// lines of Diag (sync id written, push connection and resource state).
function trackLongPolling(page) {
  const lp = { opened: 0, finished: 0, failed: 0, pending: new Map() };
  const isPush = (r) => r.method() === 'GET' && new URL(r.url()).pathname.endsWith('/PUSH');
  page.on('request', (r) => { if (isPush(r)) { lp.opened++; lp.pending.set(r, Date.now()); } });
  page.on('requestfinished', (r) => { if (isPush(r)) { lp.finished++; lp.pending.delete(r); } });
  page.on('requestfailed', (r) => { if (isPush(r)) { lp.failed++; lp.pending.delete(r); } });
  return lp;
}

async function diagnostics(page, d, tag, logs, lp) {
  const ids = (re) => logs.map((l) => { const m = l.match(re); return m ? Number(m[1]) : null; }).filter((x) => x !== null);
  const pushIds = ids(/Received push \([^)]*\) message: for\(;;\);\[\{"syncId": (\d+)/);
  const xhrIds = ids(/Received xhr message: for\(;;\);\[\{"syncId": (\d+)/);
  const now = Date.now();
  const pendingAges = Array.from(lp.pending.values()).map((t) => now - t);
  let server = '';
  try {
    const res = await page.request.get(`${BASE}/i09-theme/diag?rep=${encodeURIComponent(tag)}`);
    server = await res.text();
  } catch (e) {
    server = 'diag error: ' + e;
  }
  fs.writeFileSync(path.join(OUT, `${LABEL}-${tag}-server-diag.txt`), server);
  const lines = server.split('\n').filter((l) => l.trim());
  const resumedConnected = lines.filter((l) => /state=CONNECTED/.test(l) && /resumed=true/.test(l)).length;
  const disconnected = lines.filter((l) => /connected=false/.test(l)).length;
  d.push(`client push received=${pushIds.length} last push syncId=${pushIds.length ? pushIds[pushIds.length - 1] : '-'} last xhr syncId=${xhrIds.length ? xhrIds[xhrIds.length - 1] : '-'}`);
  d.push(`long polling GET opened=${lp.opened} finished=${lp.finished} failed=${lp.failed} pending=${pendingAges.length}${pendingAges.length ? ' (age ' + pendingAges.map((a) => Math.round(a / 100) / 10 + ' s').join(', ') + ')' : ''}`);
  const count = (re) => lines.filter((l) => re.test(l)).length;
  const lastTick = lines.filter((l) => / tick=/.test(l)).pop() || '';
  d.push(`server ticks=${count(/ tick=/)} last syncId=${(lastTick.match(/syncId=(\d+)/) || [])[1] || '-'} ticks connected with resumed resource=${resumedConnected} ticks disconnected=${disconnected} last tick=[${lastTick.replace(/^t=\d+ /, '')}]`);
  d.push(`server events: GET=${count(/atmo request GET/)} new push connection=${count(/New push connection/)} connection closed=${count(/Connection (unexpectedly )?closed/)} atmo suspend=${count(/atmo suspend/)} resume=${count(/atmo resume/)} broadcast=${count(/atmo broadcast/)} close=${count(/atmo close/)} disconnect=${count(/atmo disconnect/)}`);
}

async function pushThemeResync(page, d, id, logs) {
  const tag = `${LABEL}-${id}`.replace(/[^A-Za-z0-9._-]/g, '_').slice(0, 40);
  const lp = trackLongPolling(page);
  let resyncs = 0;
  let heldAt = null;
  let releasedAt = null;
  await page.route((url) => url.pathname.includes('/UIDL'), async (route) => {
    const data = route.request().postData() || '';
    if (/"resynchronize"\s*:\s*true/.test(data)) {
      resyncs++;
      if (heldAt === null) {
        heldAt = Date.now();
        await sleep(2000);
        releasedAt = Date.now();
      }
    }
    return route.fallback();
  });
  await gotoUi(page, 'i09-theme', `rep=${tag}`);
  await page.click('#start');
  const startEnd = Date.now() + 10000;
  while (Date.now() < startEnd && Number(await text(page, '#tick')) < 5) await sleep(100);
  await page.click('#theme');
  const resyncEnd = Date.now() + 6000;
  while (heldAt === null && Date.now() < resyncEnd) await sleep(20);
  if (heldAt === null) {
    d.push('INCONCLUSIVE', 'the theme change did not trigger a resynchronization');
    await diagnostics(page, d, tag, logs, lp);
    return false;
  }
  for (let i = 0; i < 3; i++) {
    await sleep(150);
    await page.click('#ping').catch(() => {});
  }
  while (releasedAt === null) await sleep(50);
  await sleep(3000);
  const pongsBefore = Number(await text(page, '#pongs'));
  const t1 = Number(await text(page, '#tick'));
  await sleep(5000);
  const t2 = Number(await text(page, '#tick'));
  await page.click('#ping').catch(() => {});
  let pongsAfter = pongsBefore;
  const pongEnd = Date.now() + 8000;
  while (Date.now() < pongEnd) {
    pongsAfter = Number(await text(page, '#pongs'));
    if (pongsAfter > pongsBefore) break;
    await sleep(200);
  }
  await sleep(3000);
  d.push(`resync requests=${resyncs}`, `tick after resync ${t1} -> ${t2}`, `pongs before last ping=${pongsBefore}`, `after=${pongsAfter}`);
  await diagnostics(page, d, tag, logs, lp);
  return t2 > t1 && pongsAfter > pongsBefore && resyncs <= 3;
}

// I09, third scenario: a push message that is late rather than lost. The
// long-polling response carrying it is held 8 s; a click meanwhile produces a
// newer message, the client gives up waiting (5 s) and resynchronizes; the
// resync request is held 3 s, and the late message arrives during the
// resynchronization.
async function pushLateResync(page, d) {
  let armed = false;
  let heldMessage = null;
  let resyncAt = null;
  let resyncs = 0;
  await page.route((url) => url.pathname.endsWith('/PUSH') || url.pathname.includes('/PUSH/'), async (route) => {
    if (route.request().method() !== 'GET' || !armed || heldMessage) return route.fallback();
    const response = await route.fetch();
    const body = await response.text();
    if (body.includes('"syncId"')) {
      heldMessage = new Date().toISOString();
      await sleep(8000);
    }
    return route.fulfill({ response, body });
  });
  await page.route((url) => url.pathname.includes('/UIDL'), async (route) => {
    const data = route.request().postData() || '';
    if (/"resynchronize"\s*:\s*true/.test(data)) {
      resyncs++;
      if (resyncAt === null) {
        resyncAt = Date.now();
        await sleep(3000);
      }
    }
    return route.fallback();
  });
  await gotoUi(page, 'i09');
  await page.click('#start');
  const startEnd = Date.now() + 10000;
  while (Date.now() < startEnd && Number(await text(page, '#tick')) < 3) await sleep(100);
  armed = true;
  const holdEnd = Date.now() + 5000;
  while (!heldMessage && Date.now() < holdEnd) await sleep(50);
  if (!heldMessage) {
    d.push('INCONCLUSIVE', 'no push message could be held');
    return false;
  }
  await sleep(500);
  await page.click('#ping').catch(() => {});
  const resyncEnd = Date.now() + 9000;
  while (resyncAt === null && Date.now() < resyncEnd) await sleep(50);
  if (resyncAt === null) {
    d.push('INCONCLUSIVE', 'the client did not resynchronize');
    return false;
  }
  await sleep(500);
  await page.click('#ping').catch(() => {});
  await sleep(8000);
  const t1 = Number(await text(page, '#tick'));
  const pongsBefore = Number(await text(page, '#pongs'));
  await sleep(4000);
  const t2 = Number(await text(page, '#tick'));
  await page.click('#ping').catch(() => {});
  let pongsAfter = pongsBefore;
  const pongEnd = Date.now() + 8000;
  while (Date.now() < pongEnd) {
    pongsAfter = Number(await text(page, '#pongs'));
    if (pongsAfter > pongsBefore) break;
    await sleep(200);
  }
  d.push(`held=${heldMessage}`, `resync requests=${resyncs}`, `tick ${t1} -> ${t2}`, `pongs ${pongsBefore} -> ${pongsAfter}`);
  return t2 > t1 && pongsAfter > pongsBefore && resyncs <= 3;
}

// I11 invariant: every visible body row shows the item that belongs to its
// position (row top = body top + index * row height - scrollTop), rows are
// consecutive and without duplicates.
async function gridState(page) {
  return page.$eval('#grid', (grid) => {
    const wrapper = grid.querySelector('.v-grid-tablewrapper').getBoundingClientRect();
    const header = grid.querySelector('.v-grid-header').getBoundingClientRect();
    const scroller = grid.querySelector('.v-grid-scroller-vertical');
    const all = Array.from(grid.querySelectorAll('.v-grid-body .v-grid-row'));
    const rh = all.length ? all[0].getBoundingClientRect().height : 0;
    const vis = all
      .map((r) => { const b = r.getBoundingClientRect(); return { top: b.top, bottom: b.bottom, text: r.querySelector('.v-grid-cell').textContent.trim() }; })
      .filter((r) => r.bottom > header.bottom + 1 && r.top < wrapper.bottom - 1)
      .sort((a, b) => a.top - b.top);
    const problems = [];
    let prev = null;
    for (const r of vis) {
      const n = Number(r.text.replace('row ', ''));
      const k = Math.round((r.top - header.bottom + scroller.scrollTop) / rh);
      if (k !== n) problems.push(`${r.text} at position ${k}`);
      if (prev !== null && n !== prev + 1) problems.push(`${r.text} after row ${prev}`);
      prev = n;
    }
    return { scrollTop: Math.round(scroller.scrollTop), max: scroller.scrollHeight - scroller.clientHeight, rh, first: vis.length ? vis[0].text : null, last: vis.length ? vis[vis.length - 1].text : null, problems };
  });
}

async function gridCheck(page, label, log) {
  const s = await gridState(page);
  if (s.problems.length > 0) log.push(`${label}: scrollTop=${s.scrollTop}/${s.max} ${s.first}..${s.last} PROBLEMS ${s.problems.slice(0, 4).join('; ')}`);
  return s;
}

const gridScroll = (page, how) => page.$eval('#grid .v-grid-scroller-vertical', (s, how) => {
  if (how === 'bottom') s.scrollTop = 1000000;
  else s.scrollTop = Math.max(0, s.scrollTop + how);
}, how);

// I11, stronger: several window shrinks from a scroll at the bottom, then
// scrolling to the bottom, a few rows up and down again, then growing back.
async function gridResizeSteps(page, d) {
  const log = [];
  await gotoUi(page, 'i11');
  await page.waitForSelector('.v-grid-body .v-grid-row');
  let checks = 0;
  for (const delta of [20, 40, 50, 80, 120, 200]) {
    await page.setViewportSize({ width: 1000, height: 700 });
    await sleep(700);
    await gridScroll(page, 'bottom');
    await sleep(500);
    await page.setViewportSize({ width: 1000, height: 700 - delta });
    await sleep(900);
    const rh = (await gridCheck(page, `shrink ${delta}`, log)).rh;
    await gridScroll(page, 'bottom'); await sleep(500); await gridCheck(page, `shrink ${delta}, bottom`, log);
    await gridScroll(page, -3 * rh); await sleep(400); await gridCheck(page, `shrink ${delta}, up 3`, log);
    await gridScroll(page, 'bottom'); await sleep(400);
    const last = await gridCheck(page, `shrink ${delta}, bottom again`, log);
    if (last.last !== 'row 99') log.push(`shrink ${delta}: last visible ${last.last} at the bottom`);
    await page.setViewportSize({ width: 1000, height: 700 });
    await sleep(900); await gridCheck(page, `grow back from ${delta}`, log);
    checks += 5;
  }
  d.push(`checks=${checks}`, log.length ? log.slice(0, 6).join(' / ') : 'all rows consistent');
  return log.length === 0;
}

// I11, container resized by the server, with 12 columns (horizontal
// scrollbar): from the bottom, the Grid height goes from 420 px to smaller
// values and back.
async function gridContainerResize(page, d) {
  const log = [];
  await gotoUi(page, 'i11-fixed');
  await page.waitForSelector('.v-grid-body .v-grid-row');
  const setHeight = async (h) => {
    const n = (await logLines(page)).length;
    await page.click(`#h-${h}`);
    await waitForLog(page, (l) => l.length > n, 4000);
    await sleep(700);
  };
  let checks = 0;
  for (const h of [400, 380, 350, 300, 250]) {
    await setHeight(420);
    await gridScroll(page, 'bottom'); await sleep(500);
    await setHeight(h);
    const rh = (await gridCheck(page, `height ${h}`, log)).rh;
    await gridScroll(page, 'bottom'); await sleep(500); await gridCheck(page, `height ${h}, bottom`, log);
    await gridScroll(page, -2 * rh); await sleep(400); await gridCheck(page, `height ${h}, up 2`, log);
    await gridScroll(page, 'bottom'); await sleep(400);
    const last = await gridCheck(page, `height ${h}, bottom again`, log);
    if (last.last !== 'row 99') log.push(`height ${h}: last visible ${last.last} at the bottom`);
    checks += 4;
  }
  d.push(`checks=${checks}`, log.length ? log.slice(0, 6).join(' / ') : 'all rows consistent');
  return log.length === 0;
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
    await scenario(browser, 'i09-theme-resync', 'I09', 'red on base, green on candidate', null, pushThemeResync);
    await scenario(browser, 'i09-late-resync', 'I09', 'red on base, green on candidate', null, pushLateResync);
    await scenario(browser, 'i11-grid-resize', 'I11', 'red on base, green on candidate', { width: 1000, height: 600 }, gridResize);
    await scenario(browser, 'i11-window-steps', 'I11', 'red on base, green on candidate', { width: 1000, height: 700 }, gridResizeSteps);
    await scenario(browser, 'i11-container', 'I11', 'red on base, green on candidate', { width: 1000, height: 800 }, gridContainerResize);
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
