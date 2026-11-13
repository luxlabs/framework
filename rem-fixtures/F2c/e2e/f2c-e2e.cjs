// REM F2c: browser checks of the Chromium fixture, run by esegui.sh (outside
// the sandbox, by L0). Usage:
//   node f2c-e2e.cjs <base url, e.g. http://127.0.0.1:18418> <label> <output dir>
// Environment: F2C_PLAYWRIGHT (path of the playwright package), F2C_CHROMIUM
// (Chromium executable). Every request to a host other than the fixture is
// aborted and recorded as a violation. Writes <output dir>/results-<label>.json
// and exits 0 when the checks ran (whatever their outcome), 3 on a violation,
// 4 on an internal error.
'use strict';
const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.F2C_PLAYWRIGHT);

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

async function gotoUi(page, ctx) {
  await page.goto(`${BASE}/${ctx}/`, { waitUntil: 'load' });
  await waitForLog(page, (l) => l.includes('ready'), 30000);
  await sleep(500);
}

async function scenario(browser, id, item, expectation, viewport, ctx, body) {
  const started = Date.now();
  const { context, page, logs } = await open(browser, id, viewport);
  const details = [];
  let passed = false;
  let error = null;
  try {
    await gotoUi(page, ctx);
    passed = await body(page, details, logs);
  } catch (e) {
    error = String(e && e.stack ? e.stack : e);
  }
  await page.screenshot({ path: path.join(OUT, `${LABEL}-${id}.png`), fullPage: false }).catch(() => {});
  fs.writeFileSync(path.join(OUT, `${LABEL}-${id}-console.log`), logs.join('\n') + '\n');
  await context.close();
  const r = { id, item, expectation, passed: passed === true && error === null, details, error, ms: Date.now() - started };
  results.push(r);
  console.log(`${LABEL} ${id} (${item}): ${r.passed ? 'PASS' : 'FAIL'} ${error ? error.split('\n')[0] : ''}`);
}

const pageErrors = (logs) => logs.filter((l) => l.includes('[pageerror]') || /\[error\]/.test(l));

// I05/M04: the caption of a Panel without captionAsHtml is text.
async function panelCaption(page, d) {
  const r = await page.evaluate(() => {
    const cap = document.querySelector('#panel-text .v-panel-caption');
    const html = document.querySelector('#panel-html .v-panel-caption');
    return {
      textCaptionElements: cap ? cap.querySelectorAll('b, img, #f2c-b, #f2c-img').length : -1,
      textCaptionText: cap ? cap.textContent : null,
      xss: window.f2cXss === undefined ? 'undefined' : String(window.f2cXss),
      htmlCaptionBold: html ? html.querySelectorAll('b.f2c-html').length : -1,
    };
  });
  d.push(JSON.stringify(r));
  return r.textCaptionElements === 0 && r.xss === 'undefined' && r.htmlCaptionBold === 1
    && r.textCaptionText !== null && r.textCaptionText.includes('<b id="f2c-b">bold</b>');
}

// I02 on Chromium: keyboard navigation keeps working with key down events.
async function keyboard(page, d) {
  const ok = {};
  await page.focus('#slider');
  await page.keyboard.press('ArrowRight');
  ok.slider = (await waitForLog(page, (l) => l.includes('slider=51'), 3000)) !== null;

  await page.click('#tree .v-tree-node-caption >> text=a');
  await sleep(300);
  await page.keyboard.press('ArrowDown');
  await sleep(500);
  const treeFocus = await page.$eval('#tree .v-tree-node-focused', (e) => e.textContent.trim()).catch(() => null);
  const treeLog = await logLines(page);
  ok.tree = treeFocus === 'b' || treeLog.includes('tree=b');
  d.push(`tree focused=${treeFocus}`);

  await page.focus('#table .v-table-body');
  await sleep(300);
  await page.keyboard.press('ArrowDown');
  await sleep(500);
  const tableFocus = await page.$eval('#table .v-table-focus', (e) => e.textContent.trim()).catch(() => null);
  const tableLog = await logLines(page);
  ok.table = tableFocus === 'r2' || tableLog.includes('table=r2');
  d.push(`table focused=${tableFocus}`);

  await page.focus('#menubar');
  await page.keyboard.press('ArrowRight');
  await sleep(300);
  const m1 = await page.$eval('#menubar .v-menubar-menuitem-selected', (e) => e.textContent.trim()).catch(() => null);
  await page.keyboard.press('ArrowRight');
  await sleep(300);
  const m2 = await page.$eval('#menubar .v-menubar-menuitem-selected', (e) => e.textContent.trim()).catch(() => null);
  ok.menubar = m1 !== null && m2 !== null && m1 !== m2;
  d.push(`menubar selected ${m1} -> ${m2}`);
  await page.keyboard.press('Escape');

  await page.focus('#date .v-inline-datefield-calendarpanel');
  await page.keyboard.press('ArrowRight');
  await sleep(200);
  await page.keyboard.press('Enter');
  ok.date = (await waitForLog(page, (l) => l.includes('date=2020-01-16'), 3000)) !== null;

  d.push(JSON.stringify(ok), `log=${JSON.stringify(await logLines(page))}`);
  return ok.slider && ok.tree && ok.table && ok.menubar && ok.date;
}

// I03: autocomplete="off" on the ComboBox and DateField inputs in Chrome.
async function autocomplete(page, d) {
  const combo = await page.$eval('#combo-auto input', (e) => e.getAttribute('autocomplete')).catch(() => null);
  const date = await page.$eval('#date-auto input', (e) => e.getAttribute('autocomplete')).catch(() => null);
  const ua = await page.evaluate(() => navigator.userAgent);
  d.push(`combo autocomplete=${combo}`, `date autocomplete=${date}`, `user agent=${ua}`);
  return combo === 'off' && date === 'off';
}

async function popupRect(page) {
  return page.evaluate(() => {
    const p = document.querySelector('.v-filterselect-suggestpopup');
    if (!p || p.offsetParent === null && getComputedStyle(p).position !== 'fixed' && p.style.visibility === 'hidden') return null;
    const r = p.getBoundingClientRect();
    return { left: r.left, right: r.right, top: r.top, bottom: r.bottom, width: r.width };
  });
}

// I44 (no correction, non regression): popup opened to the left at the right edge stays inside the view.
async function popupLeft(page, d) {
  await page.click('#combo-right .v-filterselect-button');
  await page.waitForSelector('.v-filterselect-suggestpopup', { timeout: 5000 });
  await sleep(800);
  const r = await popupRect(page);
  const view = await page.evaluate(() => document.documentElement.clientWidth);
  const field = await page.$eval('#combo-right', (e) => { const b = e.getBoundingClientRect(); return { left: b.left, right: b.right }; });
  d.push(`popup=${JSON.stringify(r)}`, `viewport width=${view}`, `field=${JSON.stringify(field)}`);
  return r !== null && r.right <= view + 0.5 && r.left >= 0;
}

// I40: while filtering, the open popup is not reset to its default position.
async function popupPositions(page, d) {
  await page.evaluate(() => {
    window.f2cLefts = [];
    const record = () => {
      const p = document.querySelector('.v-filterselect-suggestpopup');
      if (p && p.style.left) {
        const last = window.f2cLefts[window.f2cLefts.length - 1];
        if (last !== p.style.left) window.f2cLefts.push(p.style.left);
      }
    };
    new MutationObserver(record).observe(document.body, { subtree: true, attributes: true, attributeFilter: ['style'], childList: true });
  });
  await page.click('#combo-right .v-filterselect-button');
  await page.waitForSelector('.v-filterselect-suggestpopup', { timeout: 5000 });
  await sleep(800);
  const afterOpen = await page.evaluate(() => window.f2cLefts.slice());
  await page.focus('#combo-right input');
  for (const ch of 'Very long item caption number 1') {
    await page.keyboard.type(ch);
    await sleep(150);
  }
  await sleep(1500);
  const all = await page.evaluate(() => window.f2cLefts.slice());
  const afterFiltering = all.slice(afterOpen.length);
  d.push(`left after opening=${JSON.stringify(afterOpen)}`, `left changes while filtering=${JSON.stringify(afterFiltering)}`);
  // pass: no reset to another position and back while filtering
  return afterOpen.length > 0 && afterFiltering.length <= 1;
}

// I14: typing fast and tabbing out selects or adds the value.
async function fastTab(page, d) {
  await page.click('#combo-new input');
  await page.keyboard.type('Omega', { delay: 0 });
  await page.keyboard.press('Tab');
  const added = await waitForLog(page, (l) => l.some((s) => s === 'combo-new-new=Omega' || s === 'combo-new=Omega'), 3000);
  await page.click('#combo-sel input');
  await page.keyboard.type('Bet', { delay: 0 });
  await page.keyboard.press('Tab');
  const selected = await waitForLog(page, (l) => l.includes('combo-sel=Beta'), 3000);
  d.push(`new item added=${added !== null}`, `suggestion selected=${selected !== null}`, `log=${JSON.stringify(await logLines(page))}`);
  return added !== null && selected !== null;
}

// I42 (no correction, non regression): the popup follows the field when the page scrolls.
async function popupScroll(page, d) {
  await page.click('#combo-scroll .v-filterselect-button');
  await page.waitForSelector('.v-filterselect-suggestpopup', { timeout: 5000 });
  await sleep(800);
  const samples = await page.evaluate(async () => {
    const field = document.querySelector('#combo-scroll');
    const popup = document.querySelector('.v-filterselect-suggestpopup');
    const scroller = document.querySelector('.v-ui');
    const offset = () => Math.round(popup.getBoundingClientRect().top - field.getBoundingClientRect().bottom);
    const out = [offset()];
    scroller.scrollTop += 100;
    window.scrollBy(0, 0);
    for (let i = 0; i < 30; i++) {
      await new Promise((r) => requestAnimationFrame(r));
      out.push(offset());
    }
    await new Promise((r) => setTimeout(r, 500));
    out.push(offset());
    return out;
  });
  const first = samples[0];
  const last = samples[samples.length - 1];
  const off = samples.filter((v) => Math.abs(v - first) > 2).length;
  d.push(`popup top - field bottom per frame=${JSON.stringify(samples)}`, `frames off by more than 2 px=${off}`);
  return Math.abs(last - first) <= 2 && off <= 3;
}

async function dragSelect(page, selector) {
  const box = await page.$eval(selector, (e) => {
    const r = e.getBoundingClientRect();
    return { x: r.left, y: r.top + r.height / 2, w: r.width, text: e.textContent };
  });
  await page.mouse.move(box.x + 4, box.y);
  await page.mouse.down();
  await page.mouse.move(box.x + Math.min(160, box.w - 10), box.y, { steps: 10 });
  await page.mouse.up();
  await sleep(300);
  return box.text;
}

const selection = (page) => page.evaluate(() => String(window.getSelection()));

// I10: text selection with the mouse in a Table cell in Chrome.
async function tableSelect(page, d) {
  const cell = '#table-text .v-table-row:nth-child(2) .v-table-cell-wrapper, #table-text .v-table-row-odd:nth-child(2) .v-table-cell-wrapper';
  const text = await dragSelect(page, cell);
  const sel = await selection(page);
  d.push(`cell text=${JSON.stringify(text)}`, `selection=${JSON.stringify(sel)}`);
  return sel.length >= 5 && text.includes(sel.trim());
}

// I46: text selection inside one Grid cell survives and stays in the cell.
function gridSelect(id) {
  return async (page, d) => {
    const cell = `#${id} .v-grid-body .v-grid-row:nth-child(1) .v-grid-cell:nth-child(1)`;
    const text = await dragSelect(page, cell);
    const sel1 = await selection(page);
    await sleep(1000);
    const sel2 = await selection(page);
    await page.click(cell, { clickCount: 3 });
    await sleep(1000);
    const triple = await selection(page);
    d.push(`cell text=${JSON.stringify(text)}`, `drag selection=${JSON.stringify(sel1)}`, `after 1 s=${JSON.stringify(sel2)}`, `triple click=${JSON.stringify(triple)}`);
    return sel1.length >= 5 && text.includes(sel1.trim()) && sel2 === sel1 && triple.trim() === text.trim();
  };
}

// M03 (client side): the column hiding toggles show their captions as text.
async function gridToggles(page, d) {
  await page.click('#grid-toggles .v-grid-sidebar-button');
  await page.waitForSelector('.column-hiding-toggle', { timeout: 5000 });
  await sleep(1000);
  const r = await page.evaluate(() => {
    const toggles = Array.from(document.querySelectorAll('.column-hiding-toggle'));
    return {
      toggles: toggles.length,
      markup: toggles.reduce((n, t) => n + t.querySelectorAll('img, b, .f2c-toggle-img, .f2c-toggle-b').length, 0),
      texts: toggles.map((t) => t.textContent.trim()),
      anywhere: document.querySelectorAll('.f2c-toggle-img, .f2c-toggle-b').length,
      xss: window.f2cToggleXss === undefined ? 'undefined' : String(window.f2cToggleXss),
    };
  });
  d.push(JSON.stringify(r));
  return r.toggles === 3 && r.markup === 0 && r.anywhere === 0 && r.xss === 'undefined'
    && r.texts.filter((t) => t.includes('<img class="f2c-toggle-img"')).length === 2;
}

// I01 (no correction, non regression): sorting works after enabling and disabling multi-selection twice.
async function gridSort(page, d) {
  for (const [button, line] of [['#mode-multi', 'mode=multi'], ['#mode-none', 'mode=none'], ['#mode-multi', 'mode=multi'], ['#mode-none', 'mode=none']]) {
    const before = (await logLines(page)).filter((s) => s === line).length;
    await page.click(button);
    await waitForLog(page, (l) => l.filter((s) => s === line).length > before, 3000);
    await sleep(300);
  }
  await page.click('#grid-sort .v-grid-header .v-grid-cell >> text=Name');
  const sorted = await waitForLog(page, (l) => l.some((s) => s.startsWith('sort=name')), 3000);
  await sleep(500);
  const first = await page.$eval('#grid-sort .v-grid-body .v-grid-row:nth-child(1) .v-grid-cell:nth-child(1)', (e) => e.textContent.trim()).catch(() => null);
  d.push(`sort event=${sorted !== null}`, `first row=${first}`, `log=${JSON.stringify(await logLines(page))}`);
  return sorted !== null && first === 'Alpha';
}

// I15: adding a column when a column is hidden.
async function gridAddColumn(page, d, logs) {
  await page.click('#add-column');
  await waitForLog(page, (l) => l.includes('added=d'), 3000);
  await sleep(1000);
  const headers = await page.$$eval('#grid-cols .v-grid-header .v-grid-cell', (es) => es.map((e) => e.textContent.trim()));
  const errors = pageErrors(logs);
  d.push(`headers=${JSON.stringify(headers)}`, `errors=${JSON.stringify(errors)}`);
  return errors.length === 0 && JSON.stringify(headers) === JSON.stringify(['A', 'C', 'D']);
}

// I55: ResponsiveConnector with rules without selectorText.
async function responsive(page, d, logs) {
  await sleep(1000);
  const r = await page.evaluate(() => {
    const e = document.querySelector('#resp');
    const sheet = document.getElementById('f2c-i55');
    const types = sheet && sheet.sheet ? Array.from(sheet.sheet.cssRules).map((x) => `${x.type}:${x.constructor.name}:${x.selectorText === undefined ? 'no selectorText' : 'selectorText'}`) : null;
    return { widthRange: e ? e.getAttribute('width-range') : null, color: e ? getComputedStyle(e).color : null, types };
  });
  const errors = pageErrors(logs);
  d.push(JSON.stringify(r), `errors=${JSON.stringify(errors)}`);
  return r.widthRange === '0-300px' && r.color === 'rgb(255, 0, 0)' && errors.length === 0;
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch({
    executablePath: process.env.F2C_CHROMIUM,
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
    const RG = 'red on base, green on candidate';
    const GG = 'green on base and candidate';
    const NOREG = 'green on base and candidate (no correction: defect not reproduced on 7.7.17)';
    await scenario(browser, 'i05-panel', 'I05/M04', RG, null, 'panel', panelCaption);
    await scenario(browser, 'i02-keyboard', 'I02', GG, null, 'keyboard', keyboard);
    await scenario(browser, 'i03-autocomplete', 'I03', RG, null, 'combo', autocomplete);
    await scenario(browser, 'i44-popup-left', 'I44', NOREG, null, 'combo', popupLeft);
    await scenario(browser, 'i40-popup-positions', 'I40', RG, null, 'combo', popupPositions);
    await scenario(browser, 'i14-fast-tab', 'I14', RG, null, 'combo', fastTab);
    await scenario(browser, 'i42-popup-scroll', 'I42', NOREG, null, 'combo', popupScroll);
    await scenario(browser, 'i10-table-select', 'I10', RG, null, 'table', tableSelect);
    await scenario(browser, 'i46-grid-select-none', 'I46', RG, null, 'grid', gridSelect('grid-text-none'));
    await scenario(browser, 'i46-grid-select-single', 'I46', RG, null, 'grid', gridSelect('grid-text-single'));
    await scenario(browser, 'm03-grid-toggles', 'M03', RG, null, 'grid', gridToggles);
    await scenario(browser, 'i01-grid-sort', 'I01', NOREG, null, 'grid', gridSort);
    await scenario(browser, 'i15-grid-add-column', 'I15', RG, null, 'grid', gridAddColumn);
    await scenario(browser, 'i55-responsive', 'I55', RG, null, 'responsive', responsive);
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
