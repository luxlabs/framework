// Copyright 2026 LuxLabs. Licensed under the Apache License, Version 2.0.
//
// F2b browser checks (Chromium only): M05 action captions (Table, Tree,
// shortcut), M03 Grid header captions in the column hiding toggles.
// usage: node test.js <playwright module dir> <port> <label> <output json>
// Only http://127.0.0.1:<port> is reachable: every other request is aborted
// and recorded as a violation.
'use strict';
const [, , pwDir, port, label, outFile] = process.argv;
const { chromium } = require(pwDir);
const fs = require('fs');
const origin = `http://127.0.0.1:${port}`;
const result = { label, origin, checks: {}, violations: [], errors: [] };

function save() { fs.writeFileSync(outFile, JSON.stringify(result, null, 2)); }

async function probe(page) {
  return page.evaluate(() => ({
    xss: window.__xss || 0,
    probes: document.querySelectorAll('img.xss-probe').length,
  }));
}

async function contextMenu(page, cellText) {
  const cell = page.getByText(cellText, { exact: true }).first();
  await cell.waitFor({ timeout: 30000 });
  await cell.click({ button: 'right' });
  const menu = page.locator('.v-contextmenu').first();
  await menu.waitFor({ timeout: 10000 });
  const text = await menu.innerText();
  const html = await menu.innerHTML();
  const p = await probe(page);
  const apiBold = await page.locator('.v-contextmenu #api-bold').count();
  await page.keyboard.press('Escape');
  await page.mouse.click(5, 5);
  return { text, html, apiBold, ...p };
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    args: [
      '--host-resolver-rules=MAP * ~NOTFOUND , EXCLUDE 127.0.0.1',
      '--no-proxy-server', '--disable-background-networking',
      '--disable-component-update', '--disable-sync', '--no-first-run',
      '--disable-domain-reliability', '--disable-default-apps',
      '--disable-client-side-phishing-detection', '--metrics-recording-only',
    ],
  });
  try {
    const context = await browser.newContext();
    await context.route('**/*', (route) => {
      const url = route.request().url();
      if (url.startsWith(origin + '/')) { return route.continue(); }
      if (url.startsWith('data:') || url.startsWith('about:')) { return route.continue(); }
      result.violations.push(url);
      return route.abort();
    });
    const page = await context.newPage();
    page.on('pageerror', (e) => result.errors.push(String(e)));

    // M05: Table, Tree, shortcut
    await page.goto(origin + '/actions/', { waitUntil: 'load' });
    await page.locator('#table').waitFor({ timeout: 30000 });
    result.checks.api = await page.locator('#api').innerText();
    result.checks.table = await contextMenu(page, 'row 1');
    result.checks.tableHtml = await contextMenu(page, 'row html');
    result.checks.tree = await contextMenu(page, 'node 1');
    await page.locator('#panel-field').click();
    await page.keyboard.press('F8');
    let fired = 'none';
    for (let i = 0; i < 50 && fired !== 'shortcut'; i++) {
      await page.waitForTimeout(100);
      fired = await page.locator('#fired').innerText();
    }
    result.checks.shortcut = { fired, ...(await probe(page)) };

    // M03: Grid hiding toggles
    const grid = await context.newPage();
    grid.on('pageerror', (e) => result.errors.push(String(e)));
    await grid.goto(origin + '/grid/', { waitUntil: 'load' });
    await grid.locator('.v-grid-sidebar-button').waitFor({ timeout: 30000 });
    const header = await grid.locator('.v-grid-header').innerText();
    await grid.locator('.v-grid-sidebar-button').click();
    const content = grid.locator('.v-grid-sidebar-content');
    await content.waitFor({ timeout: 10000 });
    result.checks.grid = {
      header,
      toggles: await content.innerText(),
      togglesHtml: await content.innerHTML(),
      ...(await probe(grid)),
    };
  } catch (e) {
    result.errors.push('FATAL ' + (e && e.stack || e));
  } finally {
    await browser.close();
    save();
  }
})();
