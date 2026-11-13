# Copyright 2026 LuxLabs. Licensed under the Apache License, Version 2.0.
#
# Evaluates the JSON written by test.js for the 7.7.17.1 base (expected red:
# the hostile captions run as HTML) and for the F2b candidate (expected
# green). usage: python3 -I evaluate.py <base.json> <candidate.json> <out.tsv>
# Exit code 0 only if the base is red for the expected reason and the
# candidate is green on every check.
import json
import sys

base = json.load(open(sys.argv[1]))
cand = json.load(open(sys.argv[2]))
rows = []


def check(name, expected, observed, ok):
    rows.append((name, expected, observed, 'ok' if ok else 'FAIL'))
    return ok


def ran(r):
    return not [e for e in r['errors'] if e.startswith('FATAL')]


def unsafe(c):
    return c.get('probes', 0) > 0 or c.get('xss', 0) > 0


ok = True
ok &= check('base: run completed', 'no fatal error', str(base['errors'])[:200], ran(base))
ok &= check('candidate: run completed', 'no fatal error', str(cand['errors'])[:200], ran(cand))
for r in (base, cand):
    ok &= check(r['label'] + ': no request outside 127.0.0.1:port', '[]', str(r['violations']), not r['violations'])

b, c = base.get('checks', {}), cand.get('checks', {})
# red on the base: the hostile caption becomes an element and runs
for key in ('table', 'tree'):
    ok &= check('base M05 %s context menu (red)' % key, 'img element created', json.dumps(b.get(key, {}))[:300], unsafe(b.get(key, {})))
ok &= check('base M03 grid hiding toggle (red)', 'img element created', json.dumps(b.get('grid', {}))[:300], unsafe(b.get('grid', {})))
# green on the candidate
for key in ('table', 'tree'):
    x = c.get(key, {})
    ok &= check('candidate M05 %s caption shown as text' % key, 'text contains "<img src=x", no element, no script',
                json.dumps(x)[:300], ('<img src=x' in x.get('text', '')) and not unsafe(x))
x = c.get('tableHtml', {})
ok &= check('candidate M05 setCaptionAsHtml(true) passes HTML', 'api, #api-bold in the menu',
            '%s %s' % (c.get('api'), x.get('apiBold')), c.get('api') == 'api' and x.get('apiBold', 0) >= 1)
x = c.get('shortcut', {})
ok &= check('candidate M05 shortcut works', 'fired=shortcut, no element', json.dumps(x), x.get('fired') == 'shortcut' and not unsafe(x))
x = c.get('grid', {})
ok &= check('candidate M03 grid toggles as text', 'no element, toggle "Name bold"',
            json.dumps({k: x.get(k) for k in ('toggles', 'probes', 'xss')})[:300],
            (not unsafe(x)) and 'Name bold' in x.get('toggles', ''))

with open(sys.argv[3], 'w') as out:
    out.write('check\texpected\tobserved\tresult\n')
    for row in rows:
        out.write('\t'.join(s.replace('\t', ' ').replace('\n', ' ') for s in row) + '\n')
for row in rows:
    print('%-55s %s' % (row[0], row[3]))
sys.exit(0 if ok else 1)
